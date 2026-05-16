package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.action.ActionRegistry;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DelayedEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;

        long gameTime = server.overworld().getGameTime();
        DialogueSavedData data = DialogueSavedData.get(server.overworld());
        List<DelayedEvent> fired = new ArrayList<>();

        for (DelayedEvent ev : data.getDelayedEvents()) {
            if (gameTime >= ev.getFireTick()) {
                fired.add(ev);
            }
        }

        if (fired.isEmpty()) return;

        for (DelayedEvent ev : fired) {
            ServerPlayer targetPlayer = null;
            if (ev.getPlayerUuid() != null) {
                targetPlayer = server.getPlayerList().getPlayer(ev.getPlayerUuid());
            }

            if (targetPlayer != null) {
                data.removeDelayedEvent(ev);
                ActionRegistry.executeActions(ev.getActions(), targetPlayer, targetPlayer);
                InteractEntityMod.LOGGER.debug("Fired delayed event '{}' for player {}", ev.getId(), targetPlayer.getName().getString());
            } else {
                ev.setFireTick(gameTime + 100);
                data.setDirty();
                InteractEntityMod.LOGGER.debug("Delayed event '{}' postponed: player offline", ev.getId());
            }
        }
    }
}
