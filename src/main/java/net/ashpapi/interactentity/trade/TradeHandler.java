package net.ashpapi.interactentity.trade;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.condition.ConditionRegistry;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.OpenTradePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Серверная логика торговли. Пакеты делегируют сюда (tryStartTrade / tryExecuteTrade).
 * Торг не замораживает NPC — только проверка дистанции (≤16) при открытии/действии.
 */
public class TradeHandler {

    private static final double MAX_DISTANCE_SQR = 16.0D * 16.0D;

    /** Открыть витрину для игрока: проверить флаг, найти файл, отфильтровать офферы, посчитать stock. */
    public static void tryStartTrade(ServerPlayer player, int entityId) {
        LivingEntity target = resolveTarget(player, entityId);
        if (target == null) return;

        // Если игрок в диалоге с этим же NPC — проверяем, что это тот же NPC.
        // Диалог НЕ закрывается — оверлей торговли выезжает прямо поверх Диалога.
        if (DialogueSession.hasActiveSession(player)) {
            DialogueSession session = DialogueSession.getSession(player);
            if (session == null || session.getEntity() == null
                    || !session.getEntity().getUUID().equals(target.getUUID())) {
                return; // диалог с ДРУГИМ NPC — торг открыть нельзя
            }
        }

        DialogueTree tree = DialogueManager.get() != null
                ? DialogueManager.get().findDialogueForEntity(target) : null;
        if (tree == null) return;

        // Торговля ведётся в scope диалога персонажа (per_player/global)
        DialogueSavedData data = DialogueDataManager.get(player, tree.getScope());
        String shopFile = data.getMerchantShop(tree.getId());
        if (shopFile == null) {
            InteractEntityMod.LOGGER.debug("Trade requested for '{}' but merchant flag not set", tree.getId());
            return;
        }

        TradeCatalogManager catalogMgr = TradeCatalogManager.get();
        if (catalogMgr == null) return;
        TradeShop shop = catalogMgr.getById(shopFile);
        if (shop == null) {
            InteractEntityMod.LOGGER.warn("Trade shop file '{}' not found for dialogue '{}'", shopFile, tree.getId());
            return;
        }

        // Замораживаем NPC на время открытого экрана торга
        if (!TradeSession.open(player, target)) return;

        List<TradeOffer> view = buildFilteredView(shop, tree.getId(), data, player, target);
        ModNetwork.sendToPlayer(player, new OpenTradePacket(entityId, shop.getShopName(), tree.getId(), view));
    }

    /** Закрыть торговлю (по запросу клиента): разморозить NPC, убрать сессию.
     *  Если диалог был приостановлен ради торга — возвращаемся в него. */
    public static void handleClose(ServerPlayer player) {
        TradeSession.close(player);
        // Восстанавливаем диалог, если он был приостановлен
        DialogueSession session = DialogueSession.getSession(player);
        if (session != null && session.isSuspendedForTrade()) {
            session.resumeAfterTrade();
        }
    }

    /**
     * Выполнить обмен: повторно проверить условие/stock/расстояние, провести обмен, обновить stock,
     * вернуть обновлённую витрину.
     */
    public static void tryExecuteTrade(ServerPlayer player, int entityId, int offerIndex) {
        LivingEntity target = resolveTarget(player, entityId);
        if (target == null) return;

        DialogueTree tree = DialogueManager.get() != null
                ? DialogueManager.get().findDialogueForEntity(target) : null;
        if (tree == null) return;

        DialogueSavedData data = DialogueDataManager.get(player, tree.getScope());
        String shopFile = data.getMerchantShop(tree.getId());
        if (shopFile == null) return;

        TradeCatalogManager catalogMgr = TradeCatalogManager.get();
        if (catalogMgr == null) return;
        TradeShop shop = catalogMgr.getById(shopFile);
        if (shop == null) return;

        // Индексация: позиция в исходном файле (чтобы stock-ключ был стабильным).
        if (offerIndex < 0 || offerIndex >= shop.getOffers().size()) return;
        TradeOffer offer = shop.getOffers().get(offerIndex);

        // Повторная проверка условия (защита от устаревшего клиента)
        if (offer.getCondition() != null && !ConditionRegistry.check(offer.getCondition(), player, target)) {
            deny(player);
            return;
        }

        String stockKey = tree.getId() + ":" + offerIndex;
        int remaining = resolveStock(data, offer, stockKey);
        if (remaining == 0) {
            deny(player);
            return;
        }

        boolean ok;
        if (offer.isBuy()) {
            ok = doBuy(player, offer);
        } else if (offer.isSell()) {
            ok = doSell(player, offer);
        } else {
            ok = false;
        }

        if (ok) {
            // Уменьшаем stock только в нужном scope
            if (!offer.isInfinite()) {
                decrementStock(player, data, offer, stockKey);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.5f, 1.2f);
        } else {
            deny(player);
            return;
        }

        // Обновлённая витрина (перефильтровать условия, пересчитать stock)
        List<TradeOffer> view = buildFilteredView(shop, tree.getId(), data, player, target);
        ModNetwork.sendToPlayer(player, new OpenTradePacket(entityId, shop.getShopName(), tree.getId(), view));
    }

