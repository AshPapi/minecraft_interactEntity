package net.ashpapi.interactentity.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CustomNpcEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<String> EMOTE =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> EMOTE_UNTIL =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<String> MODEL_ID =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE_ID =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.FLOAT);
    private static final String NO_EMOTE = "";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long lastEmoteUntil = -1L;
    /** Тик (tickCount) когда пришёл новый эмот — используется для settle-паузы. */
    private long emoteStartTickCount = -1L;
    /** Сколько тиков ждать в idle прежде чем запустить эмот (время «вставания в позу»). */
    private static final int EMOTE_SETTLE_TICKS = 5;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.custom_npc.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.custom_npc.walk");
    private static final RawAnimation WAVE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.wave");
    private static final RawAnimation HANDSHAKE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.handshake");
    private static final RawAnimation NOD_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.nod");
    private static final RawAnimation SHAKE_HEAD_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.shake_head");
    private static final RawAnimation HAPPY_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.happy");
    private static final RawAnimation SHRUG_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.shrug");
    private static final RawAnimation POINT_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.point");
    private static final RawAnimation CROSSED_ARMS_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.crossed_arms");
    private static final RawAnimation PLEASE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.please");
    private static final RawAnimation CELEBRATE_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.celebrate");
    private static final RawAnimation THINK_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.think");
    private static final RawAnimation FACEPALM_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.facepalm");
    private static final RawAnimation BOW_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.bow");
    private static final RawAnimation SIX_SEVEN_ANIM = RawAnimation.begin().thenPlay("animation.custom_npc.six_seven");

    // Кастомная модель и текстура задаются через NBT
    private static final String MODEL_KEY = "InteractEntity_Model";
    private static final String TEXTURE_KEY = "InteractEntity_Texture";
    private static final String SCALE_KEY = "InteractEntity_Scale";

    public CustomNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // Базовые инстинкты: не тонуть
        this.goalSelector.addGoal(0, new FloatGoal(this));
        
        // Свободное блуждание: скорость 0.7D для стабильного и спокойного темпа
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D) {
            @Override
            public boolean canUse() {
                return super.canUse() && !net.ashpapi.interactentity.dialogue.DialogueSession.isEntityBusy(CustomNpcEntity.this);
            }
        });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3); // Увеличили базовую скорость (было 0.25)
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EMOTE, NO_EMOTE);
        this.entityData.define(EMOTE_UNTIL, 0L);
        this.entityData.define(MODEL_ID, "");
        this.entityData.define(TEXTURE_ID, "");
        this.entityData.define(SCALE, 1.0f);
    }

    @Override
    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setNpcScale(float scale) {
        this.entityData.set(SCALE, Math.max(0.1f, Math.min(5.0f, scale)));
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

    private float lookWeight = 1.0f;

    @Override
    public void tick() {
        super.tick();
        if (!getEmote().isEmpty() && this.level().getGameTime() >= this.entityData.get(EMOTE_UNTIL)) {
            clearEmote();
        }

        if (hasActiveEmote()) {
            this.lookWeight = Math.max(0.0f, this.lookWeight - 0.1f);
        } else {
            this.lookWeight = Math.min(1.0f, this.lookWeight + 0.1f);
        }
    }

    public float getLookWeight() {
        return this.lookWeight;
    }

    // === Кастомные модель и текстура ===
    public void setModelId(String modelId) {
        this.entityData.set(MODEL_ID, modelId == null ? "" : modelId);
    }

    public String getModelId() {
        return this.entityData.get(MODEL_ID);
    }

    public void setTextureId(String textureId) {
        this.entityData.set(TEXTURE_ID, textureId == null ? "" : textureId);
    }

    public String getTextureId() {
        return this.entityData.get(TEXTURE_ID);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString(MODEL_KEY, getModelId());
        compound.putString(TEXTURE_KEY, getTextureId());
        compound.putFloat(SCALE_KEY, getScale());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(MODEL_KEY)) setModelId(compound.getString(MODEL_KEY));
        if (compound.contains(TEXTURE_KEY)) setTextureId(compound.getString(TEXTURE_KEY));
        if (compound.contains(SCALE_KEY)) setNpcScale(compound.getFloat(SCALE_KEY));
    }

    public ResourceLocation getTextureLocation() {
        String tex = getTextureId();
        if (tex == null || tex.isEmpty()) {
            return new ResourceLocation("interactentity", "textures/entity/custom_npc_default.png");
        }
        // Простое имя ("harold") → ищем как динамический скин из <world>/interactentity/skins/.
        // Тот же ResourceLocation также служит fallback-путём для ресурспака.
        if (!tex.contains(":") && !tex.contains("/")) {
            return new ResourceLocation("interactentity", "textures/entity/skins/" + tex + ".png");
        }
        return new ResourceLocation(tex);
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
        // Контроллер 1: Базовые движения (ходьба/idle)
        controllers.add(new AnimationController<>(this, "base", 10, state -> {
            // Во время диалога NPC заморожен — форсируем IDLE,
            // иначе крошечное Y-движение (гравитация на неровном рельефе)
            // даёт ненулевой limbSwingAmount и вызывает рывки между IDLE и WALK.
            if (net.ashpapi.interactentity.dialogue.DialogueSession.isEntityBusy(this)) {
                state.getController().setAnimationSpeed(1.0f);
                return state.setAndContinue(IDLE_ANIM);
            }
            if (state.isMoving() && this.hurtTime == 0) {
                // Ускорили анимацию ног (x2.2), чтобы убрать эффект "замедленной съемки"
                state.getController().setAnimationSpeed(state.getLimbSwingAmount() * 2.2f);
                return state.setAndContinue(WALK_ANIM);
            }
            state.getController().setAnimationSpeed(1.0f);
            return state.setAndContinue(IDLE_ANIM);
        }));

        // Контроллер 2: Эмоции и жесты
        // Transition = 10 тиков: GeckoLib плавно интерполирует кости из idle в начало эмота.
        controllers.add(new AnimationController<>(this, "emote", 10, state -> {
            long emoteUntil = this.entityData.get(EMOTE_UNTIL);
            if (emoteUntil != lastEmoteUntil) {
                // Новый эмот — запускаем отсчёт settle-паузы.
                // НЕ вызываем forceAnimationReset: он мешает transition-блендингу GeckoLib.
                lastEmoteUntil = emoteUntil;
                emoteStartTickCount = this.tickCount;
            }

            if (this.hasActiveEmote()) {
                String emote = normalizeEmote(this.getEmote());
                RawAnimation emoteAnimation = getEmoteAnimation(emote);
                if (emoteAnimation != null) {
                    // Settle-пауза: даём base-контроллеру вернуть NPC в idle
                    if (this.tickCount - emoteStartTickCount < EMOTE_SETTLE_TICKS) {
                        return PlayState.CONTINUE;
                    }
                    // Settle закончился — GeckoLib плавно блендит из текущей позы (idle) в эмот
                    return state.setAndContinue(emoteAnimation);
                }
            }
            return PlayState.CONTINUE;
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
            case "shrug" -> SHRUG_ANIM;
            case "point" -> POINT_ANIM;
            case "crossed_arms" -> CROSSED_ARMS_ANIM;
            case "please" -> PLEASE_ANIM;
            case "celebrate" -> CELEBRATE_ANIM;
            case "think" -> THINK_ANIM;
            case "facepalm" -> FACEPALM_ANIM;
            case "bow" -> BOW_ANIM;
            case "six_seven", "67" -> SIX_SEVEN_ANIM;
            default -> null;
        };
    }

    private static String normalizeEmote(String emote) {
        return emote == null ? NO_EMOTE : emote.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public boolean isPushable() {
        if (this.getPersistentData().getBoolean("InteractEntity_DisableKnockback")) {
            return false;
        }
        return super.isPushable();
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
        if (this.getPersistentData().getBoolean("InteractEntity_DisableKnockback")) {
            return;
        }
        super.push(entity);
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        if (this.getPersistentData().getBoolean("InteractEntity_DisableKnockback")) {
            return;
        }
        super.doPush(entity);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
