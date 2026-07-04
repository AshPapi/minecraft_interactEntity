package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.camera.DialogueCameraController;
import net.ashpapi.interactentity.screen.DialogueScreen;
import net.minecraft.client.Minecraft;

/**
 * Клиентская логика S2C-пакетов, вынесенная из классов пакетов.
 * Классы пакетов грузятся и на дедике, и любой instanceof/new с клиентским
 * классом (Screen и наследники) в их байткоде роняет сервер при верификации —
 * RuntimeDistCleaner запрещает класс-лоад. Этот класс вызывается только через
 * DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...) и на сервер не попадает.
 */
public class ClientPacketHandler {

    public static void handleOpenDialogue(OpenDialoguePacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogueScreen screen) {
            screen.updateDialogue(pkt.nodeId, pkt.displayName, pkt.text, pkt.nodeType,
                    pkt.optionTexts, pkt.optionIndices, pkt.optionLocked, pkt.optionLockReasons, pkt.avatarTexture);
            screen.setFactionInfo(pkt.factionId, pkt.reputation);
        } else {
            DialogueScreen screen = new DialogueScreen(pkt.entityId, pkt.nodeId, pkt.displayName, pkt.text, pkt.nodeType,
                    pkt.optionTexts, pkt.optionIndices, pkt.optionLocked, pkt.optionLockReasons, pkt.avatarTexture);
            screen.setFactionInfo(pkt.factionId, pkt.reputation);
            mc.setScreen(screen);
        }
        DialogueCameraController.startLookAt(pkt.entityId);
    }

    public static void handleCloseDialogue() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogueScreen) {
            mc.setScreen(null);
            DialogueCameraController.stop();
        }
    }
}
