package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.data.ClientProgressData;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.history.DialogueHistoryEntry;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SyncProgressPacket {
    private final CompoundTag data;

    public SyncProgressPacket(DialogueSavedData savedData) {
        this.data = savedData.save(new CompoundTag());
    }

    public SyncProgressPacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Parse visited nodes
            Map<String, Set<String>> visited = new HashMap<>();
            CompoundTag visitedTag = data.getCompound("visited");
            for (String dialogueId : visitedTag.getAllKeys()) {
                ListTag nodeList = visitedTag.getList(dialogueId, Tag.TAG_STRING);
                Set<String> nodes = new HashSet<>();
                for (int i = 0; i < nodeList.size(); i++) {
                    nodes.add(nodeList.getString(i));
                }
                visited.put(dialogueId, nodes);
            }
            ClientProgressData.setVisitedNodes(visited);

            // Parse history
            List<DialogueHistoryEntry> history = new ArrayList<>();
            ListTag historyTag = data.getList("history", Tag.TAG_COMPOUND);
            for (int i = 0; i < historyTag.size(); i++) {
                history.add(DialogueHistoryEntry.load(historyTag.getCompound(i)));
            }
            ClientProgressData.setHistory(history);

            // Parse quests
            Map<String, QuestState> quests = new HashMap<>();
            CompoundTag questsTag = data.getCompound("quests");
            for (String questId : questsTag.getAllKeys()) {
                quests.put(questId, QuestState.load(questsTag.getCompound(questId)));
            }
            ClientProgressData.setQuests(quests);

            Set<String> completed = new HashSet<>();
            ListTag completedTag = data.getList("completed", Tag.TAG_STRING);
            for (int i = 0; i < completedTag.size(); i++) completed.add(completedTag.getString(i));
            ClientProgressData.setCompletedDialogues(completed);

            Set<String> notifications = new HashSet<>();
            ListTag notifTag = data.getList("notifications", Tag.TAG_STRING);
            for (int i = 0; i < notifTag.size(); i++) notifications.add(notifTag.getString(i));
            ClientProgressData.setNotifications(notifications);
        });
        ctx.get().setPacketHandled(true);
    }
}
