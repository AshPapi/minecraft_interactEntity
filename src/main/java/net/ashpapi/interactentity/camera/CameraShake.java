package net.ashpapi.interactentity.camera;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CameraShake {
    private static float intensity = 0f;
    private static int ticksLeft = 0;
    private static final Random RNG = new Random();

    public static void start(float i, int duration) {
        intensity = i;
        ticksLeft = duration;
    }

    @SubscribeEvent
    public static void onComputeAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ticksLeft <= 0) return;
        float falloff = Math.min(1f, ticksLeft / 10f);
        float amp = intensity * falloff;
        event.setYaw(event.getYaw() + (RNG.nextFloat() - 0.5f) * amp * 4);
        event.setPitch(event.getPitch() + (RNG.nextFloat() - 0.5f) * amp * 4);
        ticksLeft--;
    }
}
