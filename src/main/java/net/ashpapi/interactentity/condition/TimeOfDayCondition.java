package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class TimeOfDayCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        long time = player.serverLevel().getDayTime() % 24000L;
        String period = params.has("period") ? params.get("period").getAsString() : "day";
        return switch (period) {
            case "day" -> time >= 0 && time < 12000;
            case "night" -> time >= 13000 && time < 23000;
            case "dawn" -> time >= 23000 || time < 1000;
            case "dusk" -> time >= 12000 && time < 13000;
            default -> false;
        };
    }
}
