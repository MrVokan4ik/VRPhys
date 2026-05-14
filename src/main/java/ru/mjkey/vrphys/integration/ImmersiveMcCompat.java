package ru.mjkey.vrphys.integration;

public final class ImmersiveMcCompat {
    private ImmersiveMcCompat() {
    }

    public static boolean isAvailable() {
        return OptionalMods.IMMERSIVE_MC.isEnabled() && OptionalMods.IMMERSIVE_MC.isLoaded();
    }
}
