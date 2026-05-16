package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NavigatePacket {
    private final String expectedNodeId;
    private final boolean forward;

    public NavigatePacket(String expectedNodeId, boolean forward) {
        this.expectedNodeId = expectedNodeId;
        this.forward = forward;
    }

    public NavigatePacket(FriendlyByteBuf buf) {
        this.expectedNodeId = buf.readUtf();
        this.forward = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(expectedNodeId);
        buf.writeBoolean(forward);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                DialogueSession.handleNavigate(player, expectedNodeId, forward);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
