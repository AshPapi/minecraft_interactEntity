package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueTree;
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
    }

    /** Задать домашнюю позицию и радиус для NPC. */
    public static void setHome(LivingEntity entity, net.minecraft.core.BlockPos pos, int radius) {
        entity.getPersistentData().putInt("InteractEntity_HomeX", pos.getX());
        entity.getPersistentData().putInt("InteractEntity_HomeY", pos.getY());
        entity.getPersistentData().putInt("InteractEntity_HomeZ", pos.getZ());
        entity.getPersistentData().putInt("InteractEntity_HomeRadius", radius);
    }
}
