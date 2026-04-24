package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.NpcSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;
        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree != null && tree.isInvulnerable()) {
            event.setCanceled(true);
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

        // Fast path: NBT-флаг уже выставлен → не итерируем диалоги каждый тик
        if (entity.getPersistentData().getBoolean("InteractEntity_NPC")) {
            applyNPCProtections(entity);
            return;
        }

        // Slow path: первый тик — ищем диалог через manager
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;

        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree == null) return;

        // Пометить на будущее (NBT сохраняется с сущностью)
        entity.getPersistentData().putBoolean("InteractEntity_NPC", true);

        // Выставить тег для иконки над головой
        String tag = NPC_TAG_PREFIX + tree.getId() + ":" + tree.getEntryNodeId();
        entity.getTags().removeIf(t -> t.startsWith(NPC_TAG_PREFIX));
        entity.addTag(tag);

        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }

        applyNPCProtections(entity);

        // Broadcast NPC info to tracking clients for icon rendering
        ModNetwork.sendToTracking(entity, new NpcSyncPacket(entity.getId(), tree.getId(), tree.getEntryNodeId()));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel)) return;
        if (!(event.getTarget() instanceof LivingEntity entity)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;
        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree == null) return;
        ModNetwork.sendToPlayer(player, new NpcSyncPacket(entity.getId(), tree.getId(), tree.getEntryNodeId()));
    }

    private static void applyNPCProtections(LivingEntity entity) {
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