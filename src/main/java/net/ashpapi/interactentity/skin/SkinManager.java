package net.ashpapi.interactentity.skin;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.world.level.storage.LevelResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Сервер: читает PNG-скины из двух мест и валидирует. Per-world перекрывает глобальную при совпадении имён.
 *  - config/interactentity/skins/        — глобальная, общая для всех миров
 *  - &lt;world&gt;/interactentity/skins/  — per-world, едет вместе с картой
 */
public final class SkinManager {
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_]+");
    private static final Map<String, byte[]> SKINS = new HashMap<>();

    private SkinManager() {}

    public static Path getGlobalSkinsDir() {
        return FMLPaths.CONFIGDIR.get().resolve("interactentity").resolve("skins");
    }

    public static Path getWorldSkinsDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("interactentity").resolve("skins");
    }

    /** Грузит сначала глобальные, потом per-world (перекрывает). Возвращает итоговое количество. */
    public static int loadAll(MinecraftServer server) {
        SKINS.clear();
        Path globalDir = getGlobalSkinsDir();
        Path worldDir = getWorldSkinsDir(server);
        int g = scanInto(globalDir);
        int w = scanInto(worldDir);
        InteractEntityMod.LOGGER.info("[skins] loaded {} skin(s) total: global={} ({}), world={} ({})",
                SKINS.size(), g, globalDir, w, worldDir);
        return SKINS.size();
    }

    private static int scanInto(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("[skins] cannot create dir {}", dir, e);
            return 0;
        }
        int before = SKINS.size();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .forEach(SkinManager::tryLoadOne);
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("[skins] cannot scan {}", dir, e);
        }
        return SKINS.size() - before;
    }

    private static void tryLoadOne(Path file) {
        String fileName = file.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - 4); // strip .png
        if (!VALID_NAME.matcher(name).matches()) {
            InteractEntityMod.LOGGER.warn("[skins] skipped {}: name must match [a-z0-9_]+", fileName);
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                InteractEntityMod.LOGGER.warn("[skins] skipped {}: not a readable PNG", fileName);
                return;
            }
            int w = img.getWidth(), h = img.getHeight();
            boolean ok = (w == 64 && h == 64) || (w == 64 && h == 32);
            if (!ok) {
                InteractEntityMod.LOGGER.warn("[skins] skipped {}: dimensions {}x{} not supported (need 64x64 or 64x32)", fileName, w, h);
                return;
            }
            SKINS.put(name, bytes);
        } catch (Exception e) {
            InteractEntityMod.LOGGER.warn("[skins] skipped {}: {}", fileName, e.getMessage());
        }
    }

    /** Снэпшот текущих скинов — безопасный для передачи в пакет. */
    public static Map<String, byte[]> snapshot() {
        return new HashMap<>(SKINS);
    }

    public static boolean has(String name) {
        return SKINS.containsKey(name);
    }
}
