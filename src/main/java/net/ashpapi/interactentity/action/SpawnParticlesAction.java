package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class SpawnParticlesAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.get("particle").getAsString();
        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(id));
        if (!(type instanceof SimpleParticleType simple)) return;
        int count = params.has("count") ? params.get("count").getAsInt() : 20;
        double spread = params.has("spread") ? params.get("spread").getAsDouble() : 0.5;
        double speed = params.has("speed") ? params.get("speed").getAsDouble() : 0.0;
        LivingEntity target = "player".equals(params.has("target") ? params.get("target").getAsString() : "entity") ? player : entity;
        double x = target.getX(), y = target.getY() + target.getBbHeight() / 2, z = target.getZ();
        player.serverLevel().sendParticles((ParticleOptions) simple, x, y, z, count, spread, spread, spread, speed);
    }
}
