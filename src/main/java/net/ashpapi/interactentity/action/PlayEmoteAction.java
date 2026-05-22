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
            case "facepalm" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.5f, 0.5f);
                level.sendParticles(ParticleTypes.CLOUD, x, y + 0.5, z, 3, 0.1, 0.1, 0.1, 0.01);
            }
            case "shrug", "confused" -> {
                level.playSound(null, x, y, z, SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.6f, 1.0f);
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
        // Длительности соответствуют animation_length из custom_npc_default.animation.json
        // Небольшой запас (+0.1–0.2 сек) даёт анимации доиграть до конца перед возвратом в idle
        return (int) (switch (normalized) {
            case "wave"         -> 1.55f;  // animation_length: 1.45
            case "handshake"    -> 1.55f;  // animation_length: 1.4
            case "nod"          -> 1.35f;  // animation_length: 1.2
            case "shake_head", "no" -> 1.65f; // animation_length: 1.5
            case "happy"        -> 1.25f;  // animation_length: 1.1
            case "shrug"        -> 1.15f;  // animation_length: 1.0
            case "point"        -> 2.05f;  // animation_length: 1.9
            case "crossed_arms" -> 3.15f;  // animation_length: 3.0
            case "please"       -> 2.85f;  // animation_length: 2.7
            case "celebrate"    -> 1.35f;  // animation_length: 1.2
            case "think"        -> 2.05f;  // animation_length: 1.9
            case "facepalm"     -> 2.15f;  // animation_length: 2.0
            case "bow"          -> 1.95f;  // animation_length: 1.8
            case "six_seven"    -> 2.55f;  // animation_length: 2.4
            case "surprised"    -> 0.9f;
            case "dismiss"      -> 1.0f;
            case "clap"         -> 1.15f;
            case "laugh"        -> 1.15f;
            case "yawn"         -> 1.55f;
            case "beckon"       -> 1.1f;
            case "scared"       -> 1.15f;
            case "confused"     -> 1.25f;
            default             -> 1.0f;   // был 0.05 — анимация вообще не успевала сыграть
        } * 20);
    }
}
