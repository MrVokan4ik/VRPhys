package ru.mjkey.vrphys.integration;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
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

    public static SubLevelAccess findNearestSubLevel(Level level, Position center, double radius) {
        if (!OptionalMods.SABLE.isEnabled()) {
            return null;
        }

        BoundingBox3d query = new BoundingBox3d(
                center.x() - radius,
                center.y() - radius,
                center.z() - radius,
                center.x() + radius,
                center.y() + radius,
                center.z() + radius
        );

        double radiusSquared = radius * radius;
        double bestDistance = Double.POSITIVE_INFINITY;
        SubLevelAccess best = null;

        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, query)) {
            double distance = distanceSquaredToBox(center, subLevel.boundingBox());
            if (distance <= radiusSquared && distance < bestDistance) {
                bestDistance = distance;
                best = subLevel;
            }
        }

        return best;
    }

    private static double distanceSquaredToBox(Position position, BoundingBox3dc box) {
        double dx = outsideDistance(position.x(), box.minX(), box.maxX());
        double dy = outsideDistance(position.y(), box.minY(), box.maxY());
        double dz = outsideDistance(position.z(), box.minZ(), box.maxZ());
        return dx * dx + dy * dy + dz * dz;
    }

    private static double outsideDistance(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0D;
    }
}
