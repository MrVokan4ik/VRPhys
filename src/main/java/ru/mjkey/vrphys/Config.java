package ru.mjkey.vrphys;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_SABLE_COMPAT = BUILDER
            .comment("Use Sable Companion helpers for positions, distance checks, and sub-level velocities.")
            .define("enableSableCompat", true);

    public static final ModConfigSpec.BooleanValue ENABLE_VIVECRAFT_COMPAT = BUILDER
            .comment("Detect Vivecraft VR players through the optional Vivecraft API when it is installed.")
            .define("enableVivecraftCompat", true);

    public static final ModConfigSpec.BooleanValue ENABLE_IMMERSIVE_MC_COMPAT = BUILDER
            .comment("Reserve ImmersiveMC integration hooks when ImmersiveMC is installed.")
            .define("enableImmersiveMcCompat", true);

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Write extra integration diagnostics to the log.")
            .define("debugLogging", false);

    public static final ModConfigSpec.DoubleValue GRAB_RADIUS = BUILDER
            .comment("Hand sphere radius in blocks for selecting a Sable sub-level.")
            .defineInRange("grabRadius", 0.35D, 0.05D, 3.0D);

    public static final ModConfigSpec.DoubleValue SPRING_STIFFNESS = BUILDER
            .comment("Spring stiffness used to pull the grabbed anchor toward the VR hand.")
            .defineInRange("springStiffness", 1800.0D, 0.0D, 100000.0D);

    public static final ModConfigSpec.DoubleValue SPRING_DAMPING = BUILDER
            .comment("Velocity damping used by the grab spring.")
            .defineInRange("springDamping", 90.0D, 0.0D, 10000.0D);

    public static final ModConfigSpec.DoubleValue MAX_FORCE_PER_HAND = BUILDER
            .comment("Maximum spring force per hand before it is converted to a physics impulse.")
            .defineInRange("maxForcePerHand", 4500.0D, 0.0D, 1000000.0D);

    public static final ModConfigSpec.DoubleValue THROW_VELOCITY_SCALE = BUILDER
            .comment("Multiplier for hand velocity when releasing a grabbed sub-level.")
            .defineInRange("throwVelocityScale", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue MAX_THROW_VELOCITY = BUILDER
            .comment("Maximum release velocity in blocks per second.")
            .defineInRange("maxThrowVelocity", 18.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue DEBUG_GRAB_LOGGING = BUILDER
            .comment("Write extra diagnostics for VR grab accept/reject/release decisions.")
            .define("debugGrabLogging", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
