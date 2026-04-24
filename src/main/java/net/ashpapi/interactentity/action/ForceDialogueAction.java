package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ForceDialogueAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.get("dialogue_id").getAsString();
        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) return;
        DialogueTree tree = mgr.getDialogueById(id);
        if (tree == null) return;

        LivingEntity target = entity;
        if (params.has("target_tag")) {
            String tag = params.get("target_tag").getAsString();
            double r = params.has("radius") ? params.get("radius").getAsDouble() : 32.0;
            AABB box = player.getBoundingBox().inflate(r);
            List<LivingEntity> found = player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.getTags().contains(tag));
            if (!found.isEmpty()) target = found.get(0);
        }

        String startNode = params.has("start_node") ? params.get("start_node").getAsString() : tree.getEntryNodeId();
        if (DialogueSession.hasActiveSession(player)) DialogueSession.endSession(player);
        DialogueSession.startSessionFromNode(player, target, tree, startNode);
    }
}
