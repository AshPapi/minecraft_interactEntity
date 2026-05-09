package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public class PlayEmoteAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        if (!(entity instanceof CustomNpcEntity customNpc)) return;

        String emote = params.has("emote") ? params.get("emote").getAsString() : "none";
        int durationTicks = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : defaultDuration(emote);
        customNpc.playEmote(emote, durationTicks);

        // Добавляем эффекты для "идеальных" эмоций
        playEffects(player, customNpc, normalizeEmote(emote));
    }

    private void playEffects(ServerPlayer player, CustomNpcEntity entity, String emote) {
        var level = player.serverLevel();
        double x = entity.getX();
        double y = entity.getEyeY();
        double z = entity.getZ();

        switch (emote) {
            case "wave", "beckon", "dismiss" -> {
                level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.NEUTRAL, 0.5f, 1.2f);
            }
            case "handshake" -> {
                level.playSound(null, x, y, z, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.NEUTRAL, 0.6f, 1.0f);
            }
            case "nod", "shake_head", "no" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.4f, 1.0f);
            }
            case "happy" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.7f, 1.2f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.5, z, 8, 0.3, 0.3, 0.3, 0.05);
            }
            case "angry" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.7f, 0.8f);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 0.5, z, 5, 0.2, 0.2, 0.2, 0.0);
            }
            case "sad", "facepalm" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.5f, 0.5f);
                level.sendParticles(ParticleTypes.CLOUD, x, y + 0.5, z, 3, 0.1, 0.1, 0.1, 0.01);
            }
            case "shrug", "confused" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.6f, 1.0f);
            }
            case "salute" -> {
                level.playSound(null, x, y, z, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.NEUTRAL, 0.6f, 1.1f);
            }
            case "point" -> {
                level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.3f, 0.8f);
            }
            case "celebrate" -> {
                level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 0.5f, 1.2f);
                level.sendParticles(ParticleTypes.FIREWORK, x, y + 1.0, z, 10, 0.4, 0.4, 0.4, 0.1);
            }
            case "think" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.6f, 1.1f);
            }
            case "surprised" -> {
                level.playSound(null, x, y, z, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.NEUTRAL, 0.5f, 1.5f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y + 0.5, z, 10, 0.2, 0.5, 0.2, 0.1);
            }
            case "bow" -> {
                level.playSound(null, x, y, z, SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 0.8f, 0.9f);
            }
            case "clap" -> {
                level.playSound(null, x, y, z, SoundEvents.GOAT_HORN_PLAY, SoundSource.NEUTRAL, 0.2f, 2.0f);
            }
            case "laugh" -> {
                level.playSound(null, x, y, z, SoundEvents.PANDA_SNEEZE, SoundSource.NEUTRAL, 0.8f, 1.4f);
            }
            case "yawn" -> {
                level.playSound(null, x, y, z, SoundEvents.PHANTOM_AMBIENT, SoundSource.NEUTRAL, 0.2f, 0.7f);
            }
            case "scared" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_HURT, SoundSource.NEUTRAL, 0.7f, 1.2f);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y + 0.5, z, 3, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    private static String normalizeEmote(String emote) {
        return emote == null ? "" : emote.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static int defaultDuration(String emote) {
        String normalized = normalizeEmote(emote);
        return (int) (switch (normalized) {
            case "wave" -> 1.25f;
            case "handshake" -> 1.0f;
            case "nod" -> 0.7f;
            case "shake_head", "no" -> 0.85f;
            case "happy" -> 1.1f;
            case "angry" -> 1.0f;
            case "sad" -> 1.2f;
            case "shrug" -> 1.0f;
            case "salute" -> 1.0f;
            case "point" -> 1.0f;
            case "crossed_arms" -> 1.1f;
            case "please" -> 1.5f;
            case "celebrate" -> 1.2f;
            case "think" -> 1.1f;
            case "facepalm" -> 1.05f;
            case "bow" -> 1.2f;
            case "surprised" -> 0.9f;
            case "dismiss" -> 1.0f;
            case "clap" -> 1.15f;
            case "laugh" -> 1.15f;
            case "yawn" -> 1.55f;
            case "beckon" -> 1.1f;
            case "scared" -> 1.15f;
            case "confused" -> 1.25f;
            default -> 0.05f;
        } * 20);
    }
}
