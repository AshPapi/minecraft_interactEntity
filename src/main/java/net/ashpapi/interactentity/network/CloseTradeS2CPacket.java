package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S→C: сервер заставляет клиент закрыть экран торговли. */
public class CloseTradeS2CPacket {
    public CloseTradeS2CPacket() {}

    public CloseTradeS2CPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleCloseTrade()));
        ctx.get().setPacketHandled(true);
    }
}
