package ru.mjkey.vrphys.integration;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SableCompat {
    private SableCompat() {
    }

    public static Vec3 projectOut(Level level, Position position) {
        if (!OptionalMods.SABLE.isEnabled()) {
            return new Vec3(position.x(), position.y(), position.z());
        }

        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, position);
    }

    public static double distanceSquared(Level level, Position first, Position second) {
        if (!OptionalMods.SABLE.isEnabled()) {
            return new Vec3(first.x(), first.y(), first.z()).distanceToSqr(second.x(), second.y(), second.z());
        }

        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, first, second);
    }

    public static Vec3 velocityAt(Level level, Position position) {
        if (!OptionalMods.SABLE.isEnabled()) {
            return Vec3.ZERO;
        }

        return SableCompanion.INSTANCE.getVelocity(level, position);
    }

    public static boolean isInsidePlotGrid(Level level, BlockPos pos) {
        return OptionalMods.SABLE.isEnabled() && SableCompanion.INSTANCE.isInPlotGrid(level, pos);
    }

    public static SubLevelAccess containingSubLevel(Level level, Position position) {
        if (!OptionalMods.SABLE.isEnabled()) {
            return null;
        }

        return SableCompanion.INSTANCE.getContaining(level, position);
    }
}
