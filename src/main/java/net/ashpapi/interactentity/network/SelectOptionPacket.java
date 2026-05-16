package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectOptionPacket {
    private final String expectedNodeId;
    private final int optionIndex;

    public SelectOptionPacket(String expectedNodeId, int optionIndex) {
        this.expectedNodeId = expectedNodeId;
        this.optionIndex = optionIndex;
    }

    public SelectOptionPacket(FriendlyByteBuf buf) {
        this.expectedNodeId = buf.readUtf();
        this.optionIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(expectedNodeId);
        buf.writeInt(optionIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                DialogueSession.handleOptionSelected(player, expectedNodeId, optionIndex);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
