package net.ashpapi.interactentity.dialogue;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.action.ActionRegistry;
import net.ashpapi.interactentity.condition.ConditionRegistry;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.ashpapi.interactentity.event.PlayerProtectionHandler;
import net.ashpapi.interactentity.history.DialogueHistoryEntry;
import net.ashpapi.interactentity.history.HistoryLine;
import net.ashpapi.interactentity.network.CloseDialogueS2CPacket;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.OpenDialoguePacket;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.ashpapi.interactentity.summon.DespawnHandler;
import net.ashpapi.interactentity.summon.SummonScheduler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

import java.util.*;

public class DialogueSession {

    private static final Map<UUID, DialogueSession> ACTIVE_SESSIONS = new HashMap<>();

    private final ServerPlayer player;
    private final LivingEntity entity;
    private final DialogueTree tree;
    private String currentNodeId;
    private final List<String> nodeHistory = new ArrayList<>();
    private final DialogueHistoryEntry historyEntry;

    private final boolean entityWasInvulnerable;
    private final boolean entityWasNoAI;

    private boolean completed = false;
    private int nodeEnterTick = 0;
    private long nodeEnterGameTime = 0;

    public DialogueSession(ServerPlayer player, LivingEntity entity, DialogueTree tree) {
        this.player = player;
        this.entity = entity;
        this.tree = tree;
        this.currentNodeId = tree.getEntryNodeId();

        this.entityWasInvulnerable = entity.isInvulnerable();
        this.entityWasNoAI = (entity instanceof Mob mob) && mob.isNoAi();

        freezeEntity();

        // Продолжаем существующую историю если диалог был прерван ранее
        DialogueSavedData existingData = DialogueSavedData.get(player.serverLevel());
        DialogueHistoryEntry existing = existingData.getHistoryEntry(tree.getId());
        if (existing != null) {
            this.historyEntry = new DialogueHistoryEntry(
                    existing.getDialogueId(), existing.getDisplayName(),
                    existing.getLines(), existing.getTimestamp()
            );
        } else {
            this.historyEntry = new DialogueHistoryEntry(
                    tree.getId(), tree.getDisplayName(),
                    new ArrayList<>(), player.serverLevel().getGameTime()
            );
        }
    }

    public void tick() {
        entity.setDeltaMovement(0, 0, 0);
        entity.setInvulnerable(true);
        entity.clearFire();
        entity.setRemainingFireTicks(-1);

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setLastHurtByMob(null);

            // Каждый тик поворачиваем моба к игроку без проверки движения
            facePlayer(mob);
        }

        entity.getPersistentData().putBoolean("InteractEntity_NPC", true);
        entity.getPersistentData().putBoolean("NoAI", true);
        entity.getPersistentData().putBoolean("Invulnerable", true);

