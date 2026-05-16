package net.ashpapi.interactentity.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestState {
    private static final String COMPLETE_MARKER = "&a[\u2714]";

    private final String id;
    private String title;
    private String description;
    private List<String> objectives;
    private String status; // "active", "completed", "failed"
    private String giverName;
    @Nullable
    private String dialogueId;

    // Поля для автоматического отслеживания предметов
    @Nullable
    private String requiredItemId;
    private int requiredCount;
    private boolean itemCollected; // флаг, что предмет уже подобран (чтобы не обновлять objectives повторно)

    // Поля для автоматического отслеживания убийств
    @Nullable
    private String killEntityType;
    private int killRequired;
    private int killProgress;
    private int killObjectiveIndex; // индекс objective для обновления

    // Поля для дедлайна квеста
    private long deadlineTick;    // game time, когда квест фейлится (0 = нет дедлайна)
    @Nullable
    private String deadlineType;  // "ticks", "sunset", "sunrise", "game_days"

    public QuestState(String id, String title, String description, List<String> objectives, String status, String giverName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.objectives = normalizeObjectives(objectives);
        this.status = status;
        this.giverName = giverName;
        this.dialogueId = null;
        this.requiredItemId = null;
        this.requiredCount = 0;
        this.itemCollected = false;
        this.killEntityType = null;
        this.killRequired = 0;
        this.killProgress = 0;
        this.killObjectiveIndex = 0;
        this.deadlineTick = 0;
        this.deadlineType = null;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getObjectives() { return Collections.unmodifiableList(objectives); }
    public String getStatus() { return status; }
    public String getGiverName() { return giverName; }
    @Nullable
    public String getDialogueId() { return dialogueId; }

    public void setStatus(String status) { this.status = status; }
    public void setObjectives(List<String> objectives) { this.objectives = normalizeObjectives(objectives); }
    public void setDialogueId(@Nullable String dialogueId) { this.dialogueId = dialogueId; }

    @Nullable
    public String getRequiredItemId() { return requiredItemId; }
    public int getRequiredCount() { return requiredCount; }
    public boolean isItemCollected() { return itemCollected; }

    public void setRequiredItem(@Nullable String itemId, int count) {
        this.requiredItemId = itemId;
        this.requiredCount = count;
        this.itemCollected = false;
    }

    public void markItemCollected() {
        this.itemCollected = true;
    }

    @Nullable
    public String getKillEntityType() { return killEntityType; }
    public int getKillRequired() { return killRequired; }
    public int getKillProgress() { return killProgress; }
    public boolean isKillCompleted() { return killRequired > 0 && killProgress >= killRequired; }

    public void setRequiredKills(String entityType, int count, int objectiveIndex) {
        this.killEntityType = entityType;
        this.killRequired = count;
        this.killProgress = 0;
        this.killObjectiveIndex = objectiveIndex;
    }

    /** Добавляет прогресс убийств. Возвращает true если objective только что выполнен. */
    public boolean addKillProgress(int amount) {
        if (killEntityType == null || killRequired <= 0 || isKillCompleted()) return false;
        killProgress = Math.min(killProgress + amount, killRequired);
        updateKillObjectiveText();
        return isKillCompleted();
    }

    private void updateKillObjectiveText() {
        if (killObjectiveIndex < 0 || killObjectiveIndex >= objectives.size()) return;
        String current = objectives.get(killObjectiveIndex);
        // Убираем предыдущий маркер прогресса и completion, берём базовый текст
        String base = current
                .replaceAll("\\s*\\(\\d+/\\d+\\)$", "")
                .replace(COMPLETE_MARKER + " ", "")
                .replace(COMPLETE_MARKER, "")
                .trim();
        if (isKillCompleted()) {
            objectives.set(killObjectiveIndex, COMPLETE_MARKER + " " + base + " (" + killRequired + "/" + killRequired + ")");
        } else {
            objectives.set(killObjectiveIndex, base + " (" + killProgress + "/" + killRequired + ")");
        }
    }

    public long getDeadlineTick() { return deadlineTick; }
    public void setDeadlineTick(long deadlineTick) { this.deadlineTick = deadlineTick; }
    @Nullable
    public String getDeadlineType() { return deadlineType; }
    public void setDeadlineType(@Nullable String deadlineType) { this.deadlineType = deadlineType; }
    public boolean hasDeadline() { return deadlineTick > 0; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("title", title);
        tag.putString("description", description);
        tag.putString("status", status);
        tag.putString("giverName", giverName);
        if (dialogueId != null && !dialogueId.isEmpty()) {
            tag.putString("dialogueId", dialogueId);
        }

        if (requiredItemId != null) {
            tag.putString("requiredItemId", requiredItemId);
            tag.putInt("requiredCount", requiredCount);
            tag.putBoolean("itemCollected", itemCollected);
        }

        if (killEntityType != null) {
            tag.putString("killEntityType", killEntityType);
            tag.putInt("killRequired", killRequired);
            tag.putInt("killProgress", killProgress);
            tag.putInt("killObjectiveIndex", killObjectiveIndex);
        }

        if (deadlineTick > 0) {
            tag.putLong("deadlineTick", deadlineTick);
            if (deadlineType != null) tag.putString("deadlineType", deadlineType);
        }

        ListTag objTag = new ListTag();
        for (String obj : objectives) {
            objTag.add(StringTag.valueOf(obj));
        }
        tag.put("objectives", objTag);
        return tag;
    }

    public static QuestState load(CompoundTag tag) {
        String id = tag.getString("id");
        String title = tag.getString("title");
        String description = tag.getString("description");
        String status = tag.getString("status");
        String giverName = tag.getString("giverName");

        List<String> objectives = new ArrayList<>();
        ListTag objTag = tag.getList("objectives", Tag.TAG_STRING);
        for (int i = 0; i < objTag.size(); i++) {
            objectives.add(objTag.getString(i));
        }

        QuestState quest = new QuestState(id, title, description, objectives, status, giverName);
        quest.dialogueId = tag.contains("dialogueId") ? tag.getString("dialogueId") : null;

        if (tag.contains("requiredItemId")) {
            quest.requiredItemId = tag.getString("requiredItemId");
            quest.requiredCount = tag.getInt("requiredCount");
            quest.itemCollected = tag.getBoolean("itemCollected");
        }

        if (tag.contains("deadlineTick")) {
            quest.deadlineTick = tag.getLong("deadlineTick");
            quest.deadlineType = tag.contains("deadlineType") ? tag.getString("deadlineType") : null;
        }

        if (tag.contains("killEntityType")) {
            quest.killEntityType = tag.getString("killEntityType");
            quest.killRequired = tag.getInt("killRequired");
            quest.killProgress = tag.getInt("killProgress");
            quest.killObjectiveIndex = tag.getInt("killObjectiveIndex");
        }

        return quest;
    }

    public static String objectiveText(String objective) {
        if (objective == null || objective.isEmpty()) return "";
        return objective.replace(COMPLETE_MARKER, "")
                .replace("[\u2714]", "")
                .replace("[ ]", "")
                .trim();
    }

    public static boolean isObjectiveCompleted(String objective) {
        return objective != null && (objective.contains(COMPLETE_MARKER) || objective.contains("[\u2714]"));
    }

    public static String completedObjective(String objective) {
        String text = objectiveText(objective);
        return text.isEmpty() ? COMPLETE_MARKER : COMPLETE_MARKER + " " + text;
    }

    public boolean completeObjective(int index) {
        if (index < 0 || index >= objectives.size()) return false;
        objectives.set(index, completedObjective(objectives.get(index)));
        return true;
    }

    public boolean completeObjective(String text) {
        String normalizedText = objectiveText(text);
        if (normalizedText.isEmpty()) return false;

        for (int i = 0; i < objectives.size(); i++) {
            if (objectiveText(objectives.get(i)).equalsIgnoreCase(normalizedText)) {
                return completeObjective(i);
            }
        }
        return false;
    }

    public void completeAllObjectives() {
        for (int i = 0; i < objectives.size(); i++) {
            completeObjective(i);
        }
    }

    private static List<String> normalizeObjectives(List<String> objectives) {
        List<String> normalized = new ArrayList<>();
        for (String objective : objectives) {
            if (isObjectiveCompleted(objective)) {
                normalized.add(completedObjective(objective));
            } else {
                normalized.add(objectiveText(objective));
            }
        }
        return normalized;
    }
}
