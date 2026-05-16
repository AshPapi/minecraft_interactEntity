package net.ashpapi.interactentity.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CustomNpcModel extends GeoModel<CustomNpcEntity> {
    private static final ResourceLocation NPC_PLAYER_MODEL =
            new ResourceLocation("interactentity", "geo/custom_npc_default.geo.json");
    private static final ResourceLocation NPC_PLAYER_SLIM_MODEL =
            new ResourceLocation("interactentity", "geo/custom_npc_slim.geo.json");
    private static final ResourceLocation NPC_PLAYER_ANIMATIONS =
            new ResourceLocation("interactentity", "animations/custom_npc_default.animation.json");
    private static final Map<ResourceLocation, PlayerModelShape> PLAYER_MODEL_CACHE = new ConcurrentHashMap<>();

    @Override
    public ResourceLocation getModelResource(CustomNpcEntity entity) {
        ResourceLocation model = entity.getModelLocation();
        return switch (detectPlayerModelShape(model)) {
            case DEFAULT -> NPC_PLAYER_MODEL;
            case SLIM -> NPC_PLAYER_SLIM_MODEL;
            case NONE -> model;
        };
    }

    @Override
    public ResourceLocation getTextureResource(CustomNpcEntity entity) {
        return entity.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(CustomNpcEntity entity) {
        if (detectPlayerModelShape(entity.getModelLocation()) != PlayerModelShape.NONE) {
            return NPC_PLAYER_ANIMATIONS;
        }

        String modelId = entity.getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            // Конвенция: анимация рядом с моделью, .geo.json -> .animation.json
            String animPath = modelId.replace(".geo.json", ".animation.json");
            return new ResourceLocation(animPath);
        }
        return NPC_PLAYER_ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(CustomNpcEntity animatable, long instanceId, AnimationState<CustomNpcEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        CoreGeoBone neck = getAnimationProcessor().getBone("neck");
        CoreGeoBone chest = getAnimationProcessor().getBone("chest");

        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        
        // 1. Плавное слежение головой (ванильная логика)
        if (head != null) {
            float headPitch = Mth.clamp(entityData.headPitch(), -45, 45) * ((float) Math.PI / 180F);
            float headYaw = Mth.wrapDegrees(entityData.netHeadYaw()) * ((float) Math.PI / 180F);

            if (animatable.hasActiveEmote()) {
                if (neck != null) {
                    neck.updateRotation(neck.getRotX(), headYaw * 0.15f, neck.getRotZ());
                }
            } else {
                if (neck != null) {
                    neck.updateRotation(neck.getRotX(), headYaw * 0.25f, neck.getRotZ());
                }
                head.updateRotation(headPitch, headYaw * 0.75f, head.getRotZ());
            }
        }

        // 2. МЯГКОЕ ДЫХАНИЕ (Процедурное)
        // Добавляем микро-движение грудной клетки, чтобы NPC казался живым
        if (chest != null) {
            float cumulativeTick = animatable.tickCount + animationState.getPartialTick();
            // Очень медленный цикл: ~4 секунды на один вдох-выдох
            float breathing = (float) Math.sin(cumulativeTick * 0.04f);
            
            // Масштабируем только по Y и Z на 1% (почти незаметно, но оживляет модель)
            chest.setScaleY(1.0f + breathing * 0.01f);
            chest.setScaleZ(1.0f + breathing * 0.005f);
        }
    }

    private static PlayerModelShape detectPlayerModelShape(ResourceLocation model) {
        if (NPC_PLAYER_MODEL.equals(model)) return PlayerModelShape.DEFAULT;
        if (NPC_PLAYER_SLIM_MODEL.equals(model)) return PlayerModelShape.SLIM;
        return PLAYER_MODEL_CACHE.computeIfAbsent(model, CustomNpcModel::readPlayerModelShape);
    }

    private static PlayerModelShape readPlayerModelShape(ResourceLocation model) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(model);
            if (resource.isEmpty()) {
                return PlayerModelShape.NONE;
            }

            try (Reader reader = resource.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject geometry = firstGeometry(root);
                if (geometry == null || !hasTextureSize(geometry, 64, 64)) {
                    return PlayerModelShape.NONE;
                }

                Map<String, JsonObject> bones = bonesByName(geometry);
                if (!hasCube(bones, "head", 8, 8, 8)
                        || !hasCube(bones, "body", 8, 12, 4)
                        || !hasCube(bones, "rightleg", 4, 12, 4)
                        || !hasCube(bones, "leftleg", 4, 12, 4)) {
                    return PlayerModelShape.NONE;
                }

                boolean defaultArms = hasCube(bones, "rightarm", 4, 12, 4)
                        && hasCube(bones, "leftarm", 4, 12, 4);
                if (defaultArms) {
                    return PlayerModelShape.DEFAULT;
                }

                boolean slimArms = hasCube(bones, "rightarm", 3, 12, 4)
                        && hasCube(bones, "leftarm", 3, 12, 4);
                return slimArms ? PlayerModelShape.SLIM : PlayerModelShape.NONE;
            }
        } catch (Exception ignored) {
            return PlayerModelShape.NONE;
        }
    }

    private static JsonObject firstGeometry(JsonObject root) {
        if (!root.has("minecraft:geometry") || !root.get("minecraft:geometry").isJsonArray()) {
            return null;
        }
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries.size() == 0 || !geometries.get(0).isJsonObject()) {
            return null;
        }
        return geometries.get(0).getAsJsonObject();
    }

    private static boolean hasTextureSize(JsonObject geometry, int width, int height) {
        if (!geometry.has("description") || !geometry.get("description").isJsonObject()) {
            return false;
        }
        JsonObject description = geometry.getAsJsonObject("description");
        return description.has("texture_width")
                && description.has("texture_height")
                && description.get("texture_width").getAsInt() == width
                && description.get("texture_height").getAsInt() == height;
    }

    private static Map<String, JsonObject> bonesByName(JsonObject geometry) {
        Map<String, JsonObject> bones = new HashMap<>();
        if (!geometry.has("bones") || !geometry.get("bones").isJsonArray()) {
            return bones;
        }

        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            if (!element.isJsonObject()) continue;
            JsonObject bone = element.getAsJsonObject();
            if (!bone.has("name")) continue;
            bones.put(normalizeBoneName(bone.get("name").getAsString()), bone);
        }
        return bones;
    }

    private static String normalizeBoneName(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static boolean hasCube(Map<String, JsonObject> bones, String boneName, int x, int y, int z) {
        JsonObject bone = bones.get(boneName);
        if (bone == null || !bone.has("cubes") || !bone.get("cubes").isJsonArray()) {
            return false;
        }

        for (JsonElement element : bone.getAsJsonArray("cubes")) {
            if (!element.isJsonObject()) continue;
            JsonObject cube = element.getAsJsonObject();
            if (hasSize(cube, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSize(JsonObject cube, int x, int y, int z) {
        if (!cube.has("size") || !cube.get("size").isJsonArray()) {
            return false;
        }

        JsonArray size = cube.getAsJsonArray("size");
        return size.size() == 3
                && closeTo(size.get(0).getAsDouble(), x)
                && closeTo(size.get(1).getAsDouble(), y)
                && closeTo(size.get(2).getAsDouble(), z);
    }

    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) < 0.001;
    }

    private enum PlayerModelShape {
        NONE,
        DEFAULT,
        SLIM
    }
}
