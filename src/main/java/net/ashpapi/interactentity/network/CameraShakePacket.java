package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraShakePacket {
    public final float intensity;
    public final int duration;

    public CameraShakePacket(float intensity, int duration) {
        this.intensity = intensity;
        this.duration = duration;
    }

    public CameraShakePacket(FriendlyByteBuf buf) {
        this.intensity = buf.readFloat();
        this.duration = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(intensity);
        buf.writeVarInt(duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.ashpapi.interactentity.camera.CameraShake.start(intensity, duration)));
        ctx.get().setPacketHandled(true);
    }
}
