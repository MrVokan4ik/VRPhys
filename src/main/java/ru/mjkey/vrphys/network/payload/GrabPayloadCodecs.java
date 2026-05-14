package ru.mjkey.vrphys.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.UUID;

final class GrabPayloadCodecs {
    private GrabPayloadCodecs() {
    }

    static void writeHandPose(RegistryFriendlyByteBuf buffer, InteractionHand hand, Vec3 handPos, Vec3 handDir, Quaternionf handRot) {
        buffer.writeEnum(hand);
        writeVec3(buffer, handPos);
        writeVec3(buffer, handDir);
        writeQuat(buffer, handRot);
    }

    static HandPose readHandPose(RegistryFriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        Vec3 handPos = readVec3(buffer);
        Vec3 handDir = readVec3(buffer);
        Quaternionf handRot = readQuat(buffer);
        return new HandPose(hand, handPos, handDir, handRot);
    }

    static void writeVec3(RegistryFriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    static Vec3 readVec3(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    static void writeQuat(RegistryFriendlyByteBuf buffer, Quaternionf q) {
        buffer.writeFloat(q.x);
        buffer.writeFloat(q.y);
        buffer.writeFloat(q.z);
        buffer.writeFloat(q.w);
    }

    static Quaternionf readQuat(RegistryFriendlyByteBuf buffer) {
        return new Quaternionf(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    static void writeNullableUuid(RegistryFriendlyByteBuf buffer, UUID uuid) {
        buffer.writeBoolean(uuid != null);
        if (uuid != null) {
            buffer.writeUUID(uuid);
        }
    }

    static UUID readNullableUuid(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUUID() : null;
    }

    record HandPose(InteractionHand hand, Vec3 handPos, Vec3 handDir, Quaternionf handRot) {
    }
}
