package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.condition.ConditionRegistry;
import net.ashpapi.interactentity.data.ClientNpcRegistry;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.dialogue.RevisitConfig;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.OpenDialoguePacket;
import net.ashpapi.interactentity.network.StartDialoguePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityInteractHandler {
    private static long suppressClientItemUseAtTick = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        if (event.getEntity().distanceToSqr(target) > 16.0D) return;

        // 1. Shift+Right-Click для экипировки NPC
        if (event.getEntity().isSecondaryUseActive() && canWearEquipment(target) && (target.getPersistentData().getBoolean("InteractEntity_NPC") || (event.getLevel().isClientSide() && ClientNpcRegistry.get(target.getId()) != null))) {
            net.minecraft.world.entity.player.Player player = event.getEntity();
            net.minecraft.world.item.ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (event.getLevel().isClientSide()) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            if (!player.getAbilities().instabuild) {
                boolean itemsTake = !target.getPersistentData().contains("InteractEntity_ItemsTake")
                        || target.getPersistentData().getBoolean("InteractEntity_ItemsTake");
                if (!itemsTake) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Вы не можете изменять экипировку этого персонажа!")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                    return;
                }
            }

            if (heldItem.isEmpty()) {
                // Снятие экипировки
                net.minecraft.world.entity.EquipmentSlot[] slots = {
                    net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET
                };
                for (net.minecraft.world.entity.EquipmentSlot slot : slots) {
                    net.minecraft.world.item.ItemStack equipped = target.getItemBySlot(slot);
                    if (!equipped.isEmpty()) {
                        target.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
                        if (!player.getInventory().add(equipped)) {
                            player.drop(equipped, false);
                        }
                        event.getLevel().playSound(null, target.getX(), target.getY(), target.getZ(),
                                net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                        break;
                    }
                }
            } else {
                // Надевание предмета
                net.minecraft.world.entity.EquipmentSlot slot;
                if (heldItem.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                    slot = armorItem.getEquipmentSlot();
                } else if (heldItem.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                    slot = net.minecraft.world.entity.EquipmentSlot.OFFHAND;
                } else if (heldItem.getItem() instanceof net.minecraft.world.item.ElytraItem) {
                    slot = net.minecraft.world.entity.EquipmentSlot.CHEST;
                } else if (heldItem.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && 
                        (blockItem.getBlock() instanceof net.minecraft.world.level.block.AbstractSkullBlock || 
                         blockItem.getBlock() == net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN)) {
                    slot = net.minecraft.world.entity.EquipmentSlot.HEAD;
                } else {
                    slot = net.minecraft.world.entity.EquipmentSlot.MAINHAND;
                }

                net.minecraft.world.item.ItemStack currentEquipped = target.getItemBySlot(slot);
                net.minecraft.world.item.ItemStack toEquip = heldItem.copy();
                toEquip.setCount(1);
                
                target.setItemSlot(slot, toEquip);

                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                    if (!currentEquipped.isEmpty()) {
                        if (!player.getInventory().add(currentEquipped)) {
                            player.drop(currentEquipped, false);
                        }
                    }
                }
                
                net.minecraft.sounds.SoundEvent sound = net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC;
                if (heldItem.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                    sound = armorItem.getEquipSound();
                }
                event.getLevel().playSound(null, target.getX(), target.getY(), target.getZ(),
                        sound, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // 2. Старт диалога
        if (event.getLevel().isClientSide()) {
            if (isClientDialogueTarget(target)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                suppressNextClientItemUse(event);
                ModNetwork.sendToServer(new StartDialoguePacket(target.getId()));
            }
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!startDialogue(player, target)) return;

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (event.getEntity().level().getGameTime() != suppressClientItemUseAtTick) return;

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        suppressClientItemUseAtTick = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        if (event.getEntity().distanceToSqr(target) > 16.0D) return;

        if (!isClientDialogueTarget(target)) return;

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    private static void suppressNextClientItemUse(PlayerInteractEvent event) {
        suppressClientItemUseAtTick = event.getEntity().level().getGameTime();
    }

    public static boolean startDialogue(ServerPlayer player, LivingEntity target) {
        if (DialogueSession.hasActiveSession(player)) return false;

        DialogueManager manager = DialogueManager.get();
        if (manager == null) return false;

        DialogueTree tree = manager.findDialogueForEntity(target);
        if (tree == null) return false;

        DialogueSavedData data = DialogueDataManager.get(player, tree.getScope());

        // Есть незавершённая позиция — возобновить с неё
        String resumeNode = data.getResumeNode(tree.getId());
        if (resumeNode != null && tree.getNode(resumeNode) != null) {
            InteractEntityMod.LOGGER.debug("Resuming dialogue '{}' from node '{}'", tree.getId(), resumeNode);
            DialogueSession.startSessionFromNode(player, target, tree, resumeNode);
            return true;
        }

        if (tree.isRepeatable()) {
            // Repeatable диалоги всегда запускаются заново (прогресс сбрасывается при завершении)
            DialogueSession.startSession(player, target, tree);
            return true;
        }

        boolean visited = data.hasVisited(tree.getId(), tree.getEntryNodeId());
        if (visited) {
            handleRevisit(tree, player, target, manager);
            return true;
        }

        if (data.isCompleted(tree.getId())) {
            InteractEntityMod.LOGGER.debug("Dialogue '{}' is fully completed — skipping fresh start", tree.getId());
            return true;
        }

        DialogueSession.startSession(player, target, tree);
        return true;
    }

    private static boolean isClientDialogueTarget(LivingEntity target) {
        if (ClientNpcRegistry.get(target.getId()) != null) {
            return true;
        }

        DialogueManager manager = DialogueManager.get();
        return manager != null && manager.findDialogueForEntity(target) != null;
    }

    private static void handleRevisit(DialogueTree tree, ServerPlayer player,
                                      LivingEntity target, DialogueManager manager) {
        RevisitConfig revisit = tree.getRevisitConfig();
        if (revisit == null) return;

        InteractEntityMod.LOGGER.debug("Handling revisit for dialogue {}", tree.getId());
        for (RevisitConfig.ConditionalText cond : revisit.getConditions()) {
            boolean conditionMet = ConditionRegistry.check(cond.getCondition(), player, target);
            InteractEntityMod.LOGGER.debug("Condition check result: {}", conditionMet);
            if (conditionMet) {
                String startNode = cond.getStartNode();
                if (startNode != null && !startNode.isEmpty()) {
                    InteractEntityMod.LOGGER.debug("Starting session from node: {}", startNode);
                    DialogueSession.startSessionFromNode(player, target, tree, startNode);
                } else {
                    // Короткое end-сообщение — создаём сессию чтобы заморозить моба
                    DialogueSession.startRevisitSession(player, target, tree);
                    String repId = tree.getReputationId();
                    String repLabel = tree.getFaction() != null ? tree.getFaction() : repId;
                    int reputation = repId != null ? DialogueDataManager.get(player, tree.getScope()).getReputation(repId) : 0;
                    ModNetwork.sendToPlayer(player, new OpenDialoguePacket(
                            target.getId(),
                            "revisit",
                            net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(tree.getDisplayName(), player, target),
                            net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(cond.getText(), player, target),
                            "end",
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            manager.getDialogueAvatar(target),
                            repLabel,
                            reputation,
                            "npc", 0f, 0f
                    ));
                }
                return;
            }
        }

        String defaultStartNode = revisit.getDefaultStartNode();
        if (defaultStartNode != null && !defaultStartNode.isEmpty() && tree.getNode(defaultStartNode) != null) {
            DialogueSession.startSessionFromNode(player, target, tree, defaultStartNode);
            return;
        }

        String defaultText = revisit.getDefaultText();
        if (defaultText != null && !defaultText.isEmpty()) {
            DialogueSession.startRevisitSession(player, target, tree);
            String repId = tree.getReputationId();
            String repLabel = tree.getFaction() != null ? tree.getFaction() : repId;
            int reputation = repId != null ? DialogueDataManager.get(player, tree.getScope()).getReputation(repId) : 0;
            ModNetwork.sendToPlayer(player, new OpenDialoguePacket(
                    target.getId(),
                    "revisit",
                    net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(tree.getDisplayName(), player, target),
                    net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(defaultText, player, target),
                    "end",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    manager.getDialogueAvatar(target),
                    repLabel,
                    reputation,
                    "npc", 0f, 0f
            ));
        }
    }

    public static boolean canWearEquipment(LivingEntity entity) {
        if (entity instanceof net.ashpapi.interactentity.entity.CustomNpcEntity) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Zombie) return true;
        if (entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton) return true;
        if (entity instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin) return true;
        if (entity instanceof net.minecraft.world.entity.npc.AbstractVillager) return true;
        if (entity instanceof net.minecraft.world.entity.monster.AbstractIllager) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Witch) return true;
        if (entity instanceof net.minecraft.world.entity.decoration.ArmorStand) return true;
        return false;
    }
}
