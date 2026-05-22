package net.ashpapi.interactentity.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/** Клиент: принимает байты PNG от сервера, регистрирует DynamicTexture в TextureManager. */
public final class ClientSkinRegistry {
    private static final Map<String, ResourceLocation> REGISTERED = new HashMap<>();

    private ClientSkinRegistry() {}

    /** ResourceLocation, под которым регистрируется/ожидается скин с именем name. */
    public static ResourceLocation locationFor(String name) {
        return new ResourceLocation(InteractEntityMod.MOD_ID, "textures/entity/skins/" + name + ".png");
    }

    /** Полная замена набора скинов: освобождаем старые, регистрируем новые. */
    public static void applyAll(Map<String, byte[]> skins) {
        Minecraft mc = Minecraft.getInstance();
        // Освободить старые DynamicTexture.
        for (ResourceLocation loc : REGISTERED.values()) {
            mc.getTextureManager().release(loc);
        }
        REGISTERED.clear();

        for (Map.Entry<String, byte[]> entry : skins.entrySet()) {
            String name = entry.getKey();
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(entry.getValue()));
                DynamicTexture tex = new DynamicTexture(img);
                ResourceLocation loc = locationFor(name);
                mc.getTextureManager().register(loc, tex);
                REGISTERED.put(name, loc);
            } catch (Exception e) {
                InteractEntityMod.LOGGER.warn("[skins] client failed to register {}: {}", name, e.getMessage());
            }
        }
        InteractEntityMod.LOGGER.info("[skins] client registered {} skin(s)", REGISTERED.size());
    }

    public static boolean has(String name) {
        return REGISTERED.containsKey(name);
    }
}
