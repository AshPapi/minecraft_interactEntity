package net.ashpapi.interactentity.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.minecraft.commands.arguments.EntityArgument;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.event.PeacefulMobHandler;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.NpcSyncPacket;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcCommand {

    private static final Map<String, String> NPC_ENTITY_MAP = Map.ofEntries(
        Map.entry("minecraft:zombie",           "interactentity:npc_zombie"),
        Map.entry("minecraft:skeleton",         "interactentity:npc_skeleton"),
        Map.entry("minecraft:spider",           "interactentity:npc_spider"),
        Map.entry("minecraft:creeper",          "interactentity:npc_creeper"),
        Map.entry("minecraft:enderman",         "interactentity:npc_enderman"),
        Map.entry("minecraft:witch",            "interactentity:npc_witch"),
        Map.entry("minecraft:piglin",           "interactentity:npc_piglin"),
        Map.entry("minecraft:zombified_piglin", "interactentity:npc_zombified_piglin"),
        Map.entry("minecraft:pillager",         "interactentity:npc_pillager"),
        Map.entry("minecraft:vindicator",       "interactentity:npc_vindicator"),
        Map.entry("minecraft:husk",             "interactentity:npc_husk"),
        Map.entry("minecraft:drowned",          "interactentity:npc_drowned"),
        Map.entry("minecraft:stray",            "interactentity:npc_stray"),
        Map.entry("minecraft:blaze",            "interactentity:npc_blaze"),
        Map.entry("minecraft:wither_skeleton",  "interactentity:npc_wither_skeleton"),
        Map.entry("minecraft:phantom",          "interactentity:npc_phantom"),
        Map.entry("minecraft:slime",            "interactentity:npc_slime"),
        Map.entry("minecraft:magma_cube",       "interactentity:npc_magma_cube"),
        Map.entry("minecraft:guardian",         "interactentity:npc_guardian"),
        Map.entry("minecraft:elder_guardian",   "interactentity:npc_elder_guardian"),
        Map.entry("minecraft:cave_spider",      "interactentity:npc_cave_spider"),
        Map.entry("minecraft:ravager",          "interactentity:npc_ravager"),
        Map.entry("minecraft:evoker",           "interactentity:npc_evoker"),
        Map.entry("minecraft:piglin_brute",     "interactentity:npc_piglin_brute"),
        Map.entry("minecraft:hoglin",           "interactentity:npc_hoglin"),
        Map.entry("minecraft:zoglin",           "interactentity:npc_zoglin"),
        Map.entry("minecraft:ghast",            "interactentity:npc_ghast"),
        Map.entry("minecraft:warden",           "interactentity:npc_warden"),
        Map.entry("minecraft:silverfish",       "interactentity:npc_silverfish"),
        Map.entry("minecraft:endermite",        "interactentity:npc_endermite"),
        Map.entry("minecraft:vex",              "interactentity:npc_vex"),
        Map.entry("minecraft:shulker",          "interactentity:npc_shulker")
    );

    private static final SuggestionProvider<CommandSourceStack> DIALOGUE_IDS = (ctx, builder) -> {
        DialogueManager mgr = DialogueManager.get();
        if (mgr != null) {
            for (String id : mgr.getLoadedIds()) builder.suggest(id);
        }
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("npc")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("spawn")
                    .then(Commands.argument("dialogue", StringArgumentType.string())
                        .suggests(DIALOGUE_IDS)
                        .executes(ctx -> spawn(ctx.getSource(),
                                StringArgumentType.getString(ctx, "dialogue"), null))
                    )
                )
                .then(Commands.literal("tag")
                    .then(Commands.argument("dialogue", StringArgumentType.string())
                        .suggests(DIALOGUE_IDS)
                        .executes(ctx -> tagNearest(ctx.getSource(),
                                StringArgumentType.getString(ctx, "dialogue")))
                    )
                )
                .then(Commands.literal("remove")
                    .executes(ctx -> removeNearest(ctx.getSource()))
                )
                .then(Commands.literal("list")
                    .executes(ctx -> listNearby(ctx.getSource(), 32.0))
                )
                .then(Commands.literal("set_model")
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("model_path", StringArgumentType.greedyString())
                            .executes(ctx -> setModel(ctx.getSource(),
                                    EntityArgument.getEntities(ctx, "targets"),
                                    StringArgumentType.getString(ctx, "model_path"))))))
                .then(Commands.literal("set_texture")
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("texture_path", StringArgumentType.greedyString())
                            .executes(ctx -> setTexture(ctx.getSource(),
                                    EntityArgument.getEntities(ctx, "targets"),
                                    StringArgumentType.getString(ctx, "texture_path"))))))
                .then(Commands.literal("set_scale")
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.1f, 5.0f))
                            .executes(ctx -> setScale(ctx.getSource(),
                                    EntityArgument.getEntities(ctx, "targets"),
                                    FloatArgumentType.getFloat(ctx, "scale"))))))
                .then(Commands.literal("set_name")
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> setName(ctx.getSource(),
                                    EntityArgument.getEntities(ctx, "targets"),
                                    StringArgumentType.getString(ctx, "name"))))))
        );
    }

    private static int setModel(CommandSourceStack src, java.util.Collection<? extends Entity> targets, String path) {
        int count = 0;
        for (Entity e : targets) {
            if (e instanceof CustomNpcEntity npc) { npc.setModelId(path); count++; }
        }
        final int n = count;
        src.sendSuccess(() -> Component.literal("Set model to '" + path + "' on " + n + " NPCs"), true);
        return n;
    }

    private static int setTexture(CommandSourceStack src, java.util.Collection<? extends Entity> targets, String path) {
        int count = 0;
        for (Entity e : targets) {
            if (e instanceof CustomNpcEntity npc) { npc.setTextureId(path); count++; }
        }
        final int n = count;
        src.sendSuccess(() -> Component.literal("Set texture to '" + path + "' on " + n + " NPCs"), true);
        return n;
    }

    private static int setScale(CommandSourceStack src, java.util.Collection<? extends Entity> targets, float scale) {
        int count = 0;
        for (Entity e : targets) {
            if (e instanceof CustomNpcEntity npc) { npc.setNpcScale(scale); count++; }
        }
        final int n = count;
        src.sendSuccess(() -> Component.literal("Set scale to " + scale + " on " + n + " NPCs"), true);
        return n;
    }

    private static int setName(CommandSourceStack src, java.util.Collection<? extends Entity> targets, String name) {
        int count = 0;
        for (Entity e : targets) {
            if (e instanceof LivingEntity living) {
                living.setCustomName(Component.literal(name));
                living.setCustomNameVisible(true);
                count++;
            }
        }
        final int n = count;
        src.sendSuccess(() -> Component.literal("Set name to '" + name + "' on " + n + " entities"), true);
        return n;
    }

    private static int spawn(CommandSourceStack src, String dialogueId, String entityOverride) {
        try {
        return spawnInternal(src, dialogueId, entityOverride);
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("[NpcCommand] spawn failed", e);
            src.sendFailure(Component.literal("NPC spawn error: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
            return 0;
        }
    }

    @SuppressWarnings("deprecation")
    private static int spawnInternal(CommandSourceStack src, String dialogueId, String entityOverride) {
        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) { src.sendFailure(Component.literal("Dialogues not loaded")); return 0; }

        DialogueTree tree = mgr.getDialogueById(dialogueId);
        if (tree == null) { src.sendFailure(Component.literal("Unknown dialogue: " + dialogueId)); return 0; }

        String rawEntityId = entityOverride != null ? entityOverride : extractEntityType(tree);
        String entityId = NPC_ENTITY_MAP.getOrDefault(rawEntityId, rawEntityId);
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityId));
        if (type == null) { src.sendFailure(Component.literal("Unknown entity: " + entityId)); return 0; }

        ServerLevel level = src.getLevel();
        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity living)) {
            src.sendFailure(Component.literal("Entity is not living: " + entityId));
            return 0;
        }

        living.moveTo(src.getPosition().x, src.getPosition().y, src.getPosition().z,
                src.getRotation().y, 0);
        living.setCustomName(Component.literal(tree.getTarget().getName()));
        living.setCustomNameVisible(true);
        living.addTag(tree.getTarget().getTag());
        living.addTag(PeacefulMobHandler.NPC_TAG_PREFIX + tree.getId() + ":" + tree.getEntryNodeId());
        living.getPersistentData().putBoolean("InteractEntity_NPC", true);

        if (living instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.COMMAND, null, null);
        }
        if (tree.isInvulnerable()) {
            living.setInvulnerable(true);
        }

        // Apply visual config (texture/model/scale) for CustomNpcEntity.
        if (living instanceof CustomNpcEntity customNpc) {
            JsonObject visual = tree.getVisualConfig();
            if (visual != null) {
                if (visual.has("texture")) customNpc.setTextureId(visual.get("texture").getAsString());
                if (visual.has("model")) customNpc.setModelId(visual.get("model").getAsString());
                if (visual.has("scale")) customNpc.setNpcScale(visual.get("scale").getAsFloat());
            }
        }

        level.addFreshEntity(living);
        DialogueSavedData data = net.ashpapi.interactentity.data.DialogueDataManager.getGlobal(level);
        data.removeVisit(tree.getId());
        data.clearResumeNode(tree.getId());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            // Also clear per-player visit for this dialogue
            DialogueSavedData pData = net.ashpapi.interactentity.data.DialogueDataManager.getPlayer(p);
            if (pData != null) {
                pData.removeVisit(tree.getId());
                pData.clearResumeNode(tree.getId());
            }
            ModNetwork.sendToPlayer(p, SyncProgressPacket.createFor(p));
        }
        ModNetwork.sendToTracking(living, new NpcSyncPacket(living.getId(), tree.getId(), tree.getEntryNodeId()));
        src.sendSuccess(() -> Component.literal("Spawned NPC: " + tree.getTarget().getName()
                + " (" + entityId + ") for dialogue " + dialogueId), true);
        return 1;
    }

    private static int tagNearest(CommandSourceStack src, String dialogueId) {
        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) { src.sendFailure(Component.literal("Dialogues not loaded")); return 0; }
        DialogueTree tree = mgr.getDialogueById(dialogueId);
        if (tree == null) { src.sendFailure(Component.literal("Unknown dialogue: " + dialogueId)); return 0; }

        LivingEntity target = findNearestMob(src, 6.0);
        if (target == null) { src.sendFailure(Component.literal("No mob within 6 blocks")); return 0; }

        target.setCustomName(Component.literal(tree.getTarget().getName()));
        target.setCustomNameVisible(true);
        target.addTag(tree.getTarget().getTag());
        target.addTag(PeacefulMobHandler.NPC_TAG_PREFIX + tree.getId() + ":" + tree.getEntryNodeId());
        target.getPersistentData().putBoolean("InteractEntity_NPC", true);
        if (target instanceof Mob mob) mob.setPersistenceRequired();
        if (tree.isInvulnerable()) target.setInvulnerable(true);

        ModNetwork.sendToTracking(target, new NpcSyncPacket(target.getId(), tree.getId(), tree.getEntryNodeId()));
        src.sendSuccess(() -> Component.literal("Tagged " + target.getType().getDescription().getString()
                + " as " + tree.getTarget().getName()), true);
        return 1;
    }

    private static int removeNearest(CommandSourceStack src) {
        LivingEntity target = findNearestNpc(src, 10.0);
        if (target == null) { src.sendFailure(Component.literal("No NPC within 10 blocks")); return 0; }
        String name = target.getCustomName() != null ? target.getCustomName().getString()
                : target.getType().getDescription().getString();
        int id = target.getId();
        target.discard();
        ModNetwork.sendToTracking(target, new NpcSyncPacket(id, "", ""));
        src.sendSuccess(() -> Component.literal("Removed NPC: " + name), true);
        return 1;
    }

    private static int listNearby(CommandSourceStack src, double radius) {
        ServerLevel level = src.getLevel();
        AABB box = new AABB(src.getPosition().subtract(radius, radius, radius),
                            src.getPosition().add(radius, radius, radius));
        List<LivingEntity> npcs = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.getPersistentData().getBoolean("InteractEntity_NPC"));
        if (npcs.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Нет NPC в радиусе " + (int) radius + " блоков"), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("NPCs в радиусе " + (int) radius + ":"), false);
        for (LivingEntity npc : npcs) {
            String name = npc.getCustomName() != null ? npc.getCustomName().getString()
                    : npc.getType().getDescription().getString();
            String dlg = npc.getTags().stream()
                    .filter(t -> t.startsWith(PeacefulMobHandler.NPC_TAG_PREFIX))
                    .findFirst().map(t -> t.substring(PeacefulMobHandler.NPC_TAG_PREFIX.length()))
                    .orElse("—");
            int dist = (int) Math.sqrt(npc.distanceToSqr(src.getPosition()));
            src.sendSuccess(() -> Component.literal("  " + name + " [" + dlg + "] (" + dist + "m)"), false);
        }
        return npcs.size();
    }

    private static LivingEntity findNearestMob(CommandSourceStack src, double radius) {
        ServerLevel level = src.getLevel();
        AABB box = new AABB(src.getPosition().subtract(radius, radius, radius),
                            src.getPosition().add(radius, radius, radius));
        List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> !(e instanceof ServerPlayer));
        return mobs.stream()
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(src.getPosition()),
                        b.distanceToSqr(src.getPosition())))
                .orElse(null);
    }

    private static LivingEntity findNearestNpc(CommandSourceStack src, double radius) {
        ServerLevel level = src.getLevel();
        AABB box = new AABB(src.getPosition().subtract(radius, radius, radius),
                            src.getPosition().add(radius, radius, radius));
        List<LivingEntity> npcs = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.getPersistentData().getBoolean("InteractEntity_NPC"));
        return npcs.stream()
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(src.getPosition()),
                        b.distanceToSqr(src.getPosition())))
                .orElse(null);
    }

    private static String extractEntityType(DialogueTree tree) {
        // Prefer summon.entity, then target.entity_type from the dialogue JSON.
        JsonObject summon = tree.getSummonConfig();
        if (summon != null && summon.has("entity")) {
            return summon.get("entity").getAsString();
        }
        String targetType = tree.getTarget().getEntityType();
        if (targetType != null && !targetType.isEmpty()) {
            return targetType;
        }
        return "minecraft:zombie";
    }
}
