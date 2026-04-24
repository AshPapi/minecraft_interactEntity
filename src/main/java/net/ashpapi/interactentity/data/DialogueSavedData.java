package net.ashpapi.interactentity.data;

import net.ashpapi.interactentity.InteractEntityMod;
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