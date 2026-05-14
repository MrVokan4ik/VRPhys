package ru.mjkey.vrphys.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import ru.mjkey.vrphys.VRPhys;

public record GrabReleaseC2S(InteractionHand hand, Vec3 handPos, Vec3 handDir, Quaternionf handRot) implements CustomPacketPayload {
    public static final Type<GrabReleaseC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VRPhys.MODID, "grab_release")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GrabReleaseC2S> CODEC =
            CustomPacketPayload.codec(GrabReleaseC2S::write, GrabReleaseC2S::read);

    private static GrabReleaseC2S read(RegistryFriendlyByteBuf buffer) {
        GrabPayloadCodecs.HandPose pose = GrabPayloadCodecs.readHandPose(buffer);
        return new GrabReleaseC2S(pose.hand(), pose.handPos(), pose.handDir(), pose.handRot());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GrabPayloadCodecs.writeHandPose(buffer, hand, handPos, handDir, handRot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
