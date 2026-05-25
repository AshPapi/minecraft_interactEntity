package net.ashpapi.interactentity.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ashpapi.interactentity.camera.DialogueCameraController;
import net.ashpapi.interactentity.formatting.TextFormatter;
import net.ashpapi.interactentity.network.CloseDialogueC2SPacket;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.NavigatePacket;
import net.ashpapi.interactentity.network.SelectOptionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DialogueScreen extends Screen {

    private static final int PANEL_WIDTH = 300;          // Увеличено с 280
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

    private void toggleHistoryOverlay() {
        showingHistoryOverlay = !showingHistoryOverlay;
        historyOverlayStartValue = historyOverlayProgress;
        historyOverlayTarget = showingHistoryOverlay ? 1.0f : 0.0f;
        historyOverlayAnimStart = net.minecraft.Util.getMillis();
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
            String hint = net.minecraft.client.resources.language.I18n.get("gui.interactentity.hint.next");
            int hintWidth = (int)(this.font.width(hint) * TEXT_SCALE);
            drawStringScaled(graphics, Component.literal(hint),
                    panelX + panelW - PADDING - hintWidth, panelY + panelH - PADDING - 2, HINT_COLOR);
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

        // Render choice options as top-left panels
        if (renderingInlineOptions) {
            renderOptionPanels(graphics, mouseX, mouseY);
        }

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
            int bookX = panelX + panelW - 16;
            int bookY = panelY + 4;
            if (mouseX >= bookX - 2 && mouseX <= bookX + 14 && mouseY >= bookY && mouseY <= bookY + 12) {
                toggleHistoryOverlay();
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        if (ignoreNextClick && button == 1) {
            ignoreNextClick = false;
            return true;
        }
        ignoreNextClick = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.stopUsingItem();

        if (showingPlayerResponse && button == 1) {
            ModNetwork.sendToServer(new SelectOptionPacket(nodeId, pendingOptionRawIndex));
            showingPlayerResponse = false;
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
        return super.mouseScrolled(mouseX, mouseY, delta);
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
        if (keyCode == 72) { // Клавиша H
            toggleHistoryOverlay();
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (showingPlayerResponse && (keyCode == 257 || keyCode == 335 || keyCode == 32)) {
            ModNetwork.sendToServer(new SelectOptionPacket(nodeId, pendingOptionRawIndex));
            showingPlayerResponse = false;
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
        clearClientTalkingOverride();
        DialogueCameraController.stop();
        super.onClose();
    }

    @Override
    public void removed() {
        clearClientTalkingOverride();
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

    private void drawVectorBook(GuiGraphics graphics, int x, int y, float openProgress, boolean hovered) {
        int coverColor = hovered ? 0xFFC93B3B : 0xFF9E2A2B;
        int goldColor = 0xFFE5B842;
        int pageColor = 0xFFF5E6C8;
        int lineColor = 0xFF6E5D4F;

        if (openProgress < 0.05f) {
            // Закрытая книга (ширина 10, высота 12)
            // Корешок (золотой)
            graphics.fill(x, y, x + 2, y + 12, goldColor);
            // Обложка (красная)
            graphics.fill(x + 2, y, x + 10, y + 12, coverColor);
            // Золотая застежка посередине
            graphics.fill(x + 8, y + 5, x + 10, y + 7, goldColor);
            // Срез страниц (кремовый) справа
            graphics.fill(x + 10, y + 1, x + 11, y + 11, pageColor);
            // Срез страниц снизу
            graphics.fill(x + 2, y + 11, x + 10, y + 12, pageColor);
        } else {
            // Плавно приоткрывающаяся / раскрытая книга
            int wLeft = (int) (8 * openProgress);
            int wRight = 8;
            int centerX = x + 6;

            // Левая обложка (красная)
            if (wLeft > 0) {
                graphics.fill(centerX - wLeft, y, centerX, y + 12, coverColor);
            }
            // Правая обложка (красная)
            graphics.fill(centerX, y, centerX + wRight, y + 12, coverColor);

            // Корешок по центру (золотой)
            graphics.fill(centerX - 1, y, centerX + 1, y + 12, goldColor);

            // Левая страница (кремовая)
            if (wLeft > 2) {
                int pageLeft = centerX - wLeft + 1;
                int pageRight = centerX - 1;
                graphics.fill(pageLeft, y + 1, pageRight, y + 11, pageColor);

                // Строчки текста (коричневые)
                if (openProgress > 0.7f) {
                    graphics.fill(pageLeft + 1, y + 3, pageRight - 1, y + 4, lineColor);
                    graphics.fill(pageLeft + 1, y + 5, pageRight - 1, y + 6, lineColor);
                    graphics.fill(pageLeft + 1, y + 7, pageRight - 1, y + 8, lineColor);
                    graphics.fill(pageLeft + 1, y + 9, pageRight - 2, y + 10, lineColor);
                }
            }

            // Правая страница (кремовая)
            if (wRight > 2) {
                int pageLeft = centerX + 1;
                int pageRight = centerX + wRight - 1;
                graphics.fill(pageLeft, y + 1, pageRight, y + 11, pageColor);

                // Строчки текста (коричневые)
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
