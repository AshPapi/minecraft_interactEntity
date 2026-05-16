package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetVarAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String name = params.get("name").getAsString();
        String value = params.has("value") ? params.get("value").getAsString() : "";
        String op = params.has("op") ? params.get("op").getAsString() : "set";
        DialogueSavedData data = DialogueDataManager.get(player, params);
        switch (op) {
            case "set" -> data.setVar(name, value);
            case "inc" -> {
                try {
                    int cur = Integer.parseInt(data.getVar(name).isEmpty() ? "0" : data.getVar(name));
                    int delta = value.isEmpty() ? 1 : Integer.parseInt(value);
                    data.setVar(name, String.valueOf(cur + delta));
                } catch (NumberFormatException ignored) {}
            }
            case "dec" -> {
                try {
                    int cur = Integer.parseInt(data.getVar(name).isEmpty() ? "0" : data.getVar(name));
                    int delta = value.isEmpty() ? 1 : Integer.parseInt(value);
                    data.setVar(name, String.valueOf(cur - delta));
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}
