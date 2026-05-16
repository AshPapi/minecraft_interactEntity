package net.ashpapi.interactentity.dialogue;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.action.ActionRegistry;
import net.ashpapi.interactentity.condition.ConditionRegistry;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
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
import net.minecraft.util.Mth;
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
    private long nodeEnterGameTime = 0;

    private String reputationId;
    private String factionLabel;

    public String getReputationId() { return reputationId; }
    public String getFactionLabel() { return factionLabel; }

    public DialogueSession(ServerPlayer player, LivingEntity entity, DialogueTree tree) {
        this.player = player;
        this.entity = entity;
        this.tree = tree;
        this.currentNodeId = tree.getEntryNodeId();

        this.reputationId = tree.getReputationId();
        this.factionLabel = tree.getFaction();

        this.entityWasInvulnerable = entity.isInvulnerable();
        this.entityWasNoAI = (entity instanceof Mob mob) && mob.isNoAi();

        freezeEntity();

        // Продолжаем существующую историю если диалог был прерван ранее
        DialogueSavedData existingData = DialogueDataManager.get(player, tree.getScope());
        DialogueHistoryEntry existing = existingData.getHistoryEntry(tree.getId());
        String entityType = tree.getTarget().getEntityType();
        String characterInfo = tree.getCharacterInfo();

        if (existing != null) {
            this.historyEntry = new DialogueHistoryEntry(
                    existing.getDialogueId(), existing.getDisplayName(),
                    this.reputationId, this.factionLabel,
                    entityType, characterInfo,
                    existing.getLines(), existing.getTimestamp()
            );
        } else {
            String resolvedName = net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(tree.getDisplayName(), player, entity);
            this.historyEntry = new DialogueHistoryEntry(
                    tree.getId(), resolvedName,
                    this.reputationId, this.factionLabel,
                    entityType, characterInfo,
                    new ArrayList<>(), player.serverLevel().getGameTime()
            );
        }
    }

    public void tick() {
        if (player == null || entity == null || !entity.isAlive()
                || player.level() != entity.level()
                || player.distanceToSqr(entity) > 256.0D) {
            ModNetwork.sendToPlayer(player, new CloseDialogueS2CPacket());
            DialogueSession.endSession(player);
            return;
        }

        // Оставляем движение по оси Y, чтобы работала гравитация
        entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
        entity.setInvulnerable(true);

        if (entity instanceof Mob mob) {
            // КРИТИЧЕСКИЙ ФИКС: НЕ отключаем AI (setNoAi), чтобы тело могло вращаться.
            // Вместо этого просто останавливаем навигацию.
            mob.getNavigation().stop();
            mob.setTarget(null);

            // Каждый тик плавно поворачиваем моба к игроку
            facePlayer(mob);
        }

        // Auto-advance for timed linear nodes
        DialogueNode node = tree.getNode(currentNodeId);
        if (node != null && node.getAutoNextTicks() > 0 && node.isLinear()
                && player.serverLevel().getGameTime() - nodeEnterGameTime >= node.getAutoNextTicks()) {
            handleNavigate(player, currentNodeId, true);
        }
    }

    private void freezeEntity() {
        entity.setInvulnerable(true);
        entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);

        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            if (mob instanceof net.minecraft.world.entity.monster.Monster) {
                mob.setLastHurtByMob(null);
            }
            // Сразу повернуть к игроку при начале диалога
            facePlayer(mob);
        }
    }

    private void facePlayer(Mob mob) {
        // ОПТИМИЗАЦИЯ: используем встроенную систему LookControl для плавности
        // Это убирает "дерганье" головы, так как Майнкрафт сам интерполирует поворот
        mob.getLookControl().setLookAt(player, 60.0f, 60.0f);
        
        // Дополнительно синхронизируем тело, если разница углов слишком велика
        // Но делаем это мягко, не перебивая LookControl
        mob.yBodyRot = mob.yHeadRot;
    }

    private void unfreezeEntity() {
        entity.setInvulnerable(entityWasInvulnerable);
        entity.getPersistentData().remove("Invulnerable");
    }

    public void sendCurrentNode() {
        DialogueNode node = tree.getNode(currentNodeId);
        if (node == null) {
            InteractEntityMod.LOGGER.warn("Dialogue '{}' points to missing node '{}'", tree.getId(), currentNodeId);
            completed = true;
            ModNetwork.sendToPlayer(player, new CloseDialogueS2CPacket());
            endSession(player);
            return;
        }
        String nodeText = node.getText();

        nodeEnterGameTime = player.serverLevel().getGameTime();

        DialogueSavedData data = getData();
        boolean firstVisit = !data.hasVisited(tree.getId(), currentNodeId);
        data.visit(tree.getId(), currentNodeId);

        if (firstVisit) {
            String resolvedDisplayName = net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(tree.getDisplayName(), player, entity);
            String resolvedNodeText = net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(nodeText, player, entity);
            historyEntry.addLine(new HistoryLine(resolvedDisplayName, resolvedNodeText));
            data.addHistory(historyEntry);

            if (!node.getActions().isEmpty() && markActionPoint(data, "node:" + currentNodeId)) {
                ActionRegistry.executeActions(node.getActions(), player, entity);
            }
            // Если action заменил сессию (summon_npc+start_dialogue, force_dialogue) — не продолжаем
            if (ACTIVE_SESSIONS.get(player.getUUID()) != this) return;
            ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));
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
                optionTexts.add(net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(option.getText(), player, entity));
                optionIndices.add(i);
                optionLocked.add(false);
                optionLockReasons.add("");
            } else if (option.isLocked()) {
                optionTexts.add(net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(option.getText(), player, entity));
                optionIndices.add(i);
                optionLocked.add(true);
                optionLockReasons.add(option.getLockReason() != null ? net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(option.getLockReason(), player, entity) : "");
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

        String repId = tree.getReputationId();
        String repLabel = tree.getFaction() != null ? tree.getFaction() : repId;
        int reputation = 0;
        if (repId != null) {
            reputation = getData().getReputation(repId);
        }

        ModNetwork.sendToPlayer(player, new OpenDialoguePacket(
                entity.getId(),
                currentNodeId,
                net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(tree.getDisplayName(), player, entity),
                net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(nodeText, player, entity),
                nodeType,
                optionTexts,
                optionIndices,
                optionLocked,
                optionLockReasons,
                avatar,
                tree.getBackground(),
                tree.getOptionsBackground(),
                repLabel,
                reputation,
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
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.ashpapi.interactentity.api.DialogueStartEvent(player, entity, tree.getId(), session.currentNodeId));
        session.sendCurrentNode();
    }

    private static void clearNotification(ServerPlayer player, String dialogueId) {
        // Notifications live in the global store (cross-player concept).
        DialogueSavedData data = DialogueDataManager.getGlobal(player.serverLevel());
        if (data.hasNotification(dialogueId)) {
            data.removeNotification(dialogueId);
            for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
                ModNetwork.sendToPlayer(online, SyncProgressPacket.createFor(online));
            }
        }
    }

    public static void startSessionFromNode(ServerPlayer player, LivingEntity entity,
                                            DialogueTree tree, String startNodeId) {
        if (isEntityBusy(entity)) return;
        DialogueSession session = new DialogueSession(player, entity, tree);
        if (tree.getNode(startNodeId) != null) {
            session.currentNodeId = startNodeId;
        } else {
            InteractEntityMod.LOGGER.warn("Dialogue '{}' requested missing start node '{}', falling back to entry '{}'",
                    tree.getId(), startNodeId, tree.getEntryNodeId());
            session.currentNodeId = tree.getEntryNodeId();
        }
        ACTIVE_SESSIONS.put(player.getUUID(), session);
        PlayerProtectionHandler.protect(player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.ashpapi.interactentity.api.DialogueStartEvent(player, entity, tree.getId(), session.currentNodeId));
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

        DialogueSavedData data = DialogueDataManager.get(player, session.tree.getScope());

        if (!session.completed) {
            data.setResumeNode(session.tree.getId(), session.currentNodeId);
        } else if (session.tree.isRepeatable()) {
            // Сбрасываем прогресс чтобы диалог мог сработать снова
            data.resetDialogue(session.tree.getId());
        } else {
            data.clearResumeNode(session.tree.getId());
            data.markCompleted(session.tree.getId());
        }
        ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));

        LivingEntity entity = session.entity;
        if (entity.getTags().contains("interactentity_despawn")) {
            boolean walkAway = entity.getTags().contains("interactentity_walkaway");
            DespawnHandler.scheduleDespawn(entity, walkAway);
        }

        SummonScheduler.scheduleAfterDialogue(session.tree.getId(), player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.ashpapi.interactentity.api.DialogueEndEvent(player, entity, session.tree.getId(), session.currentNodeId, session.completed));
        InteractEntityMod.LOGGER.debug("Dialogue session ended for {}", player.getName().getString());
    }

    public static void jumpToNode(ServerPlayer player, String nodeId) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        if (session.tree.getNode(nodeId) == null) {
            InteractEntityMod.LOGGER.warn("Dialogue '{}' cannot jump to missing node '{}'", session.tree.getId(), nodeId);
            return;
        }
        session.nodeHistory.add(session.currentNodeId);
        session.currentNodeId = nodeId;
        session.sendCurrentNode();
    }

    public static void tickAll() {
        for (DialogueSession session : new ArrayList<>(ACTIVE_SESSIONS.values())) {
            session.tick();
        }
    }

    public static DialogueSession getSession(ServerPlayer player) {
        return ACTIVE_SESSIONS.get(player.getUUID());
    }

    public String getDialogueId() {
        return tree.getId();
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public String getDisplayName() {
        return tree.getDisplayName();
    }

    public static boolean hasActiveSession(ServerPlayer player) {
        return ACTIVE_SESSIONS.containsKey(player.getUUID());
    }

    public static void handleOptionSelected(ServerPlayer player, String expectedNodeId, int optionIndex) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        if (expectedNodeId != null && !expectedNodeId.isEmpty() && !expectedNodeId.equals(session.currentNodeId)) {
            InteractEntityMod.LOGGER.debug("SelectOption ignored: client node '{}' != server node '{}'", expectedNodeId, session.currentNodeId);
            return;
        }

        DialogueNode currentNode = session.tree.getNode(session.currentNodeId);
        if (currentNode == null || !currentNode.isChoice()) return;

        List<DialogueOption> options = currentNode.getOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        DialogueOption selected = options.get(optionIndex);
        if (selected.isLocked() || !ConditionRegistry.check(selected.getCondition(), player, session.entity)) {
            InteractEntityMod.LOGGER.warn("Player {} tried to select locked/unavailable option (dialogue '{}', node '{}', index {})",
                    player.getName().getString(), session.tree.getId(), session.currentNodeId, optionIndex);
            return;
        }
        String resolvedText = net.ashpapi.interactentity.formatting.PlaceholderResolver.resolve(selected.getText(), player, session.entity);
        session.historyEntry.addLine(new HistoryLine("player", resolvedText));

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.ashpapi.interactentity.api.DialogueChoiceEvent(
                        player, session.entity, session.tree.getId(), session.currentNodeId,
                        "option", "option_" + optionIndex));

        DialogueSavedData optData = session.getData();
        if (!selected.getActions().isEmpty() && session.markActionPoint(optData, "option:" + session.currentNodeId + ":" + optionIndex)) {
            ActionRegistry.executeActions(selected.getActions(), player, session.entity);
        }
        // Если action заменил сессию (summon_npc+start_dialogue, force_dialogue) — не продолжаем
        if (getSession(player) != session) return;

        optData.addHistory(session.historyEntry);
        ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));

        String nextId = selected.getNextNodeId();
        if (nextId == null) {
            session.completed = true; // выбор финальной опции = диалог завершён
            ModNetwork.sendToPlayer(player, new CloseDialogueS2CPacket());
            endSession(player);
        } else if (session.tree.getNode(nextId) == null) {
            InteractEntityMod.LOGGER.warn("Dialogue '{}' option from node '{}' points to missing node '{}'",
                    session.tree.getId(), session.currentNodeId, nextId);
            session.completed = true;
            ModNetwork.sendToPlayer(player, new CloseDialogueS2CPacket());
            endSession(player);
        } else {
            session.nodeHistory.add(session.currentNodeId);
            session.currentNodeId = nextId;
            session.sendCurrentNode();
        }
    }

    public static void handleNavigate(ServerPlayer player, String expectedNodeId, boolean forward) {
        DialogueSession session = getSession(player);
        if (session == null) return;
        if (expectedNodeId != null && !expectedNodeId.isEmpty() && !expectedNodeId.equals(session.currentNodeId)) {
            InteractEntityMod.LOGGER.debug("Navigate ignored: client node '{}' != server node '{}'", expectedNodeId, session.currentNodeId);
            return;
        }

        DialogueNode currentNode = session.tree.getNode(session.currentNodeId);
        if (currentNode == null) return;

        if (forward) {
            if (currentNode.isLinear()) {
                String nextId = currentNode.getNextNodeId();
                if (nextId == null || session.tree.getNode(nextId) == null) {
                    InteractEntityMod.LOGGER.warn("Dialogue '{}' linear node '{}' points to missing node '{}'",
                            session.tree.getId(), session.currentNodeId, nextId);
                    session.completed = true;
                    ModNetwork.sendToPlayer(player, new CloseDialogueS2CPacket());
                    endSession(player);
                    return;
                }
                session.nodeHistory.add(session.currentNodeId);
                session.currentNodeId = nextId;
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

    /** Returns the correct data store based on this dialogue's scope. */
    private DialogueSavedData getData() {
        return DialogueDataManager.get(player, tree.getScope());
    }
}
