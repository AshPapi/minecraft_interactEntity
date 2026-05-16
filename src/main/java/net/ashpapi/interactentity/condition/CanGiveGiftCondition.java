package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class CanGiveGiftCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String characterId = params.get("character_id").getAsString();
        DialogueSavedData data = DialogueDataManager.get(player, params);
        return data.canGiveGift(characterId);
    }
}
