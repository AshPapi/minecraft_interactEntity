package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleTrackedQuestPacket {
    private final String questId;

    public ToggleTrackedQuestPacket(String questId) {
        this.questId = questId;
    }

    public ToggleTrackedQuestPacket(FriendlyByteBuf buf) {
        this.questId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(questId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
            data.toggleTrackedQuest(questId);
            ModNetwork.sendToAll(new TrackedQuestsPacket(data.getTrackedQuestIds()));
        });
        ctx.get().setPacketHandled(true);
    }
}
