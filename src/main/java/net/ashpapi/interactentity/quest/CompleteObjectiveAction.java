package net.ashpapi.interactentity.quest;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.action.DialogueAction;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class CompleteObjectiveAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String questId = params.get("quest_id").getAsString();
        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        QuestState quest = data.getQuest(questId);
        if (quest == null) {
            InteractEntityMod.LOGGER.warn("Attempted to complete objective for unknown quest '{}'", questId);
            return;
        }

        boolean completed = false;
        if (params.has("objective_number")) {
            completed = quest.completeObjective(params.get("objective_number").getAsInt() - 1);
        } else if (params.has("objective")) {
            completed = quest.completeObjective(params.get("objective").getAsInt());
        } else if (params.has("objective_text")) {
            completed = quest.completeObjective(params.get("objective_text").getAsString());
        }

        if (completed) {
            data.setDirty();
            ModNetwork.sendToAll(new QuestUpdatePacket(quest));
            InteractEntityMod.LOGGER.debug("Objective completed for quest '{}' by player {}", questId, player.getName().getString());
        } else {
            InteractEntityMod.LOGGER.warn("Could not complete objective for quest '{}': {}", questId, params);
        }
    }
}
