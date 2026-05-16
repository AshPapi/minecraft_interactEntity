package net.ashpapi.interactentity.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.npc.NpcRoutine;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogueTree {
    private final String id;
    private final String scope;
    private final DialogueTarget target;
    private final String displayName;
    private final String entryNodeId;
    private final Map<String, DialogueNode> nodes;
    @Nullable private final RevisitConfig revisitConfig;
    @Nullable private final JsonObject summonConfig;
    @Nullable private final JsonObject startTrigger;
    private final List<JsonObject> triggers;
    @Nullable private final JsonObject visualConfig;
    @Nullable private final String faction;
    @Nullable private final String reputationId;
    @Nullable private final String characterInfo;
    @Nullable private final ResourceLocation avatar;
    @Nullable private final ResourceLocation background;
    @Nullable private final ResourceLocation optionsBackground;
    private final boolean invulnerable;
    private final boolean repeatable;
    @Nullable private final List<NpcRoutine> routines;

    public DialogueTree(String id, String scope, DialogueTarget target, String displayName, String entryNodeId,
                        Map<String, DialogueNode> nodes, @Nullable RevisitConfig revisitConfig,
                        @Nullable JsonObject summonConfig, @Nullable JsonObject startTrigger,
                        List<JsonObject> triggers,
                        @Nullable JsonObject visualConfig,
                        @Nullable String faction, @Nullable String reputationId, @Nullable String characterInfo,
                        @Nullable ResourceLocation avatar,
                        @Nullable ResourceLocation background, @Nullable ResourceLocation optionsBackground,
                        boolean invulnerable, boolean repeatable, @Nullable List<NpcRoutine> routines) {
        this.id = id;
        this.scope = scope;
        this.target = target;
        this.displayName = displayName;
        this.entryNodeId = entryNodeId;
        this.nodes = nodes;
        this.revisitConfig = revisitConfig;
        this.summonConfig = summonConfig;
        this.startTrigger = startTrigger;
        this.triggers = triggers;
        this.visualConfig = visualConfig;
        this.faction = faction;
        this.reputationId = reputationId;
        this.characterInfo = characterInfo;
        this.avatar = avatar;
        this.background = background;
        this.optionsBackground = optionsBackground;
        this.invulnerable = invulnerable;
        this.repeatable = repeatable;
        this.routines = routines;
    }

    public String getId() { return id; }
    public String getScope() { return scope; }
    public DialogueTarget getTarget() { return target; }
    public String getDisplayName() { return displayName; }
    public String getEntryNodeId() { return entryNodeId; }
    @Nullable public DialogueNode getNode(String id) { return nodes.get(id); }
    public DialogueNode getEntryNode() { return nodes.get(entryNodeId); }
    public Map<String, DialogueNode> getNodes() { return nodes; }
    @Nullable public RevisitConfig getRevisitConfig() { return revisitConfig; }
    @Nullable public JsonObject getSummonConfig() { return summonConfig; }
    @Nullable public JsonObject getStartTrigger() { return startTrigger; }
    public List<JsonObject> getTriggers() { return triggers; }
    @Nullable public JsonObject getVisualConfig() { return visualConfig; }
    @Nullable public String getFaction() { return faction; }
    @Nullable public String getReputationId() { return reputationId; }
    @Nullable public String getCharacterInfo() { return characterInfo; }
    @Nullable public ResourceLocation getAvatar() { return avatar; }
    @Nullable public ResourceLocation getBackground() { return background; }
    @Nullable public ResourceLocation getOptionsBackground() { return optionsBackground; }
    public boolean isInvulnerable() { return invulnerable; }
    public boolean isRepeatable() { return repeatable; }
    @Nullable public List<NpcRoutine> getRoutines() { return routines; }

    public static DialogueTree fromJson(String id, JsonObject json) {
        DialogueTarget target = DialogueTarget.fromJson(json.getAsJsonObject("target"));
        String scope = json.has("scope") ? json.get("scope").getAsString() : "global";
        String displayName = json.has("display_name") ? json.get("display_name").getAsString() : target.getName();
        String entry = json.get("entry").getAsString();

        Map<String, DialogueNode> nodes = new HashMap<>();
        JsonObject nodesJson = json.getAsJsonObject("nodes");
        for (Map.Entry<String, JsonElement> nodeEntry : nodesJson.entrySet()) {
            nodes.put(nodeEntry.getKey(), DialogueNode.fromJson(nodeEntry.getKey(), nodeEntry.getValue().getAsJsonObject()));
        }

        RevisitConfig revisitConfig = json.has("on_revisit")
                ? RevisitConfig.fromJson(json.getAsJsonObject("on_revisit"))
                : null;

        JsonObject summonConfig = json.has("summon") ? json.getAsJsonObject("summon") : null;
        JsonObject startTrigger = json.has("start_trigger") ? json.getAsJsonObject("start_trigger") : null;
        JsonObject visualConfig = json.has("visual") ? json.getAsJsonObject("visual") : null;

        List<JsonObject> triggers = new ArrayList<>();
        if (json.has("triggers") && json.get("triggers").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("triggers")) {
                if (!el.isJsonObject()) continue;
                JsonObject trig = el.getAsJsonObject();
                String type = trig.has("type") ? trig.get("type").getAsString() : "";
                if (!isValidInteractionTrigger(type)) {
                    net.ashpapi.interactentity.InteractEntityMod.LOGGER.warn(
                            "Dialogue '{}': triggers[] entry of type '{}' is not a valid interaction trigger and will be ignored. "
                                    + "Expected one of: proximity, on_hurt, on_death, health_below. "
                                    + "(spawn triggers like 'on_join'/'after_dialogue' go inside the 'summon.trigger' block.)",
                            id, type);
                    continue;
                }
                triggers.add(trig);
            }
        } else if (startTrigger != null) {
            triggers.add(startTrigger);
        }
        String faction = json.has("faction") ? json.get("faction").getAsString() : null;
        String reputationId = json.has("reputation_id") ? json.get("reputation_id").getAsString() : faction;
        String characterInfo = json.has("character_info") ? json.get("character_info").getAsString() : null;

        ResourceLocation avatar = null;
        if (json.has("avatar") && json.get("avatar").isJsonPrimitive()) {
            avatar = ResourceLocation.tryParse(json.get("avatar").getAsString());
        }

        ResourceLocation background = null;
        if (json.has("background") && json.get("background").isJsonPrimitive()) {
            background = ResourceLocation.tryParse(json.get("background").getAsString());
        }

        ResourceLocation optionsBackground = null;
        if (json.has("options_background") && json.get("options_background").isJsonPrimitive()) {
            optionsBackground = ResourceLocation.tryParse(json.get("options_background").getAsString());
        }

        boolean invulnerable = json.has("invulnerable") ? json.get("invulnerable").getAsBoolean() : true;
        boolean repeatable = json.has("repeatable") && json.get("repeatable").getAsBoolean();

        List<NpcRoutine> routines = null;
        if (json.has("routines")) {
            routines = new ArrayList<>();
            JsonArray routinesArr = json.getAsJsonArray("routines");
            for (int i = 0; i < routinesArr.size(); i++) {
                routines.add(NpcRoutine.fromJson(routinesArr.get(i).getAsJsonObject()));
            }
        }

        injectScope(json, scope);
        return new DialogueTree(id, scope, target, displayName, entry, nodes, revisitConfig, summonConfig, startTrigger, triggers, visualConfig, faction, reputationId, characterInfo, avatar, background, optionsBackground, invulnerable, repeatable, routines);
    }

    private static boolean isValidInteractionTrigger(String type) {
        return "proximity".equals(type) || "on_hurt".equals(type)
                || "on_death".equals(type) || "health_below".equals(type);
    }

    /** Recursively injects 'scope' into all JsonObjects that have 'type' (action/condition) and no explicit 'scope'. */
    private static void injectScope(JsonElement element, String scope) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("type") && !obj.has("scope")) {
                obj.addProperty("scope", scope);
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                injectScope(entry.getValue(), scope);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                injectScope(el, scope);
            }
        }
    }
}