package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.data.DialogueDataManager;
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

            // The quest could live in either scope — toggle in whichever has it.
            DialogueSavedData globalData = DialogueDataManager.getGlobal(player.serverLevel());
            DialogueSavedData playerData = DialogueDataManager.getPlayer(player);
            if (globalData.getQuest(questId) != null) {
                globalData.toggleTrackedQuest(questId);
                ModNetwork.sendToAll(new TrackedQuestsPacket(globalData.getTrackedQuestIds()));
            } else if (playerData != null && playerData.getQuest(questId) != null) {
                playerData.toggleTrackedQuest(questId);
                ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
