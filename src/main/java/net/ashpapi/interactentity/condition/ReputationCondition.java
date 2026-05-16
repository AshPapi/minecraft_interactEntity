package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class ReputationCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.get("id").getAsString();
        String op = params.has("op") ? params.get("op").getAsString() : "gte";
        int value = params.get("value").getAsInt();

        DialogueSavedData data = DialogueDataManager.get(player, params);
        int rep = data.getReputation(id);

        return switch (op) {
            case "eq" -> rep == value;
            case "neq" -> rep != value;
            case "gt" -> rep > value;
            case "gte" -> rep >= value;
            case "lt" -> rep < value;
            case "lte" -> rep <= value;
            default -> rep >= value;
        };
    }
}
