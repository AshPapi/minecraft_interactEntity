package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.npc.CompanionHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetCompanionAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        boolean enable = !params.has("enable") || params.get("enable").getAsBoolean();
        if (enable) {
            CompanionHandler.setCompanion(entity, player);
        } else {
            CompanionHandler.removeCompanion(entity);
        }
    }
}
