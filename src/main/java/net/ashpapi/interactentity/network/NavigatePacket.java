package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NavigatePacket {
    private final boolean forward;

    public NavigatePacket(boolean forward) {
        this.forward = forward;
    }

    public NavigatePacket(FriendlyByteBuf buf) {
        this.forward = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(forward);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                DialogueSession.handleNavigate(player, forward);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
