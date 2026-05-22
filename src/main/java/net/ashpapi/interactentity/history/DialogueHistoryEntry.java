package net.ashpapi.interactentity.history;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogueHistoryEntry {
    private final String dialogueId;
    private final String displayName;
    private final String reputationId;
    private final String factionLabel;
    private final String entityType;
    private final String characterInfo;
    private final String avatar;
    private final String visualModel;
    private final List<HistoryLine> lines;
    private long timestamp;

    public DialogueHistoryEntry(String dialogueId, String displayName, String reputationId, String factionLabel, String entityType, String characterInfo, String avatar, String visualModel, List<HistoryLine> lines, long timestamp) {
        this.dialogueId = dialogueId;
        this.displayName = displayName;
        this.reputationId = reputationId;
        this.factionLabel = factionLabel;
        this.entityType = entityType;
        this.characterInfo = characterInfo;
        this.avatar = avatar;
        this.visualModel = visualModel;
        this.lines = new ArrayList<>(lines);
        this.timestamp = timestamp;
    }

    /** Backwards-compat constructor without visualModel (delegates with null). */
    public DialogueHistoryEntry(String dialogueId, String displayName, String reputationId, String factionLabel, String entityType, String characterInfo, String avatar, List<HistoryLine> lines, long timestamp) {
        this(dialogueId, displayName, reputationId, factionLabel, entityType, characterInfo, avatar, null, lines, timestamp);
    }

    public String getDialogueId() { return dialogueId; }
    public String getDisplayName() { return displayName; }
    public String getReputationId() { return reputationId; }
    public String getFactionLabel() { return factionLabel; }
    public String getEntityType() { return entityType; }
    public String getCharacterInfo() { return characterInfo; }
    public String getAvatar() { return avatar; }
    public String getVisualModel() { return visualModel; }
    public List<HistoryLine> getLines() { return Collections.unmodifiableList(lines); }
    public long getTimestamp() { return timestamp; }

    /**
     * Добавляет новые строки, но избегает дублирования последних реплик
     */
    public void appendLinesSmart(List<HistoryLine> additionalLines, long newTimestamp) {
        if (additionalLines.isEmpty()) return;

        // Find the longest suffix of existing lines that matches a prefix of additionalLines.
        // Those are lines already stored from a previous session — skip them, append only what's new.
        int skipCount = 0;
        int maxOverlap = Math.min(additionalLines.size(), lines.size());
        for (int len = maxOverlap; len > 0; len--) {
            boolean matches = true;
            for (int i = 0; i < len; i++) {
                HistoryLine existing = lines.get(lines.size() - len + i);
                HistoryLine newLine = additionalLines.get(i);
                if (!existing.getSpeaker().equals(newLine.getSpeaker()) ||
                        !existing.getText().equals(newLine.getText())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                skipCount = len;
                break;
            }
        }

        List<HistoryLine> toAdd = additionalLines.subList(skipCount, additionalLines.size());
        if (!toAdd.isEmpty()) {
            this.lines.addAll(toAdd);
            this.timestamp = newTimestamp;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dialogueId", dialogueId);
        tag.putString("displayName", displayName);
        if (reputationId != null) tag.putString("reputationId", reputationId);
        if (factionLabel != null) tag.putString("factionLabel", factionLabel);
        if (entityType != null) tag.putString("entityType", entityType);
        if (characterInfo != null) tag.putString("characterInfo", characterInfo);
        if (avatar != null) tag.putString("avatar", avatar);
        if (visualModel != null) tag.putString("visualModel", visualModel);
        tag.putLong("timestamp", timestamp);

        ListTag linesTag = new ListTag();
        for (HistoryLine line : lines) {
            linesTag.add(line.save());
        }
        tag.put("lines", linesTag);
        return tag;
    }

    public void addLine(HistoryLine line) {
        if (line != null) {
            this.lines.add(line);
            this.timestamp = System.currentTimeMillis();  // обновляем время последнего сообщения
        }
    }

    public static DialogueHistoryEntry load(CompoundTag tag) {
        String dialogueId = tag.getString("dialogueId");
        String displayName = tag.getString("displayName");
        String reputationId = tag.contains("reputationId") ? tag.getString("reputationId") : null;
        String factionLabel = tag.contains("factionLabel") ? tag.getString("factionLabel") : null;
        String entityType = tag.contains("entityType") ? tag.getString("entityType") : null;
        String characterInfo = tag.contains("characterInfo") ? tag.getString("characterInfo") : null;
        String avatar = tag.contains("avatar") ? tag.getString("avatar") : null;
        String visualModel = tag.contains("visualModel") ? tag.getString("visualModel") : null;
        
        // Backwards compatibility for old saves
        if (reputationId == null && tag.contains("factionId")) {
            reputationId = tag.getString("factionId");
        }
        if (factionLabel == null && reputationId != null) {
            factionLabel = reputationId;
        }

        long timestamp = tag.getLong("timestamp");

        List<HistoryLine> lines = new ArrayList<>();
        ListTag linesTag = tag.getList("lines", Tag.TAG_COMPOUND);
        for (int i = 0; i < linesTag.size(); i++) {
            lines.add(HistoryLine.load(linesTag.getCompound(i)));
        }

        return new DialogueHistoryEntry(dialogueId, displayName, reputationId, factionLabel, entityType, characterInfo, avatar, visualModel, lines, timestamp);
    }
}