package net.ashpapi.interactentity.npc;

import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

public class CompanionHandler {

    private static final double FOLLOW_RANGE = 10.0;
    private static final double TELEPORT_RANGE = 30.0;
    private static final String OWNER_KEY = "InteractEntity_CompanionOwner";

    public static void setCompanion(LivingEntity entity, ServerPlayer owner) {
        entity.getPersistentData().putUUID(OWNER_KEY, owner.getUUID());
        entity.addTag("interactentity_companion");
    }

    public static void removeCompanion(LivingEntity entity) {
        entity.getPersistentData().remove(OWNER_KEY);
        entity.removeTag("interactentity_companion");
    }

    public static boolean isCompanion(LivingEntity entity) {
        return entity.getPersistentData().hasUUID(OWNER_KEY);
    }

    public static void tick(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        if (mob.isNoAi()) return;
        if (DialogueSession.isEntityBusy(entity)) return;
        if (!isCompanion(entity)) return;

        // Проверяем раз в секунду
        if (entity.tickCount % 20 != 0) return;

        UUID ownerUUID = entity.getPersistentData().getUUID(OWNER_KEY);
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null || !owner.isAlive()) return;

        double dist = entity.distanceTo(owner);

        // Телепорт если слишком далеко
        if (dist > TELEPORT_RANGE) {
            BlockPos ownerPos = owner.blockPosition();
            entity.teleportTo(ownerPos.getX() + 0.5, ownerPos.getY(), ownerPos.getZ() + 0.5);
            return;
        }

        // Следовать за игроком если далеко
        if (dist > 4.0) {
            mob.getNavigation().moveTo(owner, 1.0);
        }
    }
}
