package ru.mjkey.vrphys.physics;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import ru.mjkey.vrphys.Config;
import ru.mjkey.vrphys.VRPhys;
import ru.mjkey.vrphys.integration.SableCompat;
import ru.mjkey.vrphys.integration.VivecraftCompat;
import ru.mjkey.vrphys.network.VRPhysNetwork;
import ru.mjkey.vrphys.network.payload.GrabAckS2C;
import ru.mjkey.vrphys.network.payload.GrabHoldC2S;
import ru.mjkey.vrphys.network.payload.GrabReleaseC2S;
import ru.mjkey.vrphys.network.payload.GrabStartC2S;
import ru.mjkey.vrphys.network.payload.GrabStressS2C;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class VRGrabServer {
    private static final double MAX_HAND_DISTANCE_FROM_PLAYER = 8.0D;
    private static final double MAX_HAND_DISTANCE_FROM_PLAYER_SQUARED =
            MAX_HAND_DISTANCE_FROM_PLAYER * MAX_HAND_DISTANCE_FROM_PLAYER;
    private static final Map<GrabKey, GrabSession> SESSIONS = new ConcurrentHashMap<>();

    // VR-status cache: avoid reflection on every physics tick.
    private static final AtomicLong VR_CACHE_TICK = new AtomicLong(Long.MIN_VALUE);
    private static final Map<UUID, Boolean> VR_PLAYER_CACHE = new ConcurrentHashMap<>();

    private VRGrabServer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(VRGrabServer.class);
        SableEventPlatform.INSTANCE.onPhysicsTick(VRGrabServer::onPhysicsTick);
    }

    public static void handleStart(ServerPlayer player, GrabStartC2S packet) {
        GrabKey key = new GrabKey(player.getUUID(), packet.hand());
        SESSIONS.remove(key);

        ServerSubLevel target = findServerTarget(player, packet.handPos());
        if (target == null) {
            acknowledge(player, packet.hand(), false, null);
            debug("Rejected grab start from {}", player);
            return;
        }

        Vector3d localAnchor = target.logicalPose().transformPositionInverse(toJoml(packet.handPos()), new Vector3d());
        GrabSession session = new GrabSession(
                player.getUUID(),
                packet.hand(),
                player.level().dimension(),
                target.getUniqueId(),
                localAnchor,
                player.serverLevel().getGameTime(),
                packet.handPos(),
                packet.handDir(),
                packet.handRot()
        );

        SESSIONS.put(key, session);
        acknowledge(player, packet.hand(), true, target.getUniqueId());
        debug("Accepted grab start from {} on {}", player, target.getUniqueId());
    }

    public static void handleHold(ServerPlayer player, GrabHoldC2S packet) {
        GrabSession session = SESSIONS.get(new GrabKey(player.getUUID(), packet.hand()));
        if (session == null || packet.sequence() <= session.sequence) {
            return;
        }

        if (!isValidPlayerPose(player, packet.handPos(), packet.handDir())) {
            clear(player.getUUID(), packet.hand());
            acknowledge(player, packet.hand(), false, null);
            return;
        }

        session.updatePose(packet.handPos(), packet.handDir(), packet.handRot(),
                player.serverLevel().getGameTime(), packet.sequence());
    }

    public static void handleRelease(ServerPlayer player, GrabReleaseC2S packet) {
        GrabSession session = SESSIONS.remove(new GrabKey(player.getUUID(), packet.hand()));
        if (session == null) {
            acknowledge(player, packet.hand(), false, null);
            return;
        }

        if (isFinite(packet.handPos()) && isFinite(packet.handDir())) {
            session.updatePose(packet.handPos(), packet.handDir(), packet.handRot(),
                    player.serverLevel().getGameTime(), session.sequence + 1);
        }

        applyThrow(player.serverLevel(), session);
        acknowledge(player, packet.hand(), false, session.subLevelId);
        debug("Released grab from {} on {}", player, session.subLevelId);
    }

    public static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        ServerLevel level = physicsSystem.getLevel();
        long gameTick = level.getGameTime();

        for (Map.Entry<GrabKey, GrabSession> entry : SESSIONS.entrySet()) {
            GrabSession session = entry.getValue();
            if (!session.levelKey.equals(level.dimension())) {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.playerId);
            ServerSubLevel subLevel = resolveSubLevel(level, session.subLevelId);
            if (player == null || subLevel == null || !isSessionStillValid(player, session)) {
                SESSIONS.remove(entry.getKey(), session);
                continue;
            }

            float stress = applySpring(physicsSystem, subLevel, session, timeStep);

            // Dispatch haptic stress packet every 2 game ticks to limit network traffic.
            if (Config.ENABLE_STRESS_HAPTIC.getAsBoolean()
                    && gameTick - session.lastStressGameTick >= 2L) {
                session.lastStressGameTick = gameTick;
                VRPhysNetwork.sendStress(player, new GrabStressS2C(session.hand, stress));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            clearPlayer(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SESSIONS.clear();
        VR_PLAYER_CACHE.clear();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static ServerSubLevel findServerTarget(ServerPlayer player, Vec3 handPos) {
        if (!isValidPlayerPose(player, handPos, Vec3.ZERO)) {
            return null;
        }

        SubLevelAccess access = SableCompat.findNearestSubLevel(player.level(), handPos, Config.GRAB_RADIUS.getAsDouble());
        if (access instanceof ServerSubLevel subLevel && !subLevel.isRemoved()) {
            return subLevel;
        }
        return null;
    }

    private static boolean isSessionStillValid(ServerPlayer player, GrabSession session) {
        return player.isAlive()
                && player.level().dimension().equals(session.levelKey)
                && isVrPlayerThisTick(player)
                && isFinite(session.handPos)
                && player.position().distanceToSqr(session.handPos) <= MAX_HAND_DISTANCE_FROM_PLAYER_SQUARED;
    }

    private static boolean isValidPlayerPose(ServerPlayer player, Vec3 handPos, Vec3 handDir) {
        return isVrPlayerThisTick(player)
                && isFinite(handPos)
                && isFinite(handDir)
                && player.position().distanceToSqr(handPos) <= MAX_HAND_DISTANCE_FROM_PLAYER_SQUARED;
    }

    /**
     * Returns the VR status of the player, cached for the current game tick.
     * Avoids a reflective Vivecraft API call on every physics tick per active session.
     */
    private static boolean isVrPlayerThisTick(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        if (VR_CACHE_TICK.getAndSet(now) != now) {
            VR_PLAYER_CACHE.clear();
        }
        return VR_PLAYER_CACHE.computeIfAbsent(player.getUUID(), id -> VivecraftCompat.isVrPlayer(player));
    }

    /**
     * Applies linear spring-damper and angular spring-damper forces to the sub-level.
     *
     * @return normalized spring stress in [0, 1], where 1 means max linear force was reached.
     */
    private static float applySpring(SubLevelPhysicsSystem physicsSystem, ServerSubLevel subLevel,
                                     GrabSession session, double timeStep) {
        RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
        if (handle == null || !handle.isValid()) {
            return 0.0f;
        }

        // --- Mass-based effective stiffness ---
        double mass = subLevel.getMassTracker().getMass();
        double massThreshold = Config.MASS_THRESHOLD.getAsDouble();
        double effectiveStiffness = Config.SPRING_STIFFNESS.getAsDouble()
                / Math.max(1.0, mass / massThreshold);

        // --- Linear spring-damper ---
        Vector3d anchorGlobal = subLevel.logicalPose().transformPosition(session.localAnchor, new Vector3d());
        Vector3d handPosition = toJoml(session.handPos);
        Vector3d error = handPosition.sub(anchorGlobal, new Vector3d());

        Vector3d linearVelocity = handle.getLinearVelocity(new Vector3d());
        Vector3d angularVelocity = handle.getAngularVelocity(new Vector3d());
        Vector3d radius = anchorGlobal.sub(subLevel.logicalPose().position(), new Vector3d());
        Vector3d pointVelocity = angularVelocity.cross(radius, new Vector3d()).add(linearVelocity);
        Vector3d velocityError = session.estimatedVelocity().sub(pointVelocity, new Vector3d());

        Vector3d force = error.mul(effectiveStiffness, new Vector3d())
                .fma(Config.SPRING_DAMPING.getAsDouble(), velocityError);

        double maxForce = Config.MAX_FORCE_PER_HAND.getAsDouble();
        clamp(force, maxForce);

        float stress = 0.0f;
        if (isFinite(force)) {
            Vector3d globalImpulse = force.mul(timeStep, new Vector3d());
            Vector3d localImpulse = subLevel.logicalPose().transformNormalInverse(globalImpulse, new Vector3d());
            handle.applyImpulseAtPoint(session.localAnchor, localImpulse);
            stress = (float) Math.min(1.0, force.length() / Math.max(1.0, maxForce));
        }

        // --- Angular spring-damper (wrist rotation → sub-level orientation) ---
        if (isFiniteQuat(session.handRot) && Config.ANGULAR_STIFFNESS.getAsDouble() > 0.0) {
            applyAngularSpring(handle, subLevel, session, angularVelocity, timeStep);
        }

        return stress;
    }

    private static void applyAngularSpring(RigidBodyHandle handle, ServerSubLevel subLevel,
                                           GrabSession session, Vector3d currentAngularVelocity,
                                           double timeStep) {
        // q_error = q_target * q_current^-1 — rotation needed to reach hand orientation.
        Quaterniond currentQ = new Quaterniond(subLevel.logicalPose().orientation());
        Quaterniond targetQ = new Quaterniond(
                session.handRot.x, session.handRot.y, session.handRot.z, session.handRot.w);
        Quaterniond errorQ = targetQ.mul(currentQ.conjugate(new Quaterniond()), new Quaterniond()).normalize();

        // Ensure shortest-arc (angle ≤ π).
        if (errorQ.w < 0.0) {
            errorQ.x = -errorQ.x;
            errorQ.y = -errorQ.y;
            errorQ.z = -errorQ.z;
            errorQ.w = -errorQ.w;
        }

        // Convert to axis-angle vector: angularError = axis * angle (world space).
        double w = Math.min(1.0, errorQ.w);
        double halfAngle = Math.acos(w);
        double sinHalfAngle = Math.sin(halfAngle);
        if (sinHalfAngle < 1e-9) {
            return; // Already aligned.
        }
        double angle = 2.0 * halfAngle;
        Vector3d angularError = new Vector3d(
                errorQ.x / sinHalfAngle * angle,
                errorQ.y / sinHalfAngle * angle,
                errorQ.z / sinHalfAngle * angle
        );

        // Spring-damper torque in world space.
        Vector3d torque = angularError.mul(Config.ANGULAR_STIFFNESS.getAsDouble(), new Vector3d())
                .fma(-Config.ANGULAR_DAMPING.getAsDouble(), currentAngularVelocity);
        clamp(torque, Config.MAX_TORQUE_PER_HAND.getAsDouble());

        if (!isFinite(torque)) {
            return;
        }

        // Transform to body-local space and apply.
        Vector3d localTorque = subLevel.logicalPose().transformNormalInverse(torque, new Vector3d()).mul(timeStep);
        handle.applyAngularImpulse(localTorque);
    }

    private static void applyThrow(ServerLevel level, GrabSession session) {
        ServerSubLevel subLevel = resolveSubLevel(level, session.subLevelId);
        if (subLevel == null) {
            return;
        }

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) {
            return;
        }

        Vector3d velocity = session.estimatedVelocity()
                .mul(Config.THROW_VELOCITY_SCALE.getAsDouble(), new Vector3d());
        clamp(velocity, Config.MAX_THROW_VELOCITY.getAsDouble());

        if (isFinite(velocity)) {
            handle.addLinearAndAngularVelocity(velocity, new Vector3d());
        }
    }

    private static ServerSubLevel resolveSubLevel(ServerLevel level, UUID subLevelId) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }

        SubLevel subLevel = container.getSubLevel(subLevelId);
        if (subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
            return serverSubLevel;
        }
        return null;
    }

    private static void acknowledge(ServerPlayer player, InteractionHand hand, boolean accepted, UUID subLevelId) {
        VRPhysNetwork.sendAck(player, new GrabAckS2C(hand, accepted, subLevelId));
    }

    private static void clear(UUID playerId, InteractionHand hand) {
        SESSIONS.remove(new GrabKey(playerId, hand));
    }

    private static void clearPlayer(UUID playerId) {
        SESSIONS.keySet().removeIf(key -> key.playerId.equals(playerId));
    }

    private static Vector3d toJoml(Vec3 vec) {
        return new Vector3d(vec.x, vec.y, vec.z);
    }

    private static boolean isFinite(Vec3 vec) {
        return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
    }

    private static boolean isFinite(Vector3d vec) {
        return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
    }

    private static boolean isFiniteQuat(Quaternionf q) {
        return Float.isFinite(q.x) && Float.isFinite(q.y) && Float.isFinite(q.z) && Float.isFinite(q.w)
                && (q.x != 0.0f || q.y != 0.0f || q.z != 0.0f || q.w != 0.0f);
    }

    private static void clamp(Vector3d vec, double maxLength) {
        if (maxLength <= 0.0D) {
            vec.zero();
            return;
        }

        double lengthSquared = vec.lengthSquared();
        double maxLengthSquared = maxLength * maxLength;
        if (lengthSquared > maxLengthSquared) {
            vec.mul(maxLength / Math.sqrt(lengthSquared));
        }
    }

    private static void debug(String message, Object... args) {
        if (Config.DEBUG_GRAB_LOGGING.getAsBoolean()) {
            VRPhys.LOGGER.debug(message, args);
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private record GrabKey(UUID playerId, InteractionHand hand) {
    }

    private static final class GrabSession {
        private final UUID playerId;
        private final InteractionHand hand;
        private final ResourceKey<Level> levelKey;
        private final UUID subLevelId;
        private final Vector3d localAnchor;

        private Vec3 previousHandPos;
        private long previousTick;
        private Vec3 handPos;
        private Vec3 handDir;
        /** Current target orientation of the VR controller for Angular Pull. */
        private Quaternionf handRot;
        private long lastTick;
        private int sequence;
        /** Last game tick on which a GrabStressS2C packet was sent. */
        long lastStressGameTick = Long.MIN_VALUE;

        private GrabSession(
                UUID playerId,
                InteractionHand hand,
                ResourceKey<Level> levelKey,
                UUID subLevelId,
                Vector3d localAnchor,
                long tick,
                Vec3 handPos,
                Vec3 handDir,
                Quaternionf handRot
        ) {
            this.playerId = playerId;
            this.hand = hand;
            this.levelKey = levelKey;
            this.subLevelId = subLevelId;
            this.localAnchor = localAnchor;
            this.previousHandPos = handPos;
            this.previousTick = tick;
            this.handPos = handPos;
            this.handDir = handDir;
            this.handRot = handRot != null ? new Quaternionf(handRot) : new Quaternionf();
            this.lastTick = tick;
        }

        private void updatePose(Vec3 handPos, Vec3 handDir, Quaternionf handRot, long tick, int sequence) {
            this.previousHandPos = this.handPos;
            this.previousTick = this.lastTick;
            this.handPos = handPos;
            this.handDir = handDir;
            if (handRot != null) {
                this.handRot.set(handRot);
            }
            this.lastTick = tick;
            this.sequence = sequence;
        }

        private Vector3d estimatedVelocity() {
            double seconds = Math.max(0.05D, (this.lastTick - this.previousTick) / 20.0D);
            return new Vector3d(
                    (this.handPos.x - this.previousHandPos.x) / seconds,
                    (this.handPos.y - this.previousHandPos.y) / seconds,
                    (this.handPos.z - this.previousHandPos.z) / seconds
            );
        }
    }
}