    // ===================== Фильтрация / stock =====================

    private static List<TradeOffer> buildFilteredView(TradeShop shop, String dialogueId,
                                                      DialogueSavedData data, ServerPlayer player, LivingEntity entity) {
        List<TradeOffer> out = new ArrayList<>();
        for (int i = 0; i < shop.getOffers().size(); i++) {
            TradeOffer offer = shop.getOffers().get(i);
            if (offer.getCondition() != null
                    && !ConditionRegistry.check(offer.getCondition(), player, entity)) {
                continue;
            }
            int remaining = resolveStock(data, offer, dialogueId + ":" + i);
            int stockForClient = offer.isInfinite() ? TradeOffer.INFINITE_STOCK : remaining;
            out.add(offer.forClientView(i, stockForClient));
        }
        return out;
    }

    /** Текущий остаток: -1 = бесконечно, иначе число оставшихся сделок. */
    private static int resolveStock(DialogueSavedData data, TradeOffer offer, String stockKey) {
        if (offer.isInfinite()) return TradeOffer.INFINITE_STOCK;
        if (data.hasTradeStock(stockKey)) {
            return data.getTradeStock(stockKey);
        }
        // Ещё не инициализирован → исходный лимит
        data.setTradeStock(stockKey, offer.getStock());
        return offer.getStock();
    }

    /** Уменьшает stock в правильном хранилище (per_player уже в data; global — отдельный store). */
    private static void decrementStock(ServerPlayer player, DialogueSavedData data, TradeOffer offer, String stockKey) {
        if ("global".equals(offer.getStockScope())) {
            DialogueSavedData global = DialogueDataManager.getGlobal(player.serverLevel());
            if (!global.hasTradeStock(stockKey)) global.setTradeStock(stockKey, offer.getStock());
            global.decrementTradeStock(stockKey);
        } else {
            data.decrementTradeStock(stockKey);
        }
    }

    // ===================== Обмен предметами =====================

    /** Покупка: игрок отдаёт priceStacks, получает resultStack. */
    private static boolean doBuy(ServerPlayer player, TradeOffer offer) {
        if (!hasItems(player, offer.getPriceStacks())) return false;
        if (!canAcceptAll(player, List.of(offer.getResultStack()))) return false;
        removeItems(player, offer.getPriceStacks());
        giveItem(player, offer.getResultStack());
        return true;
    }

    /** Продажа: игрок отдаёт merchandiseStack, получает priceStacks. */
    private static boolean doSell(ServerPlayer player, TradeOffer offer) {
        if (!hasItems(player, List.of(offer.getMerchandiseStack()))) return false;
        if (!canAcceptAll(player, offer.getPriceStacks())) return false;
        removeItems(player, List.of(offer.getMerchandiseStack()));
        for (ItemStack price : offer.getPriceStacks()) giveItem(player, price);
        return true;
    }

    private static boolean hasItems(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack need : stacks) {
            if (need.isEmpty()) continue;
            int remaining = need.getCount();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack inv = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameTags(inv, need)) {
                    remaining -= inv.getCount();
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static void removeItems(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack need : stacks) {
            if (need.isEmpty()) continue;
            int remaining = need.getCount();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack inv = player.getInventory().getItem(i);
                if (ItemStack.isSameItemSameTags(inv, need)) {
                    int toRemove = Math.min(remaining, inv.getCount());
                    inv.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        }
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }

    /** Хватит ли места (хотя бы по одному стеку) — упрощённая проверка. */
    private static boolean canAcceptAll(ServerPlayer player, List<ItemStack> stacks) {
        int freeSlots = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                freeSlots++;
                if (freeSlots >= stacks.size()) return true;
            }
        }
        return freeSlots >= stacks.size();
    }

    // ===================== Утилиты =====================

    private static LivingEntity resolveTarget(ServerPlayer player, int entityId) {
        Entity e = player.level().getEntity(entityId);
        if (!(e instanceof LivingEntity target)) return null;
        if (player.distanceToSqr(target) > MAX_DISTANCE_SQR) return null;
        return target;
    }

    private static void deny(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.5f, 1.0f);
    }
}
