package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.SkinSyncPacket;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.ashpapi.interactentity.skin.SkinManager;
import net.ashpapi.interactentity.summon.SummonScheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync merged (global+player) progress to client
            ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));

            // Send dynamic skins from world folder
            ModNetwork.sendToPlayer(player, new SkinSyncPacket(SkinManager.snapshot()));

            // Schedule on_join summons
            SummonScheduler.scheduleOnJoin(player);
        }
    }
}
