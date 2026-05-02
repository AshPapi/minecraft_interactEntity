package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.quest.CompleteQuestAction;
import net.ashpapi.interactentity.quest.FailQuestAction;
import net.ashpapi.interactentity.quest.StartQuestAction;
import net.ashpapi.interactentity.quest.UpdateQuestAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionRegistry {
    private static final Map<String, DialogueAction> ACTIONS = new HashMap<>();

    public static void register(String type, DialogueAction action) {
        ACTIONS.put(type, action);
    }

    public static void executeActions(List<JsonObject> actions, ServerPlayer player, LivingEntity entity) {
        for (JsonObject actionJson : actions) {
            String type = actionJson.get("type").getAsString();
            DialogueAction action = ACTIONS.get(type);
            if (action != null) {
                try {
                    action.execute(player, entity, actionJson);
                } catch (Exception e) {
                    InteractEntityMod.LOGGER.error("Failed to execute action '{}': {}", type, e.getMessage());
                }
            } else {
                InteractEntityMod.LOGGER.warn("Unknown action type: {}", type);
            }
        }
    }

    public static void init() {
        register("give_item", new GiveItemAction());
        register("remove_item", new RemoveItemAction());
        register("run_command", new RunCommandAction());
        register("start_quest", new StartQuestAction());
        register("complete_quest", new CompleteQuestAction());
        register("fail_quest", new FailQuestAction());
        register("update_quest", new UpdateQuestAction());
        register("teleport", new TeleportAction());
        register("play_sound", new PlaySoundAction());
        register("give_effect", new GiveEffectAction());
        register("remove_effect", new RemoveEffectAction());
        register("spawn_particles", new SpawnParticlesAction());
        register("camera_shake", new CameraShakeAction());
        register("set_time", new SetTimeAction());
        register("set_weather", new SetWeatherAction());
        register("force_dialogue", new ForceDialogueAction());
        register("set_var", new SetVarAction());
        register("summon_npc", new SummonNpcAction());
        register("notify_npc", new NotifyNpcAction());
        register("add_reputation", new AddReputationAction());
        register("schedule_event", new ScheduleEventAction());
        register("set_relationship", new SetRelationshipAction());
        register("set_home", new SetHomeAction());
        register("set_companion", new SetCompanionAction());
        register("play_emote", new PlayEmoteAction());
    }
}
