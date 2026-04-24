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
    private final String id;
    private String title;
    private String description;
    private List<String> objectives;
    private String status; // "active", "completed", "failed"
    private String giverName;

    // Поля для автоматического отслеживания предметов
    @Nullable
    private String requiredItemId;
    private int requiredCount;
    private boolean itemCollected; // флаг, что предмет уже подобран (чтобы не обновлять objectives повторно)

    public QuestState(String id, String title, String description, List<String> objectives, String status, String giverName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.objectives = new ArrayList<>(objectives);
        this.status = status;
        this.giverName = giverName;
        this.requiredItemId = null;
        this.requiredCount = 0;
        this.itemCollected = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getObjectives() { return Collections.unmodifiableList(objectives); }
    public String getStatus() { return status; }
    public String getGiverName() { return giverName; }

    public void setStatus(String status) { this.status = status; }
    public void setObjectives(List<String> objectives) { this.objectives = new ArrayList<>(objectives); }

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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("title", title);
        tag.putString("description", description);
        tag.putString("status", status);
        tag.putString("giverName", giverName);

        if (requiredItemId != null) {
            tag.putString("requiredItemId", requiredItemId);
            tag.putInt("requiredCount", requiredCount);
            tag.putBoolean("itemCollected", itemCollected);
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

        if (tag.contains("requiredItemId")) {
            quest.requiredItemId = tag.getString("requiredItemId");
            quest.requiredCount = tag.getInt("requiredCount");
            quest.itemCollected = tag.getBoolean("itemCollected");
        }

        return quest;
    }
}