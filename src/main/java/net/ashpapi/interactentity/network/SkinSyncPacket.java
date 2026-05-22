package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.skin.ClientSkinRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** S→C: набор PNG-скинов (имя → байты) для регистрации как DynamicTexture. */
public class SkinSyncPacket {
    private final Map<String, byte[]> skins;

    public SkinSyncPacket(Map<String, byte[]> skins) {
        this.skins = skins;
    }

    public SkinSyncPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.skins = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            String name = buf.readUtf();
            byte[] bytes = buf.readByteArray();
            this.skins.put(name, bytes);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(skins.size());
        for (Map.Entry<String, byte[]> e : skins.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeByteArray(e.getValue());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSkinRegistry.applyAll(skins))
        );
        ctx.get().setPacketHandled(true);
    }
}
