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

    public DialogueOption(String text, @Nullable String nextNodeId, @Nullable JsonObject condition, List<JsonObject> actions) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.condition = condition;
        this.actions = actions;
    }

    public String getText() { return text; }
    @Nullable public String getNextNodeId() { return nextNodeId; }
    @Nullable public JsonObject getCondition() { return condition; }
    public List<JsonObject> getActions() { return actions; }

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

        return new DialogueOption(text, next, condition, actions);
    }
}
