package net.ashpapi.interactentity.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.event.DelayedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class ScheduleEventAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.has("id") ? params.get("id").getAsString() : "event_" + System.currentTimeMillis();
        long delay = params.get("delay").getAsLong(); // in ticks

        List<JsonObject> actions = new ArrayList<>();
        JsonArray arr = params.getAsJsonArray("actions");
        for (JsonElement el : arr) {
            actions.add(el.getAsJsonObject());
        }

        long fireTick = player.serverLevel().getGameTime() + delay;
        DelayedEvent event = new DelayedEvent(id, fireTick, actions, player.getUUID());

        DialogueSavedData data = DialogueDataManager.get(player, params);
        data.addDelayedEvent(event);
    }
}
