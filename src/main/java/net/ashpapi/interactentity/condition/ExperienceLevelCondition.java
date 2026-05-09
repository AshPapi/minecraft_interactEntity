package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class ExperienceLevelCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        int level = params.get("level").getAsInt();
        String op = params.has("op") ? params.get("op").getAsString() : "gte";

        return switch (op) {
            case "eq" -> player.experienceLevel == level;
            case "gt" -> player.experienceLevel > level;
            case "lt" -> player.experienceLevel < level;
            case "gte" -> player.experienceLevel >= level;
            case "lte" -> player.experienceLevel <= level;
            default -> player.experienceLevel >= level;
        };
    }
}
