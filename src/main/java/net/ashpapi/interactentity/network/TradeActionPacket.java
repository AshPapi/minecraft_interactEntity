package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C→S: игрок выбрал оффер в экране торговли. Сервер валидирует и выполняет обмен. */
public class TradeActionPacket {
    private final int entityId;
    private final int offerIndex;

    public TradeActionPacket(int entityId, int offerIndex) {
        this.entityId = entityId;
        this.offerIndex = offerIndex;
    }

    public TradeActionPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.offerIndex = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(offerIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                net.ashpapi.interactentity.trade.TradeHandler.tryExecuteTrade(player, entityId, offerIndex);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
