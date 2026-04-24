package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.formatting.TextFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RevisitMessagePacket {
    private final String text;

    public RevisitMessagePacket(String text) {
        this.text = text;
    }

    public RevisitMessagePacket(FriendlyByteBuf buf) {
        this.text = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(text);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Component msg = TextFormatter.format(text);
                mc.player.displayClientMessage(msg, true); // true = actionbar
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
