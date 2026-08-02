package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: игрок закрыл экран торговли (резерв под per-player состояния/лог). */
public class CloseTradeC2SPacket {
    public CloseTradeC2SPacket() {}

    public CloseTradeC2SPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                net.ashpapi.interactentity.trade.TradeHandler.handleClose(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
