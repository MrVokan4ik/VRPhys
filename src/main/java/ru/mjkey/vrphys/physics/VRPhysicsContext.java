package ru.mjkey.vrphys.physics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.mjkey.vrphys.integration.SableCompat;
import ru.mjkey.vrphys.integration.VivecraftCompat;

public record VRPhysicsContext(
        Player player,
        Level level,
        boolean vrPlayer,
        Vec3 worldPosition,
        Vec3 subLevelVelocity,
        Object vivecraftPose
) {
    public static VRPhysicsContext forPlayer(Player player) {
        Level level = player.level();
        Vec3 worldPosition = SableCompat.projectOut(level, player.position());

        return new VRPhysicsContext(
                player,
                level,
                VivecraftCompat.isVrPlayer(player),
                worldPosition,
                SableCompat.velocityAt(level, player.position()),
                VivecraftCompat.getVrPose(player)
        );
    }
}
