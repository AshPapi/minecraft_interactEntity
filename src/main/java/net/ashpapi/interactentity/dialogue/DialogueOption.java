package net.ashpapi.interactentity.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DialogueOption {
    private final String text;
    @Nullable
    private final String nextNodeId;
    @Nullable
    private final JsonObject condition;
    private final List<JsonObject> actions;
    private final boolean locked;
    @Nullable
    private final String lockReason;

    public DialogueOption(String text, @Nullable String nextNodeId, @Nullable JsonObject condition,
                          List<JsonObject> actions, boolean locked, @Nullable String lockReason) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.condition = condition;
        this.actions = actions;
        this.locked = locked;
        this.lockReason = lockReason;
    }

    public String getText() { return text; }
    @Nullable public String getNextNodeId() { return nextNodeId; }
    @Nullable public JsonObject getCondition() { return condition; }
    public List<JsonObject> getActions() { return actions; }
    public boolean isLocked() { return locked; }
    @Nullable public String getLockReason() { return lockReason; }

    public static DialogueOption fromJson(JsonObject json) {
        String text = json.get("text").getAsString();
        String next = json.has("next") && !json.get("next").isJsonNull() ? json.get("next").getAsString() : null;
        JsonObject condition = json.has("condition") ? json.getAsJsonObject("condition") : null;

        List<JsonObject> actions = new ArrayList<>();
        if (json.has("actions")) {
            JsonArray arr = json.getAsJsonArray("actions");
            for (JsonElement el : arr) {
                actions.add(el.getAsJsonObject());
            }
        }

        boolean locked = json.has("locked") && json.get("locked").getAsBoolean();
        String lockReason = json.has("lock_reason") ? json.get("lock_reason").getAsString() : null;

        return new DialogueOption(text, next, condition, actions, locked, lockReason);
    }
}
