package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (event.player.tickCount % 20 != 0) return; // Проверяем раз в секунду

        if (event.player instanceof ServerPlayer player) {
            checkItemQuests(player, null);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        checkItemQuests(player, ForgeRegistries.ITEMS.getKey(event.getStack().getItem()).toString());
    }

    private static void checkItemQuests(ServerPlayer player, String changedItemId) {
        checkItemQuestsInData(DialogueDataManager.getGlobal(player.serverLevel()), player, changedItemId);
        DialogueSavedData playerData = DialogueDataManager.getPlayer(player);
        if (playerData != null) checkItemQuestsInData(playerData, player, changedItemId);
    }

    private static void checkItemQuestsInData(DialogueSavedData data, ServerPlayer player, String changedItemId) {
        boolean dirty = false;
        for (QuestState quest : data.getActiveQuests()) {
            String requiredItem = quest.getRequiredItemId();
            if (requiredItem == null) continue;
            if (quest.isItemCollected()) continue;
            if (changedItemId != null && !requiredItem.equals(changedItemId)) continue;

            int totalInInventory = countItemInInventory(player, requiredItem);
            if (totalInInventory >= quest.getRequiredCount()) {
                quest.markItemCollected();
                quest.completeObjective(0);
                dirty = true;
                ModNetwork.sendToAll(new QuestUpdatePacket(quest));
            }
        }
        if (dirty) data.setDirty();
    }

    private static int countItemInInventory(ServerPlayer player, String itemId) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                if (id.equals(itemId)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }
}
