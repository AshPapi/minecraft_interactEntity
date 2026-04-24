package net.ashpapi.interactentity.summon;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class DespawnHandler {
    private static final Map<UUID, DespawnTask> PENDING = new HashMap<>();

    public static void scheduleDespawn(LivingEntity entity, boolean walkAway) {
        if (walkAway && entity instanceof Mob mob) {
            // Make the mob walk away from its current position
            Vec3 pos = mob.position();
            Vec3 away = pos.add(mob.getLookAngle().reverse().scale(10));
            mob.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
        }
        // Despawn after 80 ticks (4 seconds) - gives time to walk away
        int delayTicks = walkAway ? 80 : 20;
        PENDING.put(entity.getUUID(), new DespawnTask(entity, delayTicks));
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, DespawnTask>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DespawnTask> entry = it.next();
            DespawnTask task = entry.getValue();
            task.ticksRemaining--;

            if (task.ticksRemaining <= 0) {
                LivingEntity entity = task.entity;
                if (entity.isAlive() && entity.level() instanceof ServerLevel serverLevel) {
                    // Spawn particles at entity position
                    serverLevel.sendParticles(ParticleTypes.PORTAL,
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                            20, 0.3, 0.5, 0.3, 0.1);
                    entity.discard();
                    InteractEntityMod.LOGGER.debug("Despawned summoned entity: {}", entity.getUUID());
                }
                it.remove();
            }
        }
    }

    public static void clear() {
        PENDING.clear();
    }

    private static class DespawnTask {
        final LivingEntity entity;
        int ticksRemaining;

        DespawnTask(LivingEntity entity, int ticksRemaining) {
            this.entity = entity;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
