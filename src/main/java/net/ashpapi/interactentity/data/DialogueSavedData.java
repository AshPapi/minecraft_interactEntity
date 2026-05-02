package net.ashpapi.interactentity.data;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.event.DelayedEvent;
import net.ashpapi.interactentity.history.DialogueHistoryEntry;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class DialogueSavedData extends SavedData {
    private static final String DATA_NAME = InteractEntityMod.MOD_ID + "_progress";
    private static final int MAX_HISTORY = 50;

    private final Map<String, Set<String>> visitedNodes = new HashMap<>();
    private final Map<String, DialogueHistoryEntry> history = new HashMap<>();
    private final Map<String, QuestState> quests = new HashMap<>();
    /** Позиция незавершённого диалога: dialogueId → nodeId для возобновления */
    private final Map<String, String> resumeNodes = new HashMap<>();
    /** entityType (resourcelocation string) → kill count (shared across players) */
    private final Map<String, Integer> killCounts = new HashMap<>();
    /** variable name → value (shared, for set_var/if_var system) */
    private final Map<String, String> variables = new HashMap<>();
    /** Dialogues that have been fully completed (reached an end node). Cannot be replayed. */
    private final Set<String> completedDialogues = new HashSet<>();
    /** Executed action points: dialogueId -> action keys like node:start or option:start:0. */
    private final Map<String, Set<String>> executedActions = new HashMap<>();
    /** Dialogue IDs that have pending notifications (show ! icon even if already visited). */
    private final Set<String> npcNotifications = new HashSet<>();
    /** Reputation: faction/npc id → integer value (-100..100) */
    private final Map<String, Integer> reputation = new HashMap<>();
    /** Delayed events: fire actions at a future game tick */
    private final List<DelayedEvent> delayedEvents = new ArrayList<>();
    /** NPC relationships: "npcA:npcB" → relationship type (friend, rival, neutral, lover...) */
    private final Map<String, String> npcRelationships = new HashMap<>();

    // === Visited nodes (без изменений) ===
    public void visit(String dialogueId, String nodeId) {
        visitedNodes.computeIfAbsent(dialogueId, k -> new HashSet<>()).add(nodeId);
        setDirty();
    }

    public boolean hasVisited(String dialogueId, String nodeId) {
        Set<String> nodes = visitedNodes.get(dialogueId);
        return nodes != null && nodes.contains(nodeId);
    }

    public boolean hasStartedDialogue(String dialogueId) {
        return visitedNodes.containsKey(dialogueId);
    }

    public void removeVisit(String dialogueId) {
        visitedNodes.remove(dialogueId);
        setDirty();
    }

    // === Resume nodes ===

    /** Сохранить позицию незавершённого диалога. */
    public void setResumeNode(String dialogueId, String nodeId) {
        resumeNodes.put(dialogueId, nodeId);
        setDirty();
    }

    /** Получить позицию для возобновления (null = начинать с entry). */
    @javax.annotation.Nullable
    public String getResumeNode(String dialogueId) {
        return resumeNodes.get(dialogueId);
    }

    /** Удалить позицию возобновления (диалог завершён). */
    public void clearResumeNode(String dialogueId) {
        if (resumeNodes.remove(dialogueId) != null) setDirty();
    }

    // === History (НОВОЕ поведение) ===
    // --- History ---
    public void addHistory(DialogueHistoryEntry entry) {
        if (entry == null || entry.getLines().isEmpty()) return;

        // Replace entire entry — latest playthrough wins, no merging/duplicates
        history.put(entry.getDialogueId(), entry);

        if (history.size() > MAX_HISTORY) {
            history.values().stream()
                    .min(Comparator.comparingLong(DialogueHistoryEntry::getTimestamp))
                    .ifPresent(oldest -> history.remove(oldest.getDialogueId()));
        }

        setDirty();
    }

    public List<DialogueHistoryEntry> getHistory() {
        return history.values().stream()
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .toList();
    }

    @javax.annotation.Nullable
    public DialogueHistoryEntry getHistoryEntry(String dialogueId) {
        return history.get(dialogueId);
    }

    /** Полный сброс прогресса по конкретному диалогу (для тестирования). */
    public void resetDialogue(String dialogueId) {
        visitedNodes.remove(dialogueId);
        resumeNodes.remove(dialogueId);
        completedDialogues.remove(dialogueId);
        executedActions.remove(dialogueId);
        history.remove(dialogueId);
        setDirty();
    }

    /** Очистить все квесты (для тестирования). */
    public void clearAllQuests() {
        if (!quests.isEmpty()) {
            quests.clear();
            setDirty();
        }
    }

    // === Quests (без изменений) ===
    public void setQuest(QuestState quest) {
        quests.put(quest.getId(), quest);
        setDirty();
    }

    public QuestState getQuest(String questId) {
        return quests.get(questId);
    }

    public String getQuestStatus(String questId) {
        QuestState quest = quests.get(questId);
        return quest != null ? quest.getStatus() : "none";
    }

    public List<QuestState> getActiveQuests() {
        return quests.values().stream()
                .filter(q -> "active".equals(q.getStatus()))
                .toList();
    }

    public Map<String, QuestState> getAllQuests() {
        return Collections.unmodifiableMap(quests);
    }

    // === Completed dialogues ===
    public void markCompleted(String dialogueId) {
        if (completedDialogues.add(dialogueId)) setDirty();
    }
    public boolean isCompleted(String dialogueId) {
        return completedDialogues.contains(dialogueId);
    }
    public Set<String> getCompletedDialogues() {
        return Collections.unmodifiableSet(completedDialogues);
    }

    // === Executed actions ===
    public boolean hasExecutedAction(String dialogueId, String actionKey) {
        Set<String> keys = executedActions.get(dialogueId);
        return keys != null && keys.contains(actionKey);
    }

    public void markActionExecuted(String dialogueId, String actionKey) {
        executedActions.computeIfAbsent(dialogueId, k -> new HashSet<>()).add(actionKey);
        setDirty();
    }

    // === NPC Notifications ===
    public void addNotification(String dialogueId) {
        if (npcNotifications.add(dialogueId)) setDirty();
    }
    public void removeNotification(String dialogueId) {
        if (npcNotifications.remove(dialogueId)) setDirty();
    }
    public boolean hasNotification(String dialogueId) {
        return npcNotifications.contains(dialogueId);
    }
    public Set<String> getNotifications() {
        return Collections.unmodifiableSet(npcNotifications);
    }

    // === Kills ===
    public void addKill(String entityType) {
        killCounts.merge(entityType, 1, Integer::sum);
        setDirty();
    }
    public int getKillCount(String entityType) {
        return killCounts.getOrDefault(entityType, 0);
    }

    // === Variables ===
    public void setVar(String name, String value) {
        variables.put(name, value);
        setDirty();
    }
    public String getVar(String name) {
        return variables.getOrDefault(name, "");
    }
    public Map<String, String> getAllVars() {
        return Collections.unmodifiableMap(variables);
    }

    // === Reputation ===
    public int getReputation(String id) {
        return reputation.getOrDefault(id, 0);
    }
    public void setReputation(String id, int value) {
        reputation.put(id, Math.max(-100, Math.min(100, value)));
        setDirty();
    }
    public void addReputation(String id, int delta) {
        int current = getReputation(id);
        setReputation(id, current + delta);
    }
    public Map<String, Integer> getAllReputation() {
        return Collections.unmodifiableMap(reputation);
    }

    // === Delayed Events ===
    public void addDelayedEvent(DelayedEvent event) {
        delayedEvents.add(event);
        setDirty();
    }
    public List<DelayedEvent> getDelayedEvents() {
        return delayedEvents;
    }
    public void removeDelayedEvent(DelayedEvent event) {
        delayedEvents.remove(event);
        setDirty();
    }

    // === NPC Relationships ===
    private static String relationKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a;
    }
    public void setRelationship(String npcA, String npcB, String type) {
        npcRelationships.put(relationKey(npcA, npcB), type);
        setDirty();
    }
    public String getRelationship(String npcA, String npcB) {
        return npcRelationships.getOrDefault(relationKey(npcA, npcB), "neutral");
    }
    public Map<String, String> getAllRelationships() {
        return Collections.unmodifiableMap(npcRelationships);
    }

    // === Serialization (сохранение/загрузка) ===
    @Override
    public CompoundTag save(CompoundTag tag) {
        // Visited nodes
        CompoundTag visitedTag = new CompoundTag();
        for (Map.Entry<String, Set<String>> entry : visitedNodes.entrySet()) {
            ListTag nodeList = new ListTag();
            for (String nodeId : entry.getValue()) {
                nodeList.add(StringTag.valueOf(nodeId));
            }
            visitedTag.put(entry.getKey(), nodeList);
        }
        tag.put("visited", visitedTag);

        // History (сохраняем как список, чтобы старые сохранения работали)
        ListTag historyTag = new ListTag();
        for (DialogueHistoryEntry entry : history.values()) {
            historyTag.add(entry.save());
        }
        tag.put("history", historyTag);

        // Quests
        CompoundTag questsTag = new CompoundTag();
        for (Map.Entry<String, QuestState> entry : quests.entrySet()) {
            questsTag.put(entry.getKey(), entry.getValue().save());
        }
        tag.put("quests", questsTag);

        // Resume nodes
        CompoundTag resumeTag = new CompoundTag();
        for (Map.Entry<String, String> entry : resumeNodes.entrySet()) {
            resumeTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("resume", resumeTag);

        CompoundTag killsTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : killCounts.entrySet()) killsTag.putInt(e.getKey(), e.getValue());
        tag.put("kills", killsTag);

        CompoundTag varsTag = new CompoundTag();
        for (Map.Entry<String, String> e : variables.entrySet()) varsTag.putString(e.getKey(), e.getValue());
        tag.put("vars", varsTag);

        ListTag completedTag = new ListTag();
        for (String id : completedDialogues) completedTag.add(StringTag.valueOf(id));
        tag.put("completed", completedTag);

        CompoundTag executedActionsTag = new CompoundTag();
        for (Map.Entry<String, Set<String>> entry : executedActions.entrySet()) {
            ListTag actionList = new ListTag();
            for (String actionKey : entry.getValue()) {
                actionList.add(StringTag.valueOf(actionKey));
            }
            executedActionsTag.put(entry.getKey(), actionList);
        }
        tag.put("executed_actions", executedActionsTag);

        ListTag notifTag = new ListTag();
        for (String id : npcNotifications) notifTag.add(StringTag.valueOf(id));
        tag.put("notifications", notifTag);

        CompoundTag repTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : reputation.entrySet()) repTag.putInt(e.getKey(), e.getValue());
        tag.put("reputation", repTag);

        ListTag eventsTag = new ListTag();
        for (DelayedEvent ev : delayedEvents) eventsTag.add(ev.save());
        tag.put("delayed_events", eventsTag);

        CompoundTag relTag = new CompoundTag();
        for (Map.Entry<String, String> e : npcRelationships.entrySet()) relTag.putString(e.getKey(), e.getValue());
        tag.put("npc_relationships", relTag);

        return tag;
    }

    public static DialogueSavedData load(CompoundTag tag) {
        DialogueSavedData data = new DialogueSavedData();

        // Visited nodes
        CompoundTag visitedTag = tag.getCompound("visited");
        for (String dialogueId : visitedTag.getAllKeys()) {
            ListTag nodeList = visitedTag.getList(dialogueId, Tag.TAG_STRING);
            Set<String> nodes = new HashSet<>();
            for (int i = 0; i < nodeList.size(); i++) {
                nodes.add(nodeList.getString(i));
            }
            data.visitedNodes.put(dialogueId, nodes);
        }

        // History — загружаем в Map (дубликаты по dialogueId автоматически схлопываются)
        ListTag historyTag = tag.getList("history", Tag.TAG_COMPOUND);
        for (int i = 0; i < historyTag.size(); i++) {
            DialogueHistoryEntry entry = DialogueHistoryEntry.load(historyTag.getCompound(i));
            data.history.put(entry.getDialogueId(), entry);
        }

        // Quests
        CompoundTag questsTag = tag.getCompound("quests");
        for (String questId : questsTag.getAllKeys()) {
            data.quests.put(questId, QuestState.load(questsTag.getCompound(questId)));
        }

        // Resume nodes
        CompoundTag resumeTag = tag.getCompound("resume");
        for (String dialogueId : resumeTag.getAllKeys()) {
            data.resumeNodes.put(dialogueId, resumeTag.getString(dialogueId));
        }

        CompoundTag killsTag = tag.getCompound("kills");
        for (String k : killsTag.getAllKeys()) data.killCounts.put(k, killsTag.getInt(k));

        CompoundTag varsTag = tag.getCompound("vars");
        for (String k : varsTag.getAllKeys()) data.variables.put(k, varsTag.getString(k));

        ListTag completedTag = tag.getList("completed", Tag.TAG_STRING);
        for (int i = 0; i < completedTag.size(); i++) data.completedDialogues.add(completedTag.getString(i));

        CompoundTag executedActionsTag = tag.getCompound("executed_actions");
        for (String dialogueId : executedActionsTag.getAllKeys()) {
            ListTag actionList = executedActionsTag.getList(dialogueId, Tag.TAG_STRING);
            Set<String> keys = new HashSet<>();
            for (int i = 0; i < actionList.size(); i++) {
                keys.add(actionList.getString(i));
            }
            data.executedActions.put(dialogueId, keys);
        }

        ListTag notifTag = tag.getList("notifications", Tag.TAG_STRING);
        for (int i = 0; i < notifTag.size(); i++) data.npcNotifications.add(notifTag.getString(i));

        CompoundTag repTag = tag.getCompound("reputation");
        for (String k : repTag.getAllKeys()) data.reputation.put(k, repTag.getInt(k));

        ListTag eventsTag = tag.getList("delayed_events", Tag.TAG_COMPOUND);
        for (int i = 0; i < eventsTag.size(); i++) data.delayedEvents.add(DelayedEvent.load(eventsTag.getCompound(i)));

        CompoundTag relTag = tag.getCompound("npc_relationships");
        for (String k : relTag.getAllKeys()) data.npcRelationships.put(k, relTag.getString(k));

        return data;
    }

    public static DialogueSavedData get(ServerLevel level) {
        // Always use overworld storage — shared across all dimensions
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                DialogueSavedData::load,
                DialogueSavedData::new,
                DATA_NAME
        );
    }
}
