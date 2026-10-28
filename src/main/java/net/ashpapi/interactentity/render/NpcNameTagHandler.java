package net.ashpapi.interactentity.render;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.ClientNpcRegistry;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class NpcNameTagHandler {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        ClientNpcRegistry.Entry npc = ClientNpcRegistry.get(event.getEntity().getId());
        if (npc == null) return;
        switch (npc.nameTag) {
            case "hidden" -> event.setResult(Event.Result.DENY);
            case "hover" -> {
                if (Minecraft.getInstance().crosshairPickEntity != event.getEntity()) {
                    event.setResult(Event.Result.DENY);
                }
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNpcRegistry.clear();
    }
}
