package net.ashpapi.interactentity.summon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SummonConfig {
    private final String entityType;
    private final String customName;
    private final List<String> tags;
    private final SummonTrigger trigger;
    private final String spawnPosition;
    private final boolean despawnAfterDialogue;
    private final boolean walkAwayBeforeDespawn;

    public SummonConfig(String entityType, String customName, List<String> tags, SummonTrigger trigger,
                        String spawnPosition, boolean despawnAfterDialogue, boolean walkAwayBeforeDespawn) {
        this.entityType = entityType;
        this.customName = customName;
        this.tags = tags;
        this.trigger = trigger;
        this.spawnPosition = spawnPosition;
        this.despawnAfterDialogue = despawnAfterDialogue;
        this.walkAwayBeforeDespawn = walkAwayBeforeDespawn;
    }

    public String getEntityType() { return entityType; }
    public String getCustomName() { return customName; }
    public List<String> getTags() { return tags; }
    public SummonTrigger getTrigger() { return trigger; }
    public String getSpawnPosition() { return spawnPosition; }
    public boolean isDespawnAfterDialogue() { return despawnAfterDialogue; }
    public boolean isWalkAwayBeforeDespawn() { return walkAwayBeforeDespawn; }

    @Nullable
    public static SummonConfig fromJson(JsonObject json) {
        if (json == null) return null;

        String entityType = json.get("entity").getAsString();
        String customName = json.get("custom_name").getAsString();

        List<String> tags = new ArrayList<>();
        if (json.has("tags")) {
            JsonArray arr = json.getAsJsonArray("tags");
            for (int i = 0; i < arr.size(); i++) {
                tags.add(arr.get(i).getAsString());
            }
        }

        SummonTrigger trigger = SummonTrigger.fromJson(json.getAsJsonObject("trigger"));
        String spawnPosition = json.has("spawn_position") ? json.get("spawn_position").getAsString() : "behind_player";
        boolean despawn = json.has("despawn_after_dialogue") && json.get("despawn_after_dialogue").getAsBoolean();
        boolean walkAway = json.has("walk_away_before_despawn") && json.get("walk_away_before_despawn").getAsBoolean();

        return new SummonConfig(entityType, customName, tags, trigger, spawnPosition, despawn, walkAway);
    }
}
