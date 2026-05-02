package net.ashpapi.interactentity.event;

import com.google.gson.JsonObject;
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
            data.removeDelayedEvent(ev);
            // Execute actions for all online players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ActionRegistry.executeActions(ev.getActions(), player, player);
            }
            InteractEntityMod.LOGGER.debug("Fired delayed event '{}'", ev.getId());
        }
    }
}
