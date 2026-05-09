package net.ashpapi.interactentity.event;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DialogueTriggerHandler {
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final int PROXIMITY_COOLDOWN_TICKS = 200; // 10 seconds

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity target = event.getEntity();
            checkTrigger(player, target, "on_hurt");
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity target = event.getEntity();
            checkTrigger(player, target, "on_death");
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;
        if (event.getServer().getTickCount() % 10 != 0) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (DialogueSession.hasActiveSession(player)) continue;

            player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0))
                .forEach(entity -> checkTrigger(player, entity, "proximity"));
        }

        // Cleanup old cooldowns occasionally
        if (event.getServer().getTickCount() % 1200 == 0) {
            COOLDOWNS.entrySet().removeIf(e -> event.getServer().getTickCount() > e.getValue());
        }
    }

    private static void checkTrigger(ServerPlayer player, LivingEntity target, String triggerType) {
        if (DialogueSession.hasActiveSession(player)) return;

        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;

        DialogueTree tree = manager.findDialogueForEntity(target);
        if (tree == null) return;

        JsonObject trigger = tree.getStartTrigger();
        if (trigger == null) return;

        String type = trigger.has("type") ? trigger.get("type").getAsString() : "";
        if (!type.equals(triggerType)) return;

        double maxDistance = trigger.has("radius") ? trigger.get("radius").getAsDouble() : 4.0;
        if (player.distanceToSqr(target) > maxDistance * maxDistance) return;

        if (triggerType.equals("proximity")) {
            UUID id = player.getUUID();
            long now = player.getServer().getTickCount();
            if (COOLDOWNS.getOrDefault(id, 0L) > now) return;

            // Set cooldown
            COOLDOWNS.put(id, now + PROXIMITY_COOLDOWN_TICKS);
        }

        // Start dialogue
        EntityInteractHandler.startDialogue(player, target);
    }
}
