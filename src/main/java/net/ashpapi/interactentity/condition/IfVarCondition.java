package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class IfVarCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String name = params.get("name").getAsString();
        String op = params.has("op") ? params.get("op").getAsString() : "eq";
        String expected = params.has("value") ? params.get("value").getAsString() : "";
        String actual = DialogueSavedData.get(player.serverLevel()).getVar(name);
        return switch (op) {
            case "eq" -> actual.equals(expected);
            case "neq" -> !actual.equals(expected);
            case "gt", "lt", "gte", "lte" -> {
                try {
                    int a = Integer.parseInt(actual.isEmpty() ? "0" : actual);
                    int e = Integer.parseInt(expected);
                    yield switch (op) {
                        case "gt" -> a > e;
                        case "lt" -> a < e;
                        case "gte" -> a >= e;
                        case "lte" -> a <= e;
                        default -> false;
                    };
                } catch (NumberFormatException ex) { yield false; }
            }
            case "exists" -> !actual.isEmpty();
            default -> false;
        };
    }
}
