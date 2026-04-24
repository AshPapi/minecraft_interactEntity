package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.camera.DialogueCameraController;
import net.ashpapi.interactentity.screen.DialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenDialoguePacket {
    private final int entityId;
    private final String displayName;
    private final String text;
    private final String nodeType;
    private final List<String> optionTexts;
    private final List<Integer> optionIndices;
    private final ResourceLocation avatarTexture;
    private final ResourceLocation background;
    private final ResourceLocation optionsBackground;
    private final String cameraMode;
    private final float cameraYawOffset;
    private final float cameraPitchOffset;

    public OpenDialoguePacket(int entityId, String displayName, String text, String nodeType,
                              List<String> optionTexts, List<Integer> optionIndices,
                              ResourceLocation avatarTexture,
                              ResourceLocation background, ResourceLocation optionsBackground,
                              String cameraMode, float cameraYawOffset, float cameraPitchOffset) {
        this.entityId = entityId;
        this.displayName = displayName;
        this.text = text;
        this.nodeType = nodeType;
        this.optionTexts = optionTexts;
        this.optionIndices = optionIndices;
        this.avatarTexture = avatarTexture;
        this.background = background;
        this.optionsBackground = optionsBackground;
        this.cameraMode = cameraMode;
        this.cameraYawOffset = cameraYawOffset;
        this.cameraPitchOffset = cameraPitchOffset;
    }

    public OpenDialoguePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.displayName = buf.readUtf();
        this.text = buf.readUtf();
        this.nodeType = buf.readUtf();

        this.avatarTexture = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.background = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.optionsBackground = buf.readBoolean() ? buf.readResourceLocation() : null;

        int count = buf.readInt();
        this.optionTexts = new ArrayList<>(count);
        this.optionIndices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.optionTexts.add(buf.readUtf());
            this.optionIndices.add(buf.readInt());
        }

        this.cameraMode = buf.readUtf();
        this.cameraYawOffset = buf.readFloat();
        this.cameraPitchOffset = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(displayName);
        buf.writeUtf(text);
        buf.writeUtf(nodeType);

        buf.writeBoolean(avatarTexture != null);
        if (avatarTexture != null) buf.writeResourceLocation(avatarTexture);
        buf.writeBoolean(background != null);
        if (background != null) buf.writeResourceLocation(background);
        buf.writeBoolean(optionsBackground != null);
        if (optionsBackground != null) buf.writeResourceLocation(optionsBackground);

        buf.writeInt(optionTexts.size());
        for (int i = 0; i < optionTexts.size(); i++) {
            buf.writeUtf(optionTexts.get(i));
            buf.writeInt(optionIndices.get(i));
        }

        buf.writeUtf(cameraMode);
        buf.writeFloat(cameraYawOffset);
        buf.writeFloat(cameraPitchOffset);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof DialogueScreen screen) {
                screen.updateDialogue(displayName, text, nodeType, optionTexts, optionIndices, avatarTexture, background, optionsBackground);
            } else {
                mc.setScreen(new DialogueScreen(entityId, displayName, text, nodeType,
                        optionTexts, optionIndices, avatarTexture, background, optionsBackground));
            }
            applyCameraMode(entityId);
        });
        ctx.get().setPacketHandled(true);
    }

    private void applyCameraMode(int entityId) {
        DialogueCameraController.startLookAt(entityId);
    }
}