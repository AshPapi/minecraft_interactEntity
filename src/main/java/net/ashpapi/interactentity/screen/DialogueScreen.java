package net.ashpapi.interactentity.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ashpapi.interactentity.camera.DialogueCameraController;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
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

    private static final int PANEL_WIDTH = 320;
    private static final int PADDING = 8;
    private static final int HOTBAR_GAP = 40;            // подняли выше (было 30)
    private static final int LINE_HEIGHT = 8;
    private static final int HEAD_SIZE = 32;

    private static final int BG_COLOR_TOP = 0xCC1A1A2E;
    private static final int BG_COLOR_BOTTOM = 0xCC0D0D1A;
    private static final int BORDER_COLOR = 0x66FFFFFF;
    private static final int SHADOW_COLOR = 0x80000000;
    private static final int NAME_COLOR = 0xFFFFCC44;
    private static final int TEXT_COLOR = 0xFFDDDDDD;
    private static final int OPTION_COLOR = 0xFFFFFFFF;
    private static final int OPTION_HOVER = 0xFF55FF55;
    private static final int HINT_COLOR = 0xFF8888AA;

    private final int entityId;
    private String displayName;
    private String dialogueText;
    private String nodeType;
    private List<String> optionTexts;
    private List<Integer> optionIndices;
    private List<Boolean> optionLocked;
    private List<String> optionLockReasons;
    @Nullable private ResourceLocation avatarTexture;
    @Nullable private ResourceLocation background;
    @Nullable private ResourceLocation optionsBackground;

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

    public DialogueScreen(int entityId, String displayName, String text, String nodeType,
                          List<String> optionTexts, List<Integer> optionIndices,
                          List<Boolean> optionLocked, List<String> optionLockReasons,
                          @Nullable ResourceLocation avatarTexture,
                          @Nullable ResourceLocation background, @Nullable ResourceLocation optionsBackground) {
        super(Component.translatable("gui.interactentity.dialogue"));
        this.entityId = entityId;
        this.displayName = displayName;
        this.dialogueText = text;
        this.nodeType = nodeType;
        this.optionTexts = new ArrayList<>(optionTexts);
        this.optionIndices = new ArrayList<>(optionIndices);
        this.optionLocked = new ArrayList<>(optionLocked);
        this.optionLockReasons = new ArrayList<>(optionLockReasons);
        this.avatarTexture = avatarTexture;
        this.background = background;
        this.optionsBackground = optionsBackground;
        resetTypewriter();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.stopUsingItem();
    }

    @Override
    protected void init() {
        super.init();
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

        syncClientTalkingState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int alpha = (int)(0xCC * animationTime);
        int topColor = (alpha << 24) | (BG_COLOR_TOP & 0x00FFFFFF);
        int bottomColor = (alpha << 24) | (BG_COLOR_BOTTOM & 0x00FFFFFF);
        int borderColor = ((int)(0x66 * animationTime) << 24) | (BORDER_COLOR & 0x00FFFFFF);

        int effectivePanelW = Math.min(PANEL_WIDTH, this.width - 40);
        panelW = effectivePanelW;
        panelX = (this.width - panelW) / 2;

        Component nameComp = TextFormatter.format(displayName);
        Component fullTextComp = TextFormatter.format(dialogueText);
        Component visibleTextComp = TextFormatter.format(getVisibleDialogueText());

        int textStartX = panelX + PADDING + HEAD_SIZE + 12;
        int textAreaWidth = panelW - PADDING * 2 - HEAD_SIZE - 12;

        List<FormattedCharSequence> textLines = this.font.split(visibleTextComp, textAreaWidth);
        List<FormattedCharSequence> fullTextLines = this.font.split(fullTextComp, textAreaWidth);

        optionHitboxes.clear();

        // When showingOptions=false for choice nodes, render as linear (no options inline)
        boolean renderingInlineOptions = "choice".equals(nodeType) && showingOptions;
        boolean showHint = !renderingInlineOptions;

        int contentH = fullTextLines.size() * LINE_HEIGHT;
        int hintH = showHint ? LINE_HEIGHT + 4 : 0;

        int minHeightByHead = HEAD_SIZE + PADDING * 2;
        int textAreaNeeded = PADDING + LINE_HEIGHT + 2 + contentH + hintH + PADDING;
        panelH = Math.max(minHeightByHead, textAreaNeeded);
        panelY = this.height - HOTBAR_GAP - panelH;

        graphics.fill(panelX + 2, panelY + 2, panelX + panelW + 2, panelY + panelH + 2, SHADOW_COLOR);
        if (background != null) {
            RenderSystem.enableBlend();
            graphics.blit(background, panelX, panelY, 0, 0, panelW, panelH, panelW, panelH);
        } else {
            graphics.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, topColor, bottomColor);
            graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, borderColor);
            graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, borderColor);
            graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, borderColor);
            graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, borderColor);
        }

        int headX = panelX + PADDING;
        int headY = panelY + (panelH - HEAD_SIZE) / 2;
        int nameY = panelY + PADDING;
        int textY = nameY + LINE_HEIGHT + 2;

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
            for (FormattedCharSequence line : this.font.split(respComp, textAreaWidth)) {
                graphics.drawString(this.font, line, textStartX, textY, TEXT_COLOR, false);
                textY += LINE_HEIGHT;
            }
            String hint = net.minecraft.client.resources.language.I18n.get("gui.interactentity.hint.next");
            int hintWidth = this.font.width(hint);
            graphics.drawString(this.font, hint,
                    panelX + panelW - PADDING - hintWidth, panelY + panelH - PADDING - 2, HINT_COLOR, false);
        } else {
            // NPC avatar + text
            ResourceLocation texture = avatarTexture != null ? avatarTexture
                    : new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, texture);
            graphics.blit(texture, headX, headY, HEAD_SIZE, HEAD_SIZE, 8, 8, 8, 8, 64, 64);

            drawShadowedString(graphics, nameComp, textStartX, nameY, NAME_COLOR);
            for (FormattedCharSequence line : textLines) {
                graphics.drawString(this.font, line, textStartX, textY, TEXT_COLOR, false);
                textY += LINE_HEIGHT;
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
                int hintWidth = this.font.width(hint);
                graphics.drawString(this.font, hint,
                        panelX + panelW - PADDING - hintWidth, panelY + panelH - PADDING - 2, HINT_COLOR, false);
            }
        }

        // Render choice options as top-left panels
        if (renderingInlineOptions) {
            renderOptionPanels(graphics, mouseX, mouseY);
        }
    }

    private void renderOptionPanels(GuiGraphics graphics, int mouseX, int mouseY) {
        int optPanelX = 8;
        int optPanelY = 8;
        int numWidth = this.font.width("5. ");
        int maxPanelW = Math.min(this.width / 2 - 16, 200);
        int textAvailW = maxPanelW - OPT_PADDING_H * 2 - numWidth;

        List<Component> optionComps = new ArrayList<>();
        List<List<FormattedCharSequence>> optionLines = new ArrayList<>();
        int panelW = 0;
        for (String opt : optionTexts) {
            Component c = TextFormatter.format(opt);
            optionComps.add(c);
            List<FormattedCharSequence> lines = this.font.split(c, textAvailW);
            optionLines.add(lines);
            int lineW = lines.stream().mapToInt(this.font::width).max().orElse(0);
            panelW = Math.max(panelW, numWidth + lineW + OPT_PADDING_H * 2);
        }
        panelW = Math.min(panelW, maxPanelW);

        int curY = optPanelY;
        for (int i = 0; i < optionComps.size(); i++) {
            List<FormattedCharSequence> lines = optionLines.get(i);
            int panelH = lines.size() * LINE_HEIGHT + OPT_PADDING_V * 2;
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
            if (optionsBackground != null && !isLocked) {
                RenderSystem.enableBlend();
                graphics.blit(optionsBackground, optPanelX, curY, 0, 0, panelW, panelH, panelW, panelH);
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
                    textY += LINE_HEIGHT;
                }
                if (!lockReason.isEmpty()) {
                    graphics.drawString(this.font, lockReason, textX + lockIconW, textY, OPT_LOCKED_REASON, false);
                }
            } else {
                graphics.drawString(this.font, (i + 1) + ". ", textX, textY, OPT_NUM_COLOR, false);
                for (FormattedCharSequence line : lines) {
                    graphics.drawString(this.font, line, textX + numWidth, textY,
                            hovered ? OPTION_HOVER : OPTION_COLOR, false);
                    textY += LINE_HEIGHT;
                }
            }

            optionHitboxes.add(new OptionHitbox(optPanelX, curY, panelW, panelH));
            curY += panelH + OPT_MARGIN;
        }
    }

    private void drawShadowedString(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(this.font, text, x + 1, y + 1, 0x60000000, false);
        graphics.drawString(this.font, text, x, y, color, false);
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
        if (ignoreNextClick && button == 1) {
            ignoreNextClick = false;
            return true;
        }
        ignoreNextClick = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.stopUsingItem();

        if (showingPlayerResponse && button == 1) {
            ModNetwork.sendToServer(new SelectOptionPacket(pendingOptionRawIndex));
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
            ModNetwork.sendToServer(new NavigatePacket(true));
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

    private void selectOption(int filteredIndex) {
        playerResponseText = optionTexts.get(filteredIndex);
        pendingOptionRawIndex = optionIndices.get(filteredIndex);
        showingOptions = false;
        showingPlayerResponse = true;
        syncClientTalkingState();
        DialogueCameraController.stopSide();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showingPlayerResponse && (keyCode == 257 || keyCode == 335 || keyCode == 32)) {
            ModNetwork.sendToServer(new SelectOptionPacket(pendingOptionRawIndex));
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

    public void updateDialogue(String displayName, String text, String nodeType,
                               List<String> optionTexts, List<Integer> optionIndices,
                               List<Boolean> optionLocked, List<String> optionLockReasons,
                               @Nullable ResourceLocation avatarTexture,
                               @Nullable ResourceLocation background, @Nullable ResourceLocation optionsBackground) {
        this.displayName = displayName;
        this.dialogueText = text;
        this.nodeType = nodeType;
        this.optionTexts = new ArrayList<>(optionTexts);
        this.optionIndices = new ArrayList<>(optionIndices);
        this.optionLocked = new ArrayList<>(optionLocked);
        this.optionLockReasons = new ArrayList<>(optionLockReasons);
        this.avatarTexture = avatarTexture;
        this.background = background;
        this.optionsBackground = optionsBackground;
        this.hoveredOption = -1;
        this.mouseMoved = false;
        this.firstOpen = false;
        this.animationTime = 1.0f;
        this.showingOptions = false;
        this.showingPlayerResponse = false;
        this.pendingOptionRawIndex = -1;
        resetTypewriter();
        syncClientTalkingState();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.stopUsingItem();
    }

    private boolean isOptionLocked(int index) {
        return index >= 0 && index < optionLocked.size() && optionLocked.get(index);
    }

    private void resetTypewriter() {
        this.totalDialogueChars = countVisibleChars(this.dialogueText);
        this.visibleDialogueChars = 0;
        this.typewriterTickCounter = 0;
    }

    private void syncClientTalkingState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (mc.level.getEntity(this.entityId) instanceof CustomNpcEntity customNpc) {
            boolean shouldTalk = !showingPlayerResponse && !showingOptions && totalDialogueChars > 0 && !isDialogueFullyVisible();
            customNpc.setClientTalkingOverride(shouldTalk);
        }
    }

    private void clearClientTalkingOverride() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (mc.level.getEntity(this.entityId) instanceof CustomNpcEntity customNpc) {
            customNpc.setClientTalkingOverride(null);
        }
    }

    private boolean isDialogueFullyVisible() {
        return visibleDialogueChars >= totalDialogueChars;
    }

    private void revealDialogueText() {
        this.visibleDialogueChars = this.totalDialogueChars;
        this.typewriterTickCounter = 0;
        syncClientTalkingState();
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
