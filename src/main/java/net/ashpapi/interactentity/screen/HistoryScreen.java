package net.ashpapi.interactentity.screen;

import net.ashpapi.interactentity.data.ClientProgressData;
import net.ashpapi.interactentity.formatting.TextFormatter;
import net.ashpapi.interactentity.history.DialogueHistoryEntry;
import net.ashpapi.interactentity.history.HistoryLine;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.ToggleTrackedQuestPacket;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class HistoryScreen extends Screen {
    private static final int WINDOW = 0xE00A0D14;
    private static final int WINDOW_HEADER_COLOR = 0xD6151823;
    private static final int PANEL_COLOR = 0xE011141D;
    private static final int PANEL_SOFT = 0xAA1A1E2B;
    private static final int PANEL_HOVER = 0x55313A4D;
    private static final int BORDER = 0x6672849F;
    private static final int BORDER_SOFT = 0x334E5A70;
    private static final int TEXT = 0xFFE1E3EA;
    private static final int MUTED = 0xFF9BA4B6;
    private static final int DIM = 0xFF687084;
    private static final int GOLD = 0xFFE3C46D;
    private static final int CYAN = 0xFF88B9CC;
    private static final int GREEN = 0xFF95C48A;
    private static final int RED = 0xFFE08B8B;

    private static final ResourceLocation DEFAULT_WINDOW_TEX = new ResourceLocation("interactentity", "textures/gui/journal_window.png");
    private static final ResourceLocation DEFAULT_SECTION_TEX = new ResourceLocation("interactentity", "textures/gui/journal_section.png");
    private static final ResourceLocation DEFAULT_SECTION_SOFT_TEX = new ResourceLocation("interactentity", "textures/gui/journal_section_soft.png");

    private static final float TEXT_SCALE = 0.76f;
    private static final int PADDING = 10;
    private static final int GAP = 8;
    private static final int WINDOW_HEADER = 30;
    private static final int HEADER = 24;
    private static final int ROW_HEIGHT = 20;
    private static final int QUEST_ROW_HEIGHT = 28;
    private static final int QUEST_DETAIL_BUTTON_HEIGHT = 18;
    private static final int HISTORY_LINE_GAP = 6;
    private static final int HISTORY_ACCENT_WIDTH = 2;
    private static final int HISTORY_ACCENT_PAD = 2;
    private static final int OBJECTIVE_ACCENT_WIDTH = HISTORY_ACCENT_WIDTH;
    private static final int OBJECTIVE_ACCENT_PAD = HISTORY_ACCENT_PAD;
    private static final int OBJECTIVE_MARKER_GAP = 6;
    private static final int TRACK_BUTTON_HPAD = 6;
    private static final int TRACK_LABEL_GAP = 8;
    private static final String BACK_ARROW = "\u2190";

    private int panelX, panelY, panelW, panelH;
    private int dialogueX, dialogueY, dialogueW, dialogueH;
    private int historyX, historyY, historyW, historyH;
    private int questX, questY, questW, questH;

    private int selectedDialogueIndex = -1;
    private String selectedQuestId = null;
    private boolean showingCharacterDetails = false;
    
    private float expansionProgress = 0f;
    private float startExpansionValue = 0f;
    private long lastStateChangeTime = 0;
    private static final long ANIM_DURATION = 350; // Длительность анимации в мс (0.35 сек)

    private float detailRevealProgress = 0f;
    private float startDetailRevealValue = 0f;
    private float targetDetailReveal = 1f;
    private boolean closingCharacterDetails = false;
    private long lastDetailChangeTime = 0;
    private static final long DETAIL_ANIM_DURATION = 400;

    private int dialogueScroll = 0;
    private int historyScroll = 0;
    private int questScroll = 0;
    private int detailsScroll = 0;
    private int draggingScrollbar = -1; // -1=none, 0=dialogue, 1=history, 2=quest, 3=details

    // Анимация hover на кнопках X / стрелка назад: 0..1, лерпится по target
    private float closeButtonHover = 0f;
    private float backArrowHover = 0f;
    private float closeButtonPress = 0f;
    private float backArrowPress = 0f;

    // Геометрия кнопки X (вычисляется в render() и используется в hit-тесте)
    private int closeButtonX = 0, closeButtonY = 0;
    // Текущая (анимированная) геометрия панели — нужна mouseClicked'у чтобы корректно
    // определять «клик за пределами окна» именно по видимым границам, а не по полному размеру.
    private int currentPanelXCache = 0;
    private int currentPanelWCache = 0;
    private static final int CLOSE_BUTTON_SIZE = 14;
    private static final String CLOSE_X = "✕"; // ✕

    private final Map<String, LivingEntity> entityCache = new HashMap<>();
    
    // Оптимизация: кэш текстур и текста
    private boolean useWindowTex;
    private boolean useSectionTex;
    private boolean useSectionSoftTex;
    private final Map<HistoryLine, List<FormattedCharSequence>> historyLinesCache = new HashMap<>();
    private int lastHistoryWidth = -1;

    public HistoryScreen() {
        super(Component.translatable("gui.interactentity.journal"));
    }

    @Override
    protected void init() {
        int maxW = Math.min(960, this.width - 44);
        int maxH = Math.min(520, this.height - 34);
        panelW = Math.max(340, maxW);
        panelH = Math.max(240, maxH);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int sideW = Math.min(200, Math.max(120, panelW / 5)); 
        dialogueW = sideW;      // Сделали чуть шире
        questW = sideW;         // Сделали чуть уже
        historyW = panelW - dialogueW - questW - GAP * 2 - PADDING * 2;

        dialogueX = panelX + PADDING;
        dialogueY = panelY + WINDOW_HEADER + PADDING;
        dialogueH = panelH - WINDOW_HEADER - PADDING * 2;

        historyX = dialogueX + dialogueW + GAP;
        historyY = dialogueY;
        historyH = dialogueH;

        questX = historyX + historyW + GAP;
        questY = dialogueY;
        questH = dialogueH;

        // Кэшируем проверку текстур
        this.useWindowTex = textureExists(DEFAULT_WINDOW_TEX);
        this.useSectionTex = textureExists(DEFAULT_SECTION_TEX);
        this.useSectionSoftTex = textureExists(DEFAULT_SECTION_SOFT_TEX);
        this.historyLinesCache.clear();
        this.lastHistoryWidth = -1;
    }

    @Override
    public void tick() {
        super.tick();
        // Мы перешли на time-based анимацию в render(), 
        // поэтому в tick() ничего делать не нужно для плавности.
    }

    @Override
    public void removed() {
        entityCache.values().forEach(Entity::discard);
        entityCache.clear();
        historyLinesCache.clear();
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        clampScrollOffsets();

        // Прокручиваем reveal-анимацию ДО branch-выбора, чтобы close-завершение
        // переключило showingCharacterDetails в том же кадре (иначе один frame пустой).
        updateDetailRevealAnimation();

        // 1. Плавная интерполяция по времени (Time-based LERP)
        if (lastStateChangeTime > 0) {
            long elapsed = net.minecraft.Util.getMillis() - lastStateChangeTime;
            float timeFactor = net.minecraft.util.Mth.clamp((float)elapsed / ANIM_DURATION, 0.0f, 1.0f);
            
            // Плавная кривая (Ease-Out) для профессионального ощущения
            float ease = 1.0f - (1.0f - timeFactor) * (1.0f - timeFactor);
            
            float target = (selectedDialogueIndex >= 0 && !closingCharacterDetails) ? 1.0f : 0.0f;
            expansionProgress = net.minecraft.util.Mth.lerp(ease, startExpansionValue, target);
        }

        graphics.fill(0, 0, this.width, this.height, 0x7A05070D);

        // 2. Динамический расчет размеров
        int targetFullW = panelW;
        int compactW = dialogueW + PADDING * 2;
        int currentPanelW = (int) net.minecraft.util.Mth.lerp(expansionProgress, compactW, targetFullW);
        int currentPanelX = (this.width - currentPanelW) / 2;
        currentPanelXCache = currentPanelX;
        currentPanelWCache = currentPanelW;

        // Обновляем координаты относительно динамического центра
        dialogueX = currentPanelX + PADDING;
        historyX = dialogueX + dialogueW + GAP;
        questX = historyX + historyW + GAP;

        // 3. Отрисовка основного окна (на полной высоте сразу)
        if (useWindowTex) {
            graphics.blitNineSliced(DEFAULT_WINDOW_TEX, currentPanelX, panelY, currentPanelW, panelH, 6, 64, 64, 0, 0);
        } else {
            graphics.fill(currentPanelX + 4, panelY + 5, currentPanelX + currentPanelW + 4, panelY + panelH + 5, 0x66000000);
            graphics.fill(currentPanelX, panelY, currentPanelX + currentPanelW, panelY + panelH, WINDOW);
            drawBorder(graphics, currentPanelX, panelY, currentPanelW, panelH, BORDER_SOFT);
            graphics.fill(currentPanelX + 1, panelY + 1, currentPanelX + currentPanelW - 1, panelY + WINDOW_HEADER, WINDOW_HEADER_COLOR);
            graphics.fill(currentPanelX + 1, panelY + WINDOW_HEADER - 1, currentPanelX + currentPanelW - 1, panelY + WINDOW_HEADER, BORDER);
        }

        // 4. Заголовок (всегда рисуем, с обрезкой по текущей ширине)
        // Резервируем место справа под X-кнопку — заголовок не должен под неё заезжать
        int titleClipRight = currentPanelX + currentPanelW - 4 - CLOSE_BUTTON_SIZE - 6;
        graphics.enableScissor(currentPanelX + 4, panelY + 4, titleClipRight, panelY + WINDOW_HEADER - 3);
        drawStringScaled(graphics, Component.literal(tr("journal_title")), currentPanelX + 14, panelY + 11, 0xFFFFFFFF);
        graphics.disableScissor();

        // 4b. Кнопка X — закрытие окна.
        closeButtonX = currentPanelX + currentPanelW - CLOSE_BUTTON_SIZE - 6;
        closeButtonY = panelY + (WINDOW_HEADER - CLOSE_BUTTON_SIZE) / 2;
        boolean closeHovered = isInsideCloseButton(mouseX, mouseY);
        // Плавный лерп hover/press к таргетам каждый кадр.
        closeButtonHover = net.minecraft.util.Mth.lerp(0.20f, closeButtonHover, closeHovered ? 1f : 0f);
        closeButtonPress = Math.max(0f, closeButtonPress - 0.08f);
        drawCloseButton(graphics, closeButtonX, closeButtonY, closeButtonHover, closeButtonPress);

        // 5. Панель персонажей (всегда видна)
        drawSection(graphics, dialogueX, dialogueY, dialogueW, dialogueH, tr("dialogues"), CYAN, showingCharacterDetails, mouseX, mouseY);
        if (showingCharacterDetails) {
            renderCharacterDetails(graphics, mouseX, mouseY);
            drawScrollbar(graphics, dialogueX + dialogueW - 6, dialogueY + HEADER + 5, dialogueH - HEADER - 10, getDetailsContentHeight(), detailsScroll);
        } else {
            renderDialogueList(graphics, mouseX, mouseY);
            drawScrollbar(graphics, dialogueX + dialogueW - 6, dialogueY + HEADER + 5, dialogueH - HEADER - 10, getDialogueContentHeight(), dialogueScroll);
        }

        // 6. Панели Истории и Квестов (раздвигаются с обрезкой)
        if (expansionProgress > 0.01f) {
            // Обрезаем контент по ширине окна
            graphics.enableScissor(currentPanelX + 1, panelY + 1, currentPanelX + currentPanelW - 1, panelY + panelH - 1);
            
            drawSection(graphics, historyX, historyY, historyW, historyH, tr("dialogue_history"), GOLD, false, mouseX, mouseY);
            renderSelectedHistory(graphics);
            drawScrollbar(graphics, historyX + historyW - 6, historyY + HEADER + 5, historyH - HEADER - 10, getHistoryContentHeight(), historyScroll);

            drawSection(graphics, questX, questY, questW, questH, tr("character_quests"), GREEN, selectedQuestId != null, mouseX, mouseY);
            renderSelectedQuests(graphics, mouseX, mouseY);
            drawScrollbar(graphics, questX + questW - 6, questY + HEADER + 5, questH - HEADER - 10, getQuestContentHeight(), questScroll);
            
            graphics.disableScissor();
        }
    }

    /** Прокручивает таймер reveal-анимации. Вызывать ОДИН раз в начале render(),
     *  до выбора ветки renderCharacterDetails / renderDialogueList — чтобы переключение
     *  state в момент завершения close-анимации происходило в том же кадре. */
    private void updateDetailRevealAnimation() {
        if (lastDetailChangeTime > 0) {
            long elapsed = net.minecraft.Util.getMillis() - lastDetailChangeTime;
            float t = net.minecraft.util.Mth.clamp((float)elapsed / DETAIL_ANIM_DURATION, 0.0f, 1.0f);
            float ease = 1.0f - (1.0f - t) * (1.0f - t);
            detailRevealProgress = net.minecraft.util.Mth.lerp(ease, startDetailRevealValue, targetDetailReveal);
            if (t >= 1.0f) {
                detailRevealProgress = targetDetailReveal;
                lastDetailChangeTime = 0;
                if (closingCharacterDetails) {
                    showingCharacterDetails = false;
                    closingCharacterDetails = false;
                    selectedDialogueIndex = -1;
                }
            }
        } else if (!showingCharacterDetails && !closingCharacterDetails) {
            detailRevealProgress = 0f;
        }
    }

    private void renderCharacterDetails(GuiGraphics graphics, int mouseX, int mouseY) {
        DialogueHistoryEntry entry = getSelectedDialogue();
        if (entry == null) {
            showingCharacterDetails = false;
            closingCharacterDetails = false;
            return;
        }

        int contentX = dialogueX + 4;
        int contentY = dialogueY + HEADER + 5;
        int contentW = dialogueW - 8;
        int clipTop = dialogueY + HEADER + 5;
        int clipBottom = dialogueY + dialogueH - 10;

        // 1. Имя — рендерим ДО scissor, чтобы оно не клиппалось во время close-анимации
        // и было видно непрерывно (его положение совпадает с позицией строки в списке).
        // Лерп X из позиции списка (после иконки головы) к левому краю (contentX+2 как у «Фракция»).
        int nameY = contentY + 6 - detailsScroll;
        Component name = TextFormatter.format(entry.getDisplayName());
        // listX = dialogueX + 4 = contentX. Иконка: listX+5, размер 12, gap 4 → текст начинается с listX+21.
        int listNameX = contentX + 21;
        int openNameX = contentX + 2;
        int nameX = (int) net.minecraft.util.Mth.lerp(detailRevealProgress, listNameX, openNameX);
        if (nameY + 10 > clipTop && nameY < clipBottom) {
            drawStringScaled(graphics, name, nameX, nameY, GOLD);
        }

        // Применяем анимацию "сверху-вниз" через Scissor для всего ОСТАЛЬНОГО контента
        int animHeight = (int) ( (clipBottom - clipTop) * detailRevealProgress );
        graphics.enableScissor(contentX, clipTop, contentX + contentW, clipTop + animHeight);

        int y = nameY + 10;

        // 2. Фракция (белый текст, '-' если пусто)
        String faction = entry.getFactionLabel();
        String fValue = (faction != null && !faction.isEmpty()) ? faction : "-";
        String fLabel = tr("bestiary.faction") + ": " + fValue;
        if (y + 10 > clipTop && y < clipBottom) drawStringScaled(graphics, Component.literal(fLabel), contentX + 2, y, 0xFFFFFFFF);
        y += 10;

        // 3. Отрисовка 3D модели
        if (entry.getEntityType() != null) {
            LivingEntity dummy = getCachedEntity(entry.getEntityType());
            if (dummy != null) {
                int modelX = contentX + contentW / 2;
                int modelY = y + 85;

                int slideOffset = (int) (15 * (1.0f - detailRevealProgress));
                if (modelY - 90 < clipBottom && modelY > clipTop) {
                    InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, modelX, modelY - slideOffset, 30, (float)modelX - mouseX, (float)modelY - 40 - mouseY, dummy);
                }
                y += 100;
            }
        }

        // 4. Отношение (в одну строку)
        if (entry.getReputationId() != null) {
            int rep = ClientProgressData.getReputation(entry.getReputationId());
            Component rLabel = Component.literal(tr("bestiary.relationship") + ": ").withStyle(net.minecraft.ChatFormatting.WHITE);
            Component rStatus = getReputationStatus(rep);
            Component combined = Component.literal("").append(rLabel).append(rStatus);

            if (y + 10 > clipTop && y < clipBottom) drawStringScaled(graphics, combined, contentX + 2, y, -1);
            y += 10;
        }

        // 5. Квесты
        int completed = getCompletedQuestCount(entry);
        String qLabel = tr("bestiary.quests") + ": " + completed;
        if (y + 12 > clipTop && y < clipBottom) drawStringScaled(graphics, Component.literal(qLabel), contentX + 2, y, 0xFFFFFFFF);
        y += 12;

        // 6. Описание (Lore) — сужаем правый край (rightInset) чтобы текст не упирался в границу
        String lore = entry.getCharacterInfo();
        if (lore != null && !lore.isEmpty()) {
            Component loreComp = TextFormatter.format(lore);
            int loreRightInset = 14;
            drawWrapped(graphics, loreComp, contentX + 2, y, contentW - loreRightInset, MUTED, clipTop, clipBottom);
        }

        graphics.disableScissor();
    }

    private Component getReputationStatus(int value) {
        String key;
        int color;
        if (value <= -50) { key = "status.hostile"; color = 0xFFFF5555; }
        else if (value <= -20) { key = "status.unfriendly"; color = 0xFFFFAA00; }
        else if (value < 20) { key = "status.neutral"; color = 0xFFAAAAAA; }
        else if (value < 50) { key = "status.friendly"; color = 0xFF55FF55; }
        else { key = "status.allied"; color = 0xFF55FFFF; }
        
        return Component.literal(tr(key) + " (" + value + ")").withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(color));
    }

    private int getCompletedQuestCount(DialogueHistoryEntry dialogue) {
        int count = 0;
        for (QuestState quest : ClientProgressData.getAllQuests().values()) {
            if (belongsToDialogue(quest, dialogue) && "completed".equals(quest.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private LivingEntity getCachedEntity(String typeId) {
        return entityCache.computeIfAbsent(typeId, id -> {
            try {
                Optional<EntityType<?>> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id)) != null 
                        ? Optional.of(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id))) : Optional.empty();
                
                if (type.isPresent() && Minecraft.getInstance().level != null) {
                    Entity entity = type.get().create(Minecraft.getInstance().level);
                    if (entity instanceof LivingEntity living) return dummySetup(living);
                }
            } catch (Exception ignored) {}
            return null;
        });
    }

    private LivingEntity dummySetup(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
        return entity;
    }

    private void renderDialogueList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<DialogueHistoryEntry> history = ClientProgressData.getHistory();
        int listX = dialogueX + 4;
        int listY = dialogueY + HEADER + 5;
        int listW = dialogueW - 8;
        int listH = dialogueH - HEADER - 10;

        if (history.isEmpty()) {
            drawEmptyState(graphics, tr("empty_dialogues_title"), tr("empty_dialogues_subtitle"), dialogueX, dialogueY, dialogueW, dialogueH);
            return;
        }

        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        int y = listY - dialogueScroll;
        for (int i = 0; i < history.size(); i++) {
            DialogueHistoryEntry entry = history.get(i);
            int rowY = y + i * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listH) continue;

            boolean selected = i == selectedDialogueIndex;
            boolean hovered = isInside(mouseX, mouseY, listX, rowY, listW, ROW_HEIGHT);

            if (!useSectionTex) {
                int bg = selected ? 0x55425068 : hovered ? 0x33313A4D : 0;
                if (bg != 0) graphics.fill(listX, rowY, listX + listW, rowY + ROW_HEIGHT - 2, bg);
                graphics.fill(listX, rowY, listX + 2, rowY + ROW_HEIGHT - 2, selected ? GOLD : BORDER_SOFT);
            }

            // Иконка-голова из avatar-текстуры (область 8×8 от (8,8) — лицо в стандартном player-skin layout).
            int iconSize = 12;
            int iconX = listX + 5;
            int iconY = rowY + (ROW_HEIGHT - 2 - iconSize) / 2;
            drawAvatarHead(graphics, entry.getAvatar(), iconX, iconY, iconSize);

            Component label = TextFormatter.format(entry.getDisplayName());
            drawStringScaled(graphics, label, listX + 7 + iconSize + 4, rowY + 6, selected ? GOLD : TEXT);
        }        graphics.disableScissor();
    }

    /** Рисует квадратную иконку-голову из 64x64 player-skin текстуры (берёт face UV (8,8)→(16,16)). */
    private void drawAvatarHead(GuiGraphics graphics, String avatarPath, int x, int y, int size) {
        net.minecraft.resources.ResourceLocation tex = avatarPath != null && !avatarPath.isEmpty()
                ? net.minecraft.resources.ResourceLocation.tryParse(avatarPath)
                : null;
        if (tex == null) tex = new net.minecraft.resources.ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");
        // Лёгкая рамка вокруг иконки для контраста.
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF1B1F2A);
        graphics.blit(tex, x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
    }

    private void renderSelectedHistory(GuiGraphics graphics) {
        DialogueHistoryEntry entry = getSelectedDialogue();
        int contentX = historyX + PADDING;
        int contentY = historyY + HEADER + 7;
        int contentW = historyW - PADDING * 2;
        int clipTop = historyY + HEADER + 5;
        int clipBottom = historyY + historyH - PADDING;

        if (entry == null) {
            drawEmptyState(graphics, tr("select_character_title"), tr("select_character_subtitle"), historyX, historyY, historyW, historyH);
            return;
        }

        if (entry.getLines().isEmpty()) {
            drawEmptyState(graphics, tr("empty_history_title"), tr("empty_history_subtitle"), historyX, historyY, historyW, historyH);
            return;
        }

        graphics.enableScissor(historyX + 1, clipTop, historyX + historyW - 1, clipBottom);
        int y = contentY - historyScroll;
        for (HistoryLine line : entry.getLines()) {
            y = renderHistoryLine(graphics, line, contentX, y, contentW, clipTop, clipBottom);
        }
        graphics.disableScissor();
    }

    private int renderHistoryLine(GuiGraphics graphics, HistoryLine line, int x, int y, int width, int clipTop, int clipBottom) {
        boolean player = "player".equals(line.getSpeaker());
        int blockH = historyLineHeight(line, width);
        int accent = player ? CYAN : GOLD;

        // Рендерим акцентную линию ТОЛЬКО если нет текстур ресурспака
        if (y + blockH >= clipTop && y <= clipBottom) {
            int textAvailW = width - (useSectionTex ? 0 : HISTORY_ACCENT_WIDTH + 8);
            
            // ОПТИМИЗАЦИЯ: Кэшируем разбиение текста строки
            if (width != lastHistoryWidth) {
                historyLinesCache.clear();
                lastHistoryWidth = width;
            }
            
            List<FormattedCharSequence> lines = historyLinesCache.computeIfAbsent(line, l -> {
                Component textComp = TextFormatter.format(speakerLabel(l) + "&r: " + l.getText());
                return this.font.split(textComp, unscaledWidth(textAvailW));
            });

            if (!useSectionTex) {
                int accentTop = y - HISTORY_ACCENT_PAD;
                int accentBottom = y + blockH - HISTORY_ACCENT_PAD;
                graphics.fill(x, accentTop, x + HISTORY_ACCENT_WIDTH, accentBottom, accent);
                
                int curY = y;
                for (FormattedCharSequence seq : lines) {
                    if (curY >= clipTop && curY < clipBottom) {
                        drawStringScaled(graphics, seq, x + HISTORY_ACCENT_WIDTH + 8, curY, TEXT);
                    }
                    curY += lineHeight();
                }
            } else {
                int curY = y;
                for (FormattedCharSequence seq : lines) {
                    if (curY >= clipTop && curY < clipBottom) {
                        drawStringScaled(graphics, seq, x, curY, TEXT);
                    }
                    curY += lineHeight();
                }
            }
        }
        return y + blockH + HISTORY_LINE_GAP;
    }

    private void renderSelectedQuests(GuiGraphics graphics, int mouseX, int mouseY) {
        DialogueHistoryEntry dialogue = getSelectedDialogue();
        int contentX = questX + PADDING;
        int contentY = questY + HEADER + 7;
        int contentW = questW - PADDING * 2;
        int clipTop = questY + HEADER + 5;
        int clipBottom = questY + questH - PADDING;

        if (dialogue == null) {
            drawEmptyState(graphics, tr("no_character_title"), tr("no_character_subtitle"), questX, questY, questW, questH);
            return;
        }

        List<QuestState> quests = getQuestsFor(dialogue);
        if (quests.isEmpty()) {
            drawEmptyState(graphics, tr("empty_character_quests_title"), tr("empty_character_quests_subtitle"), questX, questY, questW, questH);
            return;
        }

        QuestState selectedQuest = getSelectedQuest(quests);
        if (selectedQuest != null) {
            graphics.enableScissor(questX + 1, clipTop, questX + questW - 1, clipBottom);
            renderQuestDetails(graphics, selectedQuest, contentX, contentY - questScroll, contentW, clipTop, clipBottom, mouseX, mouseY);
            graphics.disableScissor();
            return;
        }

        graphics.enableScissor(questX + 1, clipTop, questX + questW - 1, clipBottom);
        int y = contentY - questScroll;
        for (QuestState quest : quests) {
            if (y + QUEST_ROW_HEIGHT >= clipTop && y <= clipBottom) {
                renderQuestCard(graphics, quest, contentX, y, contentW, mouseX, mouseY);
            }
            y += QUEST_ROW_HEIGHT;
        }
        graphics.disableScissor();
    }

    private void renderQuestCard(GuiGraphics graphics, QuestState quest, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, x - 3, y - 2, width + 6, QUEST_ROW_HEIGHT - 2);
        boolean tracked = ClientProgressData.isQuestTracked(quest.getId());
        int color = statusColor(quest);

        if (!useSectionTex) {
            int bg = hovered ? PANEL_HOVER : 0x66131620;
            graphics.fill(x - 3, y - 2, x + width + 3, y + QUEST_ROW_HEIGHT - 3, bg);
            if (hovered || tracked) drawBorder(graphics, x - 3, y - 2, width + 6, QUEST_ROW_HEIGHT - 1, tracked ? 0x6695C48A : BORDER_SOFT);
            graphics.fill(x - 3, y - 2, x, y + QUEST_ROW_HEIGHT - 3, color);
        }

        int titleW = width - (tracked ? 22 : 8);
        graphics.enableScissor(x + 5, y, x + 5 + titleW, y + QUEST_ROW_HEIGHT - 3);
        drawStringScaled(graphics, TextFormatter.format(quest.getTitle()), x + 5, y + 4, color);
        graphics.disableScissor();
        
        int statusCol = useSectionTex ? 0xFF202020 : MUTED;
        drawStringScaled(graphics, Component.literal(statusText(quest)), x + 5, y + 15, statusCol);
        
        if (tracked && !useSectionTex) {
            graphics.fill(x + width - 12, y + 5, x + width - 8, y + 9, CYAN);
        }
    }

    private void renderQuestDetails(GuiGraphics graphics, QuestState quest, int x, int y, int width,
                                    int clipTop, int clipBottom, int mouseX, int mouseY) {
        int curY = y;
        int color = statusColor(quest);

        if (curY + 12 > clipTop && curY < clipBottom) {
            drawStringScaled(graphics, Component.literal(tr("description")), x, curY, 0xFFFFFFFF);
        }
        curY += 13;

        if (!quest.getDescription().isEmpty()) {
            Component desc = TextFormatter.format(quest.getDescription());
            int descCol = useSectionTex ? 0xFF202020 : TEXT;
            curY = drawWrapped(graphics, desc, x, curY, width, descCol, clipTop, clipBottom) + 8;
        }

        if (!quest.getObjectives().isEmpty()) {
            if (!useSectionTex) {
                drawDivider(graphics, x, curY, width, clipTop, clipBottom);
                curY += 10;
            } else {
                curY += 5;
            }
        }

        if (!quest.getObjectives().isEmpty()) {
            if (curY >= clipTop && curY < clipBottom) {
                drawStringScaled(graphics, Component.literal(tr("objectives")), x, curY, 0xFFFFFFFF);
            }
            curY += 13;
            for (String objective : quest.getObjectives()) {
                String objectiveText = QuestState.objectiveText(objective);
                int markerX = x + OBJECTIVE_ACCENT_WIDTH + OBJECTIVE_MARKER_GAP;
                int textX = x + objectiveTextOffset();
                int textWidth = objectiveTextWidth(width);
                Component objectiveComponent = TextFormatter.format(objectiveText);
                int textH = wrappedHeight(objectiveComponent, textWidth);
                int objH = textH + OBJECTIVE_ACCENT_PAD * 2;
                if (curY + objH >= clipTop && curY < clipBottom) {
                    if (!useSectionTex) {
                        int accentTop = curY - OBJECTIVE_ACCENT_PAD;
                        int accentBottom = curY + textH + OBJECTIVE_ACCENT_PAD;
                        int accent = QuestState.isObjectiveCompleted(objective) ? GREEN : DIM;
                        graphics.fill(x, accentTop, x + OBJECTIVE_ACCENT_WIDTH, accentBottom, accent);
                    }
                    int markerY = curY + Math.max(0, (textH - lineHeight()) / 2) + 1;
                    drawObjectiveMarker(graphics, objective, markerX, markerY);
                    int objTextCol = useSectionTex ? 0xFF202020 : TEXT;
                    drawWrapped(graphics, objectiveComponent, textX, curY + 1, textWidth, objTextCol, clipTop, clipBottom);
                }
                curY += objH + 2;
            }
        }

        if ("active".equals(quest.getStatus())) {
            curY += 12;
            drawTrackButton(graphics, quest, x, curY, width, clipTop, clipBottom, mouseX, mouseY);
        }
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y, int height, int totalContentHeight, int scrollAmount) {
        if (totalContentHeight <= height) return;
        
        // Дорожка
        graphics.fill(x, y, x + 3, y + height, 0x33000000);
        
        // Ползунок
        int thumbH = Math.max(12, (int) ((float) height / totalContentHeight * height));
        int maxScroll = totalContentHeight - height;
        int thumbY = y + (int) ((float) scrollAmount / maxScroll * (height - thumbH));
        
        graphics.fill(x, thumbY, x + 3, thumbY + thumbH, 0x88FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // X-кнопка — закрытие.
        if (isInsideCloseButton(mouseX, mouseY)) {
            closeButtonPress = 1f;
            onClose();
            return true;
        }
        // Outside-click закрывает — но границы берутся по ТЕКУЩЕЙ (анимированной) ширине панели,
        // а не по полной. Иначе при сжатом виде кликать «снаружи» приходилось у самого края экрана.
        if (!isInside(mouseX, mouseY, currentPanelXCache, panelY, currentPanelWCache, panelH)) {
            onClose();
            return true;
        }

        // Проверка клика по скроллбарам
        if (isInside(mouseX, mouseY, dialogueX + dialogueW - 6, dialogueY + HEADER + 5, 4, dialogueH - HEADER - 10)) {
            draggingScrollbar = showingCharacterDetails ? 3 : 0;
            return true;
        }
        if (isInside(mouseX, mouseY, historyX + historyW - 6, historyY + HEADER + 5, 4, historyH - HEADER - 10)) {
            draggingScrollbar = 1;
            return true;
        }
        if (isInside(mouseX, mouseY, questX + questW - 6, questY + HEADER + 5, 4, questH - HEADER - 10)) {
            draggingScrollbar = 2;
            return true;
        }

        if (isInsideQuestBackButton(mouseX, mouseY)) {
            selectedQuestId = null;
            questScroll = 0;
            return true;
        }

        if (isInsideCharacterBackButton(mouseX, mouseY)) {
            // Запускаем close-анимацию (снизу вверх). selectedDialogueIndex обнулится когда анимация закончится.
            closingCharacterDetails = true;
            startExpansionValue = expansionProgress;
            lastStateChangeTime = net.minecraft.Util.getMillis();
            startDetailRevealValue = detailRevealProgress;
            targetDetailReveal = 0f;
            lastDetailChangeTime = net.minecraft.Util.getMillis();
            return true;
        }

        if (isInside(mouseX, mouseY, dialogueX, dialogueY + HEADER, dialogueW, dialogueH - HEADER)) {
            if (!showingCharacterDetails) {
                int oldIndex = selectedDialogueIndex;
                selectDialogueAt(mouseY);
                if (selectedDialogueIndex >= 0 && selectedDialogueIndex != oldIndex) {
                    showingCharacterDetails = true;
                    closingCharacterDetails = false;
                    dialogueScroll = 0;
                    detailsScroll = 0;
                    startExpansionValue = expansionProgress;
                    lastStateChangeTime = net.minecraft.Util.getMillis();
                    startDetailRevealValue = 0f;
                    targetDetailReveal = 1f;
                    lastDetailChangeTime = net.minecraft.Util.getMillis();
                }
            }
            return true;
        }

        if (isInside(mouseX, mouseY, questX, questY + HEADER, questW, questH - HEADER)) {
            clickQuestArea(mouseX, mouseY);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double deltaY) {
        if (draggingScrollbar == 0) {
            dialogueScroll = handleScrollbarDrag(mouseY, dialogueY + HEADER + 5, dialogueH - HEADER - 10, getDialogueContentHeight());
            return true;
        }
        if (draggingScrollbar == 1) {
            historyScroll = handleScrollbarDrag(mouseY, historyY + HEADER + 5, historyH - HEADER - 10, getHistoryContentHeight());
            return true;
        }
        if (draggingScrollbar == 2) {
            questScroll = handleScrollbarDrag(mouseY, questY + HEADER + 5, questH - HEADER - 10, getQuestContentHeight());
            return true;
        }
        if (draggingScrollbar == 3) {
            detailsScroll = handleScrollbarDrag(mouseY, dialogueY + HEADER + 5, dialogueH - HEADER - 10, getDetailsContentHeight());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, deltaY);
    }

    private int handleScrollbarDrag(double mouseY, int y, int height, int totalContentHeight) {
        int thumbH = Math.max(12, (int) ((float) height / totalContentHeight * height));
        float relativePos = (float) (mouseY - y - thumbH / 2.0) / (height - thumbH);
        int maxScroll = Math.max(0, totalContentHeight - height);
        return (int) (net.minecraft.util.Mth.clamp(relativePos, 0.0f, 1.0f) * maxScroll);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void selectDialogueAt(double mouseY) {
        List<DialogueHistoryEntry> history = ClientProgressData.getHistory();
        int listY = dialogueY + HEADER + 6;
        int index = ((int) mouseY - listY + dialogueScroll) / ROW_HEIGHT;
        if (index < 0 || index >= history.size()) return;

        selectedDialogueIndex = index;
        selectedQuestId = null;
        historyScroll = 0;
        questScroll = 0;
    }

    private void clickQuestArea(double mouseX, double mouseY) {
        DialogueHistoryEntry dialogue = getSelectedDialogue();
        if (dialogue == null) return;

        List<QuestState> quests = getQuestsFor(dialogue);
        QuestState selectedQuest = getSelectedQuest(quests);
        int contentX = questX + PADDING;
        int contentY = questY + HEADER + 7;
        int contentW = questW - PADDING * 2;

        if (selectedQuest != null) {
            if (isInsideQuestBackButton(mouseX, mouseY)) {
                selectedQuestId = null;
                questScroll = 0;
                return;
            }

            int buttonY = getQuestTrackButtonY(selectedQuest, contentY, contentW);
            int buttonW = trackButtonWidth(selectedQuest, contentW);
            if ("active".equals(selectedQuest.getStatus())
                    && isInsideTrackButton(mouseX, mouseY, contentX, buttonY, buttonW)) {
                ClientProgressData.toggleTrackedQuest(selectedQuest.getId());
                ModNetwork.sendToServer(new ToggleTrackedQuestPacket(selectedQuest.getId()));
                return;
            }

            return;
        }

        int rowY = contentY - questScroll;
        for (QuestState quest : quests) {
            if (isInside(mouseX, mouseY, contentX - 3, rowY - 2, contentW + 6, QUEST_ROW_HEIGHT - 2)) {
                selectedQuestId = quest.getId();
                questScroll = 0;
                return;
            }
            rowY += QUEST_ROW_HEIGHT;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int amount = (int) (delta * 18);
        if (isInside(mouseX, mouseY, dialogueX, dialogueY, dialogueW, dialogueH)) {
            if (showingCharacterDetails) {
                detailsScroll = clampScroll(detailsScroll - amount, getDetailsContentHeight(), getDialogueVisibleHeight());
            } else {
                dialogueScroll = clampScroll(dialogueScroll - amount, getDialogueContentHeight(), getDialogueVisibleHeight());
            }
            return true;
        }
        if (isInside(mouseX, mouseY, historyX, historyY, historyW, historyH)) {
            historyScroll = clampScroll(historyScroll - amount, getHistoryContentHeight(), getColumnVisibleHeight(historyH));
            return true;
        }
        if (isInside(mouseX, mouseY, questX, questY, questW, questH)) {
            questScroll = clampScroll(questScroll - amount, getQuestContentHeight(), getColumnVisibleHeight(questH));
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (showingCharacterDetails && !closingCharacterDetails) {
                closingCharacterDetails = true;
                startExpansionValue = expansionProgress;
                lastStateChangeTime = net.minecraft.Util.getMillis();
                startDetailRevealValue = detailRevealProgress;
                targetDetailReveal = 0f;
                lastDetailChangeTime = net.minecraft.Util.getMillis();
                return true;
            }
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_J) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean textureExists(net.minecraft.resources.ResourceLocation location) {
        return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    private DialogueHistoryEntry getSelectedDialogue() {
        List<DialogueHistoryEntry> history = ClientProgressData.getHistory();
        if (selectedDialogueIndex < 0 || selectedDialogueIndex >= history.size()) return null;
        return history.get(selectedDialogueIndex);
    }

    private List<QuestState> getQuestsFor(DialogueHistoryEntry dialogue) {
        List<QuestState> result = new ArrayList<>();
        for (QuestState quest : ClientProgressData.getAllQuests().values()) {
            if (belongsToDialogue(quest, dialogue)) {
                result.add(quest);
            }
        }
        result.sort(Comparator.comparing(QuestState::getStatus).thenComparing(QuestState::getTitle));
        return result;
    }

    private boolean belongsToDialogue(QuestState quest, DialogueHistoryEntry dialogue) {
        String questDialogueId = quest.getDialogueId();
        if (questDialogueId != null && !questDialogueId.isEmpty()) {
            return questDialogueId.equals(dialogue.getDialogueId());
        }
        String giver = normalizeName(quest.getGiverName());
        if (giver.equals(normalizeName(dialogue.getDisplayName()))) return true;
        for (HistoryLine line : dialogue.getLines()) {
            if (!"player".equals(line.getSpeaker()) && giver.equals(normalizeName(line.getSpeaker()))) return true;
        }
        return false;
    }

    private String normalizeName(String value) {
        if (value == null) return "";
        String normalized = value.replace("[", "").replace("]", "").trim();
        return normalized.toLowerCase(java.util.Locale.ROOT);
    }

    private QuestState getSelectedQuest(List<QuestState> quests) {
        if (selectedQuestId == null) return null;
        for (QuestState quest : quests) {
            if (selectedQuestId.equals(quest.getId())) return quest;
        }
        selectedQuestId = null;
        return null;
    }

    private void clampScrollOffsets() {
        dialogueScroll = clampScroll(dialogueScroll, getDialogueContentHeight(), getDialogueVisibleHeight());
        historyScroll = clampScroll(historyScroll, getHistoryContentHeight(), getColumnVisibleHeight(historyH));
        questScroll = clampScroll(questScroll, getQuestContentHeight(), getColumnVisibleHeight(questH));
        detailsScroll = clampScroll(detailsScroll, getDetailsContentHeight(), getDialogueVisibleHeight());
    }

    private int clampScroll(int scroll, int contentHeight, int visibleHeight) {
        int maxScroll = Math.max(0, contentHeight - Math.max(0, visibleHeight));
        return Math.min(Math.max(0, scroll), maxScroll);
    }

    private int getDialogueVisibleHeight() {
        return Math.max(0, dialogueH - HEADER - 10);
    }

    private int getColumnVisibleHeight(int columnHeight) {
        return Math.max(0, columnHeight - HEADER - PADDING - 5);
    }

    private int getDialogueContentHeight() {
        return ClientProgressData.getHistory().size() * ROW_HEIGHT;
    }

    private int getDetailsContentHeight() {
        DialogueHistoryEntry entry = getSelectedDialogue();
        if (entry == null) return 0;
        int h = 142;
        String lore = entry.getCharacterInfo();
        if (lore != null && !lore.isEmpty()) {
            h += minecraft.font.wordWrapHeight(TextFormatter.format(lore), dialogueW - 12);
        }
        return h;
    }

    private int getHistoryContentHeight() {
        DialogueHistoryEntry entry = getSelectedDialogue();
        if (entry == null || entry.getLines().isEmpty()) return 0;
        int contentW = historyW - PADDING * 2;
        int height = 0;
        for (HistoryLine line : entry.getLines()) height += historyLineHeight(line, contentW);
        return height + Math.max(0, entry.getLines().size() - 1) * HISTORY_LINE_GAP;
    }

    private int getQuestContentHeight() {
        DialogueHistoryEntry dialogue = getSelectedDialogue();
        if (dialogue == null) return 0;
        List<QuestState> quests = getQuestsFor(dialogue);
        if (quests.isEmpty()) return 0;
        QuestState selectedQuest = getSelectedQuest(quests);
        if (selectedQuest == null) return quests.size() * QUEST_ROW_HEIGHT;
        int contentW = questW - PADDING * 2;
        int height = getQuestTrackButtonOffset(selectedQuest, contentW);
        if ("active".equals(selectedQuest.getStatus())) height += 5 + QUEST_DETAIL_BUTTON_HEIGHT + 4;
        return height;
    }

    private int getQuestTrackButtonY(QuestState quest, int contentY, int contentW) {
        return contentY - questScroll + getQuestTrackButtonOffset(quest, contentW);
    }

    private int getQuestTrackButtonOffset(QuestState quest, int contentW) {
        int y = 13;
        if (!quest.getDescription().isEmpty()) y += wrappedHeight(TextFormatter.format(quest.getDescription()), contentW) + 8;
        if (!quest.getObjectives().isEmpty()) {
            y += 10 + 13;
            for (String objective : quest.getObjectives()) {
                int textH = wrappedHeight(TextFormatter.format(QuestState.objectiveText(objective)), objectiveTextWidth(contentW));
                y += textH + OBJECTIVE_ACCENT_PAD * 2 + 2;
            }
        }
        return y + 12;
    }

    private int historyLineHeight(HistoryLine line, int width) {
        Component text = TextFormatter.format(speakerLabel(line) + "&r: " + line.getText());
        return wrappedHeight(text, width - HISTORY_ACCENT_WIDTH - 8) + HISTORY_ACCENT_PAD * 2;
    }

    private int wrappedHeight(Component text, int width) {
        return this.font.split(text, unscaledWidth(width)).size() * lineHeight();
    }

    private String speakerLabel(HistoryLine line) {
        if ("player".equals(line.getSpeaker())) return net.minecraft.client.resources.language.I18n.get("gui.interactentity.dialogue.you");
        return line.getSpeaker();
    }

    private String trackLabel(QuestState quest) {
        if (ClientProgressData.isQuestTracked(quest.getId())) return tr("untrack");
        return ClientProgressData.getTrackedQuestIds().size() >= 3 ? tr("track_limit") : tr("track");
    }

    private int statusColor(QuestState quest) {
        return switch (quest.getStatus()) {
            case "completed" -> GREEN;
            case "failed" -> RED;
            default -> GOLD;
        };
    }

    private String statusText(QuestState quest) {
        return switch (quest.getStatus()) {
            case "completed" -> tr("status_completed");
            case "failed" -> tr("status_failed");
            default -> tr("status_active");
        };
    }

    private void drawWindow(GuiGraphics graphics) {
        if (useWindowTex) graphics.blitNineSliced(DEFAULT_WINDOW_TEX, panelX, panelY, panelW, panelH, 6, 64, 64, 0, 0);
        else {
            graphics.fill(panelX + 4, panelY + 5, panelX + panelW + 4, panelY + panelH + 5, 0x66000000);
            graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, WINDOW);
            drawBorder(graphics, panelX, panelY, panelW, panelH, BORDER_SOFT);
            graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + WINDOW_HEADER, WINDOW_HEADER_COLOR);
            graphics.fill(panelX + 1, panelY + WINDOW_HEADER - 1, panelX + panelW - 1, panelY + WINDOW_HEADER, BORDER);
        }
    }

    private void drawSection(GuiGraphics graphics, int x, int y, int w, int h, String title, int accent, boolean showBackArrow, int mouseX, int mouseY) {
        if (useSectionTex) graphics.blitNineSliced(DEFAULT_SECTION_TEX, x, y, w, h, 4, 32, 32, 0, 0);
        else {
            graphics.fill(x, y, x + w, y + h, PANEL_COLOR);
            graphics.fill(x + 1, y + HEADER, x + w - 1, y + h - 1, 0xC010121B);
            drawBorder(graphics, x, y, w, h, BORDER_SOFT);
            graphics.fill(x + 1, y + 1, x + w - 1, y + HEADER, PANEL_SOFT);
            graphics.fill(x + 1, y + HEADER - 1, x + w - 1, y + HEADER, accent);
        }
        int titleRight = showBackArrow ? x + w - 24 : x + w - 2;
        graphics.enableScissor(x + 2, y + 1, titleRight, y + HEADER);
        drawStringScaled(graphics, Component.literal(title), x + PADDING, y + 8, 0xFFFFFFFF);
        graphics.disableScissor();
        if (showBackArrow) {
            int arrowX = x + w - 20;
            boolean hovered = isInside(mouseX, mouseY, arrowX - 4, y + 4, 18, HEADER - 8);
            // Hover/press лерп — анимация ТОЛЬКО для back-arrow панели персонажей (известная по координате)
            boolean isCharBack = (x == dialogueX);
            float hoverProg, pressProg, revealProg;
            if (isCharBack) {
                backArrowHover = net.minecraft.util.Mth.lerp(0.20f, backArrowHover, hovered ? 1f : 0f);
                backArrowPress = Math.max(0f, backArrowPress - 0.08f);
                hoverProg = backArrowHover;
                pressProg = backArrowPress;
                // Стрелка появляется/исчезает синхронно с раскрытием панели деталей
                revealProg = detailRevealProgress;
            } else {
                hoverProg = hovered ? 1f : 0f;
                pressProg = 0f;
                revealProg = 1f;
            }
            if (revealProg <= 0.01f) return; // полностью скрыта
            if (!useSectionTex && hoverProg > 0.01f) {
                int alpha = (int)(hoverProg * 0xFF);
                int bg = (alpha << 24) | (PANEL_HOVER & 0x00FFFFFF);
                graphics.fill(arrowX - 4, y + 4, arrowX + 14, y + HEADER - 4, bg);
                drawBorder(graphics, arrowX - 4, y + 4, 18, HEADER - 8, lerpColor(0x00000000, BORDER, hoverProg));
            }
            int color = lerpColor(MUTED, TEXT, hoverProg);
            // Reveal — слайд слева + scale из 0
            float scale = 0.6f + 0.4f * revealProg;
            float slideX = (1f - revealProg) * 6f; // въезжает справа
            float press = 1.0f - pressProg * 0.10f;
            graphics.pose().pushPose();
            float cx = arrowX + 5;
            float cy = y + HEADER / 2f;
            graphics.pose().translate(cx + slideX, cy, 0);
            graphics.pose().scale(scale * press, scale * press, 1f);
            graphics.pose().translate(-cx, -cy, 0);
            drawStringScaled(graphics, Component.literal(BACK_ARROW), arrowX, y + 8, color);
            graphics.pose().popPose();
        }
    }

    private void drawEmptyState(GuiGraphics graphics, String title, String subtitle, int x, int y, int w, int h) {
        int centerY = y + h / 2 - 15;
        drawWrappedCentered(graphics, Component.literal(title), x + PADDING, centerY, w - PADDING * 2, MUTED);
        drawWrappedCentered(graphics, Component.literal(subtitle), x + PADDING, centerY + 12, w - PADDING * 2, DIM);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color, int clipTop, int clipBottom) {
        for (FormattedCharSequence seq : this.font.split(text, unscaledWidth(width))) {
            if (y >= clipTop && y < clipBottom) drawStringScaled(graphics, seq, x, y, color);
            y += lineHeight();
        }
        return y;
    }

    private int drawWrappedCentered(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence seq : this.font.split(text, unscaledWidth(width))) {
            int lineW = Math.round(this.font.width(seq) * TEXT_SCALE);
            drawStringScaled(graphics, seq, x + (width - lineW) / 2, y, color);
            y += lineHeight();
        }
        return y;
    }

    private void drawStringScaled(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        graphics.drawString(this.font, text, Math.round(x / TEXT_SCALE), Math.round(y / TEXT_SCALE), color, false);
        graphics.pose().popPose();
    }

    private void drawStringScaled(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        graphics.drawString(this.font, text, Math.round(x / TEXT_SCALE), Math.round(y / TEXT_SCALE), color, false);
        graphics.pose().popPose();
    }

    private int unscaledWidth(int width) { return Math.max(1, Math.round(width / TEXT_SCALE)); }
    private int lineHeight() { return Math.max(8, Math.round(10 * TEXT_SCALE) + 1); }

    private void drawObjectiveMarker(GuiGraphics graphics, String objective, int x, int y) {
        boolean completed = QuestState.isObjectiveCompleted(objective);
        drawStringScaled(graphics, Component.literal(completed ? "[\u2714]" : "[ ]"), x, y, completed ? GREEN : DIM);
    }

    private void drawDivider(GuiGraphics graphics, int x, int y, int width, int clipTop, int clipBottom) {
        if (y >= clipTop && y < clipBottom) graphics.fill(x, y, x + width, y + 1, BORDER_SOFT);
    }

    private void drawTrackButton(GuiGraphics graphics, QuestState quest, int x, int y, int width, int clipTop, int clipBottom, int mouseX, int mouseY) {
        boolean hovered = isInsideTrackButton(mouseX, mouseY, x, y, width);
        boolean tracked = ClientProgressData.isQuestTracked(quest.getId());
        int bg = hovered ? PANEL_HOVER : 0x771A1E2B;
        int border = tracked ? GREEN : (hovered ? TEXT : BORDER);
        if (y + QUEST_DETAIL_BUTTON_HEIGHT > clipTop && y < clipBottom) {
            graphics.fill(x, y, x + width, y + QUEST_DETAIL_BUTTON_HEIGHT, bg);
            drawBorder(graphics, x, y, width, QUEST_DETAIL_BUTTON_HEIGHT, border);
            int boxSize = 10, boxX = x + 6, boxY = y + (QUEST_DETAIL_BUTTON_HEIGHT - boxSize) / 2;
            graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0x66000000);
            drawBorder(graphics, boxX, boxY, boxSize, boxSize, border);
            if (tracked) drawCheckMark(graphics, boxX + 2, boxY + 2, GREEN);
            String label = trackLabel(quest);
            int labelX = boxX + boxSize + 6, labelY = y + (QUEST_DETAIL_BUTTON_HEIGHT - lineHeight()) / 2 + 1;
            drawStringScaled(graphics, Component.literal(label), labelX, labelY, hovered ? 0xFFFFFF : (tracked ? GREEN : MUTED));
        }
    }
    private void drawCheckMark(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 2, x + 1, y + 4, color);
        graphics.fill(x + 1, y + 3, x + 2, y + 5, color);
        graphics.fill(x + 2, y + 4, x + 3, y + 6, color);
        graphics.fill(x + 3, y + 3, x + 4, y + 5, color);
        graphics.fill(x + 4, y + 2, x + 5, y + 4, color);
        graphics.fill(x + 5, y + 1, x + 6, y + 3, color);
    }
    private int trackButtonWidth(QuestState quest, int maxWidth) { return maxWidth; }
    private boolean isInsideTrackButton(double mouseX, double mouseY, int x, int y, int width) { return isInside(mouseX, mouseY, x, y, width, QUEST_DETAIL_BUTTON_HEIGHT); }
    private boolean isInsideCloseButton(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, closeButtonX - 2, closeButtonY - 2, CLOSE_BUTTON_SIZE + 4, CLOSE_BUTTON_SIZE + 4);
    }

    /** Кнопка X в правом-верхнем углу окна с hover/press-анимацией. */
    private void drawCloseButton(GuiGraphics graphics, int x, int y, float hover, float press) {
        // Hover красит фон + текст в красный, press дает scale~0.92
        int bgAlpha = (int) (hover * 0x88);
        int textColor = lerpColor(0xFFAAAAAA, 0xFFFF5566, hover);
        if (bgAlpha > 0) {
            int bg = (bgAlpha << 24) | 0x441122;
            graphics.fill(x - 2, y - 2, x + CLOSE_BUTTON_SIZE + 2, y + CLOSE_BUTTON_SIZE + 2, bg);
            drawBorder(graphics, x - 2, y - 2, CLOSE_BUTTON_SIZE + 4, CLOSE_BUTTON_SIZE + 4, lerpColor(0x00000000, 0xFFFF5566, hover));
        }
        float scale = 1.0f - press * 0.08f;
        graphics.pose().pushPose();
        float cx = x + CLOSE_BUTTON_SIZE / 2f;
        float cy = y + CLOSE_BUTTON_SIZE / 2f;
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.pose().translate(-cx, -cy, 0);
        // ✕ нарисованный как символ.
        int textW = this.font.width(CLOSE_X);
        int textY = y + (CLOSE_BUTTON_SIZE - this.font.lineHeight) / 2 + 1;
        graphics.drawString(this.font, CLOSE_X, x + (CLOSE_BUTTON_SIZE - textW) / 2, textY, textColor, false);
        graphics.pose().popPose();
    }

    /** Линейная интерполяция между двумя ARGB-цветами по t ∈ [0,1]. */
    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int fa = (from >>> 24) & 0xFF, fr = (from >>> 16) & 0xFF, fg = (from >>> 8) & 0xFF, fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF, tr = (to >>> 16) & 0xFF, tg = (to >>> 8) & 0xFF, tb = to & 0xFF;
        int a = (int)(fa + (ta - fa) * t);
        int r = (int)(fr + (tr - fr) * t);
        int g = (int)(fg + (tg - fg) * t);
        int b = (int)(fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private boolean isInsideCharacterBackButton(double mouseX, double mouseY) {
        if (!showingCharacterDetails) return false;
        int arrowX = dialogueX + dialogueW - 20;
        return isInside(mouseX, mouseY, arrowX - 4, dialogueY + 4, 18, HEADER - 8);
    }
    private boolean isInsideQuestBackButton(double mouseX, double mouseY) {
        if (selectedQuestId == null) return false;
        int arrowX = questX + questW - 20;
        return isInside(mouseX, mouseY, arrowX - 4, questY + 4, 18, HEADER - 8);
    }
    private int objectiveTextOffset() { return OBJECTIVE_ACCENT_WIDTH + OBJECTIVE_MARKER_GAP + objectiveMarkerWidth() + 4; }
    private int objectiveMarkerWidth() { return Math.round(this.font.width("[ ]") * TEXT_SCALE); }
    private int objectiveTextWidth(int contentWidth) { return Math.max(1, contentWidth - objectiveTextOffset()); }
    private String tr(String suffix) { return net.minecraft.client.resources.language.I18n.get("gui.interactentity.journal." + suffix); }
    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) { return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h; }
    @Override public boolean isPauseScreen() { return false; }
}
