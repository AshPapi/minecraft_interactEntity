package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class GiveItemAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String itemId = params.get("item").getAsString();
        int count = params.has("count") ? params.get("count").getAsInt() : 1;

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item != null) {
            ItemStack stack = new ItemStack(item, count);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }
}
