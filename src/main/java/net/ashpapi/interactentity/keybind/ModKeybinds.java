package net.ashpapi.interactentity.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.overlay.QuestHudOverlay;
import net.ashpapi.interactentity.screen.HistoryScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {
    public static final KeyMapping OPEN_JOURNAL = new KeyMapping(
            "key.interactentity.journal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.interactentity"
    );

    public static final KeyMapping TOGGLE_QUEST_HUD = new KeyMapping(
            "key.interactentity.toggle_quest_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.interactentity"
    );

    @Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_JOURNAL);
            event.register(TOGGLE_QUEST_HUD);
        }
    }

    @Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class Handler {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (OPEN_JOURNAL.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new HistoryScreen());
                }
            }
            if (TOGGLE_QUEST_HUD.consumeClick()) {
                QuestHudOverlay.toggleVisibility();
                // Сообщение убрано
            }
        }
    }
}