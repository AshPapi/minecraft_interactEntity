package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.data.ClientProgressData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public class TrackedQuestsPacket {
    private final LinkedHashSet<String> questIds;

    public TrackedQuestsPacket(Set<String> questIds) {
        this.questIds = new LinkedHashSet<>(questIds);
    }

    public TrackedQuestsPacket(FriendlyByteBuf buf) {
        this.questIds = new LinkedHashSet<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            this.questIds.add(buf.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(questIds.size());
        for (String questId : questIds) {
            buf.writeUtf(questId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientProgressData.setTrackedQuests(questIds));
        ctx.get().setPacketHandled(true);
    }
}
