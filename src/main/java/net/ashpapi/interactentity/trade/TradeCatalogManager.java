package net.ashpapi.interactentity.trade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Загрузчик файлов-витрин. Зеркало DialogueManager для config/interactentity/trades/.
 * id файла = путь относительно TRADES_DIR без ".json" (как у диалогов).
 */
public class TradeCatalogManager {

    private static final Gson GSON = new GsonBuilder().create();
    private static TradeCatalogManager instance;

    public static final Path TRADES_DIR = FMLPaths.CONFIGDIR.get().resolve("interactentity").resolve("trades");

    private final Map<String, TradeShop> shopsById = new HashMap<>();

    public void loadAll() {
        shopsById.clear();

        if (!Files.exists(TRADES_DIR)) {
            try {
                Files.createDirectories(TRADES_DIR);
                InteractEntityMod.LOGGER.info("Created trades folder: {}", TRADES_DIR);
            } catch (IOException e) {
                InteractEntityMod.LOGGER.error("Failed to create trades folder: {}", e.getMessage());
                return;
            }
        }

        try (Stream<Path> files = Files.walk(TRADES_DIR)) {
            files.filter(p -> p.toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            InteractEntityMod.LOGGER.error("Failed to read trades folder: {}", e.getMessage());
        }

        InteractEntityMod.LOGGER.info("Loaded {} trade shops", shopsById.size());
    }

    private void loadFile(Path file) {
        String fileName = TRADES_DIR.relativize(file).toString().replace("\\", "/");
        String id = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
            TradeShop shop = TradeShop.fromJson(id, json);
            shopsById.put(id, shop);
            InteractEntityMod.LOGGER.info("Loaded trade shop: {}", id);
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("Failed to load trade shop {}: {}", file, e.getMessage());
        }
    }

    @Nullable
    public TradeShop getById(String id) {
        return shopsById.get(id);
    }

    public static TradeCatalogManager get() {
        return instance;
    }

    public static void setInstance(TradeCatalogManager manager) {
        instance = manager;
    }
}
