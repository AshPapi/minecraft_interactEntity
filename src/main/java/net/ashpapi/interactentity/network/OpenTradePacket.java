package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.trade.TradeOffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** S→C: открыть/обновить экран торговли. Список офферов уже отфильтрован на сервере. */
public class OpenTradePacket {
    final int entityId;
    final String shopName;
    final String dialogueId;
    final List<TradeOffer> offers;

    public OpenTradePacket(int entityId, String shopName, String dialogueId, List<TradeOffer> offers) {
        this.entityId = entityId;
        this.shopName = shopName;
        this.dialogueId = dialogueId;
        this.offers = offers;
    }

    public OpenTradePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.shopName = buf.readUtf();
        this.dialogueId = buf.readUtf();
        int count = buf.readVarInt();
        this.offers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.offers.add(TradeOffer.decode(buf));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(shopName);
        buf.writeUtf(dialogueId);
        buf.writeVarInt(offers.size());
        for (TradeOffer offer : offers) offer.encode(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenTrade(this)));
        ctx.get().setPacketHandled(true);
    }
}
