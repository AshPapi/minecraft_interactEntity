package net.ashpapi.interactentity.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DialogueOption {
    private final com.google.gson.JsonElement text;
    @Nullable
    private final String nextNodeId;
    @Nullable
    private final JsonObject condition;
    private final List<JsonObject> actions;
    private final boolean locked;
    @Nullable
    private final com.google.gson.JsonElement lockReason;

    public DialogueOption(com.google.gson.JsonElement text, @Nullable String nextNodeId, @Nullable JsonObject condition,
                          List<JsonObject> actions, boolean locked, @Nullable com.google.gson.JsonElement lockReason) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.condition = condition;
        this.actions = actions;
        this.locked = locked;
        this.lockReason = lockReason;
    }

    public String getText(String lang) { return net.ashpapi.interactentity.formatting.TranslationResolver.resolve(text, lang); }
    public String getText() { return getText("en_us"); }
    public com.google.gson.JsonElement getTextElement() { return text; }
    @Nullable public String getNextNodeId() { return nextNodeId; }
    @Nullable public JsonObject getCondition() { return condition; }
    public List<JsonObject> getActions() { return actions; }
    public boolean isLocked() { return locked; }
    @Nullable public String getLockReason(String lang) {
        if (lockReason == null) return null;
        return net.ashpapi.interactentity.formatting.TranslationResolver.resolve(lockReason, lang);
    }
    @Nullable public String getLockReason() { return getLockReason("en_us"); }
    @Nullable public com.google.gson.JsonElement getLockReasonElement() { return lockReason; }

    public static DialogueOption fromJson(JsonObject json) {
        com.google.gson.JsonElement text = json.get("text");
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
        com.google.gson.JsonElement lockReason = json.has("lock_reason") ? json.get("lock_reason") : null;

        return new DialogueOption(text, next, condition, actions, locked, lockReason);
    }
}
