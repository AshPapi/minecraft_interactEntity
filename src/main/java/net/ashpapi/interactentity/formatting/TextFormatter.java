package net.ashpapi.interactentity.formatting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Map;

public class TextFormatter {

    private static final Map<Character, ChatFormatting> FORMAT_CODES = new HashMap<>();

    static {
        FORMAT_CODES.put('0', ChatFormatting.BLACK);
        FORMAT_CODES.put('1', ChatFormatting.DARK_BLUE);
        FORMAT_CODES.put('2', ChatFormatting.DARK_GREEN);
        FORMAT_CODES.put('3', ChatFormatting.DARK_AQUA);
        FORMAT_CODES.put('4', ChatFormatting.DARK_RED);
        FORMAT_CODES.put('5', ChatFormatting.DARK_PURPLE);
        FORMAT_CODES.put('6', ChatFormatting.GOLD);
        FORMAT_CODES.put('7', ChatFormatting.GRAY);
        FORMAT_CODES.put('8', ChatFormatting.DARK_GRAY);
        FORMAT_CODES.put('9', ChatFormatting.BLUE);
        FORMAT_CODES.put('a', ChatFormatting.GREEN);
        FORMAT_CODES.put('b', ChatFormatting.AQUA);
        FORMAT_CODES.put('c', ChatFormatting.RED);
        FORMAT_CODES.put('d', ChatFormatting.LIGHT_PURPLE);
        FORMAT_CODES.put('e', ChatFormatting.YELLOW);
        FORMAT_CODES.put('f', ChatFormatting.WHITE);
        FORMAT_CODES.put('l', ChatFormatting.BOLD);
        FORMAT_CODES.put('o', ChatFormatting.ITALIC);
        FORMAT_CODES.put('n', ChatFormatting.UNDERLINE);
        FORMAT_CODES.put('m', ChatFormatting.STRIKETHROUGH);
        FORMAT_CODES.put('k', ChatFormatting.OBFUSCATED);
        FORMAT_CODES.put('r', ChatFormatting.RESET);
    }

    public static Component format(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        Style currentStyle = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();
        int i = 0;

        while (i < raw.length()) {
            if (raw.charAt(i) == '&' && i + 1 < raw.length()) {
                // Flush buffer with current style
                if (buffer.length() > 0) {
                    result.append(Component.literal(buffer.toString()).withStyle(currentStyle));
                    buffer.setLength(0);
                }

                char next = raw.charAt(i + 1);

                // HEX color: &#RRGGBB
                if (next == '#' && i + 8 <= raw.length()) {
                    String hex = raw.substring(i + 2, i + 8);
                    if (isValidHex(hex)) {
                        int color = Integer.parseInt(hex, 16);
                        currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(color));
                        i += 8;
                        continue;
                    }
                }

                // Standard format code
                char code = Character.toLowerCase(next);
                ChatFormatting formatting = FORMAT_CODES.get(code);
                if (formatting != null) {
                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else {
                        currentStyle = applyDecoration(currentStyle, formatting);
                    }
                    i += 2;
                    continue;
                }
            }

            buffer.append(raw.charAt(i));
            i++;
        }

        if (buffer.length() > 0) {
            result.append(Component.literal(buffer.toString()).withStyle(currentStyle));
        }

        return result;
    }

    private static Style applyDecoration(Style style, ChatFormatting formatting) {
        return switch (formatting) {
            case BOLD -> style.withBold(true);
            case ITALIC -> style.withItalic(true);
            case UNDERLINE -> style.withUnderlined(true);
            case STRIKETHROUGH -> style.withStrikethrough(true);
            case OBFUSCATED -> style.withObfuscated(true);
            default -> style;
        };
    }

    private static boolean isValidHex(String hex) {
        if (hex.length() != 6) return false;
        for (char c : hex.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}
