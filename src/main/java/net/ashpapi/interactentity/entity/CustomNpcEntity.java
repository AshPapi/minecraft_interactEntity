package net.ashpapi.interactentity.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CustomNpcEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Boolean> TALKING =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> EMOTE =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> EMOTE_UNTIL =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.LONG);
    private static final String NO_EMOTE = "";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private Boolean clientTalkingOverride;
    private static final int MAIN_TRANSITION_TICKS = 10;
    private static final int TALK_TRANSITION_TICKS = 6;
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.custom_npc.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.custom_npc.walk");
    private static final RawAnimation TALK_ANIM = RawAnimation.begin().thenLoop("animation.custom_npc.talk");
    private static final RawAnimation IDLE_MOUTH_ANIM = RawAnimation.begin().thenLoop("animation.custom_npc.idle_mouth");
    private static final RawAnimation WAVE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.wave");
    private static final RawAnimation HANDSHAKE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.handshake");
    private static final RawAnimation NOD_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.nod");
    private static final RawAnimation SHAKE_HEAD_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.shake_head");
    private static final RawAnimation HAPPY_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.happy");
    private static final RawAnimation ANGRY_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.angry");
    private static final RawAnimation SAD_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.sad");
    private static final RawAnimation SHRUG_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.shrug");
    private static final RawAnimation SALUTE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.salute");
    private static final RawAnimation POINT_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.point");
    private static final RawAnimation CROSSED_ARMS_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.crossed_arms");
    private static final RawAnimation CELEBRATE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.celebrate");
    private static final RawAnimation THINK_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.think");
    private static final RawAnimation FACEPALM_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.facepalm");
    private static final RawAnimation BOW_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.bow");
    private static final RawAnimation SURPRISED_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.surprised");
    private static final RawAnimation DISMISS_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.dismiss");
    private static final RawAnimation CLAP_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.clap");
    private static final RawAnimation LAUGH_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.laugh");
    private static final RawAnimation YAWN_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.yawn");
    private static final RawAnimation BECKON_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.beckon");
    private static final RawAnimation SCARED_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.scared");
    private static final RawAnimation CONFUSED_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.confused");

    // Кастомная модель и текстура задаются через NBT
    private static final String MODEL_KEY = "InteractEntity_Model";
    private static final String TEXTURE_KEY = "InteractEntity_Texture";

    public CustomNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TALKING, false);
        this.entityData.define(EMOTE, NO_EMOTE);
        this.entityData.define(EMOTE_UNTIL, 0L);
    }

    // === Talking state (для лип-синка) ===
    public boolean isTalking() {
        if (this.level().isClientSide && clientTalkingOverride != null) {
            return clientTalkingOverride;
        }
        return this.entityData.get(TALKING);
    }

    public void setTalking(boolean talking) {
        this.entityData.set(TALKING, talking);
    }

    public void setClientTalkingOverride(@Nullable Boolean talking) {
        if (this.level().isClientSide) {
            this.clientTalkingOverride = talking;
        }
    }

    public String getEmote() {
        return this.entityData.get(EMOTE);
    }

    public boolean hasActiveEmote() {
        return !getEmote().isEmpty() && this.level().getGameTime() < this.entityData.get(EMOTE_UNTIL);
    }

    public void playEmote(String emote, int durationTicks) {
        if (emote == null || emote.isBlank() || "none".equalsIgnoreCase(emote)) {
            clearEmote();
            return;
        }

        this.entityData.set(EMOTE, normalizeEmote(emote));
        this.entityData.set(EMOTE_UNTIL, this.level().getGameTime() + Math.max(1, durationTicks));
    }

    public void clearEmote() {
        this.entityData.set(EMOTE, NO_EMOTE);
        this.entityData.set(EMOTE_UNTIL, 0L);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getEmote().isEmpty() && this.level().getGameTime() >= this.entityData.get(EMOTE_UNTIL)) {
            clearEmote();
        }
    }

    // === Кастомные модель и текстура ===
    public void setModelId(String modelId) {
        this.getPersistentData().putString(MODEL_KEY, modelId);
    }

    public String getModelId() {
        return this.getPersistentData().getString(MODEL_KEY);
    }

    public void setTextureId(String textureId) {
        this.getPersistentData().putString(TEXTURE_KEY, textureId);
    }

    public String getTextureId() {
        return this.getPersistentData().getString(TEXTURE_KEY);
    }

    public ResourceLocation getTextureLocation() {
        String tex = getTextureId();
        if (tex != null && !tex.isEmpty()) {
            return new ResourceLocation(tex);
        }
        return new ResourceLocation("interactentity", "textures/entity/custom_npc_default.png");
    }

    public ResourceLocation getModelLocation() {
        String model = getModelId();
        if (model != null && !model.isEmpty()) {
            return new ResourceLocation(model);
        }
        return new ResourceLocation("interactentity", "geo/custom_npc_default.geo.json");
    }

    // === GeckoLib ===
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", MAIN_TRANSITION_TICKS, state -> {
            RawAnimation emoteAnimation = getEmoteAnimation(this.getEmote());
            if (emoteAnimation != null && this.hasActiveEmote()) {
                return state.setAndContinue(emoteAnimation);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));

        controllers.add(new AnimationController<>(this, "talk", TALK_TRANSITION_TICKS, state -> {
            if (this.isTalking()) {
                return state.setAndContinue(TALK_ANIM);
            }
            return state.setAndContinue(IDLE_MOUTH_ANIM);
        }));
    }

    private RawAnimation getEmoteAnimation(String emote) {
        return switch (normalizeEmote(emote)) {
            case "wave" -> WAVE_ANIM;
            case "handshake" -> HANDSHAKE_ANIM;
            case "nod" -> NOD_ANIM;
            case "shake_head" -> SHAKE_HEAD_ANIM;
            case "no" -> SHAKE_HEAD_ANIM;
            case "happy" -> HAPPY_ANIM;
            case "angry" -> ANGRY_ANIM;
            case "sad" -> SAD_ANIM;
            case "shrug" -> SHRUG_ANIM;
            case "salute" -> SALUTE_ANIM;
            case "point" -> POINT_ANIM;
            case "crossed_arms" -> CROSSED_ARMS_ANIM;
            case "celebrate" -> CELEBRATE_ANIM;
            case "think" -> THINK_ANIM;
            case "facepalm" -> FACEPALM_ANIM;
            case "bow" -> BOW_ANIM;
            case "surprised" -> SURPRISED_ANIM;
            case "dismiss" -> DISMISS_ANIM;
            case "clap" -> CLAP_ANIM;
            case "laugh" -> LAUGH_ANIM;
            case "yawn" -> YAWN_ANIM;
            case "beckon" -> BECKON_ANIM;
            case "scared" -> SCARED_ANIM;
            case "confused" -> CONFUSED_ANIM;
            default -> null;
        };
    }

    private static String normalizeEmote(String emote) {
        return emote == null ? NO_EMOTE : emote.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
