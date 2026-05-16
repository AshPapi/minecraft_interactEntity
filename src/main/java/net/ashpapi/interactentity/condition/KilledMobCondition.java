package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class KilledMobCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String type = params.get("entity").getAsString();
        if (params.has("tag")) {
            type = type + "#" + params.get("tag").getAsString();
        }
        int required = params.has("count") ? params.get("count").getAsInt() : 1;
        return DialogueDataManager.get(player, params).getKillCount(type) >= required;
    }
}
