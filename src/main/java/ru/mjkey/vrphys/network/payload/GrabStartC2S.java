package ru.mjkey.vrphys.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import ru.mjkey.vrphys.VRPhys;

public record GrabStartC2S(InteractionHand hand, Vec3 handPos, Vec3 handDir, Quaternionf handRot) implements CustomPacketPayload {
    public static final Type<GrabStartC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VRPhys.MODID, "grab_start")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GrabStartC2S> CODEC =
            CustomPacketPayload.codec(GrabStartC2S::write, GrabStartC2S::read);

    private static GrabStartC2S read(RegistryFriendlyByteBuf buffer) {
        GrabPayloadCodecs.HandPose pose = GrabPayloadCodecs.readHandPose(buffer);
        return new GrabStartC2S(pose.hand(), pose.handPos(), pose.handDir(), pose.handRot());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GrabPayloadCodecs.writeHandPose(buffer, hand, handPos, handDir, handRot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
