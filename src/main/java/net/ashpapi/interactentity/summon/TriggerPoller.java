package net.ashpapi.interactentity.summon;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** Polls non-event triggers (player_near, looking, entered_area) each tick. */
@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TriggerPoller {

    /** player UUID + dialogueId -> ticks spent looking at position */
    private static final Map<String, Integer> LOOKING_TICKS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 10 != 0) return; // throttle

        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
            for (DialogueTree tree : mgr.getAllDialogues()) {
                if (tree.getSummonConfig() == null) continue;
                SummonConfig cfg = SummonConfig.fromJson(tree.getSummonConfig());
                if (cfg == null) continue;
                SummonTrigger tr = cfg.getTrigger();
                if (data.hasVisited(tree.getId(), tree.getEntryNodeId())) continue;

                switch (tr.getType()) {
                    case "player_near" -> {
                        Vec3 center = new Vec3(tr.getX(), tr.getY(), tr.getZ());
                        if (player.position().distanceToSqr(center) <= tr.getRadius() * tr.getRadius()) {
                            SummonScheduler.scheduleNow(tree, cfg, player);
                        }
                    }
                    case "player_entered_area" -> {
                        Vec3 center = new Vec3(tr.getX(), tr.getY(), tr.getZ());
                        double d2 = player.position().distanceToSqr(center);
                        double r2 = tr.getRadius() * tr.getRadius();
                        String key = player.getUUID() + ":" + tree.getId();
                        boolean wasIn = LOOKING_TICKS.getOrDefault(key, 0) > 0;
                        if (d2 <= r2 && !wasIn) {
                            LOOKING_TICKS.put(key, 1);
                            SummonScheduler.scheduleNow(tree, cfg, player);
                        } else if (d2 > r2 * 1.5) {
                            LOOKING_TICKS.remove(key);
                        }
                    }
                    case "player_looking_for_seconds" -> {
                        Vec3 target = new Vec3(tr.getX(), tr.getY(), tr.getZ());
                        Vec3 look = player.getLookAngle();
                        Vec3 toTarget = target.subtract(player.getEyePosition()).normalize();
                        double dot = look.dot(toTarget);
                        String key = player.getUUID() + ":" + tree.getId();
                        if (dot > 0.97 && player.position().distanceToSqr(target) < 64 * 64) {
                            int ticks = LOOKING_TICKS.getOrDefault(key, 0) + 10;
                            LOOKING_TICKS.put(key, ticks);
                            if (ticks >= tr.getSeconds() * 20) {
                                LOOKING_TICKS.remove(key);
                                SummonScheduler.scheduleNow(tree, cfg, player);
                            }
                        } else {
                            LOOKING_TICKS.remove(key);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) return;
        for (DialogueTree tree : mgr.getAllDialogues()) {
            if (tree.getSummonConfig() == null) continue;
            SummonConfig cfg = SummonConfig.fromJson(tree.getSummonConfig());
            if (cfg == null) continue;
            if (!"on_player_death".equals(cfg.getTrigger().getType())) continue;
            SummonScheduler.scheduleNow(tree, cfg, player);
        }
    }
}
