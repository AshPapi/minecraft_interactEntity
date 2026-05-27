package net.ashpapi.interactentity.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class DialogueManager {

    private static final Gson GSON = new GsonBuilder().create();
    private static DialogueManager instance;

    public static final Path DIALOGUES_DIR = FMLPaths.CONFIGDIR.get().resolve("interactentity").resolve("dialogues");

    private final Map<String, DialogueTree> dialoguesById = new HashMap<>();
    private final List<DialogueTree> allDialogues = new ArrayList<>();

    public void loadAll() {
        dialoguesById.clear();
        allDialogues.clear();

        if (!Files.exists(DIALOGUES_DIR)) {
            try {
                Files.createDirectories(DIALOGUES_DIR);
                InteractEntityMod.LOGGER.info("Created dialogues folder: {}", DIALOGUES_DIR);
            } catch (IOException e) {
                InteractEntityMod.LOGGER.error("Failed to create dialogues folder: {}", e.getMessage());
                return;
            }
        }

        try (Stream<Path> files = Files.walk(DIALOGUES_DIR)) {
            files.filter(p -> p.toString().endsWith(".json")).forEach(this::loadFile);
        } catch (IOException e) {
            InteractEntityMod.LOGGER.error("Failed to read dialogues folder: {}", e.getMessage());
        }

        InteractEntityMod.LOGGER.info("Loaded {} dialogues", allDialogues.size());
    }

    private void loadFile(Path file) {
        String fileName = DIALOGUES_DIR.relativize(file).toString().replace("\\", "/");
        String id = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
            DialogueTree tree = DialogueTree.fromJson(id, json);
            dialoguesById.put(id, tree);
            allDialogues.add(tree);
            InteractEntityMod.LOGGER.info("Loaded dialogue: {}", id);
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("Failed to load dialogue {}: {}", file, e.getMessage());
        }
    }

    public boolean reloadOne(String id) {
        Path file = DIALOGUES_DIR.resolve(id + ".json");
        if (!Files.exists(file)) {
            InteractEntityMod.LOGGER.warn("Dialogue file not found: {}", file);
            return false;
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonElement.class).getAsJsonObject();
            DialogueTree tree = DialogueTree.fromJson(id, json);
            DialogueTree old = dialoguesById.put(id, tree);
            if (old != null) allDialogues.remove(old);
            allDialogues.add(tree);
            InteractEntityMod.LOGGER.info("Reloaded dialogue: {}", id);
            return true;
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("Failed to reload dialogue {}: {}", id, e.getMessage());
            return false;
        }
    }

    @Nullable
    public DialogueTree findDialogueForEntity(LivingEntity entity) {
        for (DialogueTree tree : allDialogues) {
            if (tree.getTarget().matches(entity)) {
                return tree;
            }
        }
        return null;
    }

    @Nullable
    public DialogueTree getDialogueById(String id) {
        return dialoguesById.get(id);
    }

    public List<DialogueTree> getAllDialogues() {
        return allDialogues;
    }

    public Set<String> getLoadedIds() {
        return dialoguesById.keySet();
    }

    @Nullable
    public ResourceLocation getDialogueAvatar(LivingEntity entity) {
        if (entity == null) return null;

        if (entity.getPersistentData().contains("DialogueAvatar", 8)) {
            String path = entity.getPersistentData().getString("DialogueAvatar");
            if (!path.isEmpty()) {
                return new ResourceLocation(path);
            }
        }

        DialogueTree tree = findDialogueForEntity(entity);
        if (tree != null && tree.getAvatar() != null) {
            return tree.getAvatar();
        }

        if (entity instanceof CustomNpcEntity customNpc) {
            return customNpc.getTextureLocation();
        }

        return null;
    }

    public static DialogueManager get() {
        return instance;
    }

    public static void setInstance(DialogueManager manager) {
        instance = manager;
    }
}
