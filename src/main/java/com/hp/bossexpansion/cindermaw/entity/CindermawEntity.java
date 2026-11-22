package com.hp.bossexpansion.cindermaw.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import com.hp.bossexpansion.api.CricketBoss;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.config.BossExpansionConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import com.hp.bossexpansion.cindermaw.client.CinderFx;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

// 熔渊炉心：撑地行走的玄武岩炉心巨兽，所有技能由服务端时间线驱动。
public final class CindermawEntity extends Monster implements GeoEntity, CricketBoss {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(CindermawEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_START = SynchedEntityData.defineId(CindermawEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PHASE2 = SynchedEntityData.defineId(CindermawEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(CindermawEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RAPID_COMBAT = SynchedEntityData.defineId(CindermawEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cindermaw.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cindermaw.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.cindermaw.run");
    private static final RawAnimation FLICKER = RawAnimation.begin().thenLoop("animation.cindermaw.flicker");
    private static final RawAnimation FLICKER_HOT = RawAnimation.begin().thenLoop("animation.cindermaw.flicker_hot");

    // 骨骼编号：渲染器每帧写入世界坐标，客户端特效从这里取发射点
    public static final int BONE_HAND_RIGHT = 0;
    public static final int BONE_HAND_LEFT = 1;
    public static final int BONE_FURNACE = 2;
    public static final int BONE_VENT = 3;
    public static final int BONE_HEAD = 6;
    public static final int BONE_BODY = 7;
    public final Vec3[] clientBones = new Vec3[8];
    public final List<Vec3[]> clientTrail = new ArrayList<>();
    public int clientFxAge = Integer.MIN_VALUE;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final int[] cooldowns = new int[CindermawAttack.values().length];
    private int attackGap = 40;
    private int attackTick;
    private double attackStepAccumulator;
    private int attackSerial;
    private float lockedYaw;
    private CindermawAttack lastAttack = CindermawAttack.NONE;
    private boolean leapAirborne;
    // 召唤出场倒计时：等客户端收到实体后再触发动画
    private int entranceDelay;
    private Vec3 leapHorizontal = Vec3.ZERO;
    private Vec3 targetLastPos;
    private Vec3 targetVelocity = Vec3.ZERO;
    private int animDebugTick;
    private boolean configAttributesApplied;
    private boolean bypassKillInProgress;
    private int clientLastAttackKey = Integer.MIN_VALUE;
    private float clientAttackAge;

    public CindermawEntity(EntityType<? extends CindermawEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
        setMaxUpStep(1.5F);
    }

    // 基础数值：高血量高护甲，完全免疫击退
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 340.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK, 0);
        entityData.define(ATTACK_START, 0);
        entityData.define(PHASE2, false);
        entityData.define(RUNNING, false);
        entityData.define(RAPID_COMBAT, false);
    }

    // 同步读取：当前技能、技能开始至今的刻数、是否二阶段
    public CindermawAttack getAttack() {
        return CindermawAttack.byId(entityData.get(ATTACK));
    }

    // 计算当前技能时间轴推进倍速：斗蛐蛐无生存玩家时与客户端动画同速加速，起跳升空段保持物理原速
    public float currentActionStep() {
        CindermawAttack atk = getAttack();
        if (atk == CindermawAttack.NONE || isDeadOrDying()) {
            return 1.0F;
        }
        float curAge = level().isClientSide ? clientAttackAge : attackTick;
        if (atk == CindermawAttack.LEAP && curAge >= 12.0F) {
            return 1.0F;
        }
        return isRapidCombat() ? (float) BossExpansionConfig.NO_PLAYER_ACTION_SPEED.get().doubleValue() : 1.0F;
    }

    private void tickClientAttackClock() {
        CindermawAttack atk = getAttack();
        if (atk == CindermawAttack.NONE) {
            clientLastAttackKey = Integer.MIN_VALUE;
            clientAttackAge = 0.0F;
            clientFxAge = Integer.MIN_VALUE;
            return;
        }
        int key = (atk.id << 24) ^ (entityData.get(ATTACK_START) & 0x00FFFFFF);
        if (key != clientLastAttackKey) {
            clientLastAttackKey = key;
            clientAttackAge = 0.0F;
            clientFxAge = -1;
        } else {
            clientAttackAge += currentActionStep();
        }
    }

    public float attackAge(float partial) {
        if (getAttack() == CindermawAttack.NONE) {
            return 0.0F;
        }
        float base = level().isClientSide ? clientAttackAge : attackTick;
        return Math.max(0.0F, base + partial * currentActionStep());
    }

    public boolean isPhase2() {
        return entityData.get(PHASE2);
    }

    public boolean isRapidCombat() {
        return entityData.get(RAPID_COMBAT);
    }

    @Override
    public boolean hasSecondPhaseRebirth() {
        return true;
    }

    @Override
    public boolean isSecondPhaseActive() {
        return isPhase2();
    }

    @Override
    public void triggerPhase2Rebirth() {
        if (isPhase2()) {
            return;
        }
        entityData.set(PHASE2, true);
        this.dead = false;
        this.deathTime = 0;
        super.setHealth(this.getMaxHealth());
        List<MobEffect> harmful = new ArrayList<>();
        for (MobEffectInstance effectInstance : this.getActiveEffects()) {
            if (effectInstance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(effectInstance.getEffect());
            }
        }
        for (MobEffect effect : harmful) {
            this.removeEffect(effect);
        }
        bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        bossEvent.setProgress(1.0F);
        leapAirborne = false;
        setDeltaMovement(Vec3.ZERO);
        startAttack(CindermawAttack.ROAR);
    }

    // 追击、索敌与闲逛
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new ChaseGoal());
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 20.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, target -> CricketBoss.isValidCricketTarget(this, target)));
    }

    // 实体朝向换算：右、上、前三个分量转为世界坐标
    public Vec3 local(double right, double up, double forward) {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        double rx = -Mth.cos(yaw);
        double rz = -Mth.sin(yaw);
        return position().add(rx * right + fx * forward, up, rz * right + fz * forward);
    }

    public Vec3 forward() {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    public Vec3 right() {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
    }

    // 三个火山口在静止姿态下的位置（由模型实测）
    public Vec3 ventTop(int vent) {
        if (vent == 0) {
            return local(-0.75, 2.53, -0.07);
        }
        if (vent == 1) {
            return local(0, 2.9, 0.11);
        }
        return local(0.75, 2.53, -0.07);
    }

    // 可伤害判定：排除自身、同类、队友、创造与旁观玩家
    public boolean canHarm(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity == this || entity instanceof CindermawEntity) {
            return false;
        }
        if (isAlliedTo(entity)) {
            return false;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return true;
    }

    // 范围技能不穿墙：从发射点到受害者中心做方块射线
    public boolean clearPath(Vec3 from, Entity victim) {
        Vec3 to = victim.getBoundingBox().getCenter();
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    public float attackDamage() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    // 朝指定方向推开受害者并附加上抛
    public static void shove(LivingEntity victim, Vec3 direction, double strength, double lift) {
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(0, 0, 1);
        }
        flat = flat.normalize();
        victim.knockback(strength, -flat.x, -flat.z);
        victim.setDeltaMovement(victim.getDeltaMovement().add(0, lift, 0));
        victim.hurtMarked = true;
    }

    @Override
    public void tick() {
        super.tick();
        // 客户端推进技能帧时钟与粒子特效
        if (level().isClientSide) {
            tickClientAttackClock();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CinderFx.bossTick(this));
            return;
        }
        applyConfigAttributesOnce();
        if (this.tickCount % 10 == 0) {
            boolean rapid = BossExpansionConfig.CRICKET_MODE_ENABLED.get() && !CricketBoss.hasNearbySurvivalPlayer(this);
            entityData.set(RAPID_COMBAT, rapid);
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!isAlive()) {
            return;
        }
        int cdStep = isRapidCombat() ? 2 : 1;
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) {
                cooldowns[i] = Math.max(0, cooldowns[i] - cdStep);
            }
        }
        if (attackGap > 0) {
            attackGap = Math.max(0, attackGap - cdStep);
        }
        trackTarget();
        if (entranceDelay > 0) {
            entranceDelay--;
            if (entranceDelay == 0) {
                startAttack(CindermawAttack.LAND);
                MagmaEffect.shockwave(this, position(), 7.0F, 0);
                playSound(SoundEvents.RAVAGER_ROAR, 4.0F, 0.6F);
                playSound(SoundEvents.GENERIC_EXPLODE, 3.0F, 0.6F);
                attackGap = 30;
            }
            return;
        }
        CindermawAttack attack = getAttack();
        if (attack == CindermawAttack.NONE) {
            chooseAttack();
        } else {
            advanceServerAttack(attack);
        }
    }

    // 记录目标移动速度，供熔岩弹预判落点
    private void trackTarget() {
        LivingEntity target = getTarget();
        if (target == null) {
            targetLastPos = null;
            targetVelocity = Vec3.ZERO;
            return;
        }
        if (targetLastPos != null) {
            targetVelocity = targetVelocity.scale(0.6).add(target.position().subtract(targetLastPos).scale(0.4));
        }
        targetLastPos = target.position();
    }

    // 按距离、视线、冷却加权随机选技能，刚用过的技能权重降低
    private void chooseAttack() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || attackGap > 0) {
            return;
        }
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double distance = Math.sqrt(dx * dx + dz * dz) - getBbWidth() * 0.5;
        double height = Math.abs(target.getY() - getY());
        boolean sight = hasLineOfSight(target);
        List<CindermawAttack> picks = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (CindermawAttack attack : CindermawAttack.values()) {
            if (attack.weight <= 0 || cooldowns[attack.ordinal()] > 0) {
                continue;
            }
            if (attack.phase2Only && !isPhase2()) {
                continue;
            }
            if (distance < attack.minRange || distance > attack.maxRange) {
                continue;
            }
            if (!sight && attack != CindermawAttack.NOVA) {
                continue;
            }
            if (attack == CindermawAttack.LEAP && (!onGround() || height > 5)) {
                continue;
            }
            if ((attack == CindermawAttack.SLAM || attack == CindermawAttack.SWEEP) && height > 3) {
                continue;
            }
            int weight = attack.weight;
            if (attack == lastAttack) {
                weight = Math.max(1, weight / 3);
            }
            picks.add(attack);
            weights.add(weight);
            total += weight;
        }
        if (picks.isEmpty()) {
            return;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < picks.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                startAttack(picks.get(i));
                return;
            }
        }
    }

    // 开始技能：同步编号与序号，起手先快速对齐索敌目标并锁定初始朝向，随后触发动画与音效
    private void startAttack(CindermawAttack attack) {
        this.attackTick = 0;
        this.attackStepAccumulator = 0.0D;
        this.attackSerial = (this.attackSerial + 1) & 0x00FFFFFF;
        entityData.set(ATTACK, attack.id);
        entityData.set(ATTACK_START, this.attackSerial);
        getNavigation().stop();
        entityData.set(RUNNING, false);

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && attack != CindermawAttack.ROAR && attack != CindermawAttack.LAND) {
            float prevBodyYaw = this.yBodyRot;
            face(target, 55.0F, true);
            float wantYaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
            LOGGER.debug("Cindermaw [id={}] startAttack {}: bodyYaw {} -> {} (want={}, dist={})",
                    getId(), attack.name(), String.format("%.1f", prevBodyYaw), String.format("%.1f", this.yBodyRot),
                    String.format("%.1f", Mth.wrapDegrees(wantYaw)), String.format("%.2f", distanceTo(target)));
        } else {
            this.lockedYaw = this.yBodyRot;
            applyLockedYaw();
        }

        if (!attack.anim.isEmpty()) {
            triggerAnim("main", attack.anim);
        }
        if (attack.weight > 0) {
            lastAttack = attack;
        }
        leapAirborne = false;
        if (attack == CindermawAttack.ROAR) {
            playSound(SoundEvents.RAVAGER_ROAR, 4.0F, 0.55F);
        } else if (attack == CindermawAttack.BREATH) {
            playSound(SoundEvents.BLAZE_AMBIENT, 2.5F, 0.5F);
        } else if (attack == CindermawAttack.NOVA) {
            playSound(SoundEvents.RAVAGER_ROAR, 3.0F, 0.8F);
        } else if (attack == CindermawAttack.SLAM || attack == CindermawAttack.LEAP) {
            playSound(SoundEvents.RAVAGER_ATTACK, 2.0F, 0.6F);
        }
    }

    // 结束技能：写入冷却，二阶段冷却与间隔更短
    private void endAttack(CindermawAttack attack) {
        entityData.set(ATTACK, 0);
        this.attackTick = 0;
        this.attackStepAccumulator = 0.0D;
        int cooldown = attack.cooldown;
        int gap = 18;
        if (isPhase2()) {
            cooldown = (int) (cooldown * 0.65F);
            gap = 9;
        }
        if (isRapidCombat()) {
            double scale = BossExpansionConfig.NO_PLAYER_COOLDOWN_SCALE.get();
            cooldown = Math.max(4, (int) Math.round(cooldown * scale));
            gap = Math.max(3, (int) Math.round(gap * scale));
        }
        if (cooldown > 0) {
            cooldowns[attack.ordinal()] = cooldown;
        }
        attackGap = gap;
    }

    // 按当前动作速率推进服务端技能帧，确保斗蛐蛐加速时服务端判定帧与客户端动画帧严格对齐
    private void advanceServerAttack(CindermawAttack initialAttack) {
        if (initialAttack == CindermawAttack.LEAP && this.attackTick >= 12) {
            tickAttack(initialAttack, this.attackTick);
            if (getAttack() == initialAttack) {
                this.attackTick++;
            }
            return;
        }
        this.attackStepAccumulator += currentActionStep();
        int steps = (int) Math.floor(this.attackStepAccumulator);
        this.attackStepAccumulator -= steps;
        for (int s = 0; s < steps; s++) {
            CindermawAttack current = getAttack();
            if (current != initialAttack) {
                break;
            }
            tickAttack(current, this.attackTick);
            if (getAttack() != current) {
                break;
            }
            this.attackTick++;
        }
    }

    // 每帧推进技能时间线，蓄力后锁死帧显式维持 lockedYaw 防止贴身碰撞挤压把躯干挤歪
    private void tickAttack(CindermawAttack attack, int age) {
        getNavigation().stop();
        LivingEntity target = getTarget();
        boolean valid = target != null && target.isAlive();
        switch (attack) {
            case SLAM -> {
                if (age < 13 && valid) {
                    face(target, 24.0F, true);
                } else {
                    applyLockedYaw();
                }
                if (age == 16) {
                    slamImpact();
                }
            }
            case SWEEP -> {
                if (age < 9 && valid) {
                    face(target, 26.0F, true);
                } else if (age >= 13 && age < 22 && valid) {
                    face(target, 24.0F, true);
                } else {
                    applyLockedYaw();
                }
                if (age == 11) {
                    sweepHit(1);
                }
                if (age == 24) {
                    sweepHit(-1);
                }
            }
            case BREATH -> {
                if (age < 14 && valid) {
                    face(target, 24.0F, true);
                } else if (age <= 50 && valid) {
                    face(target, 4.0F, false);
                } else {
                    applyLockedYaw();
                }
                if (age >= 14 && age <= 50 && (age - 14) % 4 == 0) {
                    breathHit();
                }
                if (age >= 14 && age <= 50 && age % 6 == 0) {
                    playSound(SoundEvents.BLAZE_SHOOT, 1.8F, 0.55F + random.nextFloat() * 0.2F);
                }
            }
            case BARRAGE -> {
                if (valid) {
                    face(target, 16.0F, true);
                } else {
                    applyLockedYaw();
                }
                if (valid && (age == 12 || age == 24 || age == 36)) {
                    launchVolley(target, age / 12 - 1);
                }
            }
            case LEAP -> {
                tickLeap(target, valid, age);
                return;
            }
            case NOVA -> {
                if (age < 14 && valid) {
                    face(target, 18.0F, true);
                } else {
                    applyLockedYaw();
                }
                if (age == 22) {
                    novaRelease();
                }
            }
            case ROAR -> {
                applyLockedYaw();
                if (age == 26) {
                    roarBurst();
                }
            }
            default -> applyLockedYaw();
        }
        if (age >= attack.duration) {
            endAttack(attack);
        }
    }

    // 由召唤物生成时调用，延迟两刻播放出场
    public void beginEntrance() {
        this.lockedYaw = this.yBodyRot;
        entranceDelay = 3;
    }

    // 控制头身最大偏角，避免原地盯人时头部歪斜过大而躯干滞后
    @Override
    public int getMaxHeadYRot() {
        return 32;
    }

    // 维持锁定朝向，防止贴身实体碰撞挤压触发原版 BodyRotationControl 篡改 yBodyRot
    private void applyLockedYaw() {
        setYRot(this.lockedYaw);
        setYBodyRot(this.lockedYaw);
        setYHeadRot(this.lockedYaw);
    }

    private void face(LivingEntity target, float maxStep) {
        face(target, maxStep, true);
    }

    // 以躯干当前朝向为基准限速转向目标，近身 5 格内自动放大步幅以跟上贴脸绕行角速度
    private void face(LivingEntity target, float maxStep, boolean boostCloseRange) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq < 1.0E-6D) {
            applyLockedYaw();
            return;
        }
        float effectiveStep = maxStep;
        if (boostCloseRange && distSq < 25.0D) {
            effectiveStep = Math.max(maxStep * 1.45F, 28.0F);
        }
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(this.yBodyRot, want, effectiveStep);
        this.lockedYaw = yaw;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    // 双拳砸地：前方圆形重击，生成冲击环与扇形熔岩裂缝
    private void slamImpact() {
        Vec3 center = groundAt(local(0, 0, 1.8));
        float damage = attackDamage() * 1.15F;
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(3.4, 2.5, 3.4))) {
            Vec3 offset = victim.position().subtract(center);
            if (!canHarm(victim) || offset.horizontalDistance() > 3.2 + victim.getBbWidth() * 0.5 || !clearPath(center.add(0, 0.5, 0), victim)) {
                continue;
            }
            if (victim.hurt(damageSources().mobAttack(this), damage)) {
                shove(victim, victim.position().subtract(position()), 0.9, 0.55);
            }
        }
        MagmaEffect.shockwave(this, center, 4.5F, 0);
        int lines = 5;
        float length = 8.0F;
        if (isPhase2()) {
            lines = 7;
            length = 10.0F;
        }
        MagmaEffect.fissure(this, center, yBodyRot, lines, length);
        if (getTarget() != null) {
            LivingEntity t = getTarget();
            float wantYaw = (float) (Mth.atan2(t.getZ() - getZ(), t.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
            LOGGER.debug("Cindermaw [id={}] slamImpact at bodyYaw={} (targetWantYaw={}, errDeg={})",
                    getId(), String.format("%.1f", yBodyRot), String.format("%.1f", Mth.wrapDegrees(wantYaw)),
                    String.format("%.1f", Math.abs(Mth.degreesDifference(yBodyRot, wantYaw))));
        }
        playSound(SoundEvents.GENERIC_EXPLODE, 2.2F, 0.55F);
        playSound(SoundEvents.ANVIL_LAND, 1.4F, 0.5F);
    }

    // 横扫：前方 210 度扇形，按挥击方向侧向击飞并点燃
    private void sweepHit(int side) {
        Vec3 forward = forward();
        Vec3 push = right().scale(side).add(forward.scale(0.35));
        Vec3 chest = local(0, 1.2, 0.8);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5, 2.0, 4.5))) {
            Vec3 offset = victim.position().subtract(position());
            double flat = offset.horizontalDistance();
            if (!canHarm(victim) || flat > 5.4 + victim.getBbWidth() * 0.5 || offset.y < -1.5 || offset.y > 3.5) {
                continue;
            }
            if (flat > 0.5 && new Vec3(offset.x, 0, offset.z).normalize().dot(forward) < -0.26) {
                continue;
            }
            if (!clearPath(chest, victim)) {
                continue;
            }
            if (victim.hurt(damageSources().mobAttack(this), attackDamage() * 0.9F)) {
                shove(victim, push, 1.3, 0.35);
                victim.setSecondsOnFire(3);
            }
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
        playSound(SoundEvents.FIRECHARGE_USE, 1.5F, 0.6F);
    }

    // 炉心喷焰：固定下倾 7 度的锥形，半角 26 度，射程 9 格
    private void breathHit() {
        Vec3 origin = local(0, 1.35, 0.9);
        double pitch = Math.toRadians(-7);
        Vec3 forward = forward();
        Vec3 direction = new Vec3(forward.x * Math.cos(pitch), Math.sin(pitch), forward.z * Math.cos(pitch));
        double cone = Math.cos(Math.toRadians(26));
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(9.5))) {
            Vec3 to = victim.getBoundingBox().getCenter().subtract(origin);
            double distance = to.length();
            if (!canHarm(victim) || distance > 9.5 || distance < 0.01 || to.normalize().dot(direction) < cone || !clearPath(origin, victim)) {
                continue;
            }
            victim.invulnerableTime = 0;
            if (victim.hurt(damageSources().mobAttack(this), attackDamage() * 0.3F)) {
                victim.setSecondsOnFire(5);
                victim.setDeltaMovement(victim.getDeltaMovement().add(direction.scale(0.12)));
                victim.hurtMarked = true;
            }
        }
    }

    // 火山齐射：首发打预判点，其余散布在周围，按发射顺序轮换火山口
    private void launchVolley(LivingEntity target, int wave) {
        Vec3 predicted = target.position().add(targetVelocity.x * 16, 0, targetVelocity.z * 16);
        int count = 3;
        if (isPhase2()) {
            count = 4;
        }
        for (int i = 0; i < count; i++) {
            Vec3 aim = predicted;
            if (i > 0) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 1.8 + random.nextDouble() * 2.4;
                aim = predicted.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            }
            var ground = level().clip(new ClipContext(aim.add(0, 4, 0), aim.add(0, -10, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
            if (ground.getType() == HitResult.Type.MISS) {
                continue;
            }
            LavaBomb bomb = CindermawRegistry.LAVA_BOMB.get().create(level());
            if (bomb == null) {
                continue;
            }
            bomb.launch(this, ventTop((i + wave) % 3), ground.getLocation().add(0, 0.02, 0), 26 + i * 3 + random.nextInt(4));
            level().addFreshEntity(bomb);
        }
        playSound(SoundEvents.GENERIC_EXPLODE, 1.6F, 1.3F);
        playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
    }

    // 陨落重踏：蓄力后一次性起跳，空中保持水平速度，真正落地才结算
    private void tickLeap(LivingEntity target, boolean valid, int age) {
        if (age < 12 && valid) {
            face(target, 28.0F, true);
        } else {
            applyLockedYaw();
        }
        if (age == 12) {
            if (!valid) {
                endAttack(CindermawAttack.LEAP);
                return;
            }
            Vec3 offset = target.position().subtract(position());
            leapHorizontal = new Vec3(offset.x / 22.0, 0, offset.z / 22.0);
            setDeltaMovement(leapHorizontal.x, 1.0 + Mth.clamp(offset.y / 20.0, -0.1, 0.25), leapHorizontal.z);
            hasImpulse = true;
            playSound(SoundEvents.GENERIC_EXPLODE, 1.4F, 0.8F);
        }
        if (age > 12) {
            if (!onGround()) {
                leapAirborne = true;
                if (horizontalCollision) {
                    leapHorizontal = Vec3.ZERO;
                }
                setDeltaMovement(leapHorizontal.x, getDeltaMovement().y, leapHorizontal.z);
            } else if (leapAirborne) {
                landImpact();
                endAttack(CindermawAttack.LEAP);
                startAttack(CindermawAttack.LAND);
                return;
            }
        }
        if (age > CindermawAttack.LEAP.duration) {
            endAttack(CindermawAttack.LEAP);
        }
    }

    // 落地：近处伤害最高的冲击，随后一圈（二阶段两圈）熔岩喷泉
    private void landImpact() {
        Vec3 center = position();
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.5, 2.5, 5.5))) {
            Vec3 offset = victim.position().subtract(center);
            double distance = offset.horizontalDistance();
            if (!canHarm(victim) || distance > 6.0 || !clearPath(center.add(0, 0.6, 0), victim)) {
                continue;
            }
            float scale = (float) (1.4 - 0.6 * distance / 6.0);
            if (victim.hurt(damageSources().mobAttack(this), attackDamage() * scale)) {
                shove(victim, offset, 1.2, 0.6);
            }
        }
        MagmaEffect.shockwave(this, center, 6.5F, 0);
        MagmaEffect.geyserRing(this, center, 4.0F, 10, 8);
        if (isPhase2()) {
            MagmaEffect.geyserRing(this, center, 6.8F, 14, 18);
        }
        playSound(SoundEvents.GENERIC_EXPLODE, 3.0F, 0.45F);
        playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.4F);
    }

    // 熔核新星：贴地扩散的火墙
    private void novaRelease() {
        MagmaEffect.nova(this, position(), 16.0F);
        MagmaEffect.shockwave(this, position(), 3.5F, 1);
        playSound(SoundEvents.GENERIC_EXPLODE, 2.5F, 0.7F);
        playSound(SoundEvents.FIRECHARGE_USE, 2.5F, 0.4F);
    }

    // 咆哮热浪：推开周围目标并进入二阶段
    private void roarBurst() {
        entityData.set(PHASE2, true);
        bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8.0, 3.0, 8.0))) {
            if (!canHarm(victim) || victim.distanceTo(this) > 9.0) {
                continue;
            }
            if (victim.hurt(damageSources().mobAttack(this), attackDamage() * 0.3F)) {
                victim.setSecondsOnFire(4);
            }
            shove(victim, victim.position().subtract(position()), 1.6, 0.5);
        }
        MagmaEffect.shockwave(this, position(), 8.0F, 1);
        playSound(SoundEvents.GENERIC_EXPLODE, 3.0F, 0.6F);
    }

    // 地面吸附：从上往下找支撑面，找不到就用原高度
    public Vec3 groundAt(Vec3 point) {
        var hit = level().clip(new ClipContext(point.add(0, 2.5, 0), point.add(0, -4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) {
            return point;
        }
        return hit.getLocation();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.bypassKillInProgress = true;
            try {
                return super.hurt(source, amount);
            } finally {
                this.bypassKillInProgress = false;
            }
        }
        if (getAttack() == CindermawAttack.ROAR) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void setHealth(float health) {
        if (!level().isClientSide
                && this.configAttributesApplied
                && !isPhase2()
                && !this.bypassKillInProgress
                && !this.isRemoved()
                && health <= 0.0F) {
            triggerPhase2Rebirth();
            return;
        }
        super.setHealth(health);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // 死亡：触发倒地动画，演出持续三秒后移除
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            entityData.set(ATTACK, 0);
            triggerAnim("main", "death");
            playSound(SoundEvents.RAVAGER_DEATH, 3.0F, 0.5F);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= 60 && !level().isClientSide && !isRemoved()) {
            playSound(SoundEvents.GENERIC_EXPLODE, 3.0F, 0.7F);
            remove(RemovalReason.KILLED);
        }
    }

    // 声音：低沉的岩石与熔岩
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.LAVA_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + random.nextFloat() * 0.1F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 1.0F, 0.7F);
    }

    // 血条随追踪范围增删，名称跟随命名牌
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    private void applyConfigAttributesOnce() {
        if (this.configAttributesApplied) {
            return;
        }
        setBaseAttribute(Attributes.MAX_HEALTH, BossExpansionConfig.CINDERMAW_MAX_HEALTH.get());
        setBaseAttribute(Attributes.ATTACK_DAMAGE, BossExpansionConfig.CINDERMAW_ATTACK_DAMAGE.get());
        setBaseAttribute(Attributes.ARMOR, BossExpansionConfig.CINDERMAW_ARMOR.get());
        setBaseAttribute(Attributes.MOVEMENT_SPEED, BossExpansionConfig.CINDERMAW_MOVEMENT_SPEED.get());
        setBaseAttribute(Attributes.FOLLOW_RANGE, BossExpansionConfig.CINDERMAW_FOLLOW_RANGE.get());
        this.setHealth(this.getMaxHealth());
        this.configAttributesApplied = true;
    }

    private void setBaseAttribute(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Phase2", isPhase2());
        tag.putBoolean("ConfigAttributesApplied", this.configAttributesApplied);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE2, tag.getBoolean("Phase2"));
        this.configAttributesApplied = tag.getBoolean("ConfigAttributesApplied");
        if (isPhase2()) {
            bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        }
        bossEvent.setName(getDisplayName());
    }

    // 主控制器：行走/待机循环 + 全部技能触发动画；火焰控制器只驱动火山火焰骨骼
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::mainState)
                .setAnimationSpeedHandler(CindermawEntity::animationSpeed)
                .triggerableAnim("slam", RawAnimation.begin().thenPlay("animation.cindermaw.slam"))
                .triggerableAnim("sweep", RawAnimation.begin().thenPlay("animation.cindermaw.sweep"))
                .triggerableAnim("breath", RawAnimation.begin().thenPlay("animation.cindermaw.breath"))
                .triggerableAnim("barrage", RawAnimation.begin().thenPlay("animation.cindermaw.barrage"))
                .triggerableAnim("leap", RawAnimation.begin().thenPlay("animation.cindermaw.leap").thenLoop("animation.cindermaw.leap_air"))
                .triggerableAnim("leap_land", RawAnimation.begin().thenPlay("animation.cindermaw.leap_land"))
                .triggerableAnim("nova", RawAnimation.begin().thenPlay("animation.cindermaw.nova"))
                .triggerableAnim("roar", RawAnimation.begin().thenPlay("animation.cindermaw.roar"))
                .triggerableAnim("death", RawAnimation.begin().thenPlayAndHold("animation.cindermaw.death")));
        controllers.add(new AnimationController<>(this, "flames", 0, this::flameState));
    }

    private PlayState mainState(AnimationState<CindermawEntity> state) {
        if (isDeadOrDying() || getAttack() != CindermawAttack.NONE) {
            return PlayState.CONTINUE;
        }
        if (state.isMoving() && onGround()) {
            return state.setAndContinue(entityData.get(RUNNING) ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    private PlayState flameState(AnimationState<CindermawEntity> state) {
        if (isPhase2()) {
            return state.setAndContinue(FLICKER_HOT);
        }
        return state.setAndContinue(FLICKER);
    }

    // 行走/奔跑倍速跟随实际移动速度，基准为各自动画原速下手掌撑地的位移速度（格/刻）；技能动画始终原速
    private static double animationSpeed(CindermawEntity entity) {
        if (entity.isDeadOrDying()) {
            return 1.0D;
        }
        if (entity.getAttack() != CindermawAttack.NONE) {
            return entity.currentActionStep();
        }
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        double speed = Math.sqrt(dx * dx + dz * dz);
        if (speed < 0.01) {
            return 1.0D;
        }
        boolean running = entity.entityData.get(RUNNING);
        double multiplier = running ? Mth.clamp(speed / 0.18, 0.75, 1.3) : Mth.clamp(speed / 0.085, 0.6, 1.5);
        if (entity.tickCount - entity.animDebugTick >= 40) {
            entity.animDebugTick = entity.tickCount;
            LOGGER.debug("Cindermaw move anim: running={}, speed={} blocks/tick, multiplier={}", running, String.format("%.3f", speed), String.format("%.2f", multiplier));
        }
        return multiplier;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // 追击：地形允许时直线冲向目标，否则走寻路；进入近战距离后原地等待技能
    private final class ChaseGoal extends Goal {
        private static final double CHASE_SPEED = 1.25D;
        private int repath;
        private int directCheck;
        private boolean direct;

        ChaseGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return (target != null && target.isAlive()) || getAttack() != CindermawAttack.NONE || entranceDelay > 0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        // 直接设置移动目标点时必须每刻刷新，否则移动控制会隔一刻停一刻
        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            repath = 0;
            directCheck = 0;
            direct = false;
        }

        @Override
        public void tick() {
            // 技能释放与出场期间继续占用 MOVE 与 LOOK 锁，屏蔽闲逛与随机转头 AI，朝向由技能时间线接管
            if (getAttack() != CindermawAttack.NONE || entranceDelay > 0) {
                getNavigation().stop();
                entityData.set(RUNNING, false);
                return;
            }
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                getNavigation().stop();
                entityData.set(RUNNING, false);
                return;
            }
            boolean sight = getSensing().hasLineOfSight(target);
            double reach = getBbWidth() * 0.5 + 2.6;
            double distSq = distanceToSqr(target);
            if (distSq <= reach * reach && sight) {
                getNavigation().stop();
                entityData.set(RUNNING, false);
                // 贴身停步等待技能间隔时同步旋转躯干与头部，彻底消除只歪头不转身
                face(target, 28.0F, true);
                return;
            }
            getLookControl().setLookAt(target, 25.0F, 25.0F);
            entityData.set(RUNNING, true);
            directCheck--;
            if (directCheck <= 0) {
                boolean now = sight && canRunStraight(target);
                if (now != direct) {
                    LOGGER.debug("Cindermaw chase mode: {}", now ? "direct" : "pathfinding");
                }
                direct = now;
                directCheck = 5;
            }
            if (direct) {
                getNavigation().stop();
                getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), CHASE_SPEED);
                if (distSq <= 49.0D) {
                    face(target, 24.0F, true);
                } else {
                    lockedYaw = yBodyRot;
                }
                repath = 0;
                return;
            }
            lockedYaw = yBodyRot;
            repath--;
            if (repath <= 0) {
                getNavigation().moveTo(target, CHASE_SPEED);
                repath = 16;
            }
        }

        // 沿目标方向逐格平移碰撞箱：有高于台阶的墙或脚下 3 格内无支撑就判定不能直冲，最多检查 16 格
        private boolean canRunStraight(LivingEntity target) {
            if (Math.abs(target.getY() - getY()) > 2.0) {
                return false;
            }
            Vec3 flat = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
            double length = Math.min(flat.length(), 16.0);
            if (length < 1.0E-3) {
                return true;
            }
            Vec3 step = flat.normalize();
            AABB box = getBoundingBox();
            double lift = getStepHeight() + 0.1;
            for (double d = 1.0; d <= length; d += 1.0) {
                AABB moved = box.move(step.x * d, 0, step.z * d);
                if (!level().noCollision(CindermawEntity.this, moved.move(0, lift, 0))) {
                    return false;
                }
                AABB below = new AABB(moved.minX, moved.minY - 3.0, moved.minZ, moved.maxX, moved.minY, moved.maxZ);
                if (level().noCollision(CindermawEntity.this, below)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void stop() {
            getNavigation().stop();
            entityData.set(RUNNING, false);
        }
    }
}
