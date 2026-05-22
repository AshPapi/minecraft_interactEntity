package net.ashpapi.interactentity.overlay;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.ClientProgressData;
import net.ashpapi.interactentity.formatting.TextFormatter;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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

        List<QuestState> trackedQuests = ClientProgressData.getTrackedActiveQuests();
        if (trackedQuests.isEmpty()) return;

        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        
        // Настройки отступов
        int topPadding = 8;
        int sidePadding = 6;
        int startY = PADDING + topPadding;
        int y = startY;

        int count = Math.min(trackedQuests.size(), MAX_QUESTS);
        int maxWidth = 0;
        for (int i = 0; i < count; i++) {
            QuestState quest = trackedQuests.get(i);
            Component title = TextFormatter.format(quest.getTitle());
            int titleWidth = font.width(title);
            if (quest.hasDeadline()) {
                titleWidth += 8 + font.width(formatDeadline(1000, quest.getDeadlineType()));
            }
            maxWidth = Math.max(maxWidth, titleWidth);
            for (String obj : quest.getObjectives()) {
                Component objComp = TextFormatter.format(objectiveMarker(obj) + " " + QuestState.objectiveText(obj));
                maxWidth = Math.max(maxWidth, font.width(objComp));
            }
        }

        // Компактная ширина
        int bgWidth = maxWidth + sidePadding * 2; 
        int bgX = screenWidth - bgWidth - PADDING;
        
        int totalContentH = 0;
        for (int i = 0; i < count; i++) {
            QuestState quest = trackedQuests.get(i);
            totalContentH += 10; // Заголовок
            totalContentH += quest.getObjectives().size() * 10 + 2; // Цели
            if (i < count - 1) totalContentH += 4; // Зазор между квестами
        }
        
        int bgHeight = totalContentH + topPadding * 2; // Симметричные отступы верх/низ
        int bgY = startY - topPadding;

        // Отрисовка фона
        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0x80000000);
        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + 1, 0x40FFFFFF);
        graphics.fill(bgX, bgY + bgHeight - 1, bgX + bgWidth, bgY + bgHeight, 0x40FFFFFF);
        graphics.fill(bgX, bgY, bgX + 1, bgY + bgHeight, 0x40FFFFFF);
        graphics.fill(bgX + bgWidth - 1, bgY, bgX + bgWidth, bgY + bgHeight, 0x40FFFFFF);

        for (int i = 0; i < count; i++) {
            QuestState quest = trackedQuests.get(i);
            Component title = TextFormatter.format(quest.getTitle());
            int titleW = font.width(title);
            
            // Расчет центрирования для заголовка
            int currentTitleAreaW = titleW;
            if (quest.hasDeadline()) currentTitleAreaW += 8 + font.width(formatDeadline(1000, quest.getDeadlineType()));
            
            int titleX = bgX + (bgWidth - currentTitleAreaW) / 2;
            graphics.drawString(font, title, titleX, y, 0xFFCC00, false);

            if (quest.hasDeadline() && mc.level != null) {
                long remaining = quest.getDeadlineTick() - mc.level.getGameTime();
                if (remaining > 0) {
                    String actualTimer = formatDeadline(remaining, quest.getDeadlineType());
                    int timerColor = (remaining < 1200) ? getFlashingRed(mc.level.getGameTime()) : 0xAAAAAA;
                    graphics.drawString(font, actualTimer, titleX + titleW + 8, y, timerColor, false);
                }
            }

            y += 12;
            for (String obj : quest.getObjectives()) {
                Component objComp = TextFormatter.format(objectiveMarker(obj) + " " + QuestState.objectiveText(obj));
                int objW = font.width(objComp);
                int color = QuestState.isObjectiveCompleted(obj) ? 0x88CC88 : 0xCCCCCC;
                int objX = bgX + (bgWidth - objW) / 2;
                graphics.drawString(font, objComp, objX, y, color, false);
                y += 10;
            }
            y += 4;
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

    private static String objectiveMarker(String objective) {
        return QuestState.isObjectiveCompleted(objective) ? "&a[\u2714]&r" : "[ ]";
    }
}
