package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class VisitedNodeCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String dialogue = params.get("dialogue").getAsString();
        String node = params.get("node").getAsString();
        DialogueSavedData data = DialogueDataManager.get(player, params);
        return data.hasVisited(dialogue, node);
    }
}
