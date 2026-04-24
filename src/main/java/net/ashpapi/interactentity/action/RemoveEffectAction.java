package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class RemoveEffectAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        LivingEntity target = "entity".equals(params.has("target") ? params.get("target").getAsString() : "player") ? entity : player;
        if (params.has("effect")) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(params.get("effect").getAsString()));
            if (effect != null) target.removeEffect(effect);
        } else {
            target.removeAllEffects();
        }
    }
}
