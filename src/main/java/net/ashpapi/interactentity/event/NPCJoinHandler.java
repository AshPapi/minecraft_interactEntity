package net.ashpapi.interactentity.event;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NPCJoinHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;

        // Уже помечен как NPC в NBT (сохранилось с прошлой сессии)
        if (entity.getPersistentData().getBoolean("InteractEntity_NPC")) {
            if (entity instanceof Mob mob) mob.setPersistenceRequired();
            DialogueManager manager = DialogueManager.get();
            DialogueTree tree = manager != null ? manager.findDialogueForEntity(entity) : null;
            boolean invulnerable = tree != null
                    ? tree.isInvulnerable()
                    : !entity.getPersistentData().contains("InteractEntity_Invulnerable")
                    || entity.getPersistentData().getBoolean("InteractEntity_Invulnerable");
            entity.getPersistentData().putBoolean("InteractEntity_Invulnerable", invulnerable);
            entity.setInvulnerable(invulnerable);

            boolean disableKnockback = tree != null
                    ? tree.isDisableKnockback()
                    : entity.getPersistentData().getBoolean("InteractEntity_DisableKnockback");
            entity.getPersistentData().putBoolean("InteractEntity_DisableKnockback", disableKnockback);

            boolean disableAttacks = tree != null
                    ? tree.isDisableAttacks()
                    : entity.getPersistentData().getBoolean("InteractEntity_DisableAttacks");
            entity.getPersistentData().putBoolean("InteractEntity_DisableAttacks", disableAttacks);

            boolean itemsTake = tree != null
                    ? tree.isItemsTake()
                    : !entity.getPersistentData().contains("InteractEntity_ItemsTake")
                    || entity.getPersistentData().getBoolean("InteractEntity_ItemsTake");
            entity.getPersistentData().putBoolean("InteractEntity_ItemsTake", itemsTake);

            if (tree != null) {
                applyVisual(entity, tree);
                applyEquipment(entity, tree);
            }
            InteractEntityMod.LOGGER.debug("NPC re-joined (NBT flag): {}", entity.getName().getString());
            return;
        }

        // Первый спавн — ищем диалог через DialogueManager
        DialogueManager manager = DialogueManager.get();
        if (manager == null) return;

        DialogueTree tree = manager.findDialogueForEntity(entity);
        if (tree == null) return;

        setupNPC(entity, tree);
        InteractEntityMod.LOGGER.debug("NPC marked on join: {}", entity.getName().getString());
    }

    /** Общий метод настройки NPC, вызывается при join и при спавне через SummonScheduler. */
    public static void setupNPC(LivingEntity entity, DialogueTree tree) {
        entity.getPersistentData().putBoolean("InteractEntity_NPC", true);
        entity.getPersistentData().putBoolean("InteractEntity_Invulnerable", tree.isInvulnerable());
        entity.getPersistentData().putBoolean("InteractEntity_DisableKnockback", tree.isDisableKnockback());
        entity.getPersistentData().putBoolean("InteractEntity_DisableAttacks", tree.isDisableAttacks());
        entity.getPersistentData().putBoolean("InteractEntity_ItemsTake", tree.isItemsTake());

        entity.setInvulnerable(tree.isInvulnerable());

        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }

        // Тег для отображения иконки над головой
        String iconTag = PeacefulMobHandler.NPC_TAG_PREFIX + tree.getId() + ":" + tree.getEntryNodeId();
        entity.getTags().removeIf(t -> t.startsWith(PeacefulMobHandler.NPC_TAG_PREFIX));
        entity.addTag(iconTag);

        // Устанавливаем домашнюю позицию при первом спавне
        if (!entity.getPersistentData().contains("InteractEntity_HomeX")) {
            setHome(entity, entity.blockPosition(), 16);
        }

        applyVisual(entity, tree);
        applyEquipment(entity, tree);
    }

    /** Применяет visual.{model,texture,scale} из JSON диалога к CustomNpcEntity. */
    public static void applyVisual(LivingEntity entity, DialogueTree tree) {
        if (!(entity instanceof CustomNpcEntity customNpc)) return;
        JsonObject visual = tree.getVisualConfig();
        if (visual == null) return;
        if (visual.has("texture")) customNpc.setTextureId(visual.get("texture").getAsString());
        if (visual.has("model")) customNpc.setModelId(visual.get("model").getAsString());
        if (visual.has("scale")) customNpc.setNpcScale(visual.get("scale").getAsFloat());
    }

    /** Задать домашнюю позицию и радиус для NPC. */
    public static void setHome(LivingEntity entity, net.minecraft.core.BlockPos pos, int radius) {
        entity.getPersistentData().putInt("InteractEntity_HomeX", pos.getX());
        entity.getPersistentData().putInt("InteractEntity_HomeY", pos.getY());
        entity.getPersistentData().putInt("InteractEntity_HomeZ", pos.getZ());
        entity.getPersistentData().putInt("InteractEntity_HomeRadius", radius);
    }

    /** Применяет visual.equipment из JSON диалога к LivingEntity при первом спавне. */
    public static void applyEquipment(LivingEntity entity, DialogueTree tree) {
        if (entity.getPersistentData().getBoolean("InteractEntity_EquipmentApplied")) return;

        JsonObject visual = tree.getVisualConfig();
        if (visual != null && visual.has("equipment")) {
            JsonObject eq = visual.getAsJsonObject("equipment");
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                String key = slot.getName().toLowerCase(java.util.Locale.ROOT);
                if (eq.has(key)) {
                    String itemId = eq.get(key).getAsString();
                    if (itemId.isEmpty() || "minecraft:air".equals(itemId)) {
                        entity.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
                    } else {
                        net.minecraft.resources.ResourceLocation itemLoc = net.minecraft.resources.ResourceLocation.tryParse(itemId);
                        if (itemLoc != null) {
                            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemLoc);
                            if (item != null) {
                                entity.setItemSlot(slot, new net.minecraft.world.item.ItemStack(item));
                            }
                        }
                    }
                }
            }
        }
        entity.getPersistentData().putBoolean("InteractEntity_EquipmentApplied", true);
    }
}
