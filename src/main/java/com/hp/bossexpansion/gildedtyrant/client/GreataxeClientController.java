package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.hp.bossexpansion.gildedtyrant.network.GreataxeNetwork;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Iterator;

@EventBusSubscriber(modid = BossExpansion.MOD_ID, value = Dist.CLIENT)
public final class GreataxeClientController {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final EnumProxy<HumanoidModel.ArmPose> WHIRLWIND_ARM_POSE_PROXY = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            true,
            (IArmPoseTransformer) GreataxeClientController::transformWhirlwindArmPose
    );

    private static boolean loggedArmPoseResolved = false;

    public static final IClientItemExtensions ITEM_EXTENSIONS = new IClientItemExtensions() {
        @Override
        @Nullable
        public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
            SpinAnimState state = SPIN_STATES.get(entityLiving.getId());
            if (state != null && state.active && state.hand == hand) {
                HumanoidModel.ArmPose pose = WHIRLWIND_ARM_POSE_PROXY.getValue();
                if (!loggedArmPoseResolved) {
                    loggedArmPoseResolved = true;
                    LOGGER.debug("[GildedGreataxe] Resolved whirlwind ArmPose enum extension: {}", pose);
                }
                return pose;
            }
            return null;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                               float partialTick, float equipProcess, float swingProcess) {
            return GreataxeClientController.applyFirstPersonTransform(poseStack, player, arm, partialTick, equipProcess, swingProcess);
        }
    };

    private static final Int2ObjectMap<SpinAnimState> SPIN_STATES = new Int2ObjectOpenHashMap<>();

    // 本地客户端输入与视角状态记录
    private static boolean useTriggeredSession = false;
    private static InteractionHand triggeredHand = InteractionHand.MAIN_HAND;
    private static int holdUseTicks = 0;
    private static boolean lastSentHolding = false;
    private static boolean autoSwitchedToThirdPerson = false;
    private static boolean manualPerspectiveChanged = false;
    private static CameraType lastObservedCamera = CameraType.FIRST_PERSON;

    // 渲染线程单帧上下文：供玩家渲染期同步手臂物品旋转并结束后还原头部朝向
    @Nullable
    private static Player renderingSpinningPlayer;
    private static HumanoidArm renderingSpinningArm = HumanoidArm.RIGHT;
    private static boolean pushedPlayerSpinPose = false;
    private static float savedYHeadRot;
    private static float savedYHeadRotO;
    private static float savedXRot;
    private static float savedXRotO;

    private GreataxeClientController() {
    }

    public static void notifyUseTriggered(InteractionHand hand) {
        useTriggeredSession = true;
        triggeredHand = hand;
    }

    public static void handleStatePayload(GreataxeNetwork.StatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        InteractionHand hand = payload.handOrd() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (payload.active()) {
            SpinAnimState st = SPIN_STATES.computeIfAbsent(payload.entityId(), k -> new SpinAnimState());
            boolean newlyStarted = !st.active;
            st.active = true;
            st.hand = hand;
            if (newlyStarted) {
                st.spinTicks = 0;
            }
            if (mc.player != null && payload.entityId() == mc.player.getId() && newlyStarted) {
                onLocalWhirlwindStarted(mc);
            }
        } else {
            SPIN_STATES.remove(payload.entityId());
            if (mc.player != null && payload.entityId() == mc.player.getId()) {
                onLocalWhirlwindStopped(mc);
            }
        }
    }

    private static void onLocalWhirlwindStarted(Minecraft mc) {
        CameraType current = mc.options.getCameraType();
        manualPerspectiveChanged = false;
        if (current.isFirstPerson()) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            autoSwitchedToThirdPerson = true;
            lastObservedCamera = CameraType.THIRD_PERSON_BACK;
            LOGGER.debug("[GildedGreataxe] Local whirlwind started: switched camera from FIRST_PERSON to THIRD_PERSON_BACK");
        } else {
            autoSwitchedToThirdPerson = false;
            lastObservedCamera = current;
            LOGGER.debug("[GildedGreataxe] Local whirlwind started in existing camera mode {}", current);
        }
    }

    private static void onLocalWhirlwindStopped(Minecraft mc) {
        if (autoSwitchedToThirdPerson && !manualPerspectiveChanged && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            LOGGER.debug("[GildedGreataxe] Local whirlwind ended: restored FIRST_PERSON camera");
        }
        autoSwitchedToThirdPerson = false;
        manualPerspectiveChanged = false;
    }

    public static boolean isLocalSpinning() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        SpinAnimState st = SPIN_STATES.get(mc.player.getId());
        return st != null && st.active;
    }

    public static boolean shouldApplyThirdPersonWhirlwindPose(ItemDisplayContext perspective) {
        if (renderingSpinningPlayer == null) {
            return false;
        }
        if (perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return renderingSpinningArm == HumanoidArm.RIGHT;
        }
        if (perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return renderingSpinningArm == HumanoidArm.LEFT;
        }
        return false;
    }

    private static HumanoidArm resolveArm(LivingEntity living, InteractionHand hand) {
        HumanoidArm main = living.getMainArm();
        return hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && isLocalSpinning()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            SPIN_STATES.clear();
            useTriggeredSession = false;
            holdUseTicks = 0;
            lastSentHolding = false;
            autoSwitchedToThirdPerson = false;
            manualPerspectiveChanged = false;
            return;
        }

        // 检测旋风斩期间玩家是否主动按 F5 切换过视角（如切回第一人称）
        if (isLocalSpinning()) {
            CameraType nowCam = mc.options.getCameraType();
            if (nowCam != lastObservedCamera) {
                manualPerspectiveChanged = true;
                lastObservedCamera = nowCam;
                LOGGER.debug("[GildedGreataxe] Player manually toggled camera during whirlwind to {}", nowCam);
            }
        }

        boolean keyDown = mc.screen == null && mc.options.keyUse.isDown();
        InteractionHand validHand = GildedGreataxeItem.findWhirlwindHand(mc.player);
        if (!keyDown || validHand == null || mc.player.isSpectator() || !mc.player.isAlive()) {
            useTriggeredSession = false;
            holdUseTicks = 0;
        } else if (useTriggeredSession || lastSentHolding) {
            triggeredHand = validHand;
            holdUseTicks++;
        }

        // 长按右键判定（>= 2 tick 区分单击箱子/交互与长按旋风斩）
        boolean wantHold = holdUseTicks >= 2 && validHand != null && !mc.player.getCooldowns().isOnCooldown(GildedTyrantRegistry.GILDED_GREATAXE.get());
        if (wantHold != lastSentHolding) {
            lastSentHolding = wantHold;
            byte handOrd = (byte) (triggeredHand == InteractionHand.OFF_HAND ? 1 : 0);
            PacketDistributor.sendToServer(new GreataxeNetwork.HoldPayload(wantHold, handOrd));
            if (!wantHold && isLocalSpinning()) {
                SPIN_STATES.remove(mc.player.getId());
                onLocalWhirlwindStopped(mc);
            }
        }

        if (mc.isPaused()) {
            return;
        }

        Iterator<Int2ObjectMap.Entry<SpinAnimState>> it = SPIN_STATES.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Int2ObjectMap.Entry<SpinAnimState> entry = it.next();
            Entity entity = mc.level.getEntity(entry.getIntKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                it.remove();
                continue;
            }
            SpinAnimState st = entry.getValue();
            if (!living.getItemInHand(st.hand).is(GildedTyrantRegistry.GILDED_GREATAXE.get())) {
                it.remove();
                if (mc.player != null && entry.getIntKey() == mc.player.getId()) {
                    onLocalWhirlwindStopped(mc);
                }
                continue;
            }
            st.spinTicks++;
        }
    }

    private static boolean applyFirstPersonTransform(PoseStack ps, LocalPlayer player, HumanoidArm arm,
                                                     float partialTick, float equipProcess, float swingProcess) {
        float sign = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        SpinAnimState st = SPIN_STATES.get(player.getId());
        if (st != null && st.active) {
            HumanoidArm activeArm = resolveArm(player, st.hand);
            if (activeArm != arm) {
                ps.translate(sign * 0.65F, -1.45F, -0.60F);
                return true;
            }

            float period = GildedGreataxeItem.SPIN_PERIOD_TICKS;
            float phase = ((st.spinTicks + partialTick) % period) / period;
            float yawDeg = sign * (-110.0F + phase * 360.0F);
            if (st.spinTicks != st.lastLoggedFpTick && st.spinTicks % 20 == 0) {
                st.lastLoggedFpTick = st.spinTicks;
                LOGGER.debug("[GildedGreataxe] First-person orbital whirlwind render active: tick={}, phase={}, yawDeg={}", st.spinTicks, phase, yawDeg);
            }

            // 围绕玩家胸口中轴执行真实 360° 水平轨道回旋：握把始终朝向胸口内侧，月牙主刃在前破风横斩
            ps.translate(0.0F, -0.32F, 0.06F);
            ps.mulPose(Axis.YP.rotationDegrees(yawDeg));
            ps.translate(0.0F, 0.0F, -0.50F);
            ps.mulPose(Axis.XP.rotationDegrees(-84.0F));
            ps.mulPose(Axis.YP.rotationDegrees(sign * 78.0F));
            return true;
        }

        float equipDip = equipProcess * -0.55F;
        if (swingProcess <= 0.001F) {
            float time = player.tickCount + partialTick;
            float idleY = Mth.sin(time * 0.09F) * 0.010F;
            float idleX = Mth.cos(time * 0.045F) * 0.006F * sign;
            ps.translate(sign * 0.64F + idleX, -0.54F + equipDip + idleY, -0.88F);
            ps.mulPose(Axis.YP.rotationDegrees(sign * 12.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(-sign * 5.0F));
            ps.mulPose(Axis.XP.rotationDegrees(-28.0F));
            return true;
        }

        // 普通左键三段式重斧斜劈：蓄势上提 -> 斜向重劈经过准星右下方 -> 沉稳回位
        float tx;
        float ty;
        float tz;
        float ry;
        float rz;
        float rx;
        if (swingProcess < 0.18F) {
            float k = swingProcess / 0.18F;
            float ease = k * k * (3.0F - 2.0F * k);
            tx = sign * (0.64F + 0.06F * ease);
            ty = -0.54F + 0.16F * ease + equipDip;
            tz = -0.88F + 0.08F * ease;
            ry = sign * (12.0F + 2.0F * ease);
            rz = sign * (-5.0F - 8.0F * ease);
            rx = -28.0F + 20.0F * ease;
        } else if (swingProcess < 0.56F) {
            float k = (swingProcess - 0.18F) / 0.38F;
            float ease = 1.0F - (1.0F - k) * (1.0F - k) * (1.0F - k);
            tx = sign * (0.70F - 0.56F * ease);
            ty = -0.38F - 0.36F * ease + equipDip;
            tz = -0.80F - 0.22F * Mth.sin(k * (float) Math.PI);
            ry = sign * (14.0F - 20.0F * ease);
            rz = sign * (-13.0F + 38.0F * ease);
            rx = -8.0F - 56.0F * ease;
        } else {
            float k = (swingProcess - 0.56F) / 0.44F;
            float ease = k * k * (3.0F - 2.0F * k);
            tx = sign * (0.14F + 0.50F * ease);
            ty = -0.74F + 0.20F * ease + equipDip;
            tz = -0.80F - 0.08F * ease;
            ry = sign * (-6.0F + 18.0F * ease);
            rz = sign * (25.0F - 30.0F * ease);
            rx = -64.0F + 36.0F * ease;
        }

        ps.translate(tx, ty, tz);
        ps.mulPose(Axis.YP.rotationDegrees(ry));
        ps.mulPose(Axis.ZP.rotationDegrees(rz));
        ps.mulPose(Axis.XP.rotationDegrees(rx));
        return true;
    }

    private static void transformWhirlwindArmPose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ModelPart mainArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart offArm = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        float sign = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        // 双手平端握紧巨斧长柄，配合玩家整体 Y 轴高速回旋形成标准大风车斩姿
        mainArm.xRot = -1.42F;
        mainArm.yRot = -sign * 0.22F;
        mainArm.zRot = sign * 0.10F;

        offArm.xRot = -1.28F;
        offArm.yRot = sign * 0.68F;
        offArm.zRot = 0.0F;
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        SpinAnimState st = SPIN_STATES.get(player.getId());
        pushedPlayerSpinPose = false;
        renderingSpinningPlayer = null;
        if (st == null || !st.active) {
            return;
        }

        float partialTick = event.getPartialTick();
        HumanoidArm activeArm = resolveArm(player, st.hand);
        float spinSign = activeArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float spinDeg = ((st.spinTicks + partialTick) / (float) GildedGreataxeItem.SPIN_PERIOD_TICKS) * 360.0F;

        savedYHeadRot = player.yHeadRot;
        savedYHeadRotO = player.yHeadRotO;
        savedXRot = player.getXRot();
        savedXRotO = player.xRotO;

        // 旋转期间锁定模型头部与躯干同向，避免脖子扭向镜头方向而躯干独自旋转
        player.yHeadRot = player.yBodyRot;
        player.yHeadRotO = player.yBodyRotO;
        player.setXRot(6.0F);
        player.xRotO = 6.0F;

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(spinSign * spinDeg));

        pushedPlayerSpinPose = true;
        renderingSpinningPlayer = player;
        renderingSpinningArm = activeArm;
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!pushedPlayerSpinPose) {
            return;
        }
        Player player = event.getEntity();
        event.getPoseStack().popPose();

        player.yHeadRot = savedYHeadRot;
        player.yHeadRotO = savedYHeadRotO;
        player.setXRot(savedXRot);
        player.xRotO = savedXRotO;

        pushedPlayerSpinPose = false;
        renderingSpinningPlayer = null;
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || SPIN_STATES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean firstPersonSelf = mc.options.getCameraType().isFirstPerson();
        int selfId = mc.player != null ? mc.player.getId() : -1;
        boolean drewAny = false;

        ps.pushPose();
        ps.translate(-camPos.x, -camPos.y, -camPos.z);

        for (Int2ObjectMap.Entry<SpinAnimState> entry : SPIN_STATES.int2ObjectEntrySet()) {
            Entity ent = mc.level.getEntity(entry.getIntKey());
            if (!(ent instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            SpinAnimState st = entry.getValue();
            if (!st.active) {
                continue;
            }
            Vec3 pos = living.getPosition(partialTick);
            HumanoidArm activeArm = resolveArm(living, st.hand);
            float spinSign = activeArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float period = GildedGreataxeItem.SPIN_PERIOD_TICKS;
            float spinDeg = ((st.spinTicks + partialTick) / period) * 360.0F;
            boolean isFirstPersonOwner = firstPersonSelf && entry.getIntKey() == selfId;

            ps.pushPose();
            ps.translate(pos.x, pos.y, pos.z);
            if (isFirstPersonOwner) {
                float phase = ((st.spinTicks + partialTick) % period) / period;
                if (phase >= 0.14F && phase <= 0.48F) {
                    float k = (phase - 0.14F) / 0.34F;
                    float yaw = living.getViewYRot(partialTick);
                    GildedTyrantVfx.drawGildedSlashArc(ps, buffers, yaw, 1.24D, 0.28D,
                            1.10D, 2.35D, 0.0F, 3.0F * spinSign,
                            -68.0F * spinSign, 68.0F * spinSign, k, false);
                    drewAny = true;
                }
            } else {
                GildedTyrantVfx.drawSlashDisc(ps, buffers, 0.98D, 2.75F, spinSign * spinDeg, 0.0F, 0.0F, 0.86F, false);
                GildedTyrantVfx.drawSlashDisc(ps, buffers, 1.02D, 2.10F, spinSign * spinDeg + 180.0F, 3.0F, -3.0F, 0.72F, true);
                GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.05D, 2.35F, -spinSign * spinDeg * 0.45F, 1.0F, 0.88F, 0.34F, 0.46F, 0.50F);
                drewAny = true;
            }
            ps.popPose();
        }

        ps.popPose();

        if (drewAny) {
            buffers.endBatch(RenderType.entityTranslucent(GildedTyrantVfx.SLASH));
            buffers.endBatch(RenderType.entityTranslucent(GildedTyrantVfx.ARC_STRIP));
            buffers.endBatch(RenderType.entityTranslucent(GildedTyrantVfx.RING));
            buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.SLASH));
            buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.ARC_STRIP));
            buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.RING));
        }
    }

    private static final class SpinAnimState {
        boolean active;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        int spinTicks;
        int lastLoggedFpTick = -1;
    }
}
