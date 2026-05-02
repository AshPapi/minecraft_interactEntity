package net.ashpapi.interactentity.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.event.NPCJoinHandler;
import net.ashpapi.interactentity.summon.SpawnPositionHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SummonNpcAction implements DialogueAction {

    @Override
    public void execute(ServerPlayer player, LivingEntity currentEntity, JsonObject json) {
        String entityTypeStr = json.has("entity") ? json.get("entity").getAsString() : "minecraft:zombie";
        String name = json.has("name") ? json.get("name").getAsString() : "";
        boolean despawn = json.has("despawn") && json.get("despawn").getAsBoolean();
        boolean walkAway = json.has("walk_away") && json.get("walk_away").getAsBoolean();
        String dialogueId = json.has("start_dialogue") ? json.get("start_dialogue").getAsString() : null;
        String spawnPosition = json.has("spawn_position") ? json.get("spawn_position").getAsString() : "behind_player";

        List<String> tags = new ArrayList<>();
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("tags");
            for (int i = 0; i < arr.size(); i++) tags.add(arr.get(i).getAsString());
        }

        Optional<EntityType<?>> optType = EntityType.byString(entityTypeStr);
        if (optType.isEmpty()) {
            InteractEntityMod.LOGGER.error("summon_npc: unknown entity type '{}'", entityTypeStr);
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 spawnPos = SpawnPositionHelper.findForConfig(player, level, spawnPosition);
        if (spawnPos == null) {
            InteractEntityMod.LOGGER.warn("summon_npc: no safe spawn position found");
            return;
        }

        Entity entity = optType.get().create(level);
        if (!(entity instanceof LivingEntity living)) {
            InteractEntityMod.LOGGER.error("summon_npc: entity '{}' is not LivingEntity", entityTypeStr);
            return;
        }

        entity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
        if (!name.isEmpty()) {
            entity.setCustomName(Component.literal(name));
            entity.setCustomNameVisible(true);
        }
        for (String tag : tags) entity.addTag(tag);

        if (entity instanceof Mob mob) {
            float dx = (float) (player.getX() - spawnPos.x);
            float dz = (float) (player.getZ() - spawnPos.z);
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
            mob.setPersistenceRequired();
        }

        if (despawn) entity.addTag("interactentity_despawn");
        if (walkAway) entity.addTag("interactentity_walkaway");

        DialogueTree tree = null;
        if (dialogueId != null) {
            DialogueManager mgr = DialogueManager.get();
            if (mgr != null) tree = mgr.getDialogueById(dialogueId);
            if (tree == null) InteractEntityMod.LOGGER.warn("summon_npc: dialogue '{}' not found", dialogueId);
        }


        if (tree != null) NPCJoinHandler.setupNPC(living, tree);
        level.addFreshEntity(entity);

        if (tree != null && !DialogueSession.isEntityBusy(living)) {
            final DialogueTree finalTree = tree;
            if (DialogueSession.hasActiveSession(player)) DialogueSession.endSession(player);
            DialogueSession.startSession(player, living, finalTree);
        }
    }
}
