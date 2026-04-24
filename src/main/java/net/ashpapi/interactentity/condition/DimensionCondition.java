package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class DimensionCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        ResourceLocation dim = new ResourceLocation(params.get("dimension").getAsString());
        return player.level().dimension().location().equals(dim);
    }
}
