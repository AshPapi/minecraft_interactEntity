package net.ashpapi.interactentity.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenDialoguePacket {
    // package-private: клиентскую часть обрабатывает ClientPacketHandler
    final int entityId;
    final String nodeId;
    final String displayName;
    final String text;
    final String nodeType;
    final List<String> optionTexts;
    final List<Integer> optionIndices;
    final List<Boolean> optionLocked;
    final List<String> optionLockReasons;
    final ResourceLocation avatarTexture;
    final String factionId;
    final int reputation;
    final String cameraMode;
    final float cameraYawOffset;
    final float cameraPitchOffset;

    public OpenDialoguePacket(int entityId, String nodeId, String displayName, String text, String nodeType,
                              List<String> optionTexts, List<Integer> optionIndices,
                              List<Boolean> optionLocked, List<String> optionLockReasons,
                              ResourceLocation avatarTexture,
                              String factionId, int reputation,
                              String cameraMode, float cameraYawOffset, float cameraPitchOffset) {
        this.entityId = entityId;
        this.nodeId = nodeId;
        this.displayName = displayName;
        this.text = text;
        this.nodeType = nodeType;
        this.optionTexts = optionTexts;
        this.optionIndices = optionIndices;
        this.optionLocked = optionLocked;
        this.optionLockReasons = optionLockReasons;
        this.avatarTexture = avatarTexture;
        this.factionId = factionId;
        this.reputation = reputation;
        this.cameraMode = cameraMode;
        this.cameraYawOffset = cameraYawOffset;
        this.cameraPitchOffset = cameraPitchOffset;
    }

    public OpenDialoguePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.nodeId = buf.readUtf();
        this.displayName = buf.readUtf();
        this.text = buf.readUtf();
        this.nodeType = buf.readUtf();

        this.avatarTexture = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.factionId = buf.readBoolean() ? buf.readUtf() : null;
        this.reputation = buf.readInt();

        int count = buf.readInt();
        this.optionTexts = new ArrayList<>(count);
        this.optionIndices = new ArrayList<>(count);
        this.optionLocked = new ArrayList<>(count);
        this.optionLockReasons = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.optionTexts.add(buf.readUtf());
            this.optionIndices.add(buf.readInt());
            this.optionLocked.add(buf.readBoolean());
            this.optionLockReasons.add(buf.readUtf());
        }

        this.cameraMode = buf.readUtf();
        this.cameraYawOffset = buf.readFloat();
        this.cameraPitchOffset = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(nodeId);
        buf.writeUtf(displayName);
        buf.writeUtf(text);
        buf.writeUtf(nodeType);

        buf.writeBoolean(avatarTexture != null);
        if (avatarTexture != null) buf.writeResourceLocation(avatarTexture);
        buf.writeBoolean(factionId != null);
        if (factionId != null) buf.writeUtf(factionId);
        buf.writeInt(reputation);

        buf.writeInt(optionTexts.size());
        for (int i = 0; i < optionTexts.size(); i++) {
            buf.writeUtf(optionTexts.get(i));
            buf.writeInt(optionIndices.get(i));
            buf.writeBoolean(optionLocked.get(i));
            buf.writeUtf(optionLockReasons.get(i));
        }

        buf.writeUtf(cameraMode);
        buf.writeFloat(cameraYawOffset);
        buf.writeFloat(cameraPitchOffset);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenDialogue(this)));
        ctx.get().setPacketHandled(true);
    }
}