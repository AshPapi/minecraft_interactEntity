package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class WeatherCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        ServerLevel l = player.serverLevel();
        String w = params.get("weather").getAsString();
        return switch (w) {
            case "clear" -> !l.isRaining() && !l.isThundering();
            case "rain" -> l.isRaining() && !l.isThundering();
            case "thunder" -> l.isThundering();
            default -> false;
        };
    }
}
