package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.quest.QuestStatusCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ConditionRegistry {
    private static final Map<String, DialogueCondition> CONDITIONS = new HashMap<>();

    public static void register(String type, DialogueCondition condition) {
        CONDITIONS.put(type, condition);
    }

    public static boolean check(@Nullable JsonObject conditionJson, ServerPlayer player, LivingEntity entity) {
        if (conditionJson == null) return true;

        String type = conditionJson.get("type").getAsString();
        DialogueCondition condition = CONDITIONS.get(type);
        if (condition != null) {
            return condition.test(player, entity, conditionJson);
        }

        InteractEntityMod.LOGGER.warn("Unknown condition type: {}", type);
        return true;
    }

    public static void init() {
        register("has_item", new HasItemCondition());
        register("visited_node", new VisitedNodeCondition());
        register("quest_status", new QuestStatusCondition());
        register("has_effect", new HasEffectCondition());
        register("health_below", new HealthBelowCondition());
        register("hunger_below", new HungerBelowCondition());
        register("time_of_day", new TimeOfDayCondition());
        register("weather", new WeatherCondition());
        register("dimension", new DimensionCondition());
        register("biome", new BiomeCondition());
        register("killed_mob", new KilledMobCondition());
        register("if_var", new IfVarCondition());
    }
}
