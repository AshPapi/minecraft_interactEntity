package net.ashpapi.interactentity.formatting;

import net.ashpapi.interactentity.data.DialogueSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(player|var:([a-zA-Z0-9_]+)|reputation:([a-zA-Z0-9_]+))}");

    public static String resolve(String text, ServerPlayer player, LivingEntity entity) {
        if (text == null || text.isEmpty()) return text;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String fullMatch = matcher.group(1);
            String replacement = matcher.group(0);

            if (fullMatch.equals("player")) {
                replacement = player.getName().getString();
            } else if (fullMatch.startsWith("var:")) {
                String varName = matcher.group(2);
                replacement = DialogueSavedData.get(player.serverLevel()).getVar(varName);
            } else if (fullMatch.startsWith("reputation:")) {
                String factionId = matcher.group(3);
                replacement = String.valueOf(DialogueSavedData.get(player.serverLevel()).getReputation(factionId));
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
