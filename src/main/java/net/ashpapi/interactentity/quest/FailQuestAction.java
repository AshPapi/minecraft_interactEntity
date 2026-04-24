package net.ashpapi.interactentity.quest;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.action.DialogueAction;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class FailQuestAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String questId = params.get("quest_id").getAsString();
        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        QuestState quest = data.getQuest(questId);
        if (quest != null) {
            quest.setStatus("failed");
            data.setDirty();
            ModNetwork.sendToAll(new QuestUpdatePacket(quest));
        }
    }
}