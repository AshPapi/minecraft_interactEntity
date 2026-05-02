package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class NpcRelationshipCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String npcA = params.get("npc_a").getAsString();
        String npcB = params.get("npc_b").getAsString();
        String expected = params.get("relationship").getAsString();

        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        String actual = data.getRelationship(npcA, npcB);
        return expected.equals(actual);
    }
}
