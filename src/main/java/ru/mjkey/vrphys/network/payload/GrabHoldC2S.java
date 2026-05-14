package ru.mjkey.vrphys.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import ru.mjkey.vrphys.VRPhys;

public record GrabHoldC2S(InteractionHand hand, Vec3 handPos, Vec3 handDir, int sequence) implements CustomPacketPayload {
    public static final Type<GrabHoldC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VRPhys.MODID, "grab_hold")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GrabHoldC2S> CODEC =
            CustomPacketPayload.codec(GrabHoldC2S::write, GrabHoldC2S::read);

    private static GrabHoldC2S read(RegistryFriendlyByteBuf buffer) {
        GrabPayloadCodecs.HandPose pose = GrabPayloadCodecs.readHandPose(buffer);
        return new GrabHoldC2S(pose.hand(), pose.handPos(), pose.handDir(), buffer.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GrabPayloadCodecs.writeHandPose(buffer, hand, handPos, handDir);
        buffer.writeVarInt(sequence);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
