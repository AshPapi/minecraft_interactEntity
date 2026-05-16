package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.data.ClientProgressData;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.history.DialogueHistoryEntry;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SyncProgressPacket {
    private final CompoundTag data;

    public SyncProgressPacket(DialogueSavedData savedData) {
        this.data = savedData.save(new CompoundTag());
    }

    private SyncProgressPacket(CompoundTag merged) {
        this.data = merged;
    }

    public SyncProgressPacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    /**
     * Builds a per-player sync packet by merging the global store with the player's personal store.
     * Player entries override global ones at the compound level. ListTag entries are merged with dedup
     * (by 'dialogueId' for compounds, by string value otherwise).
     */
    public static SyncProgressPacket createFor(ServerPlayer player) {
        CompoundTag globalTag = DialogueDataManager.getGlobal(player.serverLevel()).save(new CompoundTag());
        DialogueSavedData playerData = DialogueDataManager.getPlayer(player);
        if (playerData == null) return new SyncProgressPacket(globalTag);

        CompoundTag playerTag = playerData.save(new CompoundTag());
        CompoundTag merged = globalTag.copy();
        for (String key : playerTag.getAllKeys()) {
            Tag pTag = playerTag.get(key);
            Tag gTag = merged.get(key);
            if (gTag instanceof CompoundTag gc && pTag instanceof CompoundTag pc) {
                for (String k : pc.getAllKeys()) gc.put(k, pc.get(k));
            } else if (gTag instanceof ListTag gl && pTag instanceof ListTag pl) {
                Set<String> seen = new HashSet<>();
                ListTag mergedList = new ListTag();
                for (int i = 0; i < pl.size(); i++) {
                    Tag entry = pl.get(i);
                    String dedupKey = entry instanceof CompoundTag ct && ct.contains("dialogueId") ? ct.getString("dialogueId") : entry.getAsString();
                    if (seen.add(dedupKey)) mergedList.add(entry);
                }
                for (int i = 0; i < gl.size(); i++) {
                    Tag entry = gl.get(i);
                    String dedupKey = entry instanceof CompoundTag ct && ct.contains("dialogueId") ? ct.getString("dialogueId") : entry.getAsString();
                    if (seen.add(dedupKey)) mergedList.add(entry);
                }
                merged.put(key, mergedList);
            } else {
                merged.put(key, pTag);
            }
        }
        return new SyncProgressPacket(merged);
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

            Set<String> trackedQuests = new LinkedHashSet<>();
            ListTag trackedQuestTag = data.getList("tracked_quests", Tag.TAG_STRING);
            for (int i = 0; i < trackedQuestTag.size(); i++) {
                trackedQuests.add(trackedQuestTag.getString(i));
            }
            ClientProgressData.setTrackedQuests(trackedQuests);

            Set<String> completed = new HashSet<>();
            ListTag completedTag = data.getList("completed", Tag.TAG_STRING);
            for (int i = 0; i < completedTag.size(); i++) completed.add(completedTag.getString(i));
            ClientProgressData.setCompletedDialogues(completed);

            Set<String> notifications = new HashSet<>();
            ListTag notifTag = data.getList("notifications", Tag.TAG_STRING);
            for (int i = 0; i < notifTag.size(); i++) notifications.add(notifTag.getString(i));
            ClientProgressData.setNotifications(notifications);

            Map<String, Integer> reputation = new HashMap<>();
            CompoundTag repTag = data.getCompound("reputation");
            for (String k : repTag.getAllKeys()) reputation.put(k, repTag.getInt(k));
            ClientProgressData.setReputation(reputation);
        });
        ctx.get().setPacketHandled(true);
    }
}
