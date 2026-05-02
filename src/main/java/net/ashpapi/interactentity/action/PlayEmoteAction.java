package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class PlayEmoteAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        if (!(entity instanceof CustomNpcEntity customNpc)) return;

        String emote = params.has("emote") ? params.get("emote").getAsString() : "none";
        int durationTicks = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : defaultDuration(emote);
        customNpc.playEmote(emote, durationTicks);
    }

    private static int defaultDuration(String emote) {
        String normalized = emote == null ? "" : emote.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "wave" -> 30;
            case "handshake" -> 28;
            case "nod" -> 20;
            case "no" -> 24;
            case "shake_head" -> 24;
            case "happy" -> 28;
            case "angry" -> 28;
            case "sad" -> 34;
            case "shrug" -> 28;
            case "salute" -> 26;
            case "point" -> 26;
            case "crossed_arms" -> 32;
            case "celebrate" -> 34;
            case "think" -> 30;
            case "facepalm" -> 28;
            case "bow" -> 32;
            case "surprised" -> 24;
            case "dismiss" -> 28;
            case "clap" -> 34;
            case "laugh" -> 34;
            case "yawn" -> 38;
            case "beckon" -> 30;
            case "scared" -> 30;
            case "confused" -> 32;
            default -> 1;
        };
    }
}
