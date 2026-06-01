package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.npc.CompanionHandler;
import net.ashpapi.interactentity.npc.NpcRoutineHandler;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.NpcSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PeacefulMobHandler {

    public static final String NPC_TAG_PREFIX = "interactentity:";

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        if (entity instanceof ServerPlayer player && DialogueSession.hasActiveSession(player)) {
            event.setCanceled(true);
            return;
        }

        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;
        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree != null && tree.isInvulnerable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        if (entity.getPersistentData().getBoolean("InteractEntity_NPC")) {
            if (entity.getPersistentData().getBoolean("InteractEntity_DisableAttacks")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        if (entity.getPersistentData().getBoolean("InteractEntity_NPC")) {
            if (entity.getPersistentData().getBoolean("InteractEntity_DisableKnockback")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;
        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree != null && tree.isInvulnerable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player) return;

        // Игнорируем обычных мобов, чтобы избежать утечки производительности (O(N*M) каждый тик)
        if (entity.getPersistentData().getBoolean("InteractEntity_NotNPC")) {
            return;
        }

        // Fast path: NBT-флаг уже выставлен → не итерируем диалоги каждый тик
        if (entity.getPersistentData().getBoolean("InteractEntity_NPC")) {
            boolean invulnerable = !entity.getPersistentData().contains("InteractEntity_Invulnerable")
                    || entity.getPersistentData().getBoolean("InteractEntity_Invulnerable");
            applyNPCProtections(entity, invulnerable);

            if (!entity.getPersistentData().contains("InteractEntity_DisableKnockback")) {
                DialogueManager manager = DialogueManager.get();
                DialogueTree tree = manager != null ? manager.findDialogueForEntity(entity) : null;
                boolean disableKnockback = tree != null && tree.isDisableKnockback();
                entity.getPersistentData().putBoolean("InteractEntity_DisableKnockback", disableKnockback);
            }

            if (!entity.getPersistentData().contains("InteractEntity_DisableAttacks")) {
                DialogueManager manager = DialogueManager.get();
                DialogueTree tree = manager != null ? manager.findDialogueForEntity(entity) : null;
                boolean disableAttacks = tree != null && tree.isDisableAttacks();
                entity.getPersistentData().putBoolean("InteractEntity_DisableAttacks", disableAttacks);
            }

            if (!entity.getPersistentData().contains("InteractEntity_ItemsTake")) {
                DialogueManager manager = DialogueManager.get();
                DialogueTree tree = manager != null ? manager.findDialogueForEntity(entity) : null;
                boolean itemsTake = tree == null || tree.isItemsTake();
                entity.getPersistentData().putBoolean("InteractEntity_ItemsTake", itemsTake);
            }

            reactToWeather(entity);
            returnHome(entity);
            NpcRoutineHandler.tick(entity);
            CompanionHandler.tick(entity);
            return;
        }

        // Slow path: первый тик — ищем диалог через manager
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;

        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree == null) {
            entity.getPersistentData().putBoolean("InteractEntity_NotNPC", true);
            return;
        }

        // Пометить на будущее (NBT сохраняется с сущностью)
        entity.getPersistentData().putBoolean("InteractEntity_NPC", true);
        entity.getPersistentData().putBoolean("InteractEntity_Invulnerable", tree.isInvulnerable());
        entity.getPersistentData().putBoolean("InteractEntity_DisableKnockback", tree.isDisableKnockback());
        entity.getPersistentData().putBoolean("InteractEntity_DisableAttacks", tree.isDisableAttacks());
        entity.getPersistentData().putBoolean("InteractEntity_ItemsTake", tree.isItemsTake());

        // Выставить тег для иконки над головой
        String tag = NPC_TAG_PREFIX + tree.getId() + ":" + tree.getEntryNodeId();
        entity.getTags().removeIf(t -> t.startsWith(NPC_TAG_PREFIX));
        entity.addTag(tag);

        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }

        applyNPCProtections(entity, tree.isInvulnerable());

        // Broadcast NPC info to tracking clients for icon rendering
        ModNetwork.sendToTracking(entity, new NpcSyncPacket(entity.getId(), tree.getId(), tree.getEntryNodeId()));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel)) return;
        if (!(event.getTarget() instanceof LivingEntity entity)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Быстрая проверка
        if (!entity.getPersistentData().getBoolean("InteractEntity_NPC")) return;

        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;
        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree == null) return;
        ModNetwork.sendToPlayer(player, new NpcSyncPacket(entity.getId(), tree.getId(), tree.getEntryNodeId()));
    }

    private static void applyNPCProtections(LivingEntity entity, boolean invulnerable) {
        if (!invulnerable) return;

        if (entity.isOnFire()) {
            entity.clearFire();
        }
        entity.setRemainingFireTicks(-1);
        if (entity.isInvulnerable()) return; // уже всё ок, не трогаем
        entity.setInvulnerable(true);
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
        }
    }

    private static void reactToWeather(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        if (mob.isNoAi()) return;
        if (DialogueSession.isEntityBusy(entity)) return;
        if (!entity.level().isRaining()) return;

        // Проверяем раз в 2 секунды
        if (entity.tickCount % 40 != 0) return;

        net.minecraft.core.BlockPos pos = entity.blockPosition();
        boolean exposed = entity.level().canSeeSky(pos);
        if (!exposed) return;

        // Ищем ближайший блок с крышей в радиусе 10
        net.minecraft.core.BlockPos shelter = findShelter(entity, 10);
        if (shelter != null) {
            mob.getNavigation().moveTo(shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, 0.7);
        }
    }

    private static void returnHome(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        if (mob.isNoAi()) return;
        if (DialogueSession.isEntityBusy(entity)) return;

        // Проверяем раз в 3 секунды
        if (entity.tickCount % 60 != 0) return;

        var nbt = entity.getPersistentData();
        if (!nbt.contains("InteractEntity_HomeX")) return;

        int hx = nbt.getInt("InteractEntity_HomeX");
        int hy = nbt.getInt("InteractEntity_HomeY");
        int hz = nbt.getInt("InteractEntity_HomeZ");
        int radius = nbt.getInt("InteractEntity_HomeRadius");

        double distSq = entity.blockPosition().distSqr(new net.minecraft.core.BlockPos(hx, hy, hz));
        if (distSq > (double) radius * radius) {
            mob.getNavigation().moveTo(hx + 0.5, hy, hz + 0.5, 0.7);
        }
    }

    @javax.annotation.Nullable
    private static net.minecraft.core.BlockPos findShelter(LivingEntity entity, int radius) {
        net.minecraft.core.BlockPos origin = entity.blockPosition();
        net.minecraft.core.BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                net.minecraft.core.BlockPos check = origin.offset(dx, 0, dz);
                if (!entity.level().canSeeSky(check) && entity.level().getBlockState(check).isAir()) {
                    double dist = origin.distSqr(check);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = check;
                    }
                }
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (DialogueSession.hasActiveSession(player)) {
            event.setCanceled(true);
            player.stopUsingItem();
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (DialogueSession.hasActiveSession(player)) {
            event.setCanceled(true);
            player.stopUsingItem();
        }
    }

    @SubscribeEvent
    public static void onEntityInteractDuringDialogue(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (DialogueSession.hasActiveSession(player)) {
            event.setCanceled(true);
        }
    }

}
