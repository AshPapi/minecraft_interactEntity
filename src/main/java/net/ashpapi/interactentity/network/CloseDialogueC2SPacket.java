package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CloseDialogueC2SPacket {

    public CloseDialogueC2SPacket() {}

    public CloseDialogueC2SPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                InteractEntityMod.LOGGER.debug("[C->S] CloseDialogueC2SPacket from {}", player.getName().getString());
                DialogueSession.endSession(player);
                InteractEntityMod.LOGGER.debug("[C->S] DialogueSession.endSession executed for {}", player.getName().getString());
            } else {
                InteractEntityMod.LOGGER.warn("[C->S] CloseDialogueC2SPacket with null player");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}