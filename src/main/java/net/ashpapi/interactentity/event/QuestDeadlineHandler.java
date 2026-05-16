package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.ashpapi.interactentity.network.TrackedQuestsPacket;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestDeadlineHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return; // проверка раз в секунду

        long gameTime = server.overworld().getGameTime();

        checkDeadlines(DialogueDataManager.getGlobal(server.overworld()), gameTime);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DialogueSavedData playerData = DialogueDataManager.getPlayer(player);
            if (playerData != null) checkDeadlines(playerData, gameTime);
        }
    }

    private static void checkDeadlines(DialogueSavedData data, long gameTime) {
        boolean changed = false;
        for (QuestState quest : data.getActiveQuests()) {
            if (!quest.hasDeadline()) continue;
            if (gameTime >= quest.getDeadlineTick()) {
                quest.setStatus("failed");
                data.untrackQuest(quest.getId());
                changed = true;
                ModNetwork.sendToAll(new QuestUpdatePacket(quest));
                InteractEntityMod.LOGGER.debug("Quest '{}' failed by deadline", quest.getId());
            }
        }
        if (changed) {
            data.setDirty();
            ModNetwork.sendToAll(new TrackedQuestsPacket(data.getTrackedQuestIds()));
        }
    }
}
