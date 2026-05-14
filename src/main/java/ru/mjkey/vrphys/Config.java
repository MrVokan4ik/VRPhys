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

    static final ModConfigSpec SPEC = BUILDER.build();
}
