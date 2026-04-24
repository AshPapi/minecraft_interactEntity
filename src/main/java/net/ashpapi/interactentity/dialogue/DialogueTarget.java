package net.ashpapi.interactentity.dialogue;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;

public class DialogueTarget {
    private final String name;
    private final String tag;

    public DialogueTarget(String name, String tag) {
        this.name = name;
        this.tag = tag;
    }

    public String getName() { return name; }
    public String getTag() { return tag; }

    public boolean matches(LivingEntity entity) {
        if (entity.getCustomName() == null) return false;
        String entityName = entity.getCustomName().getString();
        return entityName.equals(name) && entity.getTags().contains(tag);
    }

    public static DialogueTarget fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        String tag = json.get("tag").getAsString();
        return new DialogueTarget(name, tag);
    }
}
