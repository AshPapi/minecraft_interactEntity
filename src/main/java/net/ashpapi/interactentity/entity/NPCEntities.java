package net.ashpapi.interactentity.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;

class NPCSkeleton extends Skeleton {
    NPCSkeleton(EntityType<? extends Skeleton> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCSpider extends Spider {
    NPCSpider(EntityType<? extends Spider> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCCreeper extends Creeper {
    NPCCreeper(EntityType<? extends Creeper> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCEnderman extends EnderMan {
    NPCEnderman(EntityType<? extends EnderMan> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCWitch extends Witch {
    NPCWitch(EntityType<? extends Witch> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCPiglin extends Piglin {
    NPCPiglin(EntityType<? extends Piglin> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCZombifiedPiglin extends ZombifiedPiglin {
    NPCZombifiedPiglin(EntityType<? extends ZombifiedPiglin> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCPillager extends Pillager {
    NPCPillager(EntityType<? extends Pillager> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCVindicator extends Vindicator {
    NPCVindicator(EntityType<? extends Vindicator> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCHusk extends Husk {
    NPCHusk(EntityType<? extends Husk> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCDrowned extends Drowned {
    NPCDrowned(EntityType<? extends Drowned> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCStray extends Stray {
    NPCStray(EntityType<? extends Stray> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCBlaze extends Blaze {
    NPCBlaze(EntityType<? extends Blaze> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCWitherSkeleton extends WitherSkeleton {
    NPCWitherSkeleton(EntityType<? extends WitherSkeleton> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCPhantom extends Phantom {
    NPCPhantom(EntityType<? extends Phantom> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCSlime extends Slime {
    NPCSlime(EntityType<? extends Slime> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCMagmaCube extends MagmaCube {
    NPCMagmaCube(EntityType<? extends MagmaCube> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCGuardian extends Guardian {
    NPCGuardian(EntityType<? extends Guardian> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCElderGuardian extends ElderGuardian {
    NPCElderGuardian(EntityType<? extends ElderGuardian> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCCaveSpider extends CaveSpider {
    NPCCaveSpider(EntityType<? extends CaveSpider> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCRavager extends Ravager {
    NPCRavager(EntityType<? extends Ravager> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCEvoker extends Evoker {
    NPCEvoker(EntityType<? extends Evoker> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCPiglinBrute extends PiglinBrute {
    NPCPiglinBrute(EntityType<? extends PiglinBrute> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCHoglin extends Hoglin {
    NPCHoglin(EntityType<? extends Hoglin> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCZoglin extends Zoglin {
    NPCZoglin(EntityType<? extends Zoglin> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCGhast extends Ghast {
    NPCGhast(EntityType<? extends Ghast> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCWarden extends Warden {
    NPCWarden(EntityType<? extends Warden> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCSilverfish extends Silverfish {
    NPCSilverfish(EntityType<? extends Silverfish> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCEndermite extends Endermite {
    NPCEndermite(EntityType<? extends Endermite> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCVex extends Vex {
    NPCVex(EntityType<? extends Vex> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}

class NPCShulker extends Shulker {
    NPCShulker(EntityType<? extends Shulker> type, Level level) { super(type, level); }
    @Override public boolean shouldDespawnInPeaceful() { return !getPersistentData().getBoolean("InteractEntity_NPC"); }
    @Override public void checkDespawn() { if (!getPersistentData().getBoolean("InteractEntity_NPC")) super.checkDespawn(); }
}
