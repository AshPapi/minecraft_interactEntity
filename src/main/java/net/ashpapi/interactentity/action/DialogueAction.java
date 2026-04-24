package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface DialogueAction {
    void execute(ServerPlayer player, LivingEntity entity, JsonObject params);
}
