package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class BiomeCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        ResourceLocation want = new ResourceLocation(params.get("biome").getAsString());
        ResourceLocation actual = player.serverLevel().getBiome(player.blockPosition()).unwrapKey()
                .map(k -> k.location()).orElse(null);
        return want.equals(actual);
    }
}
