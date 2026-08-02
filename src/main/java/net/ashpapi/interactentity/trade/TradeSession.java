package net.ashpapi.interactentity.trade;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.network.CloseTradeS2CPacket;
import net.ashpapi.interactentity.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Серверная сессия активной торговли. Зеркало DialogueSession для торга:
 * замораживает NPC на время открытого экрана, держит его развёрнутым к игроку,
 * прерывается при отходе/выходе/смерти, размораживает NPC при закрытии.
 *
 * Сам экран торговли НЕ выполняет обмен — это делает TradeHandler.tryExecuteTrade.
 * TradeSession нужна только для заморозки NPC и контроля дистанции.
 */
public class TradeSession {

    private static final Map<UUID, TradeSession> ACTIVE = new HashMap<>();
    private static final double MAX_DISTANCE_SQR = 16.0D * 16.0D;

    private final ServerPlayer player;
    private final LivingEntity entity;
    private final boolean entityWasInvulnerable;
    private final boolean entityWasNoAI;

    public TradeSession(ServerPlayer player, LivingEntity entity) {
        this.player = player;
        this.entity = entity;
        this.entityWasInvulnerable = entity.isInvulnerable();
        this.entityWasNoAI = (entity instanceof Mob mob) && mob.isNoAi();
    }

    public LivingEntity getEntity() { return entity; }
    public ServerPlayer getPlayer() { return player; }

    public static boolean hasActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static TradeSession get(ServerPlayer player) {
        return ACTIVE.get(player.getUUID());
    }

    /** Открыть торговлю: заморозить NPC и зарегистрировать сессию. */
    public static boolean open(ServerPlayer player, LivingEntity entity) {
        if (DialogueSession.isEntityBusyWithOtherPlayer(entity, player)) return false; // NPC занят диалогом с ДРУГИМ игроком
        if (ACTIVE.containsKey(player.getUUID())) return false; // уже торгует
        // Не позволяем одному NPC торговать с несколькими игроками одновременно
        for (TradeSession s : ACTIVE.values()) {
            if (s.entity.getUUID().equals(entity.getUUID())) return false;
        }
        TradeSession session = new TradeSession(player, entity);
        ACTIVE.put(player.getUUID(), session);
        session.freeze();
        return true;
    }

    /** Закрыть торговлю: разморозить NPC и убрать сессию. */
    public static void close(ServerPlayer player) {
        TradeSession session = ACTIVE.remove(player.getUUID());
        if (session != null) session.unfreeze();
    }

    private void freeze() {
        entity.setInvulnerable(true);
        entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            if (mob instanceof net.minecraft.world.entity.monster.Monster) {
                mob.setLastHurtByMob(null);
            }
            mob.setNoAi(true);
            facePlayer(mob);
        }
    }

    private void unfreeze() {
        entity.setInvulnerable(entityWasInvulnerable);
        if (entity instanceof Mob mob) {
            mob.setNoAi(entityWasNoAI);
        }
    }

    private void facePlayer(Mob mob) {
        double dx = player.getX() - mob.getX();
        double dz = player.getZ() - mob.getZ();
        float targetYaw = (float)(Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0f;
        targetYaw = Mth.wrapDegrees(targetYaw);
        float newYaw = Mth.rotLerp(0.4f, mob.getYRot(), targetYaw);
        mob.setYRot(newYaw);
        mob.yBodyRot = newYaw;
        mob.yHeadRot = newYaw;
    }

    /** Тик всех активных сессий: держим NPC на месте, прерываем при отходе/выходе. */
    public static void tickAll() {
        for (TradeSession s : new java.util.ArrayList<>(ACTIVE.values())) {
            s.tick();
        }
    }

    private void tick() {
        if (player == null || entity == null || !entity.isAlive()
                || player.level() != entity.level()
                || player.distanceToSqr(entity) > MAX_DISTANCE_SQR) {
            // Игрок отошёл/вышел — сервер закрывает экран и сессию.
            // Через handleClose, чтобы приостановленный диалог тоже корректно обработался.
            ModNetwork.sendToPlayer(player, new CloseTradeS2CPacket());
            TradeHandler.handleClose(player);
            return;
        }
        // Удерживаем NPC неподвижным
        entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
        entity.setInvulnerable(true);
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            facePlayer(mob);
        }
    }
}
