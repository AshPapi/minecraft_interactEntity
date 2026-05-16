package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.ReputationToastPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class GiveGiftAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String characterId = params.get("character_id").getAsString();
        String itemId = params.get("item").getAsString();
        int amount = params.has("amount") ? params.get("amount").getAsInt() : 1;
        int repGain = params.has("reputation") ? params.get("reputation").getAsInt() : 5;
        String label = params.has("label") ? params.get("label").getAsString() : characterId;

        DialogueSavedData data = DialogueDataManager.get(player, params);

        // 1. Проверка кулдауна (1 час)
        if (!data.canGiveGift(characterId)) {
            long remaining = data.getGiftCooldownRemaining(characterId);
            long minutes = (remaining / 1000) / 60;
            long seconds = (remaining / 1000) % 60;
            
            String msgKey = params.has("cooldown_message") ? params.get("cooldown_message").getAsString() : null;
            if (msgKey != null) {
                player.sendSystemMessage(Component.literal(msgKey).withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.literal("Вы уже дарили подарок этому персонажу! Приходите через " + minutes + " мин. " + seconds + " сек.")
                        .withStyle(ChatFormatting.RED));
            }
            return;
        }

        // 2. Проверка наличия предмета
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null) return;

        int totalFound = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item && !stack.hasTag()) {
                totalFound += stack.getCount();
            }
        }

        if (totalFound < amount) {
            player.sendSystemMessage(Component.literal("У вас нет нужного предмета (" + amount + " шт.)!").withStyle(ChatFormatting.RED));
            return;
        }

        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item && !stack.hasTag()) {
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                remaining -= toTake;
            }
        }

        // 3. Выдача награды и запись кулдауна
        data.recordGift(characterId);
        data.addReputation(characterId, repGain);
        
        // Уведомление
        ModNetwork.sendToPlayer(player, new ReputationToastPacket(characterId, label, repGain));
        
        String successMsg = params.has("success_message") ? params.get("success_message").getAsString() : null;
        if (successMsg != null) {
            player.sendSystemMessage(Component.literal(successMsg).withStyle(ChatFormatting.GREEN));
        }
    }
}
