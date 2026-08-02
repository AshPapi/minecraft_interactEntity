package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwapInventorySlotsC2SPacket {
    private final int fromSlot;
    private final int toSlot;

    public SwapInventorySlotsC2SPacket(int fromSlot, int toSlot) {
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
    }

    public SwapInventorySlotsC2SPacket(FriendlyByteBuf buf) {
        this.fromSlot = buf.readVarInt();
        this.toSlot = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(fromSlot);
        buf.writeVarInt(toSlot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                Inventory inv = player.getInventory();
                if (fromSlot == toSlot) return;
                if (fromSlot >= 0 && fromSlot < inv.getContainerSize() && toSlot >= 0 && toSlot < inv.getContainerSize()) {
                    ItemStack itemFrom = inv.getItem(fromSlot);
                    ItemStack itemTo = inv.getItem(toSlot);
                    inv.setItem(fromSlot, itemTo);
                    inv.setItem(toSlot, itemFrom);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
