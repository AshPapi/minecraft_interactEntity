package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetRelationshipAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String npcA = params.get("npc_a").getAsString();
        String npcB = params.get("npc_b").getAsString();
        String type = params.get("relationship").getAsString();

        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        data.setRelationship(npcA, npcB, type);
    }
}
