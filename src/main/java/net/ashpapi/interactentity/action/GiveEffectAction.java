package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class GiveEffectAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.get("effect").getAsString();
        int duration = params.has("duration") ? params.get("duration").getAsInt() : 200;
        int amplifier = params.has("amplifier") ? params.get("amplifier").getAsInt() : 0;
        boolean ambient = params.has("ambient") && params.get("ambient").getAsBoolean();
        boolean particles = !params.has("particles") || params.get("particles").getAsBoolean();
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
        if (effect == null) return;
        LivingEntity target = "entity".equals(params.has("target") ? params.get("target").getAsString() : "player") ? entity : player;
        target.addEffect(new MobEffectInstance(effect, duration, amplifier, ambient, particles));
    }
}
