package net.ashpapi.interactentity.overlay;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.ClientProgressData;
import net.ashpapi.interactentity.formatting.TextFormatter;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class QuestHudOverlay {
    private static final int MAX_QUESTS = 3;
    private static final int PADDING = 4;
    private static boolean visible = true;

    public static void toggleVisibility() {
        visible = !visible;
    }

    public static boolean isVisible() {
        return visible;
    }

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getClass().getName().contains("DialogueScreen")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!visible) return;
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        List<QuestState> activeQuests = ClientProgressData.getActiveQuests();
        if (activeQuests.isEmpty()) return;

        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int y = PADDING + 2;

        int count = Math.min(activeQuests.size(), MAX_QUESTS);
        int maxWidth = 0;
        for (int i = 0; i < count; i++) {
            QuestState quest = activeQuests.get(i);
            Component title = TextFormatter.format(quest.getTitle());
            int titleWidth = font.width(title);
            if (quest.hasDeadline()) {
                titleWidth += 6 + font.width("\u23F1 99:99");
            }
            maxWidth = Math.max(maxWidth, titleWidth);
            for (String obj : quest.getObjectives()) {
                Component objComp = TextFormatter.format("  " + obj);
                maxWidth = Math.max(maxWidth, font.width(objComp));
            }
        }
        int bgWidth = maxWidth + PADDING * 2 + 6;
        int bgX = screenWidth - bgWidth - PADDING;
        int bgHeight = 0;
        int tempY = y;
        for (int i = 0; i < count; i++) {
            QuestState quest = activeQuests.get(i);
            tempY += 10;
            tempY += quest.getObjectives().size() * 8 + 2;
            if (i < count - 1) tempY += 2;
        }
        bgHeight = tempY - y + PADDING;
        int bgY = y - 2;

        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0x80000000);
        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + 1, 0x40FFFFFF);
        graphics.fill(bgX, bgY + bgHeight - 1, bgX + bgWidth, bgY + bgHeight, 0x40FFFFFF);
        graphics.fill(bgX, bgY, bgX + 1, bgY + bgHeight, 0x40FFFFFF);
        graphics.fill(bgX + bgWidth - 1, bgY, bgX + bgWidth, bgY + bgHeight, 0x40FFFFFF);

        int textX = bgX + PADDING;
        for (int i = 0; i < count; i++) {
            QuestState quest = activeQuests.get(i);
            Component title = TextFormatter.format(quest.getTitle());
            graphics.drawString(font, title, textX, y, 0xFFCC00, false);

            // Отображение дедлайна рядом с заголовком
            if (quest.hasDeadline() && mc.level != null) {
                long remaining = quest.getDeadlineTick() - mc.level.getGameTime();
                if (remaining > 0) {
                    String timerStr = formatDeadline(remaining, quest.getDeadlineType());
                    int timerColor = (remaining < 1200) ? getFlashingRed(mc.level.getGameTime()) : 0xAAAAAA;
                    int timerX = textX + font.width(title) + 6;
                    graphics.drawString(font, timerStr, timerX, y, timerColor, false);
                }
            }

            y += 10;
            for (String obj : quest.getObjectives()) {
                Component objComp = TextFormatter.format("  " + obj);
                graphics.drawString(font, objComp, textX, y, 0xCCCCCC, false);
                y += 8;
            }
            y += 2;
        }
    }

    private static String formatDeadline(long remainingTicks, String type) {
        if ("sunset".equals(type)) {
            long minutes = remainingTicks / 1200;
            if (minutes > 0) return "\u2638 " + minutes + "m";
            return "\u2638 <1m";
        }
        if ("sunrise".equals(type)) {
            long minutes = remainingTicks / 1200;
            if (minutes > 0) return "\u263C " + minutes + "m";
            return "\u263C <1m";
        }
        long totalSeconds = remainingTicks / 20;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("\u23F1 %d:%02d", minutes, seconds);
    }

    private static int getFlashingRed(long gameTime) {
        return (gameTime % 20 < 10) ? 0xFF5555 : 0xAA0000;
    }
}