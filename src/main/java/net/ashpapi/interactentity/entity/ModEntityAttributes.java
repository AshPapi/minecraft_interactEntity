package net.ashpapi.interactentity.entity;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NPC_ZOMBIE.get(),           Zombie.createAttributes().build());
        event.put(ModEntities.NPC_SKELETON.get(),         Skeleton.createAttributes().build());
        event.put(ModEntities.NPC_SPIDER.get(),           Spider.createAttributes().build());
        event.put(ModEntities.NPC_CREEPER.get(),          Creeper.createAttributes().build());
        event.put(ModEntities.NPC_ENDERMAN.get(),         EnderMan.createAttributes().build());
        event.put(ModEntities.NPC_WITCH.get(),            Witch.createAttributes().build());
        event.put(ModEntities.NPC_PIGLIN.get(),           Piglin.createAttributes().build());
        event.put(ModEntities.NPC_ZOMBIFIED_PIGLIN.get(), ZombifiedPiglin.createAttributes().build());
        event.put(ModEntities.NPC_PILLAGER.get(),         Pillager.createAttributes().build());
        event.put(ModEntities.NPC_VINDICATOR.get(),       Vindicator.createAttributes().build());
        event.put(ModEntities.NPC_HUSK.get(),             Husk.createAttributes().build());
        event.put(ModEntities.NPC_DROWNED.get(),          Drowned.createAttributes().build());
        event.put(ModEntities.NPC_STRAY.get(),            Stray.createAttributes().build());
        event.put(ModEntities.NPC_BLAZE.get(),            Blaze.createAttributes().build());
        event.put(ModEntities.NPC_WITHER_SKELETON.get(),  WitherSkeleton.createAttributes().build());
        event.put(ModEntities.NPC_PHANTOM.get(),          Mob.createMobAttributes().build());
        event.put(ModEntities.NPC_SLIME.get(),            Mob.createMobAttributes().build());
        event.put(ModEntities.NPC_MAGMA_CUBE.get(),       MagmaCube.createAttributes().build());
        event.put(ModEntities.NPC_GUARDIAN.get(),         Guardian.createAttributes().build());
        event.put(ModEntities.NPC_ELDER_GUARDIAN.get(),   ElderGuardian.createAttributes().build());
        event.put(ModEntities.NPC_CAVE_SPIDER.get(),      CaveSpider.createAttributes().build());
        event.put(ModEntities.NPC_RAVAGER.get(),          Ravager.createAttributes().build());
        event.put(ModEntities.NPC_EVOKER.get(),           Evoker.createAttributes().build());
        event.put(ModEntities.NPC_PIGLIN_BRUTE.get(),     PiglinBrute.createAttributes().build());
        event.put(ModEntities.NPC_HOGLIN.get(),           Hoglin.createAttributes().build());
        event.put(ModEntities.NPC_ZOGLIN.get(),           Zoglin.createAttributes().build());
        event.put(ModEntities.NPC_GHAST.get(),            Ghast.createAttributes().build());
        event.put(ModEntities.NPC_WARDEN.get(),           Warden.createAttributes().build());
        event.put(ModEntities.NPC_SILVERFISH.get(),       Silverfish.createAttributes().build());
        event.put(ModEntities.NPC_ENDERMITE.get(),        Endermite.createAttributes().build());
        event.put(ModEntities.NPC_VEX.get(),              Vex.createAttributes().build());
        event.put(ModEntities.NPC_SHULKER.get(),          Shulker.createAttributes().build());
        event.put(ModEntities.CUSTOM_NPC.get(),             CustomNpcEntity.createAttributes().build());
    }
}
