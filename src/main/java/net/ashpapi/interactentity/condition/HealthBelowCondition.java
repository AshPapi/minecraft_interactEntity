package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class HealthBelowCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        float v = params.get("value").getAsFloat();
        if (params.has("percent") && params.get("percent").getAsBoolean()) {
            return (player.getHealth() / player.getMaxHealth()) * 100f < v;
        }
        return player.getHealth() < v;
    }
}
