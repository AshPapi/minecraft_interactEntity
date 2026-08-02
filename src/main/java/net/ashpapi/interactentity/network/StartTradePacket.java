package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: игрок нажал [T] на торговце. Сервер собирает витрину и шлёт OpenTradePacket. */
public class StartTradePacket {
    private final int entityId;

    public StartTradePacket(int entityId) {
        this.entityId = entityId;
    }

    public StartTradePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                net.ashpapi.interactentity.trade.TradeHandler.tryStartTrade(player, entityId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
