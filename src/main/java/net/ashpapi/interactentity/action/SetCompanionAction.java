package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.ashpapi.interactentity.npc.CompanionHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetCompanionAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        boolean enable = !params.has("enable") || params.get("enable").getAsBoolean();
        if (enable && !(entity instanceof CustomNpcEntity)) {
            InteractEntityMod.LOGGER.warn("set_companion: entity type '{}' is not interactentity:custom_npc — follow behaviour may not tick correctly",
                    entity.getType().getDescriptionId());
        }
        if (enable) {
            CompanionHandler.setCompanion(entity, player);
        } else {
            CompanionHandler.removeCompanion(entity);
        }
    }
}
