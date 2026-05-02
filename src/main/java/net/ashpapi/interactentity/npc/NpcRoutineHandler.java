package net.ashpapi.interactentity.npc;

import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

public class NpcRoutineHandler {

    public static void tick(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        if (mob.isNoAi()) return;
        if (DialogueSession.isEntityBusy(entity)) return;

        // Проверяем раз в 3 секунды
        if (entity.tickCount % 60 != 0) return;

        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;

        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree == null) return;

        List<NpcRoutine> routines = tree.getRoutines();
        if (routines == null || routines.isEmpty()) return;

        long dayTime = entity.level().getDayTime();

        for (NpcRoutine routine : routines) {
            if (!routine.isActiveAt(dayTime)) continue;
            executeRoutine(mob, routine);
            return;
        }
    }

    private static void executeRoutine(Mob mob, NpcRoutine routine) {
        switch (routine.getType()) {
            case "idle_at" -> {
                BlockPos target = routine.getPosition();
                if (target == null) return;
                double distSq = mob.blockPosition().distSqr(target);
                if (distSq > 4.0) {
                    mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.8);
                }
            }
            case "wander" -> {
                if (mob.getNavigation().isDone()) {
                    BlockPos home = routine.getPosition() != null ? routine.getPosition() : mob.blockPosition();
                    int r = routine.getRadius();
                    int dx = mob.getRandom().nextInt(r * 2 + 1) - r;
                    int dz = mob.getRandom().nextInt(r * 2 + 1) - r;
                    BlockPos wanderTarget = home.offset(dx, 0, dz);
                    mob.getNavigation().moveTo(wanderTarget.getX() + 0.5, wanderTarget.getY(), wanderTarget.getZ() + 0.5, 0.6);
                }
            }
            case "patrol" -> {
                List<BlockPos> waypoints = routine.getWaypoints();
                if (waypoints.isEmpty()) return;
                if (mob.getNavigation().isDone()) {
                    // Берём ближайший вейпоинт, к которому ещё не пришли
                    int currentIdx = mob.getPersistentData().getInt("InteractEntity_PatrolIdx");
                    if (currentIdx >= waypoints.size()) currentIdx = 0;
                    BlockPos wp = waypoints.get(currentIdx);
                    double distSq = mob.blockPosition().distSqr(wp);
                    if (distSq < 4.0) {
                        currentIdx = (currentIdx + 1) % waypoints.size();
                        mob.getPersistentData().putInt("InteractEntity_PatrolIdx", currentIdx);
                        wp = waypoints.get(currentIdx);
                    }
                    mob.getNavigation().moveTo(wp.getX() + 0.5, wp.getY(), wp.getZ() + 0.5, 0.7);
                }
            }
        }
    }
}
