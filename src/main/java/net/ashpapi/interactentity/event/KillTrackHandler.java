package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KillTrackHandler {
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        String typeId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();
        Set<String> tags = event.getEntity().getTags();

        DialogueSavedData globalData = DialogueDataManager.getGlobal(player.serverLevel());
        DialogueSavedData playerData = DialogueDataManager.getPlayer(player);

        recordKill(globalData, typeId, tags);
        if (playerData != null) recordKill(playerData, typeId, tags);

        updateKillQuests(globalData, typeId, tags);
        if (playerData != null) updateKillQuests(playerData, typeId, tags);
    }

    private static void recordKill(DialogueSavedData data, String typeId, Set<String> tags) {
        data.addKill(typeId);
        for (String tag : tags) data.addKill(typeId + "#" + tag);
    }

    private static void updateKillQuests(DialogueSavedData data, String typeId, Set<String> tags) {
        for (QuestState quest : data.getActiveQuests()) {
            String questEntityType = quest.getKillEntityType();
            if (questEntityType == null) continue;

            boolean matches;
            if (questEntityType.contains("#")) {
                String[] parts = questEntityType.split("#", 2);
                matches = typeId.equals(parts[0]) && tags.contains(parts[1]);
            } else {
                matches = typeId.equals(questEntityType);
            }

            if (matches) {
                quest.addKillProgress(1);
                data.setDirty();
                ModNetwork.sendToAll(new QuestUpdatePacket(quest));
            }
        }
    }
}
