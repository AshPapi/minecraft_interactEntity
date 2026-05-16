package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class NotifyNpcAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject json) {
        String dialogueId = json.get("dialogue_id").getAsString();
        DialogueManager mgr = DialogueManager.get();
        if (mgr != null && mgr.getDialogueById(dialogueId) == null) {
            InteractEntityMod.LOGGER.warn("notify_npc: unknown dialogue_id '{}' — notification will hang until that dialogue exists", dialogueId);
        }
        DialogueSavedData data = DialogueDataManager.getGlobal(player.serverLevel());
        data.addNotification(dialogueId);
        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(online, SyncProgressPacket.createFor(online));
        }
    }
}
