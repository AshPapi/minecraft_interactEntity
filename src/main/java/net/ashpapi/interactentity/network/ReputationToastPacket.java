package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReputationToastPacket {
    private final String factionId;
    private final String displayLabel;
    private final int delta;

    public ReputationToastPacket(String factionId, int delta) {
        this(factionId, factionId, delta);
    }

    public ReputationToastPacket(String factionId, String displayLabel, int delta) {
        this.factionId = factionId;
        this.displayLabel = displayLabel != null ? displayLabel : factionId;
        this.delta = delta;
    }

    public ReputationToastPacket(FriendlyByteBuf buf) {
        this.factionId = buf.readUtf();
        this.displayLabel = buf.readUtf();
        this.delta = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(factionId);
        buf.writeUtf(displayLabel);
        buf.writeInt(delta);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.ashpapi.interactentity.overlay.ReputationOverlay.addNotification(displayLabel, delta);
        });
        ctx.get().setPacketHandled(true);
    }
}
