package net.ashpapi.interactentity.overlay;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ReputationOverlay {
    private static final ResourceLocation DEFAULT_NOTIFICATION_TEX = new ResourceLocation("interactentity", "textures/gui/reputation_bg.png");
    private static final List<ReputationNotification> NOTIFICATIONS = new ArrayList<>();
    private static final int DISPLAY_DURATION = 160; // 8 seconds
    private static final int FADE_DURATION = 20; // 1 second fade

    private static Boolean useNotificationTex = null;

    private static boolean checkTexture() {
        if (useNotificationTex == null) {
            useNotificationTex = Minecraft.getInstance().getResourceManager().getResource(DEFAULT_NOTIFICATION_TEX).isPresent();
        }
        return useNotificationTex;
    }

    public static void addNotification(String factionId, int delta) {
        NOTIFICATIONS.add(new ReputationNotification(factionId, delta));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Iterator<ReputationNotification> it = NOTIFICATIONS.iterator();
        while (it.hasNext()) {
            ReputationNotification note = it.next();
            note.ticksRemaining--;
            if (note.ticksRemaining <= 0) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (NOTIFICATIONS.isEmpty()) return;
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int screenHeight = mc.getWindow().getGuiScaledHeight();
        // Смещаем на уровень хотбара
        int x = 10;
        int y = screenHeight - 38;

        for (int i = 0; i < NOTIFICATIONS.size(); i++) {
            ReputationNotification note = NOTIFICATIONS.get(i);
            float alpha = 1.0f;
            if (note.ticksRemaining < FADE_DURATION) {
                alpha = note.ticksRemaining / (float) FADE_DURATION;
            } else if (DISPLAY_DURATION - note.ticksRemaining < FADE_DURATION) {
                alpha = (DISPLAY_DURATION - note.ticksRemaining) / (float) FADE_DURATION;
            }

            int alphaInt = (int)(alpha * 255);
            if (alphaInt <= 0) continue;

            int color = note.delta > 0 ? 0x55FF55 : 0xFF5555;

            String sign = note.delta > 0 ? "+" : "";
            String repLabel = net.minecraft.client.resources.language.I18n.get("gui.interactentity.reputation");
            Component text = Component.literal(repLabel + ": ")
                    .append(Component.literal(sign + note.delta).withStyle(note.delta > 0 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            int bgWidth = font.width(text) + 10;
            int bgHeight = 14;
            
            // Draw background using texture or fallback
            if (checkTexture()) {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                graphics.blitNineSliced(DEFAULT_NOTIFICATION_TEX, x, y, bgWidth, bgHeight, 4, 32, 32, 0, 0);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                graphics.fill(x, y, x + bgWidth, y + bgHeight, (alphaInt / 2 << 24));
            }
            
            // Draw text
            graphics.drawString(font, text, x + 5, y + 3, (alphaInt << 24) | 0xFFFFFF, false);

            y -= (bgHeight + 2); // Stack upwards
        }
    }

    private static class ReputationNotification {
        final String factionId;
        final int delta;
        int ticksRemaining;

        ReputationNotification(String factionId, int delta) {
            this.factionId = factionId;
            this.delta = delta;
            this.ticksRemaining = DISPLAY_DURATION;
        }
    }
}
