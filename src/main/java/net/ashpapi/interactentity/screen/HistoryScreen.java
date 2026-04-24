package net.ashpapi.interactentity.screen;

import net.ashpapi.interactentity.data.ClientProgressData;
import net.ashpapi.interactentity.formatting.TextFormatter;
import net.ashpapi.interactentity.history.DialogueHistoryEntry;
import net.ashpapi.interactentity.history.HistoryLine;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryScreen extends Screen {
    private static final int LIST_WIDTH = 140;
    private static final int TAB_HEIGHT = 24;

    private int selectedTab = 0; // 0 = dialogues, 1 = quests
    private int selectedDialogueIndex = -1;
    private int scrollOffset = 0;

    public HistoryScreen() {
        super(Component.translatable("gui.interactentity.journal"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // Tab buttons
        addRenderableWidget(Button.builder(Component.translatable("gui.interactentity.tab.dialogues"), btn -> {
            selectedTab = 0;
            selectedDialogueIndex = -1;
            scrollOffset = 0;
            rebuildWidgets();
        }).bounds(centerX - 102, 4, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.interactentity.tab.quests"), btn -> {
            selectedTab = 1;
            selectedDialogueIndex = -1;
            scrollOffset = 0;
            rebuildWidgets();
        }).bounds(centerX + 2, 4, 100, 20).build());

        if (selectedTab == 0) {
            initDialogueTab();
        }
    }

    private void initDialogueTab() {
        List<DialogueHistoryEntry> history = ClientProgressData.getHistory();
        int startY = TAB_HEIGHT + 8;

        for (int i = 0; i < history.size(); i++) {
            DialogueHistoryEntry entry = history.get(i);
            final int idx = i;
            Component label = TextFormatter.format(entry.getDisplayName());
            int btnY = startY + i * 22 - scrollOffset;
            if (btnY >= TAB_HEIGHT && btnY < this.height - 10) {
                addRenderableWidget(Button.builder(label, btn -> {
                    selectedDialogueIndex = idx;
                    scrollOffset = 0;
                }).bounds(4, btnY, LIST_WIDTH - 8, 20).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Background panels
        graphics.fill(0, TAB_HEIGHT, LIST_WIDTH, this.height, 0xCC1A1A2E);
        graphics.fill(LIST_WIDTH, TAB_HEIGHT, this.width, this.height, 0xCC0D0D1A);

        // Tab indicator
        int tabIndicatorX = selectedTab == 0 ? this.width / 2 - 102 : this.width / 2 + 2;
        graphics.fill(tabIndicatorX, 24, tabIndicatorX + 100, 26, 0xFFFFCC00);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (selectedTab == 0) {
            renderDialogueLog(graphics);
        } else {
            renderQuestTab(graphics);
        }
    }

    private void renderDialogueLog(GuiGraphics graphics) {
        List<DialogueHistoryEntry> history = ClientProgressData.getHistory();
        if (selectedDialogueIndex < 0 || selectedDialogueIndex >= history.size()) return;

        DialogueHistoryEntry entry = history.get(selectedDialogueIndex);
        int x = LIST_WIDTH + 10;
        int y = TAB_HEIGHT + 10 - scrollOffset;

        for (HistoryLine line : entry.getLines()) {
            String prefix;
            if ("player".equals(line.getSpeaker())) {
                prefix = "&b[" + net.minecraft.client.resources.language.I18n.get("gui.interactentity.dialogue.you") + "] ";
            } else {
                prefix = line.getSpeaker() + " ";
            }
            Component comp = TextFormatter.format(prefix + line.getText());

            // Word wrap
            List<net.minecraft.util.FormattedCharSequence> wrapped = this.font.split(comp, this.width - LIST_WIDTH - 20);
            for (net.minecraft.util.FormattedCharSequence seq : wrapped) {
                if (y >= TAB_HEIGHT && y < this.height - 4) {
                    graphics.drawString(this.font, seq, x, y, 0xFFFFFF, true);
                }
                y += 11;
            }
            y += 2;
        }
    }

    private void renderQuestTab(GuiGraphics graphics) {
        Map<String, QuestState> allQuests = ClientProgressData.getAllQuests();
        if (allQuests.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.interactentity.quests.empty"),
                    LIST_WIDTH + 20, TAB_HEIGHT + 20, 0x888888, false);
            return;
        }

        List<QuestState> active = new ArrayList<>();
        List<QuestState> completed = new ArrayList<>();
        List<QuestState> failed = new ArrayList<>();
        for (QuestState q : allQuests.values()) {
            switch (q.getStatus()) {
                case "active" -> active.add(q);
                case "completed" -> completed.add(q);
                case "failed" -> failed.add(q);
            }
        }

        int x = LIST_WIDTH + 10;
        int y = TAB_HEIGHT + 10 - scrollOffset;

        y = renderQuestSection(graphics, "&6" + net.minecraft.client.resources.language.I18n.get("gui.interactentity.quest.active"), active, x, y);
        y = renderQuestSection(graphics, "&a" + net.minecraft.client.resources.language.I18n.get("gui.interactentity.quest.completed"), completed, x, y);
        renderQuestSection(graphics, "&c" + net.minecraft.client.resources.language.I18n.get("gui.interactentity.quest.failed"), failed, x, y);
    }

    private int renderQuestSection(GuiGraphics graphics, String header, List<QuestState> quests, int x, int y) {
        if (quests.isEmpty()) return y;

        Component headerComp = TextFormatter.format(header);
        if (y >= TAB_HEIGHT && y < this.height - 4) {
            graphics.drawString(this.font, headerComp, x, y, 0xFFFFFF, true);
        }
        y += 14;

        for (QuestState quest : quests) {
            Component title = TextFormatter.format("  " + quest.getTitle());
            if (y >= TAB_HEIGHT && y < this.height - 4) {
                graphics.drawString(this.font, title, x, y, 0xFFFFFF, true);
            }
            y += 11;

            // Description
            if (!quest.getDescription().isEmpty()) {
                Component desc = TextFormatter.format("    &7" + quest.getDescription());
                List<net.minecraft.util.FormattedCharSequence> wrapped = this.font.split(desc, this.width - LIST_WIDTH - 30);
                for (net.minecraft.util.FormattedCharSequence seq : wrapped) {
                    if (y >= TAB_HEIGHT && y < this.height - 4) {
                        graphics.drawString(this.font, seq, x + 10, y, 0xAAAAAA, true);
                    }
                    y += 10;
                }
            }

            // Objectives
            for (String obj : quest.getObjectives()) {
                Component objComp = TextFormatter.format("    " + obj);
                if (y >= TAB_HEIGHT && y < this.height - 4) {
                    graphics.drawString(this.font, objComp, x + 10, y, 0xCCCCCC, true);
                }
                y += 10;
            }

            // Giver
            if (y >= TAB_HEIGHT && y < this.height - 4) {
                graphics.drawString(this.font, Component.translatable("gui.interactentity.quest.from", quest.getGiverName()),
                        x + 10, y, 0x666666, true);
            }
            y += 14;
        }

        return y;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset -= (int)(delta * 12);
        if (scrollOffset < 0) scrollOffset = 0;
        if (selectedTab == 0 && mouseX < LIST_WIDTH) {
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
