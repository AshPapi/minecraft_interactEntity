package net.ashpapi.interactentity.formatting;

import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(player|player_uuid|npc_uuid|var:([a-zA-Z0-9_]+)|reputation:([a-zA-Z0-9_]+))}");

    public static String resolve(String text, ServerPlayer player, LivingEntity entity) {
        if (text == null || text.isEmpty()) return text;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String fullMatch = matcher.group(1);
            String replacement = matcher.group(0);

            if (fullMatch.equals("player")) {
                replacement = player.getName().getString();
            } else if (fullMatch.equals("player_uuid")) {
                replacement = player.getUUID().toString();
            } else if (fullMatch.equals("npc_uuid")) {
                replacement = entity != null ? entity.getUUID().toString() : "";
            } else if (fullMatch.startsWith("var:")) {
                String varName = matcher.group(2);
                DialogueSavedData playerData = DialogueDataManager.getPlayer(player);
                if (playerData != null && playerData.hasVar(varName)) {
                    replacement = playerData.getVar(varName);
                } else {
                    replacement = DialogueDataManager.getGlobal(player.serverLevel()).getVar(varName);
                }
            } else if (fullMatch.startsWith("reputation:")) {
                String factionId = matcher.group(3);
                DialogueSavedData playerData = DialogueDataManager.getPlayer(player);
                int rep;
                if (playerData != null && playerData.hasReputation(factionId)) {
                    rep = playerData.getReputation(factionId);
                } else {
                    rep = DialogueDataManager.getGlobal(player.serverLevel()).getReputation(factionId);
                }
                replacement = String.valueOf(rep);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
