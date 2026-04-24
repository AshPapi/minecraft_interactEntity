package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class HungerBelowCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        return player.getFoodData().getFoodLevel() < params.get("value").getAsInt();
    }
}
