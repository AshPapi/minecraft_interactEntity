package net.ashpapi.interactentity.entity;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, InteractEntityMod.MOD_ID);

    public static final RegistryObject<EntityType<NPCZombie>> NPC_ZOMBIE =
        ENTITIES.register("npc_zombie", () -> EntityType.Builder
            .<NPCZombie>of(NPCZombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_zombie"));

    public static final RegistryObject<EntityType<NPCSkeleton>> NPC_SKELETON =
        ENTITIES.register("npc_skeleton", () -> EntityType.Builder
            .<NPCSkeleton>of(NPCSkeleton::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F).clientTrackingRange(8).build("interactentity:npc_skeleton"));

    public static final RegistryObject<EntityType<NPCSpider>> NPC_SPIDER =
        ENTITIES.register("npc_spider", () -> EntityType.Builder
            .<NPCSpider>of(NPCSpider::new, MobCategory.MONSTER)
            .sized(1.4F, 0.9F).clientTrackingRange(8).build("interactentity:npc_spider"));

    public static final RegistryObject<EntityType<NPCCreeper>> NPC_CREEPER =
        ENTITIES.register("npc_creeper", () -> EntityType.Builder
            .<NPCCreeper>of(NPCCreeper::new, MobCategory.MONSTER)
            .sized(0.6F, 1.7F).clientTrackingRange(8).build("interactentity:npc_creeper"));

    public static final RegistryObject<EntityType<NPCEnderman>> NPC_ENDERMAN =
        ENTITIES.register("npc_enderman", () -> EntityType.Builder
            .<NPCEnderman>of(NPCEnderman::new, MobCategory.MONSTER)
            .sized(0.6F, 2.9F).clientTrackingRange(8).build("interactentity:npc_enderman"));

    public static final RegistryObject<EntityType<NPCWitch>> NPC_WITCH =
        ENTITIES.register("npc_witch", () -> EntityType.Builder
            .<NPCWitch>of(NPCWitch::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_witch"));

    public static final RegistryObject<EntityType<NPCPiglin>> NPC_PIGLIN =
        ENTITIES.register("npc_piglin", () -> EntityType.Builder
            .<NPCPiglin>of(NPCPiglin::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_piglin"));

    public static final RegistryObject<EntityType<NPCZombifiedPiglin>> NPC_ZOMBIFIED_PIGLIN =
        ENTITIES.register("npc_zombified_piglin", () -> EntityType.Builder
            .<NPCZombifiedPiglin>of(NPCZombifiedPiglin::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_zombified_piglin"));

    public static final RegistryObject<EntityType<NPCPillager>> NPC_PILLAGER =
        ENTITIES.register("npc_pillager", () -> EntityType.Builder
            .<NPCPillager>of(NPCPillager::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_pillager"));

    public static final RegistryObject<EntityType<NPCVindicator>> NPC_VINDICATOR =
        ENTITIES.register("npc_vindicator", () -> EntityType.Builder
            .<NPCVindicator>of(NPCVindicator::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_vindicator"));

    public static final RegistryObject<EntityType<NPCHusk>> NPC_HUSK =
        ENTITIES.register("npc_husk", () -> EntityType.Builder
            .<NPCHusk>of(NPCHusk::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_husk"));

    public static final RegistryObject<EntityType<NPCDrowned>> NPC_DROWNED =
        ENTITIES.register("npc_drowned", () -> EntityType.Builder
            .<NPCDrowned>of(NPCDrowned::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_drowned"));

    public static final RegistryObject<EntityType<NPCStray>> NPC_STRAY =
        ENTITIES.register("npc_stray", () -> EntityType.Builder
            .<NPCStray>of(NPCStray::new, MobCategory.MONSTER)
            .sized(0.6F, 1.99F).clientTrackingRange(8).build("interactentity:npc_stray"));

    public static final RegistryObject<EntityType<NPCBlaze>> NPC_BLAZE =
        ENTITIES.register("npc_blaze", () -> EntityType.Builder
            .<NPCBlaze>of(NPCBlaze::new, MobCategory.MONSTER)
            .sized(0.6F, 1.8F).clientTrackingRange(8).build("interactentity:npc_blaze"));

    public static final RegistryObject<EntityType<NPCWitherSkeleton>> NPC_WITHER_SKELETON =
        ENTITIES.register("npc_wither_skeleton", () -> EntityType.Builder
            .<NPCWitherSkeleton>of(NPCWitherSkeleton::new, MobCategory.MONSTER)
            .sized(0.7F, 2.4F).clientTrackingRange(8).build("interactentity:npc_wither_skeleton"));

    public static final RegistryObject<EntityType<NPCPhantom>> NPC_PHANTOM =
        ENTITIES.register("npc_phantom", () -> EntityType.Builder
            .<NPCPhantom>of(NPCPhantom::new, MobCategory.MONSTER)
            .sized(0.9F, 0.5F).clientTrackingRange(8).build("interactentity:npc_phantom"));

    public static final RegistryObject<EntityType<NPCSlime>> NPC_SLIME =
        ENTITIES.register("npc_slime", () -> EntityType.Builder
            .<NPCSlime>of(NPCSlime::new, MobCategory.MONSTER)
            .sized(2.04F, 2.04F).clientTrackingRange(8).build("interactentity:npc_slime"));

    public static final RegistryObject<EntityType<NPCMagmaCube>> NPC_MAGMA_CUBE =
        ENTITIES.register("npc_magma_cube", () -> EntityType.Builder
            .<NPCMagmaCube>of(NPCMagmaCube::new, MobCategory.MONSTER)
            .sized(2.04F, 2.04F).clientTrackingRange(8).build("interactentity:npc_magma_cube"));

    public static final RegistryObject<EntityType<NPCGuardian>> NPC_GUARDIAN =
        ENTITIES.register("npc_guardian", () -> EntityType.Builder
            .<NPCGuardian>of(NPCGuardian::new, MobCategory.MONSTER)
            .sized(0.85F, 0.85F).clientTrackingRange(8).build("interactentity:npc_guardian"));

    public static final RegistryObject<EntityType<NPCElderGuardian>> NPC_ELDER_GUARDIAN =
        ENTITIES.register("npc_elder_guardian", () -> EntityType.Builder
            .<NPCElderGuardian>of(NPCElderGuardian::new, MobCategory.MONSTER)
            .sized(1.9975F, 1.9975F).clientTrackingRange(10).build("interactentity:npc_elder_guardian"));

    public static final RegistryObject<EntityType<NPCCaveSpider>> NPC_CAVE_SPIDER =
        ENTITIES.register("npc_cave_spider", () -> EntityType.Builder
            .<NPCCaveSpider>of(NPCCaveSpider::new, MobCategory.MONSTER)
            .sized(0.7F, 0.5F).clientTrackingRange(8).build("interactentity:npc_cave_spider"));

    public static final RegistryObject<EntityType<NPCRavager>> NPC_RAVAGER =
        ENTITIES.register("npc_ravager", () -> EntityType.Builder
            .<NPCRavager>of(NPCRavager::new, MobCategory.MONSTER)
            .sized(1.95F, 2.2F).clientTrackingRange(10).build("interactentity:npc_ravager"));

    public static final RegistryObject<EntityType<NPCEvoker>> NPC_EVOKER =
        ENTITIES.register("npc_evoker", () -> EntityType.Builder
            .<NPCEvoker>of(NPCEvoker::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_evoker"));

    public static final RegistryObject<EntityType<NPCPiglinBrute>> NPC_PIGLIN_BRUTE =
        ENTITIES.register("npc_piglin_brute", () -> EntityType.Builder
            .<NPCPiglinBrute>of(NPCPiglinBrute::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F).clientTrackingRange(8).build("interactentity:npc_piglin_brute"));

    public static final RegistryObject<EntityType<NPCHoglin>> NPC_HOGLIN =
        ENTITIES.register("npc_hoglin", () -> EntityType.Builder
            .<NPCHoglin>of(NPCHoglin::new, MobCategory.MONSTER)
            .sized(1.3965F, 1.4F).clientTrackingRange(8).build("interactentity:npc_hoglin"));

    public static final RegistryObject<EntityType<NPCZoglin>> NPC_ZOGLIN =
        ENTITIES.register("npc_zoglin", () -> EntityType.Builder
            .<NPCZoglin>of(NPCZoglin::new, MobCategory.MONSTER)
            .sized(1.3965F, 1.4F).clientTrackingRange(8).build("interactentity:npc_zoglin"));

    public static final RegistryObject<EntityType<NPCGhast>> NPC_GHAST =
        ENTITIES.register("npc_ghast", () -> EntityType.Builder
            .<NPCGhast>of(NPCGhast::new, MobCategory.MONSTER)
            .sized(4.0F, 4.0F).clientTrackingRange(10).build("interactentity:npc_ghast"));

    public static final RegistryObject<EntityType<NPCWarden>> NPC_WARDEN =
        ENTITIES.register("npc_warden", () -> EntityType.Builder
            .<NPCWarden>of(NPCWarden::new, MobCategory.MONSTER)
            .sized(0.9F, 2.9F).clientTrackingRange(16).build("interactentity:npc_warden"));

    public static final RegistryObject<EntityType<NPCSilverfish>> NPC_SILVERFISH =
        ENTITIES.register("npc_silverfish", () -> EntityType.Builder
            .<NPCSilverfish>of(NPCSilverfish::new, MobCategory.MONSTER)
            .sized(0.4F, 0.3F).clientTrackingRange(8).build("interactentity:npc_silverfish"));

    public static final RegistryObject<EntityType<NPCEndermite>> NPC_ENDERMITE =
        ENTITIES.register("npc_endermite", () -> EntityType.Builder
            .<NPCEndermite>of(NPCEndermite::new, MobCategory.MONSTER)
            .sized(0.4F, 0.3F).clientTrackingRange(8).build("interactentity:npc_endermite"));

    public static final RegistryObject<EntityType<NPCVex>> NPC_VEX =
        ENTITIES.register("npc_vex", () -> EntityType.Builder
            .<NPCVex>of(NPCVex::new, MobCategory.MONSTER)
            .sized(0.4F, 0.8F).clientTrackingRange(8).build("interactentity:npc_vex"));

    public static final RegistryObject<EntityType<NPCShulker>> NPC_SHULKER =
        ENTITIES.register("npc_shulker", () -> EntityType.Builder
            .<NPCShulker>of(NPCShulker::new, MobCategory.MONSTER)
            .sized(1.0F, 1.0F).clientTrackingRange(10).build("interactentity:npc_shulker"));
}
