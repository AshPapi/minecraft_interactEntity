package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.api.DialogueChoiceEvent;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

/**
 * Posts a DialogueChoiceEvent with source="action" and the given tag.
 * JSON: { "type": "fire_event", "tag": "my_event_name" }
 */
public class FireEventAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String tag = params.has("tag") ? params.get("tag").getAsString() : "default";
        DialogueSession session = DialogueSession.getSession(player);
        String dialogueId = session != null ? session.getDialogueId() : "";
        String nodeId = session != null ? session.getCurrentNodeId() : "";
        MinecraftForge.EVENT_BUS.post(new DialogueChoiceEvent(player, entity, dialogueId, nodeId, "action", tag));
    }
}
