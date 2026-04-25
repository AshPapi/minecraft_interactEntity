package net.ashpapi.interactentity.summon;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.event.NPCJoinHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SummonScheduler {
    private static final List<ScheduledSummon> SCHEDULED = new ArrayList<>();
    private static final Set<String> TRIGGERED_DIALOGUES = new HashSet<>();

    public static void scheduleAfterDialogue(String completedDialogueId, ServerPlayer player) {
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;

        for (DialogueTree tree : manager.getAllDialogues()) {
            if (tree.getSummonConfig() == null) continue;

            SummonConfig config = SummonConfig.fromJson(tree.getSummonConfig());
            if (config == null) continue;

            SummonTrigger trigger = config.getTrigger();
            if (!"after_dialogue".equals(trigger.getType())) continue;
            if (!completedDialogueId.equals(trigger.getDialogueId())) continue;

            String key = tree.getId() + ":" + player.getUUID();
            if (!tree.isRepeatable()) {
                if (TRIGGERED_DIALOGUES.contains(key)) continue;
                DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
                if (data.hasVisited(tree.getId(), tree.getEntryNodeId())) continue;
                TRIGGERED_DIALOGUES.add(key);
            }

            SCHEDULED.add(new ScheduledSummon(tree, config, player, trigger.getDelay()));
            InteractEntityMod.LOGGER.debug("Scheduled summon for dialogue '{}' after '{}', delay={} ticks",
                    tree.getId(), completedDialogueId, trigger.getDelay());
        }
    }

    public static void scheduleNow(DialogueTree tree, SummonConfig config, ServerPlayer player) {
        String key = tree.getId() + ":" + player.getUUID();
        if (!tree.isRepeatable()) {
            if (TRIGGERED_DIALOGUES.contains(key)) return;
            DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
            if (data.hasVisited(tree.getId(), tree.getEntryNodeId())) return;
            TRIGGERED_DIALOGUES.add(key);
        }
        SCHEDULED.add(new ScheduledSummon(tree, config, player, config.getTrigger().getDelay()));
    }

    public static void scheduleOnJoin(ServerPlayer player) {
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;

        for (DialogueTree tree : manager.getAllDialogues()) {
            if (tree.getSummonConfig() == null) continue;

            SummonConfig config = SummonConfig.fromJson(tree.getSummonConfig());
            if (config == null) continue;

            SummonTrigger trigger = config.getTrigger();
            if (!"on_join".equals(trigger.getType())) continue;

            String key = tree.getId() + ":" + player.getUUID();
            if (!tree.isRepeatable()) {
                if (TRIGGERED_DIALOGUES.contains(key)) continue;
                DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
                if (data.hasVisited(tree.getId(), tree.getEntryNodeId())) continue;
                TRIGGERED_DIALOGUES.add(key);
            }

            SCHEDULED.add(new ScheduledSummon(tree, config, player, trigger.getDelay()));
            InteractEntityMod.LOGGER.debug("Scheduled summon for dialogue '{}' on join, delay={} ticks",
                    tree.getId(), trigger.getDelay());
        }
    }

    public static void tick() {
        DespawnHandler.tick();

        List<ScheduledSummon> toReschedule = new ArrayList<>();

        Iterator<ScheduledSummon> it = SCHEDULED.iterator();
        while (it.hasNext()) {
            ScheduledSummon summon = it.next();
            summon.ticksRemaining--;

            if (summon.ticksRemaining <= 0) {
                executeSummon(summon, toReschedule);
                it.remove();
            }
        }

        SCHEDULED.addAll(toReschedule);
    }

    private static void executeSummon(ScheduledSummon summon, List<ScheduledSummon> toReschedule) {
        ServerPlayer player = summon.player;
        if (!player.isAlive() || player.hasDisconnected()) return;

        // Don't summon if player is already in dialogue
        if (DialogueSession.hasActiveSession(player)) {
            // Re-schedule with short delay
            toReschedule.add(new ScheduledSummon(summon.tree, summon.config, player, 40));
            return;
        }

        ServerLevel level = player.serverLevel();
        SummonConfig config = summon.config;

        // Resolve entity type
        Optional<EntityType<?>> optType = EntityType.byString(config.getEntityType());
        if (optType.isEmpty()) {
            InteractEntityMod.LOGGER.error("Unknown entity type: {}", config.getEntityType());
            return;
        }

        // Find spawn position
        Vec3 spawnPos = SpawnPositionHelper.findBehindPlayer(player, level);
        if (spawnPos == null) {
            InteractEntityMod.LOGGER.warn("Could not find safe spawn position for summon");
            return;
        }

        // Spawn entity
        Entity entity = optType.get().create(level);
        if (!(entity instanceof LivingEntity livingEntity)) {
            InteractEntityMod.LOGGER.error("Summoned entity is not LivingEntity: {}", config.getEntityType());
            return;
        }

        entity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);

        // Set custom name
        entity.setCustomName(Component.literal(config.getCustomName()));
        entity.setCustomNameVisible(true);

        // Add scoreboard tags
        for (String tag : config.getTags()) {
            entity.addTag(tag);
        }

        // Face the player
        if (entity instanceof Mob mob) {
            float dx = (float)(player.getX() - spawnPos.x);
            float dz = (float)(player.getZ() - spawnPos.z);
            float yaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
            // Disable AI aggression for dialogue mobs
            mob.setNoAi(false);
            mob.setPersistenceRequired();
        }

        // Пометить как NPC до добавления в мир
        NPCJoinHandler.setupNPC(livingEntity, summon.tree);

        level.addFreshEntity(entity);
        InteractEntityMod.LOGGER.debug("Summoned {} at ({}, {}, {}) for dialogue '{}'",
                config.getEntityType(), spawnPos.x, spawnPos.y, spawnPos.z, summon.tree.getId());

        // Store despawn config on the entity for later
        if (config.isDespawnAfterDialogue()) {
            entity.addTag("interactentity_despawn");
            if (config.isWalkAwayBeforeDespawn()) {
                entity.addTag("interactentity_walkaway");
            }
        }

    }

    public static void clearAll() {
        SCHEDULED.clear();
        TRIGGERED_DIALOGUES.clear();
        DespawnHandler.clear();
    }

    private static class ScheduledSummon {
        final DialogueTree tree;
        final SummonConfig config;
        final ServerPlayer player;
        int ticksRemaining;

        ScheduledSummon(DialogueTree tree, SummonConfig config, ServerPlayer player, int ticksRemaining) {
            this.tree = tree;
            this.config = config;
            this.player = player;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
