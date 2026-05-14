package ru.mjkey.vrphys;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import ru.mjkey.vrphys.integration.IntegrationStatus;
import ru.mjkey.vrphys.network.VRPhysNetwork;
import ru.mjkey.vrphys.physics.VRGrabServer;

@Mod(VRPhys.MODID)
public class VRPhys {
    public static final String MODID = "vrphys";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VRPhys(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(VRPhysNetwork::register);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        VRGrabServer.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(IntegrationStatus::logCurrent);
    }
}
