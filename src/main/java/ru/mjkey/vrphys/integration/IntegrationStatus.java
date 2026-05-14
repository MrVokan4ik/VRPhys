package ru.mjkey.vrphys.integration;

import ru.mjkey.vrphys.VRPhys;

public final class IntegrationStatus {
    private IntegrationStatus() {
    }

    public static void logCurrent() {
        for (OptionalMods mod : OptionalMods.values()) {
            VRPhys.LOGGER.info(
                    "{} integration: config={}, loaded={}",
                    mod.modId(),
                    mod.isEnabled() ? "enabled" : "disabled",
                    mod.isLoaded()
            );
        }
    }
}
