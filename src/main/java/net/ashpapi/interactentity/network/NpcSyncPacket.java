package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.data.ClientNpcRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells the client that an entity has a dialogue attached (for icon rendering). */
public class NpcSyncPacket {
    public final int entityId;
    public final String dialogueId;      // empty string = remove
    public final String entryNodeId;

    public NpcSyncPacket(int entityId, String dialogueId, String entryNodeId) {
        this.entityId = entityId;
        this.dialogueId = dialogueId;
        this.entryNodeId = entryNodeId;
    }

    public NpcSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.dialogueId = buf.readUtf();
        this.entryNodeId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(dialogueId);
        buf.writeUtf(entryNodeId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (dialogueId.isEmpty()) {
                ClientNpcRegistry.remove(entityId);
            } else {
                ClientNpcRegistry.set(entityId, dialogueId, entryNodeId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
