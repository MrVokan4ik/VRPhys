package ru.mjkey.vrphys;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import ru.mjkey.vrphys.integration.OptionalMods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = VRPhys.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = VRPhys.MODID, value = Dist.CLIENT)
public class VRPhysClient {
    public VRPhysClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(VRPhysClient::registerOptionalClientHooks);
        VRPhys.LOGGER.info("VRPhys client hooks are ready");
    }

    private static void registerOptionalClientHooks() {
        if (!OptionalMods.VIVECRAFT.isLoaded()) {
            VRPhys.LOGGER.info("Vivecraft is not loaded; VRPhys grab module is disabled on this client");
            return;
        }

        try {
            Class<?> bridge = Class.forName("ru.mjkey.vrphys.client.VivecraftGrabClient");
            Method register = bridge.getMethod("register");
            register.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            VRPhys.LOGGER.warn("Failed to register VRPhys Vivecraft client bridge", exception);
        } catch (LinkageError error) {
            VRPhys.LOGGER.warn("Vivecraft is loaded, but the VRPhys client bridge could not link against its API", error);
        }
    }
}
