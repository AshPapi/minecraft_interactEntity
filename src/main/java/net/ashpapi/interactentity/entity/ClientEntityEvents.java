package net.ashpapi.interactentity.entity;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEntityEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NPC_ZOMBIE.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_SPIDER.get(), SpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_ENDERMAN.get(), EndermanRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_WITCH.get(), WitchRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_PIGLIN.get(), ctx -> new PiglinRenderer(ctx, ModelLayers.PIGLIN, ModelLayers.PIGLIN_INNER_ARMOR, ModelLayers.PIGLIN_OUTER_ARMOR, false));
        event.registerEntityRenderer(ModEntities.NPC_ZOMBIFIED_PIGLIN.get(), ctx -> new PiglinRenderer(ctx, ModelLayers.ZOMBIFIED_PIGLIN, ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR, ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR, true));
        event.registerEntityRenderer(ModEntities.NPC_PILLAGER.get(), PillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_VINDICATOR.get(), VindicatorRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_HUSK.get(), HuskRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_DROWNED.get(), DrownedRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_STRAY.get(), StrayRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_BLAZE.get(), BlazeRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_WITHER_SKELETON.get(), WitherSkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_PHANTOM.get(), PhantomRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_SLIME.get(), SlimeRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_MAGMA_CUBE.get(), MagmaCubeRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_GUARDIAN.get(), GuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_ELDER_GUARDIAN.get(), ElderGuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_CAVE_SPIDER.get(), CaveSpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_RAVAGER.get(), RavagerRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_EVOKER.get(), EvokerRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_PIGLIN_BRUTE.get(), ctx -> new PiglinRenderer(ctx, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_INNER_ARMOR, ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR, true));
        event.registerEntityRenderer(ModEntities.NPC_HOGLIN.get(), HoglinRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_ZOGLIN.get(), ZoglinRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_GHAST.get(), GhastRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_WARDEN.get(), WardenRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_SILVERFISH.get(), SilverfishRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_ENDERMITE.get(), EndermiteRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_VEX.get(), VexRenderer::new);
        event.registerEntityRenderer(ModEntities.NPC_SHULKER.get(), ShulkerRenderer::new);
    }
}
