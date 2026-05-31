package net.ashpapi.interactentity.condition;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class HasItemCondition implements DialogueCondition {
    @Override
    public boolean test(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String itemId = params.get("item").getAsString();
        int requiredCount = params.has("count") ? params.get("count").getAsInt() : 1;

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null) return false;

        CompoundTag expectedTag = null;
        if (params.has("nbt")) {
            try {
                expectedTag = TagParser.parseTag(params.get("nbt").getAsString());
            } catch (CommandSyntaxException e) {
                InteractEntityMod.LOGGER.error("Failed to parse NBT string for has_item condition: " + params.get("nbt").getAsString(), e);
                return false;
            }
        }

        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                if (expectedTag != null) {
                    if (!stack.hasTag() || !NbtUtils.compareNbt(expectedTag, stack.getTag(), true)) {
                        continue;
                    }
                }
                total += stack.getCount();
                if (total >= requiredCount) return true;
            }
        }
        return false;
    }
}
