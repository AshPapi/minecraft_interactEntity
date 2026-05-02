package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class QuestEventHandler {

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getStack();
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();

        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());

        for (QuestState quest : data.getActiveQuests()) {
            String requiredItem = quest.getRequiredItemId();
            if (requiredItem == null) continue;
            if (quest.isItemCollected()) continue;

            if (requiredItem.equals(itemId)) {
                int totalInInventory = countItemInInventory(player, itemId);
                if (totalInInventory >= quest.getRequiredCount()) {
                    quest.markItemCollected();

                    List<String> objectives = new ArrayList<>(quest.getObjectives());
                    if (!objectives.isEmpty()) {
                        String firstObjective = objectives.get(0);
                        String updated = firstObjective.replace("[ ]", "&a[✔]");
                        objectives.set(0, updated);
                    }
                    quest.setObjectives(objectives);
                    data.setDirty();
                    ModNetwork.sendToAll(new QuestUpdatePacket(quest));
                }
            }
        }
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
