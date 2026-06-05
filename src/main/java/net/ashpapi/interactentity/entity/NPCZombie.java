package net.ashpapi.interactentity.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class NPCZombie extends Zombie {

    public NPCZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return !getPersistentData().getBoolean("InteractEntity_NPC");
    }

    @Override
    public void checkDespawn() {
        if (getPersistentData().getBoolean("InteractEntity_NPC")) {
            return;
        }
        super.checkDespawn();
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return false;
    }
}
