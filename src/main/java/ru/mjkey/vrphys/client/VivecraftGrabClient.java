package ru.mjkey.vrphys.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.vivecraft.api.client.HeldInteractModule;
import org.vivecraft.api.client.VRClientAPI;
import org.vivecraft.api.data.VRBodyPart;
import org.vivecraft.api.data.VRBodyPartData;
import org.vivecraft.api.data.VRPose;
import ru.mjkey.vrphys.Config;
import ru.mjkey.vrphys.VRPhys;
import ru.mjkey.vrphys.integration.SableCompat;
import ru.mjkey.vrphys.network.VRPhysNetwork;
import ru.mjkey.vrphys.network.payload.GrabAckS2C;
import ru.mjkey.vrphys.network.payload.GrabHoldC2S;
import ru.mjkey.vrphys.network.payload.GrabReleaseC2S;
import ru.mjkey.vrphys.network.payload.GrabStartC2S;
import ru.mjkey.vrphys.network.payload.GrabStressS2C;
import org.joml.Quaternionf;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class VivecraftGrabClient {
    private static final VRGrabInteractModule MODULE = new VRGrabInteractModule();

    private VivecraftGrabClient() {
    }

    public static void register() {
        VRClientAPI.instance().addClientRegistrationHandler(event -> event.registerInteractModules(MODULE));
        VRPhys.LOGGER.info("Registered VRPhys Vivecraft grab module");
    }

    public static void handleAck(GrabAckS2C packet) {
        float duration = packet.accepted() ? 0.08F : 0.025F;
        float amplitude = packet.accepted() ? 0.75F : 0.25F;
        VRClientAPI.instance().triggerHapticPulse(
                VRBodyPart.fromInteractionHand(packet.hand()),
                duration,
                160.0F,
                amplitude,
                0.0F
        );
        MODULE.setAccepted(packet.hand(), packet.accepted());
    }

    public static void handleStress(GrabStressS2C packet) {
        if (packet.stressNormalized() > 0.01F) {
            VRClientAPI.instance().triggerHapticPulse(
                    VRBodyPart.fromInteractionHand(packet.hand()),
                    0.1F, // continuous pulse for 2 ticks
                    160.0F,
                    packet.stressNormalized(),
                    0.0F
            );
        }
    }

    private static final class VRGrabInteractModule implements HeldInteractModule {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(VRPhys.MODID, "sable_grab");
        private final Map<InteractionHand, Integer> sequences = new EnumMap<>(InteractionHand.class);
        private final Set<InteractionHand> rejected = EnumSet.noneOf(InteractionHand.class);

        @Override
        public int getPriority() {
            return 250;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public boolean isActive(LocalPlayer player, InteractionHand hand, Vec3 handPosition) {
            return VRClientAPI.instance().isVRActive()
                    && SableCompat.findNearestSubLevel(player.level(), handPosition, Config.GRAB_RADIUS.getAsDouble()) != null;
        }

        @Override
        public boolean onPress(LocalPlayer player, InteractionHand hand) {
            HandPose pose = readHandPose(hand, player.position());
            sequences.put(hand, 0);
            rejected.remove(hand);
            VRPhysNetwork.sendStart(new GrabStartC2S(hand, pose.position(), pose.direction(), pose.rotation()));
            return true;
        }

        @Override
        public boolean onHoldTick(LocalPlayer player, InteractionHand hand) {
            if (!VRClientAPI.instance().isVRActive()) {
                return false;
            }
            if (rejected.remove(hand)) {
                return false;
            }

            HandPose pose = readHandPose(hand, player.position());
            int sequence = sequences.merge(hand, 1, Integer::sum);
            VRPhysNetwork.sendHold(new GrabHoldC2S(hand, pose.position(), pose.direction(), pose.rotation(), sequence));
            return true;
        }

        @Override
        public void onRelease(LocalPlayer player, InteractionHand hand) {
            Vec3 fallback = player == null ? Vec3.ZERO : player.position();
            HandPose pose = readHandPose(hand, fallback);
            VRPhysNetwork.sendRelease(new GrabReleaseC2S(hand, pose.position(), pose.direction(), pose.rotation()));
            sequences.remove(hand);
            rejected.remove(hand);
        }

        @Override
        public boolean swingsArm() {
            return false;
        }

        private void setAccepted(InteractionHand hand, boolean value) {
            if (!value && sequences.containsKey(hand)) {
                rejected.add(hand);
            }
        }
    }

    private static HandPose readHandPose(InteractionHand hand, Vec3 fallbackPosition) {
        VRPose pose = VRClientAPI.instance().getPreTickWorldPose();
        if (pose == null) {
            return new HandPose(fallbackPosition, Vec3.ZERO, new Quaternionf());
        }

        VRBodyPartData handData = pose.getHand(hand);
        if (handData == null) {
            return new HandPose(fallbackPosition, Vec3.ZERO, new Quaternionf());
        }

        Quaternionf rot = new Quaternionf(handData.getRotation());
        return new HandPose(handData.getPos(), handData.getDir(), rot);
    }

    private record HandPose(Vec3 position, Vec3 direction, Quaternionf rotation) {
    }
}
