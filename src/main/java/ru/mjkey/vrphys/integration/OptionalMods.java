package ru.mjkey.vrphys.integration;

import net.neoforged.fml.ModList;
import ru.mjkey.vrphys.Config;

public enum OptionalMods {
    SABLE("sable"),
    VIVECRAFT("vivecraft"),
    IMMERSIVE_MC("immersivemc");

    private final String modId;

    OptionalMods(String modId) {
        this.modId = modId;
    }

    public String modId() {
        return modId;
    }

    public boolean isLoaded() {
        return ModList.get().isLoaded(modId);
    }

    public boolean isEnabled() {
        return switch (this) {
            case SABLE -> Config.ENABLE_SABLE_COMPAT.getAsBoolean();
            case VIVECRAFT -> Config.ENABLE_VIVECRAFT_COMPAT.getAsBoolean();
            case IMMERSIVE_MC -> Config.ENABLE_IMMERSIVE_MC_COMPAT.getAsBoolean();
        };
    }
}
