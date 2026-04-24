package net.ashpapi.interactentity.history;

import net.minecraft.nbt.CompoundTag;

public class HistoryLine {
    private final String speaker;
    private final String text;

    public HistoryLine(String speaker, String text) {
        this.speaker = speaker;
        this.text = text;
    }

    public String getSpeaker() { return speaker; }
    public String getText() { return text; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("speaker", speaker);
        tag.putString("text", text);
        return tag;
    }

    public static HistoryLine load(CompoundTag tag) {
        return new HistoryLine(tag.getString("speaker"), tag.getString("text"));
    }
}
