package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.event.EntityInteractHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StartDialoguePacket {
    private final int entityId;

    public StartDialoguePacket(int entityId) {
        this.entityId = entityId;
    }

    public StartDialoguePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof LivingEntity target)) return;
            if (player.distanceToSqr(target) > 16.0D) return;
            // Не открываем диалог, если игрок уже торгует
            if (net.ashpapi.interactentity.trade.TradeSession.hasActive(player)) return;

            EntityInteractHandler.startDialogue(player, target);
        });
        ctx.get().setPacketHandled(true);
    }
}
