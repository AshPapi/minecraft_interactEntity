package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetWeatherAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String w = params.get("weather").getAsString();
        int duration = params.has("duration") ? params.get("duration").getAsInt() : 6000;
        ServerLevel level = player.serverLevel();
        switch (w) {
            case "clear" -> level.setWeatherParameters(duration, 0, false, false);
            case "rain" -> level.setWeatherParameters(0, duration, true, false);
            case "thunder" -> level.setWeatherParameters(0, duration, true, true);
        }
    }
}
