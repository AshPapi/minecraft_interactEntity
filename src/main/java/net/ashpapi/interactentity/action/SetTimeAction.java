package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetTimeAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String v = params.get("time").getAsString();
        long time = switch (v) {
            case "day" -> 1000L;
            case "noon" -> 6000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            default -> {
                try { yield Long.parseLong(v); } catch (NumberFormatException e) { yield 1000L; }
            }
        };
        player.serverLevel().setDayTime(time);
    }
}
