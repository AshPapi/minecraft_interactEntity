package net.ashpapi.interactentity.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ashpapi.interactentity.render.MobIconRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomNpcRenderer extends GeoEntityRenderer<CustomNpcEntity> {

    // Кости-локаторы для предметов в руках
    private static final String RIGHT_HAND = "right_hand";
    private static final String LEFT_HAND = "left_hand";

    // Кости-плейсхолдеры для брони (объявлены в geo, совпадают с частями тела)
    private static final String ARMOR_HEAD = "armorHead";
    private static final String ARMOR_BODY = "armorBody";
    private static final String ARMOR_LEGGINGS_BODY = "armorLeggingsBody";
    private static final String ARMOR_RIGHT_ARM = "armorRightArm";
    private static final String ARMOR_LEFT_ARM = "armorLeftArm";
    private static final String ARMOR_RIGHT_LEG = "armorRightLeg";
    private static final String ARMOR_LEFT_LEG = "armorLeftLeg";
    private static final String ARMOR_RIGHT_BOOT = "armorRightBoot";
    private static final String ARMOR_LEFT_BOOT = "armorLeftBoot";

    // === Подгонка ориентации предмета в руке ===
    // GeckoLib кладёт предмет в кость-локатор и применяет ванильный THIRD_PERSON_*_HAND-трансформ.
    // На player-риге кисть смотрит вверх (вдоль руки), поэтому ванильный трансформ кладёт предмет
    // не так, как у игрока. Доворачиваем здесь. Углы в градусах, смещение в пикселях (1/16 блока).
    // Раздельно для правой руки (инструменты/оружие) и левой руки (щит)
    private static final float MAIN_ROT_X = -60f;
    private static final float MAIN_ROT_Y = 0f;     // лезвие/зуб кирки лицом вперед
    private static final float MAIN_ROT_Z = 0f;

    private static final float OFF_ROT_X = 90f;
    private static final float OFF_ROT_Y = 180f;    // щит сохраняет свою стандартную правильную ориентацию
    private static final float OFF_ROT_Z = 0f;
    // Смещения в системе кости (до поворота): X — наружу от тела, Y — вверх, Z — назад (минус = вперёд).
    // Раздельно для главной руки (меч) и левой/offhand (щит) — у них разная геометрия.
    private static final float MAIN_OFF_X = 0f;
    private static final float MAIN_OFF_Y = 1f;    // приподнять меч (уменьшено с 4f, чтобы опустить оружие пониже)
    private static final float MAIN_OFF_Z = -1f;   // толкнуть меч вперёд
    private static final float OFF_OFF_X = 0f;
    private static final float OFF_OFF_Y = 24f;    // щит был ниже руки на ~блок
    private static final float OFF_OFF_Z = -2f;   // толкнуть щит вперёд
    // Тонкая рука (Alex) уже основной: щит смещаем по X, чтобы сел по центру руки
    private static final float OFF_SLIM_DX = 1f;

    public CustomNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new CustomNpcModel());

        // Предметы в руках
        addRenderLayer(new BlockAndItemGeoLayer<CustomNpcEntity>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, CustomNpcEntity animatable) {
                return switch (bone.getName()) {
                    case RIGHT_HAND -> animatable.getMainHandItem();
                    case LEFT_HAND -> animatable.getOffhandItem();
                    default -> null;
                };
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, CustomNpcEntity animatable) {
                return switch (bone.getName()) {
                    case RIGHT_HAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                    case LEFT_HAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                    default -> ItemDisplayContext.NONE;
                };
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, CustomNpcEntity animatable,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                boolean rightHand = bone.getName().equals(RIGHT_HAND);
                float mirror = rightHand ? 1f : -1f;
                float offX = rightHand ? MAIN_OFF_X : OFF_OFF_X;
                float offY = rightHand ? MAIN_OFF_Y : OFF_OFF_Y;
                float offZ = rightHand ? MAIN_OFF_Z : OFF_OFF_Z;
                poseStack.pushPose();
                // Смещение в системе кости (до поворота): Y — вверх, X — наружу от тела
                poseStack.translate(offX / 16f * mirror, offY / 16f, offZ / 16f);
                // На тонкой руке (Alex) центр руки сдвинут — поправляем offhand-предмет (щит)
                if (!rightHand && CustomNpcModel.isSlimModel(animatable)) {
                    poseStack.translate(OFF_SLIM_DX / 16f, 0f, 0f);
                }
                float rotX = rightHand ? MAIN_ROT_X : OFF_ROT_X;
                float rotY = rightHand ? MAIN_ROT_Y : OFF_ROT_Y;
                float rotZ = rightHand ? MAIN_ROT_Z : OFF_ROT_Z;
                poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
                poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
                poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });

        // Броня: рендерим ванильные части модели брони по плейсхолдер-костям
        addRenderLayer(new ItemArmorGeoLayer<CustomNpcEntity>(this) {
            @Override
            public void render(PoseStack poseStack, CustomNpcEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel bakedModel, net.minecraft.client.renderer.RenderType renderType, MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                // Временно делаем кости брони видимыми, чтобы GeckoLib мог их обработать и отрендерить броню
                String[] armorBones = {
                    "armorHead", "armorBody", "armorLeggingsBody",
                    "armorRightArm", "armorLeftArm",
                    "armorRightLeg", "armorLeftLeg",
                    "armorRightBoot", "armorLeftBoot"
                };
                for (String boneName : armorBones) {
                    bakedModel.getBone(boneName).ifPresent(bone -> bone.setHidden(false));
                }

                super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);

                // Возвращаем скрытое состояние, чтобы кости-пустышки не влияли на обычный рендеринг сущности
                for (String boneName : armorBones) {
                    bakedModel.getBone(boneName).ifPresent(bone -> bone.setHidden(true));
                }
            }

            @Nullable
            @Override
            protected ItemStack getArmorItemForBone(GeoBone bone, CustomNpcEntity animatable) {
                return switch (bone.getName()) {
                    case ARMOR_HEAD -> this.helmetStack;
                    case ARMOR_BODY, ARMOR_RIGHT_ARM, ARMOR_LEFT_ARM -> this.chestplateStack;
                    case ARMOR_LEGGINGS_BODY, ARMOR_RIGHT_LEG, ARMOR_LEFT_LEG -> this.leggingsStack;
                    case ARMOR_RIGHT_BOOT, ARMOR_LEFT_BOOT -> this.bootsStack;
                    default -> null;
                };
            }

            @Nonnull
            @Override
            protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, CustomNpcEntity animatable) {
                return switch (bone.getName()) {
                    case ARMOR_HEAD -> EquipmentSlot.HEAD;
                    case ARMOR_BODY, ARMOR_RIGHT_ARM, ARMOR_LEFT_ARM -> EquipmentSlot.CHEST;
                    case ARMOR_LEGGINGS_BODY, ARMOR_RIGHT_LEG, ARMOR_LEFT_LEG -> EquipmentSlot.LEGS;
                    case ARMOR_RIGHT_BOOT, ARMOR_LEFT_BOOT -> EquipmentSlot.FEET;
                    default -> super.getEquipmentSlotForBone(bone, stack, animatable);
                };
            }

            @Nonnull
            @Override
            protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, CustomNpcEntity animatable, HumanoidModel<?> baseModel) {
                return switch (bone.getName()) {
                    case ARMOR_HEAD -> baseModel.head;
                    case ARMOR_BODY, ARMOR_LEGGINGS_BODY -> baseModel.body;
                    case ARMOR_RIGHT_ARM -> baseModel.rightArm;
                    case ARMOR_LEFT_ARM -> baseModel.leftArm;
                    case ARMOR_RIGHT_LEG, ARMOR_RIGHT_BOOT -> baseModel.rightLeg;
                    case ARMOR_LEFT_LEG, ARMOR_LEFT_BOOT -> baseModel.leftLeg;
                    default -> super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
                };
            }
        });
    }

    @Override
    public void render(CustomNpcEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        MobIconRenderer.tryRender(entity, poseStack, bufferSource);
    }

    @Override
    public void renderNameTag(CustomNpcEntity entity, net.minecraft.network.chat.Component name,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        String pose = entity.getCustomPose().trim().toLowerCase(java.util.Locale.ROOT);
        boolean sleeping = pose.equals("sleeping");
        boolean crawling = pose.equals("swimming") || pose.equals("crawling");
        if (sleeping || crawling) {
            // У лежащей модели голова смещена от позиции сущности вдоль корпуса:
            // во сне (на спине) — позади, ползком (лицом вниз) — впереди.
            // Переносим неймтег к голове.
            float yawRad = entity.yBodyRot * ((float) Math.PI / 180F);
            // Тело отцентрировано на позиции сущности, голова в ~0.75 блока от центра
            float dist = sleeping ? -0.8f : 0.8f;
            float dx = -net.minecraft.util.Mth.sin(yawRad) * dist;
            float dz = net.minecraft.util.Mth.cos(yawRad) * dist;
            poseStack.pushPose();
            poseStack.translate(dx, 0, dz);
            super.renderNameTag(entity, name, poseStack, bufferSource, packedLight);
            poseStack.popPose();
            return;
        }
        super.renderNameTag(entity, name, poseStack, bufferSource, packedLight);
    }
}
