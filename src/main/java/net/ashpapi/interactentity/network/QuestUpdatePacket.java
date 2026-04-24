package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.data.ClientProgressData;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestUpdatePacket {
    private final CompoundTag questData;

    public QuestUpdatePacket(QuestState quest) {
        this.questData = quest.save();
    }

    public QuestUpdatePacket(FriendlyByteBuf buf) {
        this.questData = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(questData);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            QuestState quest = QuestState.load(questData);
            ClientProgressData.updateQuest(quest);
        });
        ctx.get().setPacketHandled(true);
    }
}
