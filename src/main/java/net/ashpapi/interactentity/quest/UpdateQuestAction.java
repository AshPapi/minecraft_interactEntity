package net.ashpapi.interactentity.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.action.DialogueAction;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class UpdateQuestAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String questId = params.get("quest_id").getAsString();
        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        QuestState quest = data.getQuest(questId);
        if (quest != null && params.has("objectives")) {
            JsonArray arr = params.getAsJsonArray("objectives");
            List<String> objectives = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                objectives.add(arr.get(i).getAsString());
            }
            quest.setObjectives(objectives);
            data.setDirty();
            ModNetwork.sendToAll(new QuestUpdatePacket(quest));
        }
    }
}