        // Auto-advance for timed linear nodes
        DialogueNode node = tree.getNode(currentNodeId);
        if (node != null && node.getAutoNextTicks() > 0 && node.isLinear()
                && player.serverLevel().getGameTime() - nodeEnterGameTime >= node.getAutoNextTicks()) {
            handleNavigate(player, true);
        }
    }

    private void freezeEntity() {
        entity.setInvulnerable(true);
        entity.setDeltaMovement(0, 0, 0);
        entity.clearFire();
        entity.setRemainingFireTicks(-1);

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
            if (mob instanceof Monster) {
                mob.setLastHurtByMob(null);
            }
            // Сразу повернуть к игроку при начале диалога
            facePlayer(mob);
        }
    }

    private void facePlayer(Mob mob) {
        double dx = player.getX() - entity.getX();
        double dz = player.getZ() - entity.getZ();
        float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYHeadRot(yaw);
        mob.yHeadRotO = yaw;
        mob.setYBodyRot(yaw);
        mob.yBodyRotO = yaw;
    }

    private void unfreezeEntity() {
        entity.setInvulnerable(entityWasInvulnerable);
        if (entity instanceof Mob mob) {
            mob.setNoAi(entityWasNoAI);
        }
        entity.getPersistentData().remove("NoAI");
        entity.getPersistentData().remove("Invulnerable");
    }

    public void sendCurrentNode() {
        DialogueNode node = tree.getNode(currentNodeId);
        if (node == null) {
            endSession(player);
            return;
        }

        nodeEnterGameTime = player.serverLevel().getGameTime();

        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        boolean firstVisit = !data.hasVisited(tree.getId(), currentNodeId);
        data.visit(tree.getId(), currentNodeId);

        if (firstVisit) {
            historyEntry.addLine(new HistoryLine(tree.getDisplayName(), node.getText()));
            data.addHistory(historyEntry);

            if (!node.getActions().isEmpty() && markActionPoint(data, "node:" + currentNodeId)) {
                ActionRegistry.executeActions(node.getActions(), player, entity);
            }
            // Если action заменил сессию (summon_npc+start_dialogue, force_dialogue) — не продолжаем
            if (ACTIVE_SESSIONS.get(player.getUUID()) != this) return;
            ModNetwork.sendToPlayer(player, new SyncProgressPacket(data));
        }
        if (ACTIVE_SESSIONS.get(player.getUUID()) != this) return;

        // Сначала фильтруем опции, потом определяем тип узла
        List<String> optionTexts    = new ArrayList<>();
        List<Integer> optionIndices = new ArrayList<>();
        List<Boolean> optionLocked  = new ArrayList<>();
        List<String> optionLockReasons = new ArrayList<>();
        for (int i = 0; i < node.getOptions().size(); i++) {
            DialogueOption option = node.getOptions().get(i);
            boolean conditionMet = ConditionRegistry.check(option.getCondition(), player, entity);
            if (conditionMet) {
                optionTexts.add(option.getText());
                optionIndices.add(i);
                optionLocked.add(false);
                optionLockReasons.add("");
            } else if (option.isLocked()) {
                optionTexts.add(option.getText());
                optionIndices.add(i);
                optionLocked.add(true);
                optionLockReasons.add(option.getLockReason() != null ? option.getLockReason() : "");
            }
        }

        String nodeType;
        if (node.isEnd()) {
            nodeType = "end";
            completed = true;
        } else if (node.isChoice() && optionTexts.isEmpty()) {
            // Все опции отфильтрованы условиями — ведём себя как end-узел
            nodeType = "end";
            completed = true;
        } else if (node.isLinear()) {
            nodeType = "linear";
        } else {
            nodeType = "choice";
        }

        ResourceLocation avatar = null;
        DialogueManager manager = DialogueManager.get();
        if (manager != null) avatar = manager.getDialogueAvatar(entity);

        // Лип-синк: включаем анимацию рта если это CustomNpcEntity
        if (entity instanceof CustomNpcEntity customNpc) {
            customNpc.setTalking(true);
        }

        ModNetwork.sendToPlayer(player, new OpenDialoguePacket(
                entity.getId(),
                tree.getDisplayName(),
                node.getText(),
                nodeType,
                optionTexts,
                optionIndices,
                optionLocked,
                optionLockReasons,
                avatar,
                tree.getBackground(),
                tree.getOptionsBackground(),
                node.getCameraMode(),
                node.getCameraYawOffset(),
                node.getCameraPitchOffset()
        ));
    }

    public static void startSession(ServerPlayer player, LivingEntity entity, DialogueTree tree) {
        if (isEntityBusy(entity)) return; // моб уже занят другим игроком
        clearNotification(player, tree.getId());
        DialogueSession session = new DialogueSession(player, entity, tree);
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        PlayerProtectionHandler.protect(player);
        session.sendCurrentNode();
    }

    private static void clearNotification(ServerPlayer player, String dialogueId) {
        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        if (data.hasNotification(dialogueId)) {
            data.removeNotification(dialogueId);
            for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
                ModNetwork.sendToPlayer(online, new net.ashpapi.interactentity.network.SyncProgressPacket(data));
            }
        }
    }

    public static void startSessionFromNode(ServerPlayer player, LivingEntity entity,
                                            DialogueTree tree, String startNodeId) {
        if (isEntityBusy(entity)) return;
        DialogueSession session = new DialogueSession(player, entity, tree);
        session.currentNodeId = startNodeId;
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        PlayerProtectionHandler.protect(player);
        session.sendCurrentNode();
    }

    /**
     * Создаёт «немую» сессию для показа revisit-сообщения (nodeType=end без start_node).
     * Моб замораживается, игрок защищён. completed=true сразу — прогресс не стирается.
     * Caller сам отправляет OpenDialoguePacket.
     */
    public static void startRevisitSession(ServerPlayer player, LivingEntity entity, DialogueTree tree) {
        if (isEntityBusy(entity)) return;
        DialogueSession session = new DialogueSession(player, entity, tree);
        session.completed = true;
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        PlayerProtectionHandler.protect(player);
    }

    /** Возвращает true если моб уже ведёт диалог с каким-то игроком. */
    public static boolean isEntityBusy(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        for (DialogueSession s : ACTIVE_SESSIONS.values()) {
            if (s.entity.getUUID().equals(entityUUID)) return true;
        }
        return false;
    }

    public static void endSession(ServerPlayer player) {
        DialogueSession session = ACTIVE_SESSIONS.remove(player.getUUID());
        if (session == null) return;

        PlayerProtectionHandler.unprotect(player);
        session.unfreezeEntity();

        // Лип-синк: выключаем анимацию рта
        if (session.entity instanceof CustomNpcEntity customNpc) {
            customNpc.setTalking(false);
        }

        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());

        if (!session.completed) {
            data.setResumeNode(session.tree.getId(), session.currentNodeId);
        } else if (session.tree.isRepeatable()) {
            // Сбрасываем прогресс чтобы диалог мог сработать снова
            data.resetDialogue(session.tree.getId());
        } else {
            data.clearResumeNode(session.tree.getId());
            data.markCompleted(session.tree.getId());
        }
        ModNetwork.sendToPlayer(player, new SyncProgressPacket(data));

        LivingEntity entity = session.entity;
        if (entity.getTags().contains("interactentity_despawn")) {
            boolean walkAway = entity.getTags().contains("interactentity_walkaway");
            DespawnHandler.scheduleDespawn(entity, walkAway);
        }

        SummonScheduler.scheduleAfterDialogue(session.tree.getId(), player);
        InteractEntityMod.LOGGER.debug("Dialogue session ended for {}", player.getName().getString());
    }

    public static void jumpToNode(ServerPlayer player, String nodeId) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        session.nodeHistory.add(session.currentNodeId);
        session.currentNodeId = nodeId;
        session.sendCurrentNode();
    }

    public static void tickAll() {
        for (DialogueSession session : ACTIVE_SESSIONS.values()) {
            session.tick();
        }
    }

    public static DialogueSession getSession(ServerPlayer player) {
        return ACTIVE_SESSIONS.get(player.getUUID());
    }

    public static boolean hasActiveSession(ServerPlayer player) {
        return ACTIVE_SESSIONS.containsKey(player.getUUID());
    }

    public static void handleOptionSelected(ServerPlayer player, int optionIndex) {
        DialogueSession session = getSession(player);
        if (session == null) return;

        DialogueNode currentNode = session.tree.getNode(session.currentNodeId);
        if (currentNode == null || !currentNode.isChoice()) return;

        List<DialogueOption> options = currentNode.getOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        DialogueOption selected = options.get(optionIndex);
        session.historyEntry.addLine(new HistoryLine("player", selected.getText()));

        DialogueSavedData optData = DialogueSavedData.get(player.serverLevel());
        if (!selected.getActions().isEmpty() && session.markActionPoint(optData, "option:" + session.currentNodeId + ":" + optionIndex)) {
            ActionRegistry.executeActions(selected.getActions(), player, session.entity);
        }
        // Если action заменил сессию (summon_npc+start_dialogue, force_dialogue) — не продолжаем
        if (getSession(player) != session) return;

        optData.addHistory(session.historyEntry);
        ModNetwork.sendToPlayer(player, new SyncProgressPacket(optData));

        String nextId = selected.getNextNodeId();
        if (nextId == null) {
            session.completed = true; // выбор финальной опции = диалог завершён
            ModNetwork.sendToPlayer(player, new CloseDialogueS2CPacket());
            endSession(player);
        } else {
            session.nodeHistory.add(session.currentNodeId);
            session.currentNodeId = nextId;
            session.sendCurrentNode();
        }
    }

    public static void handleNavigate(ServerPlayer player, boolean forward) {
        DialogueSession session = getSession(player);
        if (session == null) return;

        DialogueNode currentNode = session.tree.getNode(session.currentNodeId);
        if (currentNode == null) return;

        if (forward) {
            if (currentNode.isLinear()) {
                session.nodeHistory.add(session.currentNodeId);
                session.currentNodeId = currentNode.getNextNodeId();
                session.sendCurrentNode();
            } else if (currentNode.isEnd()) {
                ModNetwork.sendToPlayer(player, new CloseDialogueS2CPacket());
                endSession(player);
            }
        } else {
            if (!session.nodeHistory.isEmpty()) {
                session.currentNodeId = session.nodeHistory.remove(session.nodeHistory.size() - 1);
                session.sendCurrentNode();
            }
        }
    }

    private boolean markActionPoint(DialogueSavedData data, String actionKey) {
        if (data.hasExecutedAction(tree.getId(), actionKey)) {
            return false;
        }
        data.markActionExecuted(tree.getId(), actionKey);
        return true;
    }
}
