package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface DialogueCondition {
    boolean test(ServerPlayer player, LivingEntity entity, JsonObject params);
}
