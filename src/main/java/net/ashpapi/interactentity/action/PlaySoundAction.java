package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class PlaySoundAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String id = params.get("sound").getAsString();
        float volume = params.has("volume") ? params.get("volume").getAsFloat() : 1.0f;
        float pitch = params.has("pitch") ? params.get("pitch").getAsFloat() : 1.0f;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));
        if (sound == null) return;
        String target = params.has("target") ? params.get("target").getAsString() : "player";
        double x = player.getX(), y = player.getY(), z = player.getZ();
        if ("entity".equals(target)) { x = entity.getX(); y = entity.getY(); z = entity.getZ(); }
        player.serverLevel().playSound(null, x, y, z, sound, SoundSource.NEUTRAL, volume, pitch);
    }
}
