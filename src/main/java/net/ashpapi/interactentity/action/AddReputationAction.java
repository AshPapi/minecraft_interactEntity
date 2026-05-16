package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.ReputationToastPacket;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class AddReputationAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.get("id").getAsString();
        int delta = params.get("value").getAsInt();

        // Проверяем наличие кастомного ярлыка в JSON действия
        String label = params.has("label") ? params.get("label").getAsString() : null;
        
        // Если ярлыка нет, пробуем взять его из текущей сессии диалога
        if (label == null) {
            DialogueSession session = DialogueSession.getSession(player);
            if (session != null) {
                // Если ID в действии совпадает с системным ID репутации диалога
                if (id.equals(session.getReputationId())) {
                    label = session.getFactionLabel(); // Берем "красивое" название
                }
            }
        }
        
        if (label == null) label = id;

        DialogueSavedData data = DialogueDataManager.get(player, params);
        data.addReputation(id, delta);
        ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));
        ModNetwork.sendToPlayer(player, new ReputationToastPacket(id, label, delta));
    }
}
