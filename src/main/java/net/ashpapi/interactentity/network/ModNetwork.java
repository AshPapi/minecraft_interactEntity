package net.ashpapi.interactentity.network;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1.0";
    private static SimpleChannel CHANNEL;
    private static int packetId = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(InteractEntityMod.MOD_ID, "main"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        // S2C: open/update dialogue on client
        CHANNEL.messageBuilder(OpenDialoguePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDialoguePacket::encode)
                .decoder(OpenDialoguePacket::new)
                .consumerMainThread(OpenDialoguePacket::handle)
                .add();

        // C2S: player selected an option
        CHANNEL.messageBuilder(SelectOptionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectOptionPacket::encode)
                .decoder(SelectOptionPacket::new)
                .consumerMainThread(SelectOptionPacket::handle)
                .add();

        // C2S: player navigated forward/back (linear nodes)
        CHANNEL.messageBuilder(NavigatePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NavigatePacket::encode)
                .decoder(NavigatePacket::new)
                .consumerMainThread(NavigatePacket::handle)
                .add();

        // C2S: player closed dialogue
        CHANNEL.messageBuilder(CloseDialogueC2SPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CloseDialogueC2SPacket::encode)
                .decoder(CloseDialogueC2SPacket::new)
                .consumerMainThread(CloseDialogueC2SPacket::handle)
                .add();

        // S2C: server closes dialogue on client
        CHANNEL.messageBuilder(CloseDialogueS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CloseDialogueS2CPacket::encode)
                .decoder(CloseDialogueS2CPacket::new)
                .consumerMainThread(CloseDialogueS2CPacket::handle)
                .add();

        // S2C: sync full progress data to client
        CHANNEL.messageBuilder(SyncProgressPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncProgressPacket::encode)
                .decoder(SyncProgressPacket::new)
                .consumerMainThread(SyncProgressPacket::handle)
                .add();

        // S2C: quest update to client
        CHANNEL.messageBuilder(QuestUpdatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(QuestUpdatePacket::encode)
                .decoder(QuestUpdatePacket::new)
                .consumerMainThread(QuestUpdatePacket::handle)
                .add();

        // S2C: on_revisit short message
        CHANNEL.messageBuilder(RevisitMessagePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RevisitMessagePacket::encode)
                .decoder(RevisitMessagePacket::new)
                .consumerMainThread(RevisitMessagePacket::handle)
                .add();

        // S2C: camera shake
        CHANNEL.messageBuilder(CameraShakePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CameraShakePacket::encode)
                .decoder(CameraShakePacket::new)
                .consumerMainThread(CameraShakePacket::handle)
                .add();

        // S2C: NPC dialogue info sync (for icon rendering)
        CHANNEL.messageBuilder(NpcSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NpcSyncPacket::encode)
                .decoder(NpcSyncPacket::new)
                .consumerMainThread(NpcSyncPacket::handle)
                .add();
    }

    public static void sendToTracking(Entity entity, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    public static void sendToTrackingAndSelf(Entity entity, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }
}
