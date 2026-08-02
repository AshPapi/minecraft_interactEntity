package net.ashpapi.interactentity.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ashpapi.interactentity.camera.DialogueCameraController;
import net.ashpapi.interactentity.formatting.TextFormatter;
import net.ashpapi.interactentity.network.CloseDialogueC2SPacket;
import net.ashpapi.interactentity.network.CloseTradeC2SPacket;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.NavigatePacket;
import net.ashpapi.interactentity.network.SelectOptionPacket;
import net.ashpapi.interactentity.network.StartTradePacket;
import net.ashpapi.interactentity.network.SwapInventorySlotsC2SPacket;
import net.ashpapi.interactentity.network.TradeActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DialogueScreen extends Screen {

    private static final int PANEL_WIDTH = 340;          // Увеличено до 340 для широких карточек
    private static final int PADDING = 8;                // Увеличено с 5
    private static final int HOTBAR_GAP = 20;            // еще ниже
    private static final int LINE_HEIGHT = 9;            // увеличено с 8
    private static final int LINE_SPACING = 1;           // минимальный интервал
    private static final int HEAD_SIZE = 24;             // уменьшено
    private static final float TEXT_SCALE = 0.90f;       // масштаб текста 90%

    private static final ResourceLocation DEFAULT_WINDOW = new ResourceLocation("interactentity", "textures/gui/dialogue_window.png");
    private static final ResourceLocation DEFAULT_OPTIONS = new ResourceLocation("interactentity", "textures/gui/option_window.png");
    private static final ResourceLocation DEFAULT_OPTIONS_HOVER = new ResourceLocation("interactentity", "textures/gui/option_window_hover.png");

    private static final int BG_COLOR_TOP = 0xCC1A1A2E;
    private static final int BG_COLOR_BOTTOM = 0xCC0D0D1A;
    private static final int BORDER_COLOR = 0x66FFFFFF;
    private static final int SHADOW_COLOR = 0x80000000;

    private boolean textureExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }
    private static final int NAME_COLOR = 0xFFFFCC44;
    private static final int TEXT_COLOR = 0xFFDDDDDD;
    private static final int OPTION_COLOR = 0xFFFFFFFF;
    private static final int OPTION_HOVER = 0xFF55FF55;
    private static final int HINT_COLOR = 0xFF8888AA;

    private final int entityId;
    private String nodeId;
    private String dialogueId = "";
    private String displayName;
    private String dialogueText;
    private String nodeType;
    private List<String> optionTexts;
    private List<Integer> optionIndices;
    private List<Boolean> optionLocked;
    private List<String> optionLockReasons;
    private String rawDisplayName;
    private String rawDialogueText;
    private List<String> rawOptionTexts;
    private List<String> rawOptionLockReasons;
    private String lastResolvedLang = "";
    @Nullable private ResourceLocation avatarTexture;
    @Nullable private String factionId;
    private int reputation;

    private boolean ignoreNextClick = false;
    private int hoveredOption = -1;
    private boolean mouseMoved = false;
    private float animationTime = 0.0f;
    private boolean firstOpen = true;
    private boolean showingOptions = false;
    private boolean showingPlayerResponse = false;
    private String playerResponseText = "";
    private int pendingOptionRawIndex = -1;
    // Игрок подтвердил ответ (SelectOptionPacket отправлен) — ждём реакции сервера
    // (CloseDialogueS2CPacket для end-опции или updateDialogue с новой нодой).
    // Пока ждём, НЕ сбрасываем showingPlayerResponse, чтобы старая NPC-реплика не мигнула.
    private boolean awaitingServerAfterResponse = false;
    private static final float ANIMATION_SPEED = 0.15f;
    private static final int TYPEWRITER_TICKS_PER_CHAR = 1;
    private int visibleDialogueChars = 0;
    private int totalDialogueChars = 0;
    private int typewriterTickCounter = 0;

    // История диалога в текущей сессии
    public static class SessionHistoryLine {
        public final String speaker;
        public final String text;
        public final boolean isPlayer;

        public SessionHistoryLine(String speaker, String text, boolean isPlayer) {
            this.speaker = speaker;
            this.text = text;
            this.isPlayer = isPlayer;
        }
    }

    public static class CachedHistoryItem {
        public final SessionHistoryLine line;
        public final List<FormattedCharSequence> splitLines;
        public final int height;
        public final Component formattedSpeaker;

        public CachedHistoryItem(SessionHistoryLine line, List<FormattedCharSequence> splitLines, int height, Component formattedSpeaker) {
            this.line = line;
            this.splitLines = splitLines;
            this.height = height;
            this.formattedSpeaker = formattedSpeaker;
        }
    }

    private final List<CachedHistoryItem> cachedHistoryItems = new ArrayList<>();
    private int cachedHistoryTotalHeight = 0;
    private final List<SessionHistoryLine> sessionHistory = new ArrayList<>();
    private boolean showingHistoryOverlay = false;
    private float historyOverlayProgress = 0.0f;
    private float historyOverlayStartValue = 0.0f;
    private float historyOverlayTarget = 0.0f;
    private long historyOverlayAnimStart = 0L;
    private static final long HISTORY_OVERLAY_ANIM_MS = 280L;
    private int historyScroll = 0;
    private float bookOpenProgress = 0.0f;
    private float bookOpenStartValue = 0.0f;
    private float bookOpenTarget = 0.0f;
    private long bookOpenAnimStart = 0L;
    private static final long BOOK_OPEN_ANIM_MS = 220L;
    private boolean bookHovered = false;
    private boolean lastBookTargetOpen = false;
    private boolean tradeHovered = false;
    private float pouchOpenProgress = 0.0f;
    private float pouchOpenStartValue = 0.0f;
    private float pouchOpenTarget = 0.0f;
    private long pouchOpenAnimStart = 0L;
    private boolean lastPouchTargetOpen = false;
    private static final long POUCH_OPEN_ANIM_MS = 220L;

    /** Кликабельная область торговли. Границы строго совпадают с нарисованным прямоугольником. */
    public record TradeCardHitbox(int x, int y, int w, int h, int index) {
        public boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private boolean showingTradeOverlay = false;
    private float tradeOverlayProgress = 0.0f;
    private float tradeOverlayStartValue = 0.0f;
    private float tradeOverlayTarget = 0.0f;
    private long tradeOverlayAnimStart = 0L;
    private static final long TRADE_OVERLAY_ANIM_MS = 280L;
    private int tradeScrollRow = 0;
    private int selectedTradeIndex = -1;    // -1 = панель товара закрыта
    private int tradeDescScroll = 0;
    private int[] tradeDescArea = null;     // [x, y, w, h] окна описания — по нему роутится колесо
    private String tradeShopName = "";

    // Боковая панель товара: выезжает вправо из-под витрины.
    private float tradeDetailProgress = 0.0f;
    private float tradeDetailStartValue = 0.0f;
    private float tradeDetailTarget = 0.0f;
    private long tradeDetailAnimStart = 0L;
    private static final long TRADE_DETAIL_ANIM_MS = 200L;
    /** Закрытие витрины ждёт, пока уедет панель товара. */
    private boolean pendingTradeClose = false;

    // --- Геометрия оверлея торговли ---
    // Ширина у витрины та же, что у истории и инвентаря — panelW окна диалога.
    private static final int TRADE_H = 175;
    private static final int TRADE_PREVIEW = 36;
    private static final int TRADE_DESC_LINE_H = 10;
    private static final float TRADE_DESC_SCALE = TEXT_SCALE;
    private static final int TRADE_DESC_COLOR = 0xFFB0B0C4;
    private static final int TRADE_PAD = 8;
    private static final int TRADE_HEADER_H = 24;        // заголовок + разделитель
    private static final int TRADE_DETAIL_W = 128;
    private static final int TRADE_DETAIL_GAP = 4;       // зазор между витриной и выехавшей панелью
    private static final int TRADE_CARD_H = 56;          // целевая высота карточки; ряд растягивается по месту
    private static final int TRADE_CARD_H_MAX = 84;
    private static final int TRADE_CARD_H_MIN = 46;
    private static final int TRADE_CARD_MIN_W = 72;
    private static final int TRADE_CARD_MAX_W = 84;
    private static final int TRADE_CARD_GAP = 5;
    private static final int TRADE_SCROLLBAR_W = 4;
    private static final int TRADE_BTN_H = 14;
    private static final int TRADE_DETAIL_BTN_H = 18;
    private static final int TRADE_ICON_BOX = 12;        // размер иконки-мешочка (и её хитбокса)

    // --- Палитра торговли ---
    private static final int TRADE_CARD_BG = 0xCC15152A;
    private static final int TRADE_CARD_BG_HOVER = 0xCC232342;
    private static final int TRADE_CARD_BG_SELECTED = 0xCC2E2818;
    private static final int TRADE_CARD_BORDER = 0x66AAAACC;
    private static final int TRADE_CARD_BORDER_HOVER = 0xAACCCCEE;
    private static final int TRADE_CARD_BORDER_SELECTED = 0xFFFFCC44;
    // Покупка — зелёная, продажа — синяя. Нехватка предметов гасит цвет, но не стирает разницу.
    private static final int TRADE_BUY_BG = 0xCC1F5A25;
    private static final int TRADE_BUY_BG_HOVER = 0xEE2C7C33;
    private static final int TRADE_BUY_BORDER = 0xFF5FD164;
    private static final int TRADE_BUY_BG_DIM = 0xCC1B2E1D;
    private static final int TRADE_BUY_BORDER_DIM = 0xFF456B47;
    private static final int TRADE_SELL_BG = 0xCC1F4468;
    private static final int TRADE_SELL_BG_HOVER = 0xEE2A5E90;
    private static final int TRADE_SELL_BORDER = 0xFF5FA8E0;
    private static final int TRADE_SELL_BG_DIM = 0xCC1B2836;
    private static final int TRADE_SELL_BORDER_DIM = 0xFF44607A;
    private static final int TRADE_DISABLED_BG = 0xCC2A2A32;
    private static final int TRADE_DISABLED_BORDER = 0x88707080;
    private static final int TRADE_DISABLED_TEXT = 0xFF8A8A96;
    private static final int TRADE_STOCK_COLOR = 0xFFFFAA55;
    private static final int TRADE_LACKING_COLOR = 0xFFFF7070;
    public record FloatingTextAnim(net.minecraft.world.item.ItemStack stack, String text, int startX, int startY, int color, long startTime) {}
    private final List<FloatingTextAnim> activeFloatingTexts = new ArrayList<>();

    private List<net.ashpapi.interactentity.trade.TradeOffer> tradeOffers = new ArrayList<>();
    /** Кнопки «Купить/Продать» — клик совершает сделку. */
    private final List<TradeCardHitbox> tradeBuyHitboxes = new ArrayList<>();
    /** Тела карточек — клик только выбирает товар для панели деталей. */
    private final List<TradeCardHitbox> tradeSelectHitboxes = new ArrayList<>();
    private net.minecraft.world.item.ItemStack tradeHoveredItem = net.minecraft.world.item.ItemStack.EMPTY;
    // Размеры сетки последнего кадра — нужны для навигации стрелками.
    private int tradeCols = 1;
    private int tradeRowsVisible = 1;
    // Кэш разбитого на строки описания выбранного товара.
    private int descCacheIndex = -1;
    private int descCacheWidth = -1;
    private List<FormattedCharSequence> descCacheLines = new ArrayList<>();

    private static final int YOU_COLOR = 0xFF88DDFF;

    private static final int OPT_PADDING_H = 10;
    private static final int OPT_PADDING_V = 5;
    private static final int OPT_MARGIN = 4;
    private static final int OPT_BG = 0xCC1A1A2E;
    private static final int OPT_BG_HOVER = 0xCC2A3A4E;
    private static final int OPT_BORDER = 0x88AAAACC;
    private static final int OPT_BORDER_HOVER = 0xFFFFCC44;
    private static final int OPT_NUM_COLOR = 0xFF888888;
    private static final int OPT_LOCKED_TEXT = 0xFF666666;
    private static final int OPT_LOCKED_REASON = 0xFF995555;
    private static final int OPT_LOCKED_BG = 0xCC111118;
    private static final int OPT_LOCKED_BORDER = 0x66666666;

    private int panelX, panelY, panelW, panelH;
    private final List<OptionHitbox> optionHitboxes = new ArrayList<>();

    // Оптимизация: кэш текстур и текста
    private boolean useDefaultWindowTex;
    private boolean useDefaultOptionsTex;
    private boolean useDefaultOptionsHoverTex;
    private List<FormattedCharSequence> cachedFullLines = new ArrayList<>();
    private int cachedTextAreaWidth = -1;
    private String lastDialogueText = "";
    private final List<Component> cachedOptionComps = new ArrayList<>();
    private final List<List<FormattedCharSequence>> cachedOptionLines = new ArrayList<>();
    private int cachedOptionPanelW = 0;
    private int cachedOptionAvailW = -1;
    private Component cachedNameComp = null;
    private Component cachedVisibleTextComp = null;
    private int cachedVisibleDialogueChars = -1;
    private List<FormattedCharSequence> cachedTextLines = new ArrayList<>();
    private int cachedTextLinesWidth = -1;
    private int cachedTextLinesVisibleChars = -1;

    private boolean showingInventoryOverlay = false;
    private float inventoryOverlayProgress = 0.0f;
    private float inventoryOverlayStartValue = 0.0f;
    private float inventoryOverlayTarget = 0.0f;
    private long inventoryOverlayAnimStart = 0L;
    private static final long INVENTORY_OVERLAY_ANIM_MS = 280L;
    private net.minecraft.world.item.ItemStack inventoryHoveredItem = net.minecraft.world.item.ItemStack.EMPTY;

    public record InventorySlotHitbox(int x, int y, int size, int slotIndex) {
        public boolean contains(int mx, int my) {
            return mx >= x && mx <= x + size && my >= y && my <= y + size;
        }
    }

    private final List<InventorySlotHitbox> inventoryHitboxes = new ArrayList<>();
    private int draggedSlotIndex = -1;
    private net.minecraft.world.item.ItemStack carriedItemStack = net.minecraft.world.item.ItemStack.EMPTY;

    private void returnCarriedItemToInventory() {
        if (draggedSlotIndex != -1 && !carriedItemStack.isEmpty()) {
            draggedSlotIndex = -1;
            carriedItemStack = net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    /** Есть ли у игрока нужное количество предмета в инвентаре. */
    private boolean hasEnough(net.minecraft.world.item.ItemStack need) {
        if (need.isEmpty()) return true;
        if (this.minecraft == null || this.minecraft.player == null) return false;
        net.minecraft.world.entity.player.Inventory inv = this.minecraft.player.getInventory();
        int found = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack slot = inv.getItem(i);
            if (!slot.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameTags(slot, need)) {
                found += slot.getCount();
                if (found >= need.getCount()) return true;
            }
        }
        return false;
    }

    /** Покупка — хватает ли цены; продажа — есть ли сам товар. */
    private boolean canPlayerAffordTrade(net.ashpapi.interactentity.trade.TradeOffer offer) {
        if (offer == null) return false;
        if (offer.isBuy()) {
            for (net.minecraft.world.item.ItemStack price : offer.getPriceStacks()) {
                if (!hasEnough(price)) return false;
            }
            return true;
        }
        if (offer.isSell()) return hasEnough(offer.getDisplayStack());
        return false;
    }

    /** Оверлей, который откроется, когда предыдущий полностью уедет вниз. */
    private enum PendingOverlay { NONE, TRADE, HISTORY, INVENTORY }
    private PendingOverlay pendingOverlay = PendingOverlay.NONE;

    /** Хоть один оверлей открыт или ещё доигрывает анимацию. */
    private boolean overlayBusy() {
        return showingTradeOverlay || showingHistoryOverlay || showingInventoryOverlay
                || tradeOverlayProgress > 0.02f || historyOverlayProgress > 0.02f
                || inventoryOverlayProgress > 0.02f;
    }

    /**
     * Просит открытые оверлеи закрыться и ставит next в очередь.
     * true = закрытие пошло, открывать next прямо сейчас нельзя — плашки наедут друг на друга.
     */
    private boolean closeOverlaysBefore(PendingOverlay next) {
        boolean closing = false;
        if (showingTradeOverlay) { requestCloseTradeOverlay(); closing = true; }
        if (showingHistoryOverlay) { closeHistoryOverlay(); closing = true; }
        if (showingInventoryOverlay) { closeInventoryOverlay(); closing = true; }
        if (!closing && overlayBusy()) closing = true;   // предыдущий ещё доезжает вниз
        if (closing) pendingOverlay = next;
        return closing;
    }

    /** Очередь оверлеев: открываем следующий, только когда предыдущий уехал полностью. */
    private void tickPendingOverlay() {
        if (pendingOverlay == PendingOverlay.NONE || overlayBusy()) return;
        PendingOverlay next = pendingOverlay;
        pendingOverlay = PendingOverlay.NONE;
        switch (next) {
            case TRADE -> openTradeOverlay();
            case HISTORY -> openHistoryOverlay();
            case INVENTORY -> openInventoryOverlay();
            default -> { }
        }
    }

    private boolean wasTradeActiveBeforeInventory = false;
    private boolean wasHistoryActiveBeforeInventory = false;

    private void openInventoryOverlay() {
        showingInventoryOverlay = true;
        inventoryOverlayStartValue = inventoryOverlayProgress;
        inventoryOverlayTarget = 1.0f;
        inventoryOverlayAnimStart = net.minecraft.Util.getMillis();
    }

    private void closeInventoryOverlay() {
        returnCarriedItemToInventory();
        showingInventoryOverlay = false;
        inventoryOverlayStartValue = inventoryOverlayProgress;
        inventoryOverlayTarget = 0.0f;
        inventoryOverlayAnimStart = net.minecraft.Util.getMillis();
    }

    public void toggleInventoryOverlay() {
        if (!showingInventoryOverlay) {
            wasTradeActiveBeforeInventory = showingTradeOverlay;
            wasHistoryActiveBeforeInventory = showingHistoryOverlay;
            if (closeOverlaysBefore(PendingOverlay.INVENTORY)) return;
            openInventoryOverlay();
        } else {
            closeInventoryOverlay();
            // Возвращаемся к тому, что было открыто до инвентаря — но только когда он уедет.
            if (wasTradeActiveBeforeInventory) {
                pendingOverlay = PendingOverlay.TRADE;
                tradeNeedsStartPacket = true;   // сессию торга на сервере уже закрыли
                wasTradeActiveBeforeInventory = false;
            } else if (wasHistoryActiveBeforeInventory) {
                pendingOverlay = PendingOverlay.HISTORY;
                wasHistoryActiveBeforeInventory = false;
            }
        }
    }

    private void handleInventorySlotClick(int clickedSlot) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        net.minecraft.world.entity.player.Inventory inv = this.minecraft.player.getInventory();

        if (draggedSlotIndex == -1) {
            net.minecraft.world.item.ItemStack stack = inv.getItem(clickedSlot);
            if (!stack.isEmpty()) {
                draggedSlotIndex = clickedSlot;
                carriedItemStack = stack.copy();
            }
        } else {
            int originSlot = draggedSlotIndex;
            net.minecraft.world.item.ItemStack stackDest = inv.getItem(clickedSlot);

            if (originSlot == clickedSlot) {
                draggedSlotIndex = -1;
                carriedItemStack = net.minecraft.world.item.ItemStack.EMPTY;
            } else {
                inv.setItem(originSlot, stackDest);
                inv.setItem(clickedSlot, carriedItemStack);
                ModNetwork.sendToServer(new SwapInventorySlotsC2SPacket(originSlot, clickedSlot));

                draggedSlotIndex = -1;
                carriedItemStack = net.minecraft.world.item.ItemStack.EMPTY;
            }
        }
    }

    private void toggleHistoryOverlay() {
        if (!showingHistoryOverlay) {
            if (closeOverlaysBefore(PendingOverlay.HISTORY)) return;
            openHistoryOverlay();
        } else {
            closeHistoryOverlay();
        }
    }

    private void openHistoryOverlay() {
        showingHistoryOverlay = true;
        historyOverlayStartValue = historyOverlayProgress;
        historyOverlayTarget = 1.0f;
        historyOverlayAnimStart = net.minecraft.Util.getMillis();
    }

    private void closeHistoryOverlay() {
        showingHistoryOverlay = false;
        historyOverlayStartValue = historyOverlayProgress;
        historyOverlayTarget = 0.0f;
        historyOverlayAnimStart = net.minecraft.Util.getMillis();
    }

    public void toggleTradeOverlay() {
        if (!showingTradeOverlay) {
            tradeNeedsStartPacket = true;
            openTradeOverlay();   // сам встанет в очередь, если другой оверлей ещё уезжает
        } else {
            requestCloseTradeOverlay();
        }
    }

    /** Закрытие витрины: сперва уезжает панель товара, и только потом опускается сама витрина. */
    private void requestCloseTradeOverlay() {
        if (tradeDetailProgress > 0.02f) {
            animateTradeDetail(0.0f);
            pendingTradeClose = true;
            return;
        }
        pendingTradeClose = false;
        showingTradeOverlay = false;
        tradeOverlayStartValue = tradeOverlayProgress;
        tradeOverlayTarget = 0.0f;
        tradeOverlayAnimStart = net.minecraft.Util.getMillis();
        ModNetwork.sendToServer(new CloseTradeC2SPacket());
    }

    private void animateTradeDetail(float target) {
        if (tradeDetailTarget == target && tradeDetailAnimStart == 0L && tradeDetailProgress == target) return;
        tradeDetailStartValue = tradeDetailProgress;
        tradeDetailTarget = target;
        tradeDetailAnimStart = net.minecraft.Util.getMillis();
    }

    /** Клик по товару: открыть панель, а по уже выбранному — закрыть её. */
    private void toggleTradeDetail(int index) {
        if (index == selectedTradeIndex && tradeDetailTarget > 0.5f) {
            animateTradeDetail(0.0f);
            return;
        }
        setSelectedTrade(index);
        animateTradeDetail(1.0f);
    }

    private void resetTradeDetail() {
        selectedTradeIndex = -1;
        tradeDescScroll = 0;
        tradeDetailProgress = 0.0f;
        tradeDetailTarget = 0.0f;
        tradeDetailAnimStart = 0L;
        pendingTradeClose = false;
        descCacheIndex = -1;
    }

    public void updateTradeData(String shopName, List<net.ashpapi.interactentity.trade.TradeOffer> offers) {
        this.tradeShopName = shopName == null ? "" : shopName;
        this.tradeOffers = offers != null ? offers : new ArrayList<>();
        // Витрина приходит заново после каждой сделки — выбор и прокрутка не должны уехать за границы.
        if (selectedTradeIndex >= this.tradeOffers.size()) {
            selectedTradeIndex = Math.max(0, this.tradeOffers.size() - 1);
        }
        if (this.tradeOffers.isEmpty()) {
            tradeScrollRow = 0;
            resetTradeDetail();
        }
        descCacheIndex = -1;   // под тем же индексом теперь может лежать другой оффер
    }

    /** Выбор товара в витрине: описание всегда открывается с начала. */
    private void setSelectedTrade(int index) {
        if (selectedTradeIndex == index) return;
        selectedTradeIndex = index;
        tradeDescScroll = 0;
    }

    /** Запрос витрины у сервера шлём в момент реального открытия, а не постановки в очередь. */
    private boolean tradeNeedsStartPacket = false;

    public void openTradeOverlay() {
        if (showingTradeOverlay) return;
        if (closeOverlaysBefore(PendingOverlay.TRADE)) return;
        showingTradeOverlay = true;
        tradeScrollRow = 0;
        resetTradeDetail();
        tradeOverlayStartValue = tradeOverlayProgress;
        tradeOverlayTarget = 1.0f;
        tradeOverlayAnimStart = net.minecraft.Util.getMillis();
        if (tradeNeedsStartPacket) {
            tradeNeedsStartPacket = false;
            ModNetwork.sendToServer(new StartTradePacket(entityId));
        }
    }

    /** Закрытие по команде сервера: панель товара уезжает вместе с витриной, без пакета обратно. */
    public void closeTradeOverlay() {
        if (!showingTradeOverlay) return;
        showingTradeOverlay = false;
        pendingTradeClose = false;
        animateTradeDetail(0.0f);
        tradeOverlayStartValue = tradeOverlayProgress;
        tradeOverlayTarget = 0.0f;
        tradeOverlayAnimStart = net.minecraft.Util.getMillis();
    }

    public static DialogueScreen createStandaloneTrade(int entityId, String shopName, String dialogueId, List<net.ashpapi.interactentity.trade.TradeOffer> offers) {
        Minecraft mc = Minecraft.getInstance();
        String name = "";
        ResourceLocation avatar = null;
        if (mc.level != null) {
            net.minecraft.world.entity.Entity e = mc.level.getEntity(entityId);
            if (e != null) name = e.getDisplayName().getString();
        }
        DialogueScreen screen = new DialogueScreen(entityId, "trade", name, "", "none",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), avatar);
        screen.updateTradeData(shopName, offers);
        screen.openTradeOverlay();
        return screen;
    }

    private void rebuildHistoryCache() {
        if (this.width <= 0) {
            return;
        }
        cachedHistoryItems.clear();
        cachedHistoryTotalHeight = 0;
        
        int effectivePanelW = Math.min(PANEL_WIDTH, this.width - 40);
        int textAvailW = effectivePanelW - PADDING * 2 - 14;
        int scaledTextWidth = Math.max(1, (int)(textAvailW / TEXT_SCALE));

        List<SessionHistoryLine> historyList;
        synchronized (sessionHistory) {
            historyList = new ArrayList<>(sessionHistory);
        }

        String lang = getClientLanguage();
        for (SessionHistoryLine line : historyList) {
            String translatedSpeaker = net.ashpapi.interactentity.formatting.TranslationResolver.resolve(
                    net.ashpapi.interactentity.formatting.TranslationResolver.parseSafe(line.speaker), lang);
            String translatedText = net.ashpapi.interactentity.formatting.TranslationResolver.resolve(
                    net.ashpapi.interactentity.formatting.TranslationResolver.parseSafe(line.text), lang);

            List<FormattedCharSequence> splitLines = this.font.split(TextFormatter.format(translatedText), scaledTextWidth);
            int height = LINE_HEIGHT + splitLines.size() * (LINE_HEIGHT + LINE_SPACING) + 12 + 6;
            Component speakerComp = TextFormatter.format(translatedSpeaker);
            cachedHistoryItems.add(new CachedHistoryItem(line, splitLines, height, speakerComp));
            cachedHistoryTotalHeight += height;
        }
    }

    public void setFactionInfo(@Nullable String factionId, int reputation) {
        this.factionId = factionId;
        this.reputation = reputation;
    }

    public DialogueScreen(int entityId, String nodeId, String displayName, String text, String nodeType,
                          List<String> optionTexts, List<Integer> optionIndices,
                          List<Boolean> optionLocked, List<String> optionLockReasons,
                          @Nullable ResourceLocation avatarTexture) {
        super(Component.translatable("gui.interactentity.dialogue"));
        this.entityId = entityId;
        this.nodeId = nodeId;
        this.displayName = displayName;
        this.dialogueText = text;
        this.nodeType = nodeType;
        this.rawOptionTexts = new ArrayList<>(optionTexts);
        this.optionIndices = new ArrayList<>(optionIndices);
        this.optionLocked = new ArrayList<>(optionLocked);
        this.rawOptionLockReasons = new ArrayList<>(optionLockReasons);
        this.avatarTexture = avatarTexture;
        this.rawDisplayName = displayName;
        this.rawDialogueText = text;
        this.lastResolvedLang = "";
        resolveAllTexts();
        synchronized (sessionHistory) {
            this.sessionHistory.add(new SessionHistoryLine(displayName, text, false));
        }
        resetTypewriter();
        rebuildHistoryCache();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.stopUsingItem();
    }

    @Override
    protected void init() {
        super.init();
        this.useDefaultWindowTex = textureExists(DEFAULT_WINDOW);
        this.useDefaultOptionsTex = textureExists(DEFAULT_OPTIONS);
        this.useDefaultOptionsHoverTex = textureExists(DEFAULT_OPTIONS_HOVER);
        this.cachedTextAreaWidth = -1; // Сброс кэша текста при ресайзе
        this.cachedOptionAvailW = -1;
        this.cachedVisibleDialogueChars = -1;
        this.cachedTextLinesWidth = -1;
        this.lastResolvedLang = "";
        resolveAllTexts();
        rebuildHistoryCache();
    }

    @Override
    public void tick() {
        super.tick();
        if (firstOpen && animationTime < 1.0f) {
            animationTime = Mth.clamp(animationTime + ANIMATION_SPEED, 0.0f, 1.0f);
        } else if (!firstOpen) {
            animationTime = 1.0f;   // мгновенно для последующих нод
        }

        if (!showingPlayerResponse && !showingOptions && !isDialogueFullyVisible()) {
            typewriterTickCounter++;
            if (typewriterTickCounter >= TYPEWRITER_TICKS_PER_CHAR) {
                visibleDialogueChars = Math.min(totalDialogueChars, visibleDialogueChars + 1);
                typewriterTickCounter = 0;
            }
        } else {
            typewriterTickCounter = 0;
        }

        // Анимации оверлея и книжки — time-based ease-out, обновляются в render() по реальному времени.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        resolveAllTexts();
        if (net.ashpapi.interactentity.camera.DialogueCameraController.isTransitioningFromThirdPerson()) {
            return;
        }
        int effectivePanelW = Math.min(PANEL_WIDTH, this.width - 40);
        panelW = effectivePanelW;
        panelX = (this.width - panelW) / 2;

        Component nameComp = cachedNameComp != null ? cachedNameComp : TextFormatter.format(displayName);
        Component fullTextComp = TextFormatter.format(dialogueText);
        
        if (visibleDialogueChars != cachedVisibleDialogueChars || cachedVisibleTextComp == null) {
            cachedVisibleTextComp = TextFormatter.format(getVisibleDialogueText());
            cachedVisibleDialogueChars = visibleDialogueChars;
        }
        Component visibleTextComp = cachedVisibleTextComp;

        int textStartX = panelX + PADDING + HEAD_SIZE + 8;
        int textAreaWidth = panelW - PADDING * 2 - HEAD_SIZE - 8;
        int scaledTextWidth = Math.max(1, (int)(textAreaWidth / TEXT_SCALE));

        // ОПТИМИЗАЦИЯ: Кэшируем разбиение полного текста
        if (scaledTextWidth != cachedTextAreaWidth || !dialogueText.equals(lastDialogueText)) {
            cachedFullLines = this.font.split(TextFormatter.format(dialogueText), scaledTextWidth);
            cachedTextAreaWidth = scaledTextWidth;
            lastDialogueText = dialogueText;
        }

        if (scaledTextWidth != cachedTextLinesWidth || visibleDialogueChars != cachedTextLinesVisibleChars || cachedTextLines.isEmpty()) {
            cachedTextLines = this.font.split(visibleTextComp, scaledTextWidth);
            cachedTextLinesWidth = scaledTextWidth;
            cachedTextLinesVisibleChars = visibleDialogueChars;
        }
        List<FormattedCharSequence> textLines = cachedTextLines;

        optionHitboxes.clear();

        // When showingOptions=false for choice nodes, render as linear (no options inline)
        boolean renderingInlineOptions = "choice".equals(nodeType) && showingOptions;
        boolean showHint = false; // ПОДСКАЗКИ ОТКЛЮЧЕНЫ

        int contentH = cachedFullLines.size() * (LINE_HEIGHT + LINE_SPACING);
        int hintH = showHint ? (int)(10 * TEXT_SCALE) + 2 : 0;

        int textAreaNeeded = PADDING + LINE_HEIGHT + contentH + hintH + PADDING;

        panelH = Math.max(HEAD_SIZE + PADDING * 2, textAreaNeeded);
        panelY = this.height - HOTBAR_GAP - panelH;

        ResourceLocation tex = DEFAULT_WINDOW;
        boolean hasTex = useDefaultWindowTex;

        if (hasTex) {
            RenderSystem.enableBlend();
            // ТОЛЬКО ТЕКСТУРА И ТЕКСТ. Никаких программных теней/рамок.
            graphics.blitNineSliced(tex, panelX, panelY, panelW, panelH, 4, 32, 32, 0, 0);
        } else {
            // ПРОЦЕДУРНЫЙ ТЕМНЫЙ RPG ГУИ (по умолчанию)
            graphics.fill(panelX + 2, panelY + 2, panelX + panelW + 2, panelY + panelH + 2, SHADOW_COLOR);
            
            int alpha = (int)(0xCC * animationTime);
            int topColor = (alpha << 24) | (BG_COLOR_TOP & 0x00FFFFFF);
            int bottomColor = (alpha << 24) | (BG_COLOR_BOTTOM & 0x00FFFFFF);
            int borderColor = ((int)(0x66 * animationTime) << 24) | (BORDER_COLOR & 0x00FFFFFF);

            graphics.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, topColor, bottomColor);
            graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, borderColor);
            graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, borderColor);
            graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, borderColor);
            graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, borderColor);
        }

        int headX = panelX + PADDING;
        int headY = panelY + (panelH - HEAD_SIZE) / 2;
        
        // Центрируем блок текста (имя + реплики) относительно головы
        int totalTextHeight = LINE_HEIGHT + contentH;
        int nameY = panelY + (panelH - totalTextHeight) / 2;
        int textY = nameY + LINE_HEIGHT + 2; // Увеличено расстояние (было +0)

        if (showingPlayerResponse) {
            // Show player avatar + response text instead of NPC content
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ResourceLocation skin = mc.player.getSkinTextureLocation();
                RenderSystem.enableBlend();
                graphics.blit(skin, headX, headY, HEAD_SIZE, HEAD_SIZE, 8, 8, 8, 8, 64, 64);
            }
            String youLabel = net.minecraft.client.resources.language.I18n.get("gui.interactentity.dialogue.you");
            drawShadowedString(graphics, net.minecraft.network.chat.Component.literal(youLabel), textStartX, nameY, YOU_COLOR);
            Component respComp = TextFormatter.format(playerResponseText);
            for (FormattedCharSequence line : this.font.split(respComp, scaledTextWidth)) {
                drawStringScaled(graphics, line, textStartX, textY, TEXT_COLOR);
                textY += LINE_HEIGHT + LINE_SPACING;
            }
        } else {
            // NPC avatar + text
            ResourceLocation rawTexture = avatarTexture != null ? avatarTexture
                    : new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");
            ResourceLocation texture = net.ashpapi.interactentity.skin.ClientSkinRegistry.getDynamicOrFallback(rawTexture);
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, texture);
            graphics.blit(texture, headX, headY, HEAD_SIZE, HEAD_SIZE, 8, 8, 8, 8, 64, 64);

            drawShadowedString(graphics, nameComp, textStartX, nameY, NAME_COLOR);
            for (FormattedCharSequence line : textLines) {
                drawStringScaled(graphics, line, textStartX, textY, TEXT_COLOR);
                textY += LINE_HEIGHT + LINE_SPACING;
            }

            if (showHint) {
                String hintKey;
                if (!isDialogueFullyVisible()) {
                    hintKey = "gui.interactentity.hint.reveal";
                } else if ("end".equals(nodeType)) {
                    hintKey = "gui.interactentity.hint.close";
                } else if ("choice".equals(nodeType) && !showingOptions) {
                    hintKey = "gui.interactentity.hint.choose";
                } else {
                    hintKey = "gui.interactentity.hint.next";
                }
                String hint = net.minecraft.client.resources.language.I18n.get(hintKey);
                int hintWidth = (int)(this.font.width(hint) * TEXT_SCALE);
                drawStringScaled(graphics, Component.literal(hint),
                        panelX + panelW - PADDING - hintWidth, panelY + panelH - PADDING - (int)(8 * TEXT_SCALE), HINT_COLOR);
            }

            // Render Faction Info (Top-Right of the panel)
            if (factionId != null) {
                int repColor = reputation > 0 ? 0xFF55FF55 : (reputation < 0 ? 0xFFFF5555 : 0xFFAAAAAA);

                String repLabel = net.minecraft.client.resources.language.I18n.get("gui.interactentity.reputation");
                Component factionText = Component.literal(repLabel + ": ").withStyle(net.minecraft.ChatFormatting.GRAY)
                        .append(Component.literal(String.valueOf(reputation)).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(repColor)));
                
                int factionWidth = (int)(this.font.width(factionText) * TEXT_SCALE);
                drawStringScaled(graphics, factionText, panelX + panelW - 24 - factionWidth, panelY + 5, 0xFFFFFFFF);
            }

            // Кнопка-книжка истории в самом углу
            int bookX = panelX + panelW - 16;
            int bookY = panelY + 4;

            // Иконка торга слева от книжки (только если NPC — торговец)
            boolean isMerchant = isCurrentMerchant();
            int tradeX = tradeIconX();
            int tradeY = tradeIconY();
            if (isMerchant) {
                tradeHovered = isOverTradeIcon(mouseX, mouseY);
                boolean pouchTarget = showingTradeOverlay || tradeHovered;
                if (pouchTarget != lastPouchTargetOpen) {
                    pouchOpenStartValue = pouchOpenProgress;
                    pouchOpenTarget = pouchTarget ? 1.0f : 0.0f;
                    pouchOpenAnimStart = net.minecraft.Util.getMillis();
                    lastPouchTargetOpen = pouchTarget;
                }
                if (pouchOpenAnimStart > 0L) {
                    long pe = net.minecraft.Util.getMillis() - pouchOpenAnimStart;
                    float pt = Mth.clamp((float) pe / POUCH_OPEN_ANIM_MS, 0f, 1f);
                    float pEase = 1f - (1f - pt) * (1f - pt);
                    pouchOpenProgress = Mth.lerp(pEase, pouchOpenStartValue, pouchOpenTarget);
                    if (pt >= 1f) { pouchOpenProgress = pouchOpenTarget; pouchOpenAnimStart = 0L; }
                }
                drawVectorTradePouch(graphics, tradeX, tradeY, pouchOpenProgress, tradeHovered);
            } else {
                tradeHovered = false;
                pouchOpenProgress = 0.0f;
                lastPouchTargetOpen = false;
            }

            bookHovered = mouseX >= bookX - 2 && mouseX <= bookX + 14 && mouseY >= bookY && mouseY <= bookY + 12;
            boolean bookTarget = showingHistoryOverlay || bookHovered;
            if (bookTarget != lastBookTargetOpen) {
                bookOpenStartValue = bookOpenProgress;
                bookOpenTarget = bookTarget ? 1.0f : 0.0f;
                bookOpenAnimStart = net.minecraft.Util.getMillis();
                lastBookTargetOpen = bookTarget;
            }
            if (bookOpenAnimStart > 0L) {
                long be = net.minecraft.Util.getMillis() - bookOpenAnimStart;
                float bt = Mth.clamp((float) be / BOOK_OPEN_ANIM_MS, 0f, 1f);
                float bEase = 1f - (1f - bt) * (1f - bt);
                bookOpenProgress = Mth.lerp(bEase, bookOpenStartValue, bookOpenTarget);
                if (bt >= 1f) { bookOpenProgress = bookOpenTarget; bookOpenAnimStart = 0L; }
            }
            drawVectorBook(graphics, bookX, bookY, bookOpenProgress, bookHovered);
        }

        // Варианты ответа: под оверлеем их всё равно не видно, а хитбоксы ловили бы наведение и клик.
        if (renderingInlineOptions && !isAnyOverlayOpen()) {
            renderOptionPanels(graphics, mouseX, mouseY);
        }

        tickPendingOverlay();

        // Time-based ease-out для оверлея истории.
        if (historyOverlayAnimStart > 0L) {
            long he = net.minecraft.Util.getMillis() - historyOverlayAnimStart;
            float ht = Mth.clamp((float) he / HISTORY_OVERLAY_ANIM_MS, 0f, 1f);
            float hEase = 1f - (1f - ht) * (1f - ht);
            historyOverlayProgress = Mth.lerp(hEase, historyOverlayStartValue, historyOverlayTarget);
            if (ht >= 1f) { historyOverlayProgress = historyOverlayTarget; historyOverlayAnimStart = 0L; }
        }
        if (historyOverlayProgress > 0.01f) {
            renderHistoryOverlay(graphics, historyOverlayProgress);
        }

        // Time-based ease-out для выезжающей панели товара.
        if (tradeDetailAnimStart > 0L) {
            long de = net.minecraft.Util.getMillis() - tradeDetailAnimStart;
            float dt = Mth.clamp((float) de / TRADE_DETAIL_ANIM_MS, 0f, 1f);
            float dEase = 1f - (1f - dt) * (1f - dt);
            tradeDetailProgress = Mth.lerp(dEase, tradeDetailStartValue, tradeDetailTarget);
            if (dt >= 1f) { tradeDetailProgress = tradeDetailTarget; tradeDetailAnimStart = 0L; }
        }
        // Панель товара уехала — теперь можно опускать саму витрину.
        if (pendingTradeClose && tradeDetailProgress <= 0.02f) {
            requestCloseTradeOverlay();
        }

        // Time-based ease-out для оверлея торговли.
        if (tradeOverlayAnimStart > 0L) {
            long te = net.minecraft.Util.getMillis() - tradeOverlayAnimStart;
            float tt = Mth.clamp((float) te / TRADE_OVERLAY_ANIM_MS, 0f, 1f);
            float tEase = 1f - (1f - tt) * (1f - tt);
            tradeOverlayProgress = Mth.lerp(tEase, tradeOverlayStartValue, tradeOverlayTarget);
            if (tt >= 1f) { tradeOverlayProgress = tradeOverlayTarget; tradeOverlayAnimStart = 0L; }
        }
        if (tradeOverlayProgress > 0.01f) {
            renderTradeOverlay(graphics, tradeOverlayProgress, mouseX, mouseY);
        }

        // Time-based ease-out для оверлея инвентаря.
        if (inventoryOverlayAnimStart > 0L) {
            long ie = net.minecraft.Util.getMillis() - inventoryOverlayAnimStart;
            float it = Mth.clamp((float) ie / INVENTORY_OVERLAY_ANIM_MS, 0f, 1f);
            float iEase = 1f - (1f - it) * (1f - it);
            inventoryOverlayProgress = Mth.lerp(iEase, inventoryOverlayStartValue, inventoryOverlayTarget);
            if (it >= 1f) { inventoryOverlayProgress = inventoryOverlayTarget; inventoryOverlayAnimStart = 0L; }
        }
        if (inventoryOverlayProgress > 0.01f) {
            renderInventoryOverlay(graphics, inventoryOverlayProgress, mouseX, mouseY);
        }

        renderFloatingEffects(graphics);
    }

    private void renderFloatingEffects(GuiGraphics graphics) {
        long now = net.minecraft.Util.getMillis();
        activeFloatingTexts.removeIf(anim -> now - anim.startTime() > 1400L);

        for (FloatingTextAnim anim : activeFloatingTexts) {
            float progress = (now - anim.startTime()) / 1400.0f;
            float offsetY = progress * 30.0f;
            int alpha = (int) ((1.0f - progress) * 255);
            if (alpha <= 0) continue;

            int colorWithAlpha = (alpha << 24) | (anim.color() & 0x00FFFFFF);
            int curY = (int) (anim.startY() - offsetY);

            int textW = (int) (this.font.width(anim.text()) * TEXT_SCALE);
            int itemW = anim.stack().isEmpty() ? 0 : 16;
            int gap = anim.stack().isEmpty() ? 0 : 4;
            int totalW = textW + gap + itemW;

            int curX = anim.startX();

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);

            // Стильный аккуратный бейдж подложки
            int padH = 6;
            int padV = 4;
            int bgAlpha = (int) (0xDD * (1.0f - progress));
            int bgColor = (bgAlpha << 24) | 0x1A1A2E;
            int borderCol = (alpha << 24) | (anim.color() & 0x00FFFFFF);

            graphics.fill(curX - padH + 2, curY - padV + 2, curX + totalW + padH + 2, curY + 12 + padV + 2, ((int)(0x50 * (1.0f - progress)) << 24));
            graphics.fill(curX - padH, curY - padV, curX + totalW + padH, curY + 12 + padV, bgColor);
            graphics.fill(curX - padH, curY - padV, curX + totalW + padH, curY - padV + 1, borderCol);
            graphics.fill(curX - padH, curY + 12 + padV - 1, curX + totalW + padH, curY + 12 + padV, borderCol);
            graphics.fill(curX - padH, curY - padV, curX - padH + 1, curY + 12 + padV, borderCol);
            graphics.fill(curX + totalW + padH - 1, curY - padV, curX + totalW + padH, curY + 12 + padV, borderCol);

            drawStringScaled(graphics, Component.literal(anim.text()), curX, curY + 2, colorWithAlpha);

            if (!anim.stack().isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(curX + textW + gap, curY - 2, 0);
                graphics.pose().scale(0.85f, 0.85f, 1.0f);
                graphics.renderItem(anim.stack(), 0, 0);
                graphics.pose().popPose();
            }

            graphics.pose().popPose();
        }
    }

    private void renderOptionPanels(GuiGraphics graphics, int mouseX, int mouseY) {
        int optPanelX = 8;
        int optPanelY = 8;
        int numWidth = this.font.width("5. ");
        int maxPanelW = Math.min(this.width / 2 - 16, 200);
        int textAvailW = Math.max(1, maxPanelW - OPT_PADDING_H * 2 - numWidth);

        if (textAvailW != cachedOptionAvailW) {
            cachedOptionComps.clear();
            cachedOptionLines.clear();
            int panelW = 0;
            for (String opt : optionTexts) {
                Component c = TextFormatter.format(opt);
                cachedOptionComps.add(c);
                List<FormattedCharSequence> lines = this.font.split(c, textAvailW);
                cachedOptionLines.add(lines);
                int lineW = lines.stream().mapToInt(this.font::width).max().orElse(0);
                panelW = Math.max(panelW, numWidth + lineW + OPT_PADDING_H * 2);
            }
            cachedOptionPanelW = Math.min(panelW, maxPanelW);
            cachedOptionAvailW = textAvailW;
        }

        int panelW = cachedOptionPanelW;
        int curY = optPanelY;
        for (int i = 0; i < cachedOptionComps.size(); i++) {
            List<FormattedCharSequence> lines = cachedOptionLines.get(i);
            int panelH = lines.size() * (LINE_HEIGHT + LINE_SPACING) + OPT_PADDING_V * 2;
            boolean hovered = mouseMoved && (i == hoveredOption);

            boolean isLocked = i < optionLocked.size() && optionLocked.get(i);
            String lockReason = (i < optionLockReasons.size()) ? optionLockReasons.get(i) : "";

            int bgColor = isLocked ? OPT_LOCKED_BG : (hovered ? OPT_BG_HOVER : OPT_BG);
            int borderCol = isLocked ? OPT_LOCKED_BORDER : (hovered ? OPT_BORDER_HOVER : OPT_BORDER);

            int extraH = (isLocked && !lockReason.isEmpty()) ? LINE_HEIGHT : 0;
            panelH += extraH;

            int textX = optPanelX + OPT_PADDING_H;
            int textY = curY + OPT_PADDING_V;

            graphics.fill(optPanelX + 2, curY + 2, optPanelX + panelW + 2, curY + panelH + 2, SHADOW_COLOR);
            
            ResourceLocation optTex = isLocked ? DEFAULT_OPTIONS : (hovered ? DEFAULT_OPTIONS_HOVER : DEFAULT_OPTIONS);
            boolean hasOptTex;
            if (isLocked) {
                hasOptTex = useDefaultOptionsTex;
            } else {
                hasOptTex = hovered ? useDefaultOptionsHoverTex : useDefaultOptionsTex;
            }

            if (hasOptTex) {
                RenderSystem.enableBlend();
                // ТОЛЬКО КНОПКА-ТЕКСТУРА (без программных теней/рамок)
                graphics.blitNineSliced(optTex, optPanelX, curY, panelW, panelH, 4, 32, 32, 0, 0);
            } else {
                graphics.fill(optPanelX, curY, optPanelX + panelW, curY + panelH, bgColor);
                graphics.fill(optPanelX, curY, optPanelX + panelW, curY + 1, borderCol);
                graphics.fill(optPanelX, curY + panelH - 1, optPanelX + panelW, curY + panelH, borderCol);
                graphics.fill(optPanelX, curY, optPanelX + 1, curY + panelH, borderCol);
                graphics.fill(optPanelX + panelW - 1, curY, optPanelX + panelW, curY + panelH, borderCol);
            }

            if (isLocked) {
                graphics.drawString(this.font, "[X] ", textX, textY, OPT_LOCKED_TEXT, false);
                int lockIconW = this.font.width("[X] ");
                for (FormattedCharSequence line : lines) {
                    graphics.drawString(this.font, line, textX + lockIconW, textY, OPT_LOCKED_TEXT, false);
                    textY += LINE_HEIGHT + LINE_SPACING;
                }
                if (!lockReason.isEmpty()) {
                    graphics.drawString(this.font, lockReason, textX + lockIconW, textY, OPT_LOCKED_REASON, false);
                }
            } else {
                graphics.drawString(this.font, (i + 1) + ". ", textX, textY, OPT_NUM_COLOR, false);
                int textCol = hasOptTex ? 0xFF202020 : (hovered ? OPTION_HOVER : OPTION_COLOR);
                for (FormattedCharSequence line : lines) {
                    graphics.drawString(this.font, line, textX + numWidth, textY, textCol, false);
                    textY += LINE_HEIGHT + LINE_SPACING;
                }
            }

            optionHitboxes.add(new OptionHitbox(optPanelX, curY, panelW, panelH));
            curY += panelH + OPT_MARGIN;
        }
    }

    private void drawShadowedString(GuiGraphics graphics, Component text, int x, int y, int color) {
        drawStringScaled(graphics, text, x + 1, y + 1, 0x60000000);
        drawStringScaled(graphics, text, x, y, color);
    }

    private void drawStringScaled(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        graphics.drawString(this.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawStringScaled(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        graphics.drawString(this.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawStringScaled(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawStringScaled(GuiGraphics graphics, Component text, int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(this.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    /** Открыт (или анимируется) любой оверлей поверх окна диалога. */
    private boolean isAnyOverlayOpen() {
        return showingTradeOverlay || showingHistoryOverlay || showingInventoryOverlay
                || tradeOverlayProgress > 0.01f || historyOverlayProgress > 0.01f || inventoryOverlayProgress > 0.01f;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mouseMoved = true;
        hoveredOption = -1;
        for (int i = 0; i < optionHitboxes.size(); i++) {
            OptionHitbox hb = optionHitboxes.get(i);
            if (mouseX >= hb.x && mouseX <= hb.x + hb.width &&
                    mouseY >= hb.y && mouseY <= hb.y + hb.height) {
                if (!isOptionLocked(i)) hoveredOption = i;
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (net.ashpapi.interactentity.camera.DialogueCameraController.isTransitioningFromThirdPerson()) {
            return false;
        }
        if (button == 0) {
            if (showingInventoryOverlay && inventoryOverlayProgress > 0.5f) {
                for (InventorySlotHitbox hb : inventoryHitboxes) {
                    if (hb.contains((int) mouseX, (int) mouseY)) {
                        handleInventorySlotClick(hb.slotIndex());
                        if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
                            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }

            if (showingTradeOverlay && tradeOverlayProgress > 0.5f) {
                // Кнопки сделки перекрывают тело карточки — проверяем их первыми.
                for (TradeCardHitbox hb : tradeBuyHitboxes) {
                    if (hb.contains((int) mouseX, (int) mouseY)) {
                        executeTradeOffer(hb.index(), hb.x() + hb.w() / 2, hb.y() + 4);
                        return true;
                    }
                }
                for (TradeCardHitbox hb : tradeSelectHitboxes) {
                    if (hb.contains((int) mouseX, (int) mouseY)) {
                        toggleTradeDetail(hb.index());
                        playUiSound(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        return true;
                    }
                }
            }

            // Иконка торга (слева от книжки) — доступна только когда она нарисована
            if (isCurrentMerchant() && !showingPlayerResponse && isOverTradeIcon(mouseX, mouseY)) {
                toggleTradeOverlay();
                playUiSound(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }

            int bookX = panelX + panelW - 16;
            int bookY = panelY + 4;
            if (mouseX >= bookX - 2 && mouseX <= bookX + 14 && mouseY >= bookY && mouseY <= bookY + 12) {
                toggleHistoryOverlay();
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        // Если открыта торговля, история или инвентарь, клики по экрану НЕ продвигают диалог!
        if (showingTradeOverlay || showingHistoryOverlay || showingInventoryOverlay) {
            return true;
        }

        if (ignoreNextClick && button == 1) {
            ignoreNextClick = false;
            return true;
        }
        ignoreNextClick = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.stopUsingItem();

        if (showingPlayerResponse && button == 1 && !awaitingServerAfterResponse) {
            ModNetwork.sendToServer(new SelectOptionPacket(nodeId, pendingOptionRawIndex));
            // НЕ сбрасываем showingPlayerResponse — оставляем ответ игрока на экране,
            // пока сервер не пришлёт CloseDialogueS2CPacket (end) или updateDialogue (новая нода).
            // Иначе на 1+ кадр мигает старая NPC-реплика.
            awaitingServerAfterResponse = true;
            return true;
        }

        if (button == 1 && !showingPlayerResponse && !isDialogueFullyVisible()) {
            revealDialogueText();
            return true;
        }

        if ("end".equals(nodeType) && button == 1) {
            onClose();
            return true;
        }

        if ("linear".equals(nodeType) && button == 1) {
            ModNetwork.sendToServer(new NavigatePacket(nodeId, true));
            return true;
        }

        if ("choice".equals(nodeType)) {
            if (!showingOptions && button == 1) {
                showingOptions = true;
                DialogueCameraController.lookSideAt(entityId);
                return true;
            }
            if (showingOptions) {
                for (int i = 0; i < optionHitboxes.size(); i++) {
                    OptionHitbox hb = optionHitboxes.get(i);
                    if (mouseX >= hb.x && mouseX <= hb.x + hb.width &&
                            mouseY >= hb.y && mouseY <= hb.y + hb.height) {
                        if (!isOptionLocked(i)) selectOption(i);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showingHistoryOverlay && historyOverlayProgress > 0.5f) {
            int amount = (int) (delta * 14);
            historyScroll = Math.max(0, historyScroll - amount);
            return true;
        }
        if (showingTradeOverlay && tradeOverlayProgress > 0.5f) {
            // Над описанием колесо крутит описание, в остальной витрине — сетку по рядам.
            if (tradeDescArea != null && isMouseIn((int) mouseX, (int) mouseY,
                    tradeDescArea[0], tradeDescArea[1], tradeDescArea[2], tradeDescArea[3])) {
                tradeDescScroll = Math.max(0, tradeDescScroll - (int) Math.signum(delta));
                return true;
            }
            tradeScrollRow = Math.max(0, tradeScrollRow - (int) Math.signum(delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** Совершает сделку: проверки остатка и наличия предметов, звук, всплывающий текст, пакет серверу. */
    private void executeTradeOffer(int index, int animX, int animY) {
        if (index < 0 || index >= tradeOffers.size()) return;
        net.ashpapi.interactentity.trade.TradeOffer offer = tradeOffers.get(index);

        if ((!offer.isInfinite() && offer.getStock() <= 0) || !canPlayerAffordTrade(offer)) {
            playUiSound(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 1.0F));
            return;
        }

        playUiSound(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 1.2F));

        net.minecraft.world.item.ItemStack displayStack = offer.getDisplayStack();
        int count = displayStack.isEmpty() ? 1 : displayStack.getCount();
        String animText = offer.isBuy() ? ("+" + count) : ("-" + count);
        int animColor = offer.isBuy() ? 0xFF55FF55 : 0xFF55AAFF;
        activeFloatingTexts.add(new FloatingTextAnim(
                displayStack.copy(), animText, animX, animY, animColor, net.minecraft.Util.getMillis()));

        // Серверу нужен индекс в файле витрины: список у клиента отфильтрован по условиям.
        ModNetwork.sendToServer(new TradeActionPacket(entityId, offer.getSourceIndex()));
    }

    /** Выбор стрелками: панель товара открывается сразу, выбранная карточка остаётся видимой. */
    private void selectTradeByKey(int index) {
        setSelectedTrade(index);
        animateTradeDetail(1.0f);
        scrollToSelectedTrade();
    }

    /** Держит выбранную карточку в поле зрения при навигации стрелками. */
    private void scrollToSelectedTrade() {
        if (tradeCols <= 0 || selectedTradeIndex < 0) return;
        int row = selectedTradeIndex / tradeCols;
        if (row < tradeScrollRow) {
            tradeScrollRow = row;
        } else if (row >= tradeScrollRow + tradeRowsVisible) {
            tradeScrollRow = row - tradeRowsVisible + 1;
        }
    }

    private void playUiSound(SoundInstance sound) {
        if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
            this.minecraft.getSoundManager().play(sound);
        }
    }

    private void selectOption(int filteredIndex) {
        playerResponseText = optionTexts.get(filteredIndex);
        pendingOptionRawIndex = optionIndices.get(filteredIndex);
        showingOptions = false;
        showingPlayerResponse = true;
        DialogueCameraController.stopSide();
        
        String youLabel = net.minecraft.client.resources.language.I18n.get("gui.interactentity.dialogue.you");
        synchronized (sessionHistory) {
            this.sessionHistory.add(new SessionHistoryLine(youLabel, playerResponseText, true));
        }
        rebuildHistoryCache();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (net.ashpapi.interactentity.camera.DialogueCameraController.isTransitioningFromThirdPerson()) {
            return false;
        }
        if (keyCode == 256) { // ESC
            if (showingInventoryOverlay) {
                toggleInventoryOverlay();
                return true;
            }
            if (showingTradeOverlay) {
                // Первый Esc убирает панель товара, второй — всю витрину.
                if (tradeDetailTarget > 0.5f) {
                    animateTradeDetail(0.0f);
                } else {
                    toggleTradeOverlay();
                }
                return true;
            }
            if (showingHistoryOverlay) {
                toggleHistoryOverlay();
                return true;
            }
        }

        // Открытие/закрытие оверлея инвентаря игрока по нажатию назначенной клавиши (E по умолчанию)
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            toggleInventoryOverlay();
            if (this.minecraft.getSoundManager() != null) {
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }

        if (keyCode == 72) { // Клавиша H
            toggleHistoryOverlay();
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        // Навигация по витрине: стрелки выбирают товар (и открывают панель), Enter совершает сделку.
        if (showingTradeOverlay && tradeOverlayProgress > 0.5f && !tradeOffers.isEmpty()) {
            int cur = Math.max(0, selectedTradeIndex);
            switch (keyCode) {
                case 263 -> { // ←
                    selectTradeByKey(selectedTradeIndex < 0 ? 0 : Math.max(0, cur - 1));
                    return true;
                }
                case 262 -> { // →
                    selectTradeByKey(selectedTradeIndex < 0 ? 0 : Math.min(tradeOffers.size() - 1, cur + 1));
                    return true;
                }
                case 265 -> { // ↑
                    selectTradeByKey(selectedTradeIndex < 0 ? 0 : Math.max(cur % tradeCols, cur - tradeCols));
                    return true;
                }
                case 264 -> { // ↓
                    selectTradeByKey(selectedTradeIndex < 0 ? 0
                            : (cur + tradeCols < tradeOffers.size() ? cur + tradeCols : cur));
                    return true;
                }
                case 257, 335, 32 -> { // Enter / Numpad Enter / Space
                    executeTradeOffer(selectedTradeIndex, panelX + panelW / 2, panelY - 20);
                    return true;
                }
            }
        }

        // Если открыта торговля, история или инвентарь, клавиши НЕ продвигают диалог!
        if (showingTradeOverlay || showingHistoryOverlay || showingInventoryOverlay) {
            return true;
        }

        if (showingPlayerResponse && (keyCode == 257 || keyCode == 335 || keyCode == 32) && !awaitingServerAfterResponse) {
            ModNetwork.sendToServer(new SelectOptionPacket(nodeId, pendingOptionRawIndex));
            awaitingServerAfterResponse = true;
            return true;
        }
        if (!showingPlayerResponse && (keyCode == 257 || keyCode == 335 || keyCode == 32) && !isDialogueFullyVisible()) {
            revealDialogueText();
            return true;
        }
        if ("choice".equals(nodeType) && showingOptions && !optionTexts.isEmpty()) {
            if (keyCode >= 49 && keyCode <= 53) {
                int idx = keyCode - 49;
                if (idx < optionTexts.size() && !isOptionLocked(idx)) {
                    selectOption(idx);
                    return true;
                }
            }
            if (keyCode == 264) {
                hoveredOption = (hoveredOption + 1) % optionTexts.size();
                mouseMoved = true;
                return true;
            }
            if (keyCode == 265) {
                hoveredOption = (hoveredOption - 1 + optionTexts.size()) % optionTexts.size();
                mouseMoved = true;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                if (hoveredOption >= 0 && hoveredOption < optionTexts.size() && !isOptionLocked(hoveredOption)) {
                    selectOption(hoveredOption);
                    return true;
                }
            }
        }
        if (keyCode == 256) {
            if (showingHistoryOverlay) {
                toggleHistoryOverlay();
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        returnCarriedItemToInventory();
        clearClientTalkingOverride();
        DialogueCameraController.stop();
        super.onClose();
    }

    @Override
    public void removed() {
        returnCarriedItemToInventory();
        clearClientTalkingOverride();
        if (showingTradeOverlay) {
            ModNetwork.sendToServer(new CloseTradeC2SPacket());
        }
        ModNetwork.sendToServer(new CloseDialogueC2SPacket());
        DialogueCameraController.stop();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void updateDialogue(String nodeId, String displayName, String text, String nodeType,
                               List<String> optionTexts, List<Integer> optionIndices,
                               List<Boolean> optionLocked, List<String> optionLockReasons,
                               @Nullable ResourceLocation avatarTexture) {
        this.nodeId = nodeId;
        this.rawDisplayName = displayName;
        this.rawDialogueText = text;
        this.nodeType = nodeType;
        this.rawOptionTexts = new ArrayList<>(optionTexts);
        this.optionIndices = new ArrayList<>(optionIndices);
        this.optionLocked = new ArrayList<>(optionLocked);
        this.rawOptionLockReasons = new ArrayList<>(optionLockReasons);
        this.avatarTexture = avatarTexture;
        this.lastResolvedLang = "";
        resolveAllTexts();
        synchronized (sessionHistory) {
            this.sessionHistory.add(new SessionHistoryLine(displayName, text, false));
        }
        this.hoveredOption = -1;
        this.mouseMoved = false;
        this.firstOpen = false;
        this.animationTime = 1.0f;
        this.showingOptions = false;
        this.showingPlayerResponse = false;
        this.pendingOptionRawIndex = -1;
        this.awaitingServerAfterResponse = false;
        resetTypewriter();
        rebuildHistoryCache();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.stopUsingItem();
    }

    private String getClientLanguage() {
        try {
            return Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Exception e) {
            return "en_us";
        }
    }

    private void resolveAllTexts() {
        String lang = getClientLanguage();
        if (lang.equals(lastResolvedLang)) {
            return;
        }
        lastResolvedLang = lang;

        this.displayName = net.ashpapi.interactentity.formatting.TranslationResolver.resolve(
                net.ashpapi.interactentity.formatting.TranslationResolver.parseSafe(rawDisplayName), lang);
        this.dialogueText = net.ashpapi.interactentity.formatting.TranslationResolver.resolve(
                net.ashpapi.interactentity.formatting.TranslationResolver.parseSafe(rawDialogueText), lang);

        this.optionTexts = new ArrayList<>();
        if (rawOptionTexts != null) {
            for (String opt : rawOptionTexts) {
                this.optionTexts.add(net.ashpapi.interactentity.formatting.TranslationResolver.resolve(
                        net.ashpapi.interactentity.formatting.TranslationResolver.parseSafe(opt), lang));
            }
        }

        this.optionLockReasons = new ArrayList<>();
        if (rawOptionLockReasons != null) {
            for (String reason : rawOptionLockReasons) {
                this.optionLockReasons.add(net.ashpapi.interactentity.formatting.TranslationResolver.resolve(
                        net.ashpapi.interactentity.formatting.TranslationResolver.parseSafe(reason), lang));
            }
        }

        this.cachedNameComp = TextFormatter.format(displayName);
        this.cachedOptionAvailW = -1;
        this.cachedTextAreaWidth = -1;
        this.resetTypewriter();
        rebuildHistoryCache();
    }

    private boolean isOptionLocked(int index) {
        return index >= 0 && index < optionLocked.size() && optionLocked.get(index);
    }

    private void resetTypewriter() {
        this.totalDialogueChars = countVisibleChars(this.dialogueText);
        this.visibleDialogueChars = 0;
        this.typewriterTickCounter = 0;
        this.cachedVisibleTextComp = null;
        this.cachedVisibleDialogueChars = -1;
        this.cachedTextLinesWidth = -1;
        this.cachedTextLinesVisibleChars = -1;
    }

    private boolean isDialogueFullyVisible() {
        return visibleDialogueChars >= totalDialogueChars;
    }

    private void revealDialogueText() {
        this.visibleDialogueChars = this.totalDialogueChars;
        this.typewriterTickCounter = 0;
    }

    private void clearClientTalkingOverride() {
    }

    private String getVisibleDialogueText() {
        return sliceVisibleText(this.dialogueText, this.visibleDialogueChars);
    }

    private static int countVisibleChars(String raw) {
        if (raw == null || raw.isEmpty()) return 0;

        int visible = 0;
        for (int i = 0; i < raw.length();) {
            if (raw.charAt(i) == '&' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == '#' && i + 8 <= raw.length() && isValidHex(raw.substring(i + 2, i + 8))) {
                    i += 8;
                    continue;
                }
                if (isFormatCode(next)) {
                    i += 2;
                    continue;
                }
            }
            visible++;
            i++;
        }
        return visible;
    }

    private static String sliceVisibleText(String raw, int visibleLimit) {
        if (raw == null || raw.isEmpty() || visibleLimit <= 0) return "";

        StringBuilder builder = new StringBuilder();
        int visible = 0;
        for (int i = 0; i < raw.length() && visible < visibleLimit;) {
            if (raw.charAt(i) == '&' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == '#' && i + 8 <= raw.length() && isValidHex(raw.substring(i + 2, i + 8))) {
                    builder.append(raw, i, i + 8);
                    i += 8;
                    continue;
                }
                if (isFormatCode(next)) {
                    builder.append(raw, i, i + 2);
                    i += 2;
                    continue;
                }
            }

            builder.append(raw.charAt(i));
            visible++;
            i++;
        }
        return builder.toString();
    }

    private static boolean isFormatCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'l', 'o', 'n', 'm', 'k', 'r' -> true;
            default -> false;
        };
    }

    private static boolean isValidHex(String hex) {
        if (hex.length() != 6) return false;
        for (char c : hex.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /** Проверяет, является ли текущий NPC торговцем (флаг merchant включён). */
    private boolean isCurrentMerchant() {
        net.ashpapi.interactentity.data.ClientNpcRegistry.Entry entry =
                net.ashpapi.interactentity.data.ClientNpcRegistry.get(entityId);
        return entry != null
                && net.ashpapi.interactentity.data.ClientProgressData.isMerchantEnabled(entry.dialogueId);
    }

    /** Левый верхний угол иконки-мешочка. Хитбокс равен нарисованному квадрату TRADE_ICON_BOX. */
    private int tradeIconX() { return panelX + panelW - 34; }

    private int tradeIconY() { return panelY + 4; }

    private boolean isOverTradeIcon(double mx, double my) {
        int x = tradeIconX();
        int y = tradeIconY();
        return mx >= x && mx < x + TRADE_ICON_BOX && my >= y && my < y + TRADE_ICON_BOX;
    }

    /**
     * Иконка торга: кожаный мешочек 12×12 — узкая горловина, круглый низ.
     * При наведении кожа светлеет, горловина чуть приоткрывается и из неё
     * вылетают монетки; вся анимация укладывается в квадрат иконки.
     */
    private void drawVectorTradePouch(GuiGraphics graphics, int x, int y, float openProgress, boolean hovered) {
        boolean lit = openProgress > 0.5f;
        final int leatherDark = lit ? 0xFF5B3A1D : 0xFF3F2712;
        final int leather = lit ? 0xFFA26A32 : 0xFF7C5126;
        final int leatherLight = lit ? 0xFFC98F4C : 0xFF9E6C39;
        final int gold = lit ? 0xFFFFD65C : 0xFFD9AE3E;
        final int innerDark = 0xFF1C1208;

        // Силуэт: сверху узкая горловина, к низу мешок расширяется и скругляется.
        graphics.fill(x + 4, y + 2, x + 8, y + 3, leatherDark);
        graphics.fill(x + 3, y + 3, x + 9, y + 4, leather);
        graphics.fill(x + 2, y + 4, x + 10, y + 5, leather);
        graphics.fill(x + 1, y + 5, x + 11, y + 9, leather);
        graphics.fill(x + 2, y + 9, x + 10, y + 10, leather);
        graphics.fill(x + 3, y + 10, x + 9, y + 11, leatherDark);

        // Объём: блик слева-сверху, тень справа-снизу
        graphics.fill(x + 2, y + 5, x + 4, y + 8, leatherLight);
        graphics.fill(x + 9, y + 5, x + 11, y + 9, leatherDark);

        // Завязка на горловине
        graphics.fill(x + 3, y + 4, x + 9, y + 5, gold);

        if (openProgress < 0.05f) {
            graphics.fill(x + 5, y + 1, x + 7, y + 2, gold);   // кончик шнурка
        } else {
            // Приоткрытая горловина: маленькое тёмное отверстие с золотым ободком
            int half = 1 + Math.round(openProgress);           // полуширина 1 → 2
            int neckX1 = x + 6 - half;
            int neckX2 = x + 6 + half;
            int neckTop = y + 2 - Math.round(openProgress);
            graphics.fill(neckX1, neckTop, neckX2, y + 4, innerDark);
            graphics.fill(neckX1, neckTop, neckX2, neckTop + 1, gold);
        }

        // Монетки: путь y+2 → y, строго внутри иконки
        if (openProgress > 0.35f) {
            long time = net.minecraft.Util.getMillis();
            for (int i = 0; i < 3; i++) {
                float p = ((time + i * 260L) % 780L) / 780.0f;
                int coinY = y + 2 - Math.round(p * 2);
                int coinX = x + 3 + i * 3;
                int a = (int) (255 * (1.0f - p) * openProgress);
                if (a < 24) continue;
                graphics.fill(coinX, coinY, coinX + 2, coinY + 2, (a << 24) | 0x00E5B842);
                graphics.fill(coinX, coinY, coinX + 1, coinY + 1, (a << 24) | 0x00FFE9A0);
            }
        }
    }

    /**
     * Витрина: главная панель шириной с окно диалога поднимается снизу, а по клику по товару
     * из-под её правого края выезжает панель с информацией. Закрытие идёт в обратном порядке —
     * сначала панель товара уезжает влево, потом вся витрина опускается.
     */
    private void renderTradeOverlay(GuiGraphics graphics, float progress, int mouseX, int mouseY) {
        tradeBuyHitboxes.clear();
        tradeSelectHitboxes.clear();
        tradeDescArea = null;
        tradeHoveredItem = net.minecraft.world.item.ItemStack.EMPTY;

        int bottom = panelY - 2;
        int overlayH = Math.min(TRADE_H, bottom - 4);
        if (panelW < 140 || overlayH < 60) return;

        int revealH = (int) (overlayH * progress);
        if (revealH < 6) return;
        int top = bottom - overlayH;
        int revealTop = bottom - revealH;

        boolean hasOffers = tradeOffers != null && !tradeOffers.isEmpty();
        boolean hasSelection = hasOffers && selectedTradeIndex >= 0 && selectedTradeIndex < tradeOffers.size();

        int detailW = Math.min(TRADE_DETAIL_W, this.width - 12 - panelW - TRADE_DETAIL_GAP);
        boolean detailFits = detailW >= 96;
        float detailAnim = detailFits ? tradeDetailProgress : 0.0f;

        // Витрина стоит там же, где окно диалога, и сдвигается влево только если
        // выехавшая панель иначе не поместилась бы на экране.
        int overflow = Math.max(0, panelX + panelW + TRADE_DETAIL_GAP + detailW + 6 - this.width);
        int mainX = Math.max(4, panelX - (int) (overflow * detailAnim));
        boolean interactive = progress >= 0.999f;

        // Панель товара рисуется первой и обрезается по правому краю главной — выглядит
        // так, будто выезжает из-под неё.
        if (detailAnim > 0.01f && hasSelection) {
            int hidden = (int) ((1.0f - detailAnim) * (detailW + TRADE_DETAIL_GAP));
            int detailX = mainX + panelW + TRADE_DETAIL_GAP - hidden;
            graphics.enableScissor(mainX + panelW, revealTop,
                    mainX + panelW + TRADE_DETAIL_GAP + detailW + 4, bottom);
            drawTradePanelFrame(graphics, detailX, revealTop, detailW, bottom - revealTop, progress);
            renderTradeDetails(graphics, mouseX, mouseY, interactive && detailAnim > 0.99f,
                    detailX, top, overlayH, detailW);
            graphics.disableScissor();
        }

        drawTradePanelFrame(graphics, mainX, revealTop, panelW, bottom - revealTop, progress);
        graphics.enableScissor(mainX + 1, revealTop + 1, mainX + panelW - 1, bottom - 1);

        String defaultTitle = net.minecraft.client.resources.language.I18n.get("gui.interactentity.trade.title");
        String title = (tradeShopName == null || tradeShopName.isEmpty()) ? defaultTitle : tradeShopName;
        drawShadowedString(graphics, TextFormatter.format(title), mainX + TRADE_PAD, top + 6, NAME_COLOR);

        String closeHint = net.minecraft.client.resources.language.I18n.get("gui.interactentity.trade.close");
        drawStringScaled(graphics, Component.literal(closeHint),
                mainX + panelW - TRADE_PAD - scaledWidth(closeHint, TEXT_SCALE), top + 6, HINT_COLOR);

        int sepY = top + 18;
        graphics.fill(mainX + TRADE_PAD, sepY, mainX + panelW - TRADE_PAD, sepY + 1, 0x44FFFFFF);

        int contentTop = top + TRADE_HEADER_H;
        int contentH = overlayH - TRADE_HEADER_H - TRADE_PAD;

        if (!hasOffers) {
            String emptyMsg = net.minecraft.client.resources.language.I18n.get("gui.interactentity.trade.empty");
            drawStringScaled(graphics, Component.literal(emptyMsg),
                    mainX + (panelW - scaledWidth(emptyMsg, TEXT_SCALE)) / 2, contentTop + 8, HINT_COLOR);
        } else {
            int gridAvail = panelW - TRADE_PAD * 2;
            TradeGridLayout grid = computeGridLayout(gridAvail, contentH);
            renderTradeGrid(graphics, mouseX, mouseY, interactive, grid,
                    mainX + TRADE_PAD, gridAvail, contentTop, !detailFits);
        }

        graphics.disableScissor();

        if (!tradeHoveredItem.isEmpty()) {
            graphics.renderTooltip(this.font, tradeHoveredItem, mouseX, mouseY);
        }
    }

    /** Рамка панели витрины в стиле остальных оверлеев. */
    private void drawTradePanelFrame(GuiGraphics graphics, int x, int y, int w, int h, float progress) {
        graphics.fill(x + 2, y + 2, x + w + 2, y + h + 2, SHADOW_COLOR);
        int alpha = (int) (0xCC * progress);
        int topColor = (alpha << 24) | (BG_COLOR_TOP & 0x00FFFFFF);
        int bottomColor = (alpha << 24) | (BG_COLOR_BOTTOM & 0x00FFFFFF);
        int borderColor = ((int) (0x66 * progress) << 24) | (BORDER_COLOR & 0x00FFFFFF);
        graphics.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
        graphics.fill(x, y, x + w, y + 1, borderColor);
        graphics.fill(x, y + h - 1, x + w, y + h, borderColor);
        graphics.fill(x, y, x + 1, y + h, borderColor);
        graphics.fill(x + w - 1, y, x + w, y + h, borderColor);
    }

    /** Раскладка сетки: считается один раз и используется и для отрисовки, и для навигации. */
    private record TradeGridLayout(int cols, int cardW, int cardH, int rowsVisible, int totalRows, int height) {}

    /**
     * Ряды растягиваются по высоте области, а не жмутся комком: сколько рядов влезло —
     * столько и рисуем, добивая пустыми слотами, чтобы витрина выглядела целой.
     */
    private TradeGridLayout computeGridLayout(int gridAvail, int areaH) {
        int count = Math.max(1, tradeOffers.size());
        int usable = gridAvail - TRADE_SCROLLBAR_W - 3;
        int cols = Math.max(1, Math.min(6, (usable + TRADE_CARD_GAP) / (TRADE_CARD_MIN_W + TRADE_CARD_GAP)));
        cols = Math.min(cols, count);
        int cardW = Math.min(TRADE_CARD_MAX_W, (usable - TRADE_CARD_GAP * (cols - 1)) / cols);
        int totalRows = (count + cols - 1) / cols;
        int rows = Math.max(1, (areaH + TRADE_CARD_GAP) / (TRADE_CARD_H + TRADE_CARD_GAP));
        int cardH = Mth.clamp((areaH - TRADE_CARD_GAP * (rows - 1)) / rows, TRADE_CARD_H_MIN, TRADE_CARD_H_MAX);
        return new TradeGridLayout(cols, cardW, cardH, rows, totalRows,
                rows * cardH + TRADE_CARD_GAP * (rows - 1));
    }

    /** Сетка товаров с прокруткой по рядам: видимые карточки всегда целые. */
    private void renderTradeGrid(GuiGraphics graphics, int mouseX, int mouseY, boolean interactive,
                                 TradeGridLayout grid, int gridLeft, int gridAvail,
                                 int top, boolean showPrice) {
        int cols = grid.cols();
        int cardW = grid.cardW();
        int cardH = grid.cardH();
        int maxScrollRow = Math.max(0, grid.totalRows() - grid.rowsVisible());

        tradeCols = cols;
        tradeRowsVisible = grid.rowsVisible();
        tradeScrollRow = Mth.clamp(tradeScrollRow, 0, maxScrollRow);

        int rowW = cols * cardW + (cols - 1) * TRADE_CARD_GAP;
        int startX = gridLeft + (gridAvail - TRADE_SCROLLBAR_W - 3 - rowW) / 2;

        for (int row = 0; row < grid.rowsVisible(); row++) {
            for (int col = 0; col < cols; col++) {
                int index = (tradeScrollRow + row) * cols + col;
                int cx = startX + col * (cardW + TRADE_CARD_GAP);
                int cy = top + row * (cardH + TRADE_CARD_GAP);
                if (index < tradeOffers.size()) {
                    drawTradeCard(graphics, mouseX, mouseY, interactive, tradeOffers.get(index), index,
                            cx, cy, cardW, cardH, showPrice);
                } else {
                    graphics.fill(cx, cy, cx + cardW, cy + cardH, 0x55101024);
                    drawRectBorder(graphics, cx, cy, cardW, cardH, 0x33AAAACC);
                }
            }
        }

        if (maxScrollRow > 0) {
            int trackX = gridLeft + gridAvail - TRADE_SCROLLBAR_W;
            int trackH = grid.height();
            graphics.fill(trackX, top, trackX + TRADE_SCROLLBAR_W, top + trackH, 0x44000000);
            int thumbH = Math.max(12, trackH * grid.rowsVisible() / grid.totalRows());
            int thumbY = top + (trackH - thumbH) * tradeScrollRow / maxScrollRow;
            graphics.fill(trackX, thumbY, trackX + TRADE_SCROLLBAR_W, thumbY + thumbH, 0xAA8888AA);
        }
    }

    /** Карточка товара: иконка и кнопка сделки. Клик по телу открывает панель товара. */
    private void drawTradeCard(GuiGraphics graphics, int mouseX, int mouseY, boolean interactive,
                               net.ashpapi.interactentity.trade.TradeOffer offer, int index,
                               int x, int y, int w, int h, boolean showPrice) {
        boolean soldOut = !offer.isInfinite() && offer.getStock() <= 0;
        boolean affordable = canPlayerAffordTrade(offer);
        boolean selected = index == selectedTradeIndex && tradeDetailProgress > 0.5f;
        boolean hovered = isMouseIn(mouseX, mouseY, x, y, w, h);

        int bg = selected ? TRADE_CARD_BG_SELECTED : (hovered ? TRADE_CARD_BG_HOVER : TRADE_CARD_BG);
        int border = selected ? TRADE_CARD_BORDER_SELECTED : (hovered ? TRADE_CARD_BORDER_HOVER : TRADE_CARD_BORDER);
        graphics.fill(x, y, x + w, y + h, bg);
        drawRectBorder(graphics, x, y, w, h, border);

        int btnY = y + h - 3 - TRADE_BTN_H;
        int topArea = btnY - 3 - y;
        net.minecraft.world.item.ItemStack display = offer.getDisplayStack();

        // На карточке — только иконка: число рядом читалось как «продаётся пачкой».
        if (showPrice) {
            // Панель товара не помещается на экран — цену показываем прямо на карточке.
            if (!display.isEmpty()) {
                drawStackIcon(graphics, mouseX, mouseY, display, x + (w - 16) / 2, y + 5, 1);
            }
            drawStackChipRow(graphics, mouseX, mouseY, offer.getPriceStacks(),
                    x + 4, y + topArea - 14, w - 8, 2, offer.isBuy(), true);
        } else if (!display.isEmpty()) {
            // Иконка крупная, если карточка позволяет: мелкий значок в большой рамке теряется.
            int scale = topArea >= 40 ? 2 : 1;
            int size = 16 * scale;
            drawStackIcon(graphics, mouseX, mouseY, display,
                    x + (w - size) / 2, y + 3 + (topArea - size) / 2, scale);
        }

        int btnX = x + 4;
        int btnW = w - 8;
        boolean btnHovered = !soldOut && isMouseIn(mouseX, mouseY, btnX, btnY, btnW, TRADE_BTN_H);
        drawTradeButton(graphics, btnX, btnY, btnW, TRADE_BTN_H, offer, soldOut, affordable, btnHovered);

        if (interactive) {
            if (!soldOut) {
                tradeBuyHitboxes.add(new TradeCardHitbox(btnX, btnY, btnW, TRADE_BTN_H, index));
            }
            tradeSelectHitboxes.add(new TradeCardHitbox(x, y, w, h, index));
        }
    }

    private static final float CHIP_TEXT_SCALE = TEXT_SCALE;

    /** Ширина блока «иконка + ×N» — считается только для ряда цены. */
    private int measureStackChip(net.minecraft.world.item.ItemStack stack, int scale) {
        return 16 * scale + 2 + scaledWidth("×" + stack.getCount(), CHIP_TEXT_SCALE);
    }

    /** Иконка товара — без количества. */
    private void drawStackIcon(GuiGraphics graphics, int mouseX, int mouseY,
                               net.minecraft.world.item.ItemStack stack, int x, int y, int scale) {
        int size = 16 * scale;
        if (scale == 1) {
            graphics.renderItem(stack, x, y);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        }
        if (isMouseIn(mouseX, mouseY, x, y, size, size)) tradeHoveredItem = stack;
    }

    /**
     * Иконка и количество текстом справа — только для цены.
     * checkOwned = число краснеет, если у игрока столько предметов нет.
     */
    private void drawStackChip(GuiGraphics graphics, int mouseX, int mouseY,
                               net.minecraft.world.item.ItemStack stack, int x, int y, int scale, boolean checkOwned) {
        drawStackIcon(graphics, mouseX, mouseY, stack, x, y, scale);
        boolean lacking = checkOwned && !hasEnough(stack);
        drawStringScaled(graphics, Component.literal("×" + stack.getCount()),
                x + 16 * scale + 2, y + (16 * scale) / 2 - 3,
                lacking ? TRADE_LACKING_COLOR : 0xFFFFFFFF, CHIP_TEXT_SCALE);
    }

    /** Ряд блоков «иконка + ×N»: лишние сворачиваются в «+N», ряд не выходит за maxW. */
    private void drawStackChipRow(GuiGraphics graphics, int mouseX, int mouseY,
                                  List<net.minecraft.world.item.ItemStack> stacks,
                                  int left, int y, int maxW, int maxChips, boolean checkOwned, boolean center) {
        int total = 0;
        for (net.minecraft.world.item.ItemStack stack : stacks) {
            if (!stack.isEmpty()) total++;
        }
        if (total == 0) return;

        int shown = Math.min(total, maxChips);
        int rowW;
        while (true) {
            rowW = measureChipRow(stacks, shown, total);
            if (shown <= 1 || rowW <= maxW) break;
            shown--;
        }

        int px = center ? left + (maxW - rowW) / 2 : left;
        int drawn = 0;
        for (net.minecraft.world.item.ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (drawn >= shown) break;
            if (drawn > 0) px += 5;
            drawStackChip(graphics, mouseX, mouseY, stack, px, y, 1, checkOwned);
            px += measureStackChip(stack, 1);
            drawn++;
        }
        int extra = total - shown;
        if (extra > 0) {
            drawStringScaled(graphics, Component.literal("+" + extra), px + 3, y + 5, HINT_COLOR, TEXT_SCALE);
        }
    }

    private int measureChipRow(List<net.minecraft.world.item.ItemStack> stacks, int shown, int total) {
        int width = 0;
        int drawn = 0;
        for (net.minecraft.world.item.ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (drawn >= shown) break;
            if (drawn > 0) width += 5;
            width += measureStackChip(stack, 1);
            drawn++;
        }
        int extra = total - shown;
        if (extra > 0) width += 3 + scaledWidth("+" + extra, TEXT_SCALE);
        return width;
    }

    /** Кнопка сделки. Текст ужимается по ширине, чтобы никогда не вылезать за рамку. */
    private void drawTradeButton(GuiGraphics graphics, int x, int y, int w, int h,
                                 net.ashpapi.interactentity.trade.TradeOffer offer,
                                 boolean soldOut, boolean affordable, boolean hovered) {
        boolean isBuy = offer.isBuy();
        String actionKey = isBuy ? "gui.interactentity.trade.buy" : "gui.interactentity.trade.sell";
        String label;
        int bg, border, textColor;

        if (soldOut) {
            label = net.minecraft.client.resources.language.I18n.get("gui.interactentity.trade.sold_out");
            bg = TRADE_DISABLED_BG;
            border = TRADE_DISABLED_BORDER;
            textColor = TRADE_DISABLED_TEXT;
        } else if (!affordable) {
            // Цвет покупки/продажи сохраняем, но гасим: чего не хватает — видно по красному числу.
            label = net.minecraft.client.resources.language.I18n.get(actionKey);
            bg = isBuy ? TRADE_BUY_BG_DIM : TRADE_SELL_BG_DIM;
            border = isBuy ? TRADE_BUY_BORDER_DIM : TRADE_SELL_BORDER_DIM;
            textColor = TRADE_DISABLED_TEXT;
        } else {
            label = net.minecraft.client.resources.language.I18n.get(actionKey);
            bg = isBuy ? (hovered ? TRADE_BUY_BG_HOVER : TRADE_BUY_BG)
                       : (hovered ? TRADE_SELL_BG_HOVER : TRADE_SELL_BG);
            border = isBuy ? TRADE_BUY_BORDER : TRADE_SELL_BORDER;
            textColor = 0xFFFFFFFF;
        }

        graphics.fill(x, y, x + w, y + h, bg);
        drawRectBorder(graphics, x, y, w, h, border);

        float scale = TEXT_SCALE;
        int rawW = this.font.width(label);
        if (rawW * scale > w - 6) {
            scale = Math.max(0.6f, (w - 6.0f) / rawW);
        }
        int labelW = (int) (rawW * scale);
        int labelH = (int) (8 * scale);
        drawStringScaled(graphics, Component.literal(label),
                x + (w - labelW) / 2, y + (h - labelH) / 2, textColor, scale);
    }

    /** Строки описания выбранного товара; пустой список, если описания нет. */
    private List<FormattedCharSequence> tradeDescriptionLines(net.ashpapi.interactentity.trade.TradeOffer offer, int width) {
        String desc = offer.getDescription();
        if (desc == null || desc.isEmpty()) return List.of();
        if (selectedTradeIndex == descCacheIndex && width == descCacheWidth) return descCacheLines;
        descCacheLines = this.font.split(TextFormatter.format(desc), (int) (width / TRADE_DESC_SCALE));
        descCacheIndex = selectedTradeIndex;
        descCacheWidth = width;
        return descCacheLines;
    }

    /**
     * Выехавшая панель товара: превью, название, таблица «подпись — значение»
     * и описание, забирающее остаток места. Кнопка сделки прижата к низу.
     */
    private void renderTradeDetails(GuiGraphics graphics, int mouseX, int mouseY, boolean interactive,
                                    int x, int top, int h, int w) {
        net.ashpapi.interactentity.trade.TradeOffer offer = tradeOffers.get(selectedTradeIndex);
        boolean soldOut = !offer.isInfinite() && offer.getStock() <= 0;
        boolean affordable = canPlayerAffordTrade(offer);
        net.minecraft.world.item.ItemStack display = offer.getDisplayStack();

        int dy = top + 6;
        int previewX = x + (w - TRADE_PREVIEW) / 2;
        graphics.fill(previewX, dy, previewX + TRADE_PREVIEW, dy + TRADE_PREVIEW, 0x66000000);
        drawRectBorder(graphics, previewX, dy, TRADE_PREVIEW, TRADE_PREVIEW, TRADE_CARD_BORDER);
        if (!display.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(previewX + (TRADE_PREVIEW - 32) / 2.0f, dy + (TRADE_PREVIEW - 32) / 2.0f, 0);
            graphics.pose().scale(2.0f, 2.0f, 1.0f);
            graphics.renderItem(display, 0, 0);
            graphics.pose().popPose();
            if (isMouseIn(mouseX, mouseY, previewX, dy, TRADE_PREVIEW, TRADE_PREVIEW)) {
                tradeHoveredItem = display;
            }
        }
        dy += TRADE_PREVIEW + 3;

        String info = offer.getInfo();
        Component heading = (info != null && !info.isEmpty()) ? TextFormatter.format(info) : display.getHoverName();
        if (!display.isEmpty() || (info != null && !info.isEmpty())) {
            List<FormattedCharSequence> nameLines = this.font.split(heading, (int) ((w - 10) / TEXT_SCALE));
            for (int i = 0; i < Math.min(2, nameLines.size()); i++) {
                FormattedCharSequence line = nameLines.get(i);
                int lineW = (int) (this.font.width(line) * TEXT_SCALE);
                drawStringScaled(graphics, line, x + (w - lineW) / 2, dy, 0xFFFFFFFF);
                dy += 10;
            }
        }

        dy += 2;
        graphics.fill(x + 6, dy, x + w - 6, dy + 1, 0x33FFFFFF);
        dy += 5;

        // Значение идёт сразу за своей подписью, без общей колонки — иначе цена улетает вправо.
        String priceLabel = net.minecraft.client.resources.language.I18n.get(
                offer.isBuy() ? "gui.interactentity.trade.price" : "gui.interactentity.trade.reward") + ":";
        drawStringScaled(graphics, Component.literal(priceLabel), x + 6, dy + 4, HINT_COLOR);
        int priceValueX = x + 6 + scaledWidth(priceLabel, TEXT_SCALE) + 5;
        drawStackChipRow(graphics, mouseX, mouseY, offer.getPriceStacks(),
                priceValueX, dy, x + w - 6 - priceValueX, 3, offer.isBuy(), false);
        dy += 16;

        if (!offer.isInfinite()) {
            String stockLabel = net.minecraft.client.resources.language.I18n.get("gui.interactentity.trade.stock") + ":";
            drawStringScaled(graphics, Component.literal(stockLabel), x + 6, dy, HINT_COLOR);
            drawStringScaled(graphics, Component.literal(String.valueOf(Math.max(0, offer.getStock()))),
                    x + 6 + scaledWidth(stockLabel, TEXT_SCALE) + 5, dy,
                    soldOut ? TRADE_DISABLED_TEXT : TRADE_STOCK_COLOR);
            dy += 10;
        }

        int btnY = top + h - 6 - TRADE_DETAIL_BTN_H;
        List<FormattedCharSequence> descLines = tradeDescriptionLines(offer, w - 16);
        if (!descLines.isEmpty()) {
            int descTop = dy + 10;                       // строка «Описание:»
            int descSpace = btnY - 4 - descTop;
            // Строк рисуем не больше, чем их есть: места может хватить и на лишние.
            int visible = Math.min(descLines.size(), descSpace / TRADE_DESC_LINE_H);
            if (visible > 0) {
                String descLabel = net.minecraft.client.resources.language.I18n.get(
                        "gui.interactentity.trade.description") + ":";
                drawStringScaled(graphics, Component.literal(descLabel), x + 6, dy, HINT_COLOR);

                int descH = visible * TRADE_DESC_LINE_H;
                int maxScroll = Math.max(0, descLines.size() - visible);
                tradeDescScroll = Mth.clamp(tradeDescScroll, 0, maxScroll);
                tradeDescArea = new int[]{x + 6, descTop, w - 12, descH};

                for (int i = 0; i < visible; i++) {
                    drawStringScaled(graphics, descLines.get(i + tradeDescScroll),
                            x + 7, descTop + i * TRADE_DESC_LINE_H, TRADE_DESC_COLOR, TRADE_DESC_SCALE);
                }

                if (maxScroll > 0) {
                    int trackX = x + w - 9;
                    graphics.fill(trackX, descTop, trackX + 2, descTop + descH, 0x33000000);
                    int thumbH = Math.max(6, descH * visible / descLines.size());
                    int thumbY = descTop + (descH - thumbH) * tradeDescScroll / maxScroll;
                    graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0x998888AA);
                }
            }
        }

        int btnX = x + 6;
        int btnW = w - 12;
        boolean btnHovered = !soldOut && isMouseIn(mouseX, mouseY, btnX, btnY, btnW, TRADE_DETAIL_BTN_H);
        drawTradeButton(graphics, btnX, btnY, btnW, TRADE_DETAIL_BTN_H, offer, soldOut, affordable, btnHovered);
        if (interactive && !soldOut) {
            tradeBuyHitboxes.add(new TradeCardHitbox(btnX, btnY, btnW, TRADE_DETAIL_BTN_H, selectedTradeIndex));
        }
    }


    private void drawRectBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private boolean isMouseIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int scaledWidth(String text, float scale) {
        return (int) (this.font.width(text) * scale);
    }

    private void drawVectorBook(GuiGraphics graphics, int x, int y, float openProgress, boolean hovered) {
        int coverColor = hovered ? 0xFFC93B3B : 0xFF9E2A2B;
        int goldColor = 0xFFE5B842;
        int pageColor = 0xFFF5E6C8;
        int lineColor = 0xFF6E5D4F;

        if (openProgress < 0.05f) {
            // Закрытая книга (ширина 10, высота 12)
            graphics.fill(x, y, x + 2, y + 12, goldColor);
            graphics.fill(x + 2, y, x + 10, y + 12, coverColor);
            graphics.fill(x + 8, y + 5, x + 10, y + 7, goldColor);
            graphics.fill(x + 10, y + 1, x + 11, y + 11, pageColor);
            graphics.fill(x + 2, y + 11, x + 10, y + 12, pageColor);
        } else {
            int wLeft = (int) (8 * openProgress);
            int wRight = 8;
            int centerX = x + 6;

            if (wLeft > 0) {
                graphics.fill(centerX - wLeft, y, centerX, y + 12, coverColor);
            }
            graphics.fill(centerX, y, centerX + wRight, y + 12, coverColor);
            graphics.fill(centerX - 1, y, centerX + 1, y + 12, goldColor);

            if (wLeft > 2) {
                int pageLeft = centerX - wLeft + 1;
                int pageRight = centerX - 1;
                graphics.fill(pageLeft, y + 1, pageRight, y + 11, pageColor);

                if (openProgress > 0.7f) {
                    graphics.fill(pageLeft + 1, y + 3, pageRight - 1, y + 4, lineColor);
                    graphics.fill(pageLeft + 1, y + 5, pageRight - 1, y + 6, lineColor);
                    graphics.fill(pageLeft + 1, y + 7, pageRight - 1, y + 8, lineColor);
                    graphics.fill(pageLeft + 1, y + 9, pageRight - 2, y + 10, lineColor);
                }
            }

            if (wRight > 2) {
                int pageLeft = centerX + 1;
                int pageRight = centerX + wRight - 1;
                graphics.fill(pageLeft, y + 1, pageRight, y + 11, pageColor);

                if (openProgress > 0.7f) {
                    graphics.fill(pageLeft + 1, y + 3, pageRight - 1, y + 4, lineColor);
                    graphics.fill(pageLeft + 1, y + 5, pageRight - 2, y + 6, lineColor);
                    graphics.fill(pageLeft + 1, y + 7, pageRight - 1, y + 8, lineColor);
                    graphics.fill(pageLeft + 1, y + 9, pageRight - 1, y + 10, lineColor);
                }
            }
        }
    }

    private void renderHistoryOverlay(GuiGraphics graphics, float progress) {
        int historyH = 140;
        int currentHistoryH = (int)(historyH * progress);
        int historyY = panelY - currentHistoryH - 2;
        
        // Рисуем тень/задний фон
        graphics.fill(panelX + 2, historyY + 2, panelX + panelW + 2, historyY + currentHistoryH + 2, SHADOW_COLOR);
        
        int alpha = (int)(0xCC * progress);
        int topColor = (alpha << 24) | (BG_COLOR_TOP & 0x00FFFFFF);
        int bottomColor = (alpha << 24) | (BG_COLOR_BOTTOM & 0x00FFFFFF);
        int borderColor = ((int)(0x66 * progress) << 24) | (BORDER_COLOR & 0x00FFFFFF);

        graphics.fillGradient(panelX, historyY, panelX + panelW, historyY + currentHistoryH, topColor, bottomColor);
        graphics.fill(panelX, historyY, panelX + panelW, historyY + 1, borderColor);
        graphics.fill(panelX, historyY + currentHistoryH - 1, panelX + panelW, historyY + currentHistoryH, borderColor);
        graphics.fill(panelX, historyY, panelX + 1, historyY + currentHistoryH, borderColor);
        graphics.fill(panelX + panelW - 1, historyY, panelX + panelW, historyY + currentHistoryH, borderColor);

        // Заголовок рисуем ВНЕ scissor контента, чтобы прокручиваемый текст не накладывался на него.
        // Заворачиваем в условие, чтобы при анимации слайда текст не наезжал на рамку основного диалога
        if (currentHistoryH >= 20) {
            int titleY = historyY + 6;
            String historyTitle = net.minecraft.client.resources.language.I18n.get("gui.interactentity.journal.current_dialogue");
            drawStringScaled(graphics, Component.literal(historyTitle), panelX + PADDING, titleY, NAME_COLOR);
        }

        int listY = historyY + 18;
        int staticListH = 116;

        // Scissor только под область списка — заголовок остаётся «зафиксированным».
        int scissorBottom = Math.min(listY + staticListH, historyY + currentHistoryH - 1);
        if (scissorBottom <= listY) {
            return;
        }
        graphics.enableScissor(panelX + 1, listY, panelX + panelW - 1, scissorBottom);
        
        // Ограничиваем скролл истории по статическим границам
        int maxHistoryScroll = Math.max(0, cachedHistoryTotalHeight - staticListH);
        if (historyScroll > maxHistoryScroll) {
            historyScroll = maxHistoryScroll;
        }
        
        int curHistoryY = listY - historyScroll;
        for (CachedHistoryItem item : cachedHistoryItems) {
            int color = item.line.isPlayer ? YOU_COLOR : NAME_COLOR;
            int itemH = item.height;
            
            if (curHistoryY + itemH - 6 >= listY && curHistoryY < listY + staticListH) {
                // Рисуем полупрозрачную подложку блока
                graphics.fill(panelX + PADDING, curHistoryY, panelX + panelW - PADDING, curHistoryY + itemH - 6, 0x18FFFFFF);
                
                // Рисуем вертикальную полоску-акцент слева
                int accentColor = item.line.isPlayer ? 0xFF88DDFF : 0xFFFFCC44;
                graphics.fill(panelX + PADDING, curHistoryY, panelX + PADDING + 2, curHistoryY + itemH - 6, accentColor);
                
                // Рисуем имя спикера с форматированием
                drawStringScaled(graphics, item.formattedSpeaker, panelX + PADDING + 8, curHistoryY + 5, color);
                int textLineY = curHistoryY + LINE_HEIGHT + 8;
                
                // Рисуем текст реплики
                for (FormattedCharSequence seq : item.splitLines) {
                    drawStringScaled(graphics, seq, panelX + PADDING + 8, textLineY, TEXT_COLOR);
                    textLineY += LINE_HEIGHT + LINE_SPACING;
                }
            }
            curHistoryY += itemH;
        }
        
        // Отрисовка скроллбара по статическим границам
        if (cachedHistoryTotalHeight > staticListH) {
            int scrollbarX = panelX + panelW - 5;
            int thumbH = Math.max(12, (int) ((float) staticListH / cachedHistoryTotalHeight * staticListH));
            int thumbY = listY + (int) ((float) historyScroll / maxHistoryScroll * (staticListH - thumbH));
            
            graphics.fill(scrollbarX, listY, scrollbarX + 2, listY + staticListH, 0x33000000);
            graphics.fill(scrollbarX, thumbY, scrollbarX + 2, thumbY + thumbH, 0x88FFFFFF);
        }
        
        graphics.disableScissor();
    }

    private void renderInventoryOverlay(GuiGraphics graphics, float progress, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        int invH = 140;
        int currentInvH = (int)(invH * progress);
        int invY = panelY - currentInvH - 2;

        graphics.fill(panelX + 2, invY + 2, panelX + panelW + 2, invY + currentInvH + 2, SHADOW_COLOR);

        int alpha = (int)(0xCC * progress);
        int topColor = (alpha << 24) | (BG_COLOR_TOP & 0x00FFFFFF);
        int bottomColor = (alpha << 24) | (BG_COLOR_BOTTOM & 0x00FFFFFF);
        int borderColor = ((int)(0x66 * progress) << 24) | (BORDER_COLOR & 0x00FFFFFF);

        graphics.fillGradient(panelX, invY, panelX + panelW, invY + currentInvH, topColor, bottomColor);
        graphics.fill(panelX, invY, panelX + panelW, invY + 1, borderColor);
        graphics.fill(panelX, invY + currentInvH - 1, panelX + panelW, invY + currentInvH, borderColor);
        graphics.fill(panelX, invY, panelX + 1, invY + currentInvH, borderColor);
        graphics.fill(panelX + panelW - 1, invY, panelX + panelW, invY + currentInvH, borderColor);

        if (currentInvH >= 20) {
            int titleY = invY + 6;
            String invTitle = net.minecraft.client.resources.language.I18n.get("container.inventory");
            drawStringScaled(graphics, Component.literal(invTitle), panelX + PADDING, titleY, NAME_COLOR);
        }

        int listY = invY + 18;
        int staticListH = 116;

        int scissorBottom = Math.min(listY + staticListH, invY + currentInvH - 1);
        if (scissorBottom <= listY) return;

        graphics.enableScissor(panelX + 1, listY, panelX + panelW - 1, scissorBottom);
        inventoryHitboxes.clear();
        inventoryHoveredItem = net.minecraft.world.item.ItemStack.EMPTY;

        net.minecraft.world.entity.player.Inventory inv = this.minecraft.player.getInventory();
        int slotSize = 18;
        int slotGap = 2;
        int gridW = 9 * slotSize + 8 * slotGap;
        int gridLeft = panelX + (panelW - gridW) / 2;

        // 1. Основной инвентарь (слоты 9..35)
        int startY = listY + 4;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = 9 + row * 9 + col;
                int sx = gridLeft + col * (slotSize + slotGap);
                int sy = startY + row * (slotSize + slotGap);

                inventoryHitboxes.add(new InventorySlotHitbox(sx, sy, slotSize, slotIdx));

                boolean hovered = mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize && mouseY >= listY && mouseY <= scissorBottom;
                int slotBg = 0xCC1A1A2E;
                int slotBorder = 0x66AAAACC;

                graphics.fill(sx, sy, sx + slotSize, sy + slotSize, slotBg);
                graphics.fill(sx, sy, sx + slotSize, sy + 1, slotBorder);
                graphics.fill(sx, sy + slotSize - 1, sx + slotSize, sy + slotSize, slotBorder);
                graphics.fill(sx, sy, sx + 1, sy + slotSize, slotBorder);
                graphics.fill(sx + slotSize - 1, sy, sx + slotSize, sy + slotSize, slotBorder);

                if (slotIdx != draggedSlotIndex) {
                    net.minecraft.world.item.ItemStack stack = inv.getItem(slotIdx);
                    if (!stack.isEmpty()) {
                        graphics.renderItem(stack, sx + 1, sy + 1);
                        graphics.renderItemDecorations(this.font, stack, sx + 1, sy + 1, null);
                        if (hovered && carriedItemStack.isEmpty()) {
                            inventoryHoveredItem = stack;
                        }
                    }
                }
            }
        }

        // 2. Хотбар (слоты 0..8)
        int hotbarY = startY + 3 * (slotSize + slotGap) + 6;
        for (int col = 0; col < 9; col++) {
            int slotIdx = col;
            int sx = gridLeft + col * (slotSize + slotGap);
            int sy = hotbarY;

            inventoryHitboxes.add(new InventorySlotHitbox(sx, sy, slotSize, slotIdx));

            boolean hovered = mouseX >= sx && mouseX <= sx + slotSize && mouseY >= sy && mouseY <= sy + slotSize && mouseY >= listY && mouseY <= scissorBottom;
            int slotBg = 0xCC1A1A2E;
            int slotBorder = 0x66AAAACC;

            graphics.fill(sx, sy, sx + slotSize, sy + slotSize, slotBg);
            graphics.fill(sx, sy, sx + slotSize, sy + 1, slotBorder);
            graphics.fill(sx, sy + slotSize - 1, sx + slotSize, sy + slotSize, slotBorder);
            graphics.fill(sx, sy, sx + 1, sy + slotSize, slotBorder);
            graphics.fill(sx + slotSize - 1, sy, sx + slotSize, sy + slotSize, slotBorder);

            if (slotIdx != draggedSlotIndex) {
                net.minecraft.world.item.ItemStack stack = inv.getItem(slotIdx);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, sx + 1, sy + 1);
                    graphics.renderItemDecorations(this.font, stack, sx + 1, sy + 1, null);
                    if (hovered && carriedItemStack.isEmpty()) {
                        inventoryHoveredItem = stack;
                    }
                }
            }
        }

        graphics.disableScissor();

        if (!carriedItemStack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            graphics.renderItem(carriedItemStack, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(this.font, carriedItemStack, mouseX - 8, mouseY - 8, null);
            graphics.pose().popPose();
        } else if (!inventoryHoveredItem.isEmpty()) {
            graphics.renderTooltip(this.font, inventoryHoveredItem, mouseX, mouseY);
        }
    }

    private static class OptionHitbox {
        final int x, y, width, height;

        OptionHitbox(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
