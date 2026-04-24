package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.camera.DialogueCameraController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CloseDialogueS2CPacket {

    public CloseDialogueS2CPacket() {}

    public CloseDialogueS2CPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof net.ashpapi.interactentity.screen.DialogueScreen) {
                mc.setScreen(null);
                DialogueCameraController.stop();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
