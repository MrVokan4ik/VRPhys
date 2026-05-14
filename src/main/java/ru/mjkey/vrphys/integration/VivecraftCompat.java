package ru.mjkey.vrphys.integration;

import net.minecraft.world.entity.player.Player;
import ru.mjkey.vrphys.Config;
import ru.mjkey.vrphys.VRPhys;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class VivecraftCompat {
    private static final String VR_API_CLASS = "org.vivecraft.api.VRAPI";

    private static Object apiInstance;
    private static Method isVrPlayerMethod;
    private static Method getVrPoseMethod;
    private static boolean attemptedLoad;

    private VivecraftCompat() {
    }

    public static boolean isAvailable() {
        return OptionalMods.VIVECRAFT.isEnabled() && OptionalMods.VIVECRAFT.isLoaded() && loadApi();
    }

    public static boolean isVrPlayer(Player player) {
        if (!isAvailable() || isVrPlayerMethod == null || apiInstance == null) {
            return false;
        }

        Object result = invoke(isVrPlayerMethod, player);
        return result instanceof Boolean vrPlayer && vrPlayer;
    }

    public static Object getVrPose(Player player) {
        if (!isAvailable() || getVrPoseMethod == null || apiInstance == null) {
            return null;
        }

        return invoke(getVrPoseMethod, player);
    }

    private static boolean loadApi() {
        if (attemptedLoad) {
            return apiInstance != null;
        }

        attemptedLoad = true;

        try {
            Class<?> apiClass = Class.forName(VR_API_CLASS);
            Method instanceMethod = apiClass.getMethod("instance");
            apiInstance = instanceMethod.invoke(null);
            isVrPlayerMethod = apiClass.getMethod("isVRPlayer", Player.class);
            getVrPoseMethod = apiClass.getMethod("getVRPose", Player.class);
            return true;
        } catch (ClassNotFoundException exception) {
            debug("Vivecraft API class was not found");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            VRPhys.LOGGER.warn("Failed to initialize Vivecraft API bridge", exception);
        } catch (LinkageError error) {
            VRPhys.LOGGER.warn("Vivecraft API binary is incompatible with this version of VRPhys — integration disabled", error);
        }

        apiInstance = null;
        isVrPlayerMethod = null;
        getVrPoseMethod = null;
        return false;
    }

    private static Object invoke(Method method, Player player) {
        try {
            return method.invoke(apiInstance, player);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            VRPhys.LOGGER.warn("Vivecraft API call failed for {}", player.getGameProfile().getName(), exception);
            return null;
        } catch (LinkageError error) {
            VRPhys.LOGGER.warn("Vivecraft API linkage failure — disabling integration", error);
            apiInstance = null;
            isVrPlayerMethod = null;
            getVrPoseMethod = null;
            return null;
        }
    }

    private static void debug(String message) {
        if (Config.DEBUG_LOGGING.getAsBoolean()) {
            VRPhys.LOGGER.debug(message);
        }
    }
}
