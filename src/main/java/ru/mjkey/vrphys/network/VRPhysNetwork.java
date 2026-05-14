package ru.mjkey.vrphys.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ru.mjkey.vrphys.VRPhys;
import ru.mjkey.vrphys.network.payload.GrabAckS2C;
import ru.mjkey.vrphys.network.payload.GrabHoldC2S;
import ru.mjkey.vrphys.network.payload.GrabReleaseC2S;
import ru.mjkey.vrphys.network.payload.GrabStartC2S;
import ru.mjkey.vrphys.physics.VRGrabServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class VRPhysNetwork {
    private static final String CLIENT_BRIDGE_CLASS = "ru.mjkey.vrphys.client.VivecraftGrabClient";

    private VRPhysNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VRPhys.MODID).optional();

        registrar.playBidirectional(GrabStartC2S.TYPE, GrabStartC2S.CODEC,
                new DirectionalPayloadHandler<>(
                        (packet, context) -> {
                        },
                        (packet, context) -> context.enqueueWork(() -> VRGrabServer.handleStart((ServerPlayer) context.player(), packet))
                ));
        registrar.playBidirectional(GrabHoldC2S.TYPE, GrabHoldC2S.CODEC,
                new DirectionalPayloadHandler<>(
                        (packet, context) -> {
                        },
                        (packet, context) -> context.enqueueWork(() -> VRGrabServer.handleHold((ServerPlayer) context.player(), packet))
                ));
        registrar.playBidirectional(GrabReleaseC2S.TYPE, GrabReleaseC2S.CODEC,
                new DirectionalPayloadHandler<>(
                        (packet, context) -> {
                        },
                        (packet, context) -> context.enqueueWork(() -> VRGrabServer.handleRelease((ServerPlayer) context.player(), packet))
                ));
        registrar.playBidirectional(GrabAckS2C.TYPE, GrabAckS2C.CODEC,
                new DirectionalPayloadHandler<>(
                        (packet, context) -> context.enqueueWork(() -> handleAck(packet)),
                        (packet, context) -> {
                        }
                ));
    }

    public static void sendStart(GrabStartC2S packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendHold(GrabHoldC2S packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendRelease(GrabReleaseC2S packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendAck(ServerPlayer player, GrabAckS2C packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    private static void handleAck(GrabAckS2C packet) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        try {
            Class<?> bridge = Class.forName(CLIENT_BRIDGE_CLASS);
            Method method = bridge.getMethod("handleAck", GrabAckS2C.class);
            method.invoke(null, packet);
        } catch (ClassNotFoundException exception) {
            VRPhys.LOGGER.debug("Vivecraft client grab bridge is not present");
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            VRPhys.LOGGER.warn("Failed to deliver VRPhys grab acknowledgement to client bridge", exception);
        } catch (LinkageError error) {
            VRPhys.LOGGER.warn("VRPhys grab acknowledgement bridge could not link against Vivecraft", error);
        }
    }
}
