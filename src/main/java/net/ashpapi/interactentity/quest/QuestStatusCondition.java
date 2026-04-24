package net.ashpapi.interactentity.quest;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.condition.DialogueCondition;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class QuestStatusCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String questId = params.get("quest_id").getAsString();
        String expectedStatus = params.get("status").getAsString();
        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        return data.getQuestStatus(questId).equals(expectedStatus);
    }
}