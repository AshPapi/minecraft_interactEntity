package net.ashpapi.interactentity.camera;

import net.ashpapi.interactentity.InteractEntityMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DialogueCameraController {

    private static final float LERP_SPEED = 0.025f;

    private enum Mode { NPC, SIDE, SIDE_REWIND }

    private static class KeyFrame {
        final float yaw, pitch;
        final Vec3 pos;
        KeyFrame(float yaw, float pitch, Vec3 pos) {
            this.yaw = yaw; this.pitch = pitch; this.pos = pos;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────
    private static boolean active         = false;
    private static Mode    mode           = Mode.NPC;
    private static int     targetEntityId = -1;

    private static final List<KeyFrame> recording = new ArrayList<>();
    private static int sideStartIndex = 0;
    private static int rewindIndex    = 0;

    private static float startYaw, startPitch, targetYaw, targetPitch;
    private static Vec3  startPos = Vec3.ZERO, targetPos = Vec3.ZERO, controlPos = Vec3.ZERO;
    private static boolean overridePos   = false;
    private static float   progress      = 0f;
    private static boolean segmentActive = false;

    private static float lastAppliedYaw, lastAppliedPitch;
    private static Vec3  lastCamPos = Vec3.ZERO;

    private static float savedPlayerYaw, savedPlayerPitch;
    private static CameraType savedCameraType = null;

    private static boolean pendingLookAt   = false;
    private static int     pendingEntityId = -1;
    private static Vec3    pendingFirstPersonStartPos = null;
    private static boolean introPositionOverride = false;

    // ── Reflection ────────────────────────────────────────────────────────

    private static final Method CAMERA_SET_POSITION = resolveSetPosition();
    private static Method resolveSetPosition() {
        try {
            Method m = Camera.class.getDeclaredMethod("setPosition", double.class, double.class, double.class);
            m.setAccessible(true);
            return m;
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("Camera.setPosition access failed: {}", e.getMessage());
            return null;
        }
    }

    // Camera.detached — контролирует рендер тела игрока без переключения CameraType
    private static final Field CAMERA_DETACHED = resolveDetached();
    private static Field resolveDetached() {
        try {
            Field f = Camera.class.getDeclaredField("detached");
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            InteractEntityMod.LOGGER.error("Camera.detached access failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    public static boolean isActive() { return active; }

    public static void startLookAt(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!active) {
            savedPlayerYaw   = mc.player.getYRot();
            savedPlayerPitch = mc.player.getXRot();
            recording.clear();
            recording.add(new KeyFrame(savedPlayerYaw, savedPlayerPitch, null));
            lastAppliedYaw   = savedPlayerYaw;
            lastAppliedPitch = savedPlayerPitch;
            active = true;
            // Если игрок в F5, оставляем его CameraType как есть, но анимируем камеру к глазам вручную.
            // Так не видно рывка F5 -> первое лицо -> F5.
            CameraType current = mc.options.getCameraType();
            if (current != CameraType.FIRST_PERSON) {
                savedCameraType = current;
                pendingFirstPersonStartPos = mc.gameRenderer.getMainCamera().getPosition();
            }
        }

        if (mode == Mode.SIDE_REWIND) {
            pendingLookAt   = true;
            pendingEntityId = entityId;
            return;
        }

        targetEntityId = entityId;
        beginNpcSegment(mc);
    }

    public static void lookSideAt(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Entity target = mc.level.getEntity(entityId);
        if (target == null) return;

        if (!active) {
            savedPlayerYaw   = mc.player.getYRot();
            savedPlayerPitch = mc.player.getXRot();
            recording.clear();
            recording.add(new KeyFrame(savedPlayerYaw, savedPlayerPitch, null));
            lastAppliedYaw   = savedPlayerYaw;
            lastAppliedPitch = savedPlayerPitch;
            active = true;
        }

        sideStartIndex = recording.size();
        targetEntityId = entityId;
        mode = Mode.SIDE;

        Vec3 playerEye = mc.player.getEyePosition();
        Vec3 npcPos    = target.position().add(0, target.getBbHeight() * 0.7, 0);
        Vec3 toPlayer  = playerEye.subtract(npcPos).normalize();
        Vec3 right     = new Vec3(-toPlayer.z, 0, toPlayer.x).normalize();
        Vec3 camPos = npcPos
                .subtract(toPlayer.scale(0.35))
                .add(right.scale(1.2))
                .add(0, 0.45, 0);

        Vec3 camDir = playerEye.subtract(camPos).normalize();
        double hDist = Math.sqrt(camDir.x * camDir.x + camDir.z * camDir.z);
        targetYaw   = (float)(Mth.atan2(camDir.z, camDir.x) * Mth.RAD_TO_DEG) - 90f;
        targetPitch = (float)(-(Mth.atan2(camDir.y, hDist) * Mth.RAD_TO_DEG));
        targetPos   = camPos;

        startYaw   = lastAppliedYaw;
        startPitch = lastAppliedPitch;
        startPos   = (overridePos && lastCamPos.lengthSqr() > 0.001) ? lastCamPos : playerEye;
        controlPos = arcControl(startPos, targetPos, mc);
        overridePos   = true;
        progress      = 0f;
        segmentActive = true;
    }

    public static void stopSide() {
        if (!active || mode == Mode.NPC) return;
        mode          = Mode.SIDE_REWIND;
        segmentActive = false;
        rewindIndex   = recording.size() - 1;
    }

    public static void stop() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            float yaw   = lastAppliedYaw;
            float pitch = lastAppliedPitch;
            // В сайд-луке lastApplied — угол камеры сбоку; берём угол NPC-взгляда
            if ((mode == Mode.SIDE || mode == Mode.SIDE_REWIND)
                    && sideStartIndex > 0 && sideStartIndex <= recording.size()) {
                KeyFrame npc = recording.get(sideStartIndex - 1);
                yaw   = npc.yaw;
                pitch = npc.pitch;
            }
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }
        active        = false;
        mode          = Mode.NPC;
        segmentActive = false;
        pendingLookAt = false;
        pendingFirstPersonStartPos = null;
        introPositionOverride = false;
        recording.clear();
        targetEntityId = -1;
        savedCameraType = null;
    }

    public static void release() { stop(); }

    // ── Internals ─────────────────────────────────────────────────────────

    private static void beginNpcSegment(Minecraft mc) {
        mode        = Mode.NPC;
        startYaw    = lastAppliedYaw;
        startPitch  = lastAppliedPitch;
        progress    = 0f;
        segmentActive = true;
        calcNpcTarget(mc);

        if (pendingFirstPersonStartPos != null && mc.player != null) {
            startPos = pendingFirstPersonStartPos;
            targetPos = mc.player.getEyePosition();
            controlPos = arcControl(startPos, targetPos, mc);
            lastCamPos = startPos;
            overridePos = true;
            introPositionOverride = true;
            pendingFirstPersonStartPos = null;
        } else if (savedCameraType != null && mc.player != null) {
            startPos = mc.player.getEyePosition();
            targetPos = startPos;
            controlPos = startPos;
            lastCamPos = startPos;
            overridePos = true;
            introPositionOverride = false;
        } else {
            overridePos = false;
            introPositionOverride = false;
        }
    }

    private static void calcNpcTarget(Minecraft mc) {
        if (mc.level == null) return;
        Entity target = mc.level.getEntity(targetEntityId);
        if (target == null) return;
        Vec3 eye  = mc.player.getEyePosition();
        Vec3 tgt  = target.position().add(0, target.getBbHeight() * 0.8, 0);
        Vec3 diff = tgt.subtract(eye);
        double hDist = diff.horizontalDistance();
        targetYaw   = (float)(Mth.atan2(diff.z, diff.x) * Mth.RAD_TO_DEG) - 90f;
        targetPitch = (float)(-(Mth.atan2(diff.y, hDist) * Mth.RAD_TO_DEG));
    }

    private static Vec3 arcControl(Vec3 from, Vec3 to, Minecraft mc) {
        Vec3 mid    = from.add(to).scale(0.5);
        Vec3 center = mc.player.position().add(0, mc.player.getBbHeight() * 0.5, 0);
        Vec3 out    = mid.subtract(center);
        double len  = out.length();
        if (len < 0.01) out = new Vec3(1, 0, 0);
        else out = out.scale(1.0 / len);
        return mid.add(out.scale(1.0)).add(0, 0.3, 0);
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, float t) {
        float u = 1f - t;
        return new Vec3(
                u*u*a.x + 2*u*t*b.x + t*t*c.x,
                u*u*a.y + 2*u*t*b.y + t*t*c.y,
                u*u*a.z + 2*u*t*b.z + t*t*c.z
        );
    }

    private static void setCameraPos(Camera camera, Vec3 pos) {
        if (CAMERA_SET_POSITION == null) return;
        try { CAMERA_SET_POSITION.invoke(camera, pos.x, pos.y, pos.z); }
        catch (Exception e) { InteractEntityMod.LOGGER.error("Camera.setPosition failed: {}", e.getMessage()); }
    }

    private static void setCameraDetached(Camera camera, boolean detached) {
        if (CAMERA_DETACHED == null) return;
        try { CAMERA_DETACHED.set(camera, detached); }
        catch (Exception e) { InteractEntityMod.LOGGER.error("Camera.detached set failed: {}", e.getMessage()); }
    }

    private static float lerpAngle(float from, float to, float t) {
        return from + Mth.wrapDegrees(to - from) * t;
    }

    private static float easeInOut(float t) {
        return t < 0.5f ? 2f*t*t : 1f - (float)Math.pow(-2f*t + 2f, 2) / 2f;
    }

    // ── Events ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Camera camera = event.getCamera();

        // Форсируем detached=true во время сайд-лука чтобы тело игрока рендерилось
        // без переключения CameraType (без эффекта "F5")
        setCameraDetached(camera, mode == Mode.SIDE || mode == Mode.SIDE_REWIND);

        if (mode == Mode.SIDE_REWIND) {
            doRewind(event, camera, mc);
            return;
        }

        if (!segmentActive) {
            event.setYaw(lastAppliedYaw);
            event.setPitch(lastAppliedPitch);
            if (overridePos) {
                if (mode == Mode.NPC && savedCameraType != null) {
                    lastCamPos = mc.player.getEyePosition();
                }
                setCameraPos(camera, lastCamPos);
            }
            return;
        }

        if (mode == Mode.NPC) calcNpcTarget(mc);

        progress = Math.min(1f, progress + LERP_SPEED);
        float t = easeInOut(progress);

        float curYaw   = lerpAngle(startYaw, targetYaw, t);
        float curPitch = Mth.lerp(t, startPitch, targetPitch);
        event.setYaw(curYaw);
        event.setPitch(curPitch);
        lastAppliedYaw   = curYaw;
        lastAppliedPitch = curPitch;

        if (mode == Mode.NPC) {
            mc.player.setYRot(curYaw);
            mc.player.setXRot(curPitch);
        }

        Vec3 appliedPos = null;
        if (overridePos) {
            appliedPos = bezier(startPos, controlPos, targetPos, t);
            setCameraPos(camera, appliedPos);
            lastCamPos = appliedPos;
        }

        recording.add(new KeyFrame(curYaw, curPitch, appliedPos));

        if (progress >= 1f) {
            segmentActive = false;
            if (introPositionOverride && mode == Mode.NPC) {
                overridePos = savedCameraType != null;
                if (overridePos) {
                    lastCamPos = mc.player.getEyePosition();
                }
                introPositionOverride = false;
            }
        }
    }

    private static void doRewind(ViewportEvent.ComputeCameraAngles event, Camera camera, Minecraft mc) {
        if (rewindIndex < sideStartIndex) {
            if (sideStartIndex > 0 && sideStartIndex <= recording.size()) {
                KeyFrame npc = recording.get(sideStartIndex - 1);
                event.setYaw(npc.yaw);
                event.setPitch(npc.pitch);
                lastAppliedYaw   = npc.yaw;
                lastAppliedPitch = npc.pitch;
                mc.player.setYRot(npc.yaw);
                mc.player.setXRot(npc.pitch);
            }
            if (recording.size() > sideStartIndex) {
                recording.subList(sideStartIndex, recording.size()).clear();
            }
            mode          = Mode.NPC;
            segmentActive = false;
            if (savedCameraType != null) {
                lastCamPos = mc.player.getEyePosition();
                setCameraPos(camera, lastCamPos);
                setCameraDetached(camera, false);
                overridePos = true;
            } else {
                overridePos = false;
                setCameraDetached(camera, false);
            }

            if (pendingLookAt) {
                pendingLookAt  = false;
                targetEntityId = pendingEntityId;
                beginNpcSegment(mc);
            }
            return;
        }

        KeyFrame k = recording.get(rewindIndex--);
        event.setYaw(k.yaw);
        event.setPitch(k.pitch);
        lastAppliedYaw   = k.yaw;
        lastAppliedPitch = k.pitch;
        if (k.pos != null) {
            setCameraPos(camera, k.pos);
            lastCamPos = k.pos;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        // Скрываем руку во время сайд-лука (камера смотрит сбоку, FP-рука выглядит странно)
        if (active && mode != Mode.NPC) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player) return;
        
        boolean hiding = false;
        if (active && mode == Mode.SIDE && segmentActive) {
            hiding = progress < 0.35f; // Скрываем в начале анимации "от лица"
        } else if (active && mode == Mode.SIDE_REWIND) {
            int total = recording.size() - sideStartIndex;
            if (total > 0) {
                float rewindProgress = 1f - (float)(rewindIndex - sideStartIndex) / total;
                // Скрываем в конце анимации возврата "в лицо"
                hiding = savedCameraType == null && rewindProgress > 0.65f;
            }
        }
        
        // КРИТИЧЕСКИЙ ФИКС: Если мы НЕ в режиме SIDE/REWIND (уже вернулись в тело),
        // но камера всё еще в режиме detached (предыдущий кадр), принудительно скрываем тело.
        if (active && mode == Mode.NPC && savedCameraType == null) {
            hiding = true;
        }

        if (hiding) event.setCanceled(true);
    }

}
