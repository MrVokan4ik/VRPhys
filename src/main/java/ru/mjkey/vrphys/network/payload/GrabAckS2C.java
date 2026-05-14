package ru.mjkey.vrphys.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import ru.mjkey.vrphys.VRPhys;

import java.util.UUID;

public record GrabAckS2C(InteractionHand hand, boolean accepted, UUID subLevelId) implements CustomPacketPayload {
    public static final Type<GrabAckS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VRPhys.MODID, "grab_ack")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GrabAckS2C> CODEC =
            CustomPacketPayload.codec(GrabAckS2C::write, GrabAckS2C::read);

    private static GrabAckS2C read(RegistryFriendlyByteBuf buffer) {
        return new GrabAckS2C(
                buffer.readEnum(InteractionHand.class),
                buffer.readBoolean(),
                GrabPayloadCodecs.readNullableUuid(buffer)
        );
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        buffer.writeBoolean(accepted);
        GrabPayloadCodecs.writeNullableUuid(buffer, subLevelId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
