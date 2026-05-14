package ru.mjkey.vrphys.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import ru.mjkey.vrphys.VRPhys;

/**
 * Sent from server to client every 2 game ticks while a grab is active.
 * {@code stressNormalized} is in [0, 1], where 1 means the spring is at max force.
 * Used by the client to drive continuous haptic feedback.
 */
public record GrabStressS2C(InteractionHand hand, float stressNormalized) implements CustomPacketPayload {
    public static final Type<GrabStressS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VRPhys.MODID, "grab_stress")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GrabStressS2C> CODEC =
            CustomPacketPayload.codec(GrabStressS2C::write, GrabStressS2C::read);

    private static GrabStressS2C read(RegistryFriendlyByteBuf buffer) {
        return new GrabStressS2C(buffer.readEnum(InteractionHand.class), buffer.readFloat());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        buffer.writeFloat(stressNormalized);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
