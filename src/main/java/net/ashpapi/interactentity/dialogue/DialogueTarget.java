package net.ashpapi.interactentity.dialogue;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class DialogueTarget {
    private final String name;
    private final String tag;
    @Nullable
    private final String entityType;
    @Nullable
    private final String faction;

    public DialogueTarget(String name, String tag, @Nullable String entityType, @Nullable String faction) {
        this.name = name;
        this.tag = tag;
        this.entityType = entityType;
        this.faction = faction;
    }

    public String getName() { return name; }
    public String getTag() { return tag; }
    @Nullable public String getEntityType() { return entityType; }
    @Nullable public String getFaction() { return faction; }

    public boolean matches(LivingEntity entity) {
        if (entity.getCustomName() == null) return false;
        String entityName = entity.getCustomName().getString();
        if (!entityName.equals(name)) return false;
        if (!entity.getTags().contains(tag)) return false;

        if (entityType != null && !entityType.isEmpty()) {
            ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (typeId == null) return false;
            String typeStr = typeId.toString();
            if (!typeStr.equals(entityType)) {
                String mapped = net.ashpapi.interactentity.command.NpcCommand.getMappedNpcType(entityType);
                if (mapped == null || !typeStr.equals(mapped)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static DialogueTarget fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        String tag = json.get("tag").getAsString();
        String entityType = json.has("entity_type") ? json.get("entity_type").getAsString() : null;
        String faction = json.has("faction") ? json.get("faction").getAsString() : null;
        return new DialogueTarget(name, tag, entityType, faction);
    }
}
