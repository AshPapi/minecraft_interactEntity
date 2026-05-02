package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class AddReputationAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.get("id").getAsString();
        int delta = params.get("value").getAsInt();

        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        data.addReputation(id, delta);
        ModNetwork.sendToAll(new SyncProgressPacket(data));
    }
}
