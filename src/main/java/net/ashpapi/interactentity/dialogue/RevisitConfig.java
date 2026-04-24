package net.ashpapi.interactentity.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RevisitConfig {
    private final String defaultText;
    @Nullable
    private final String defaultStartNode;
    private final List<ConditionalText> conditions;

    public RevisitConfig(String defaultText, @Nullable String defaultStartNode, List<ConditionalText> conditions) {
        this.defaultText = defaultText;
        this.defaultStartNode = defaultStartNode;
        this.conditions  = conditions;
    }

    public String getDefaultText()           { return defaultText; }
    @Nullable
    public String getDefaultStartNode()      { return defaultStartNode; }
    public List<ConditionalText> getConditions() { return conditions; }

    public static class ConditionalText {
        private final JsonObject condition;
        private final String text;
        @Nullable
        private final String startNode; // ← новое: опциональный переход к ноде дерева

        public ConditionalText(JsonObject condition, String text, @Nullable String startNode) {
            this.condition = condition;
            this.text      = text;
            this.startNode = startNode;
        }

        public JsonObject getCondition() { return condition; }
        public String getText()          { return text; }

        /** Если задано — запускает полноценный диалог с этой ноды вместо end-сообщения */
        @Nullable
        public String getStartNode()     { return startNode; }
    }

    @Nullable
    public static RevisitConfig fromJson(@Nullable JsonObject json) {
        if (json == null) return null;

        String defaultText = json.has("default") ? json.get("default").getAsString() : "";
        String defaultStartNode = json.has("default_start_node") ? json.get("default_start_node").getAsString() : null;
        List<ConditionalText> conditions = new ArrayList<>();

        if (json.has("conditions")) {
            JsonArray arr = json.getAsJsonArray("conditions");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String startNode = obj.has("start_node") ? obj.get("start_node").getAsString() : null;
                conditions.add(new ConditionalText(
                        obj.getAsJsonObject("condition"),
                        obj.has("text") ? obj.get("text").getAsString() : "",
                        startNode
                ));
            }
        }

        return new RevisitConfig(defaultText, defaultStartNode, conditions);
    }
}