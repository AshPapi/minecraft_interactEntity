package net.ashpapi.interactentity.formatting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.Map;

public class TranslationResolver {
    public static String resolve(JsonElement element, String lang) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            String targetLang = lang.toLowerCase();
            if (obj.has(targetLang)) {
                return obj.get(targetLang).getAsString();
            }
            // Fallback to en_us
            if (obj.has("en_us")) {
                return obj.get("en_us").getAsString();
            }
            // Fallback to the first available translation
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    return entry.getValue().getAsString();
                }
            }
        }
        return "";
    }

    public static JsonElement parseSafe(String str) {
        if (str == null || str.isEmpty()) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        try {
            return JsonParser.parseString(str);
        } catch (Exception e) {
            return new JsonPrimitive(str);
        }
    }
}
