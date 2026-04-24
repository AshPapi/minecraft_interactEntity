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
    private final List<HistoryLine> lines;
    private long timestamp;

    public DialogueHistoryEntry(String dialogueId, String displayName, List<HistoryLine> lines, long timestamp) {
        this.dialogueId = dialogueId;
        this.displayName = displayName;
        this.lines = new ArrayList<>(lines);
        this.timestamp = timestamp;
    }

    public String getDialogueId() { return dialogueId; }
    public String getDisplayName() { return displayName; }
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
        long timestamp = tag.getLong("timestamp");

        List<HistoryLine> lines = new ArrayList<>();
        ListTag linesTag = tag.getList("lines", Tag.TAG_COMPOUND);
        for (int i = 0; i < linesTag.size(); i++) {
            lines.add(HistoryLine.load(linesTag.getCompound(i)));
        }

        return new DialogueHistoryEntry(dialogueId, displayName, lines, timestamp);
    }
}