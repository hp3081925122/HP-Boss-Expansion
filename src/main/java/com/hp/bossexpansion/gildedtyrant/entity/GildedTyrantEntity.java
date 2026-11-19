package com.hp.bossexpansion.gildedtyrant.entity;

import com.hp.bossexpansion.api.CricketBoss;
import com.hp.bossexpansion.config.BossExpansionConfig;
import com.hp.bossexpansion.event.BossCombatEvents;
import com.hp.bossexpansion.gildedtyrant.config.GildedTyrantConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class GildedTyrantEntity extends Monster implements GeoEntity, CricketBoss {
    public static final String BAR_KEY = "bossbar.hp_boss_expansion.gilded_tyrant";
    public static final double MOLTEN_RAY_RANGE = 26.0D;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_CONFIG_APPLIED = "ConfigAttributesApplied";
    private static final String NBT_PHASE2 = "Phase2Triggered";

    private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_SERIAL = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE2 = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RAPID_COMBAT = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AXE_THROWN = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BEAM_MODE = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_BEAM_TARGET = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_SHOCK_RING_ORIGIN = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_SHOCK_RING_RADIUS = SynchedEntityData.defineId(GildedTyrantEntity.class, EntityDataSerializers.FLOAT);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.gilded_tyrant.idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.gilded_tyrant.walk");
    private static final RawAnimation ANIM_RUN = RawAnimation.begin().thenLoop("animation.gilded_tyrant.run");
    private static final RawAnimation[] ACTION_ANIMS = new RawAnimation[GildedTyrantAttack.values().length];

    static {
        for (GildedTyrantAttack attack : GildedTyrantAttack.values()) {
            if (attack.animation != null) {
                ACTION_ANIMS[attack.id] = RawAnimation.begin().thenPlay(attack.animation);
            }
        }
    }

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable(BAR_KEY, getDisplayName()),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10
    );
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final int[] skillCooldowns = new int[GildedTyrantAttack.values().length];

    private boolean configAttributesApplied;
    private boolean phase2Triggered;
    private boolean bypassKillInProgress;
    private int attackTick;
    private double attackStepAccumulator;
    private int globalAttackDelay = 24;
    private GildedTyrantAttack lastSkill = GildedTyrantAttack.NONE;
    private float lockedYaw;

    private Vec3 rushDir = Vec3.ZERO;
    private boolean rushHalted;
    private final Set<Integer> rushHitIds = new HashSet<>();

    private Vec3 leapDest = Vec3.ZERO;
    private boolean leapLanded;

    private int chainHookedId = -1;
    private Vec3 rayDir = new Vec3(0.0D, 0.0D, 1.0D);

    private final Set<UUID> activeObelisks = new HashSet<>();
    private boolean obeliskBatchActive;

    private final List<DelayedSpike> pendingSpikes = new ArrayList<>();
    private final List<FissureLine> activeFissures = new ArrayList<>();
    private final List<ExpandingShockRing> activeRings = new ArrayList<>();
    private final List<FallingAxeStrike> pendingAxeDrops = new ArrayList<>();

    private int clientVfxSerial = -1;
    private float clientAttackAge;

    private static final class DelayedSpike {
        final Vec3 pos;
        int delay;
        final double mult;
        final float radius;

        DelayedSpike(Vec3 pos, int delay, double mult, float radius) {
            this.pos = pos;
            this.delay = delay;
            this.mult = mult;
            this.radius = radius;
        }
    }

    private static final class FissureLine {
        final Vec3 start;
        final Vec3 dir;
        final int maxSteps;
        final double stepDist;
        final double mult;
        int currentStep;
        int tickCounter;
        final Set<Integer> hitSet = new HashSet<>();

        FissureLine(Vec3 start, Vec3 dir, int maxSteps, double stepDist, double mult) {
            this.start = start;
            this.dir = dir;
            this.maxSteps = maxSteps;
            this.stepDist = stepDist;
            this.mult = mult;
        }
    }

    private static final class ExpandingShockRing {
        final Vec3 center;
        double radius;
        final double maxRadius;
        final double speed;
        final double mult;
        final Set<Integer> hitSet = new HashSet<>();

        ExpandingShockRing(Vec3 center, double startRadius, double maxRadius, double speed, double mult) {
            this.center = center;
            this.radius = startRadius;
            this.maxRadius = maxRadius;
            this.speed = speed;
            this.mult = mult;
        }
    }

    private static final class FallingAxeStrike {
        final Vec3 pos;
        int timer;

        FallingAxeStrike(Vec3 pos, int timer) {
            this.pos = pos;
            this.timer = timer;
        }
    }

    public GildedTyrantEntity(EntityType<? extends GildedTyrantEntity> type, Level level) {
        super(type, level);
        this.xpReward = 380;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 360.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 44.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.90D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK, 0);
        builder.define(DATA_ATTACK_SERIAL, 0);
        builder.define(DATA_PHASE2, false);
        builder.define(DATA_RAPID_COMBAT, false);
        builder.define(DATA_AXE_THROWN, false);
        builder.define(DATA_BEAM_MODE, 0);
        builder.define(DATA_BEAM_TARGET, new Vector3f());
        builder.define(DATA_SHOCK_RING_ORIGIN, new Vector3f());
        builder.define(DATA_SHOCK_RING_RADIUS, 0.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TyrantCombatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                candidate -> CricketBoss.isValidCricketTarget(this, candidate)));
    }

    public GildedTyrantAttack getAttack() {
        return GildedTyrantAttack.byId(this.entityData.get(DATA_ATTACK));
    }

    public boolean isPhase2() {
        return this.entityData.get(DATA_PHASE2);
    }

    public boolean isRapidCombat() {
        return this.entityData.get(DATA_RAPID_COMBAT);
    }

    public boolean isAxeThrown() {
        return this.entityData.get(DATA_AXE_THROWN);
    }

    public void setAxeThrown(boolean thrown) {
        this.entityData.set(DATA_AXE_THROWN, thrown);
    }

    public int getBeamMode() {
        return this.entityData.get(DATA_BEAM_MODE);
    }

    public Vec3 getBeamTarget() {
        Vector3f v = this.entityData.get(DATA_BEAM_TARGET);
        return new Vec3(v.x, v.y, v.z);
    }

    public Vec3 getShockRingOrigin() {
        Vector3f v = this.entityData.get(DATA_SHOCK_RING_ORIGIN);
        return new Vec3(v.x, v.y, v.z);
    }

    public float getShockRingRadius() {
        return this.entityData.get(DATA_SHOCK_RING_RADIUS);
    }

    public float getActionSpeedMultiplier() {
        GildedTyrantAttack atk = getAttack();
        if (atk == GildedTyrantAttack.GREED_CORE_OVERDRIVE || atk == GildedTyrantAttack.STAGGER) {
            return 1.0F;
        }
        return isRapidCombat() ? (float) BossExpansionConfig.NO_PLAYER_ACTION_SPEED.get().doubleValue() : 1.0F;
    }

    public float getClientAttackAge(float partialTick) {
        if (getAttack() == GildedTyrantAttack.NONE) {
            return 0.0F;
        }
        return this.clientAttackAge + partialTick * getActionSpeedMultiplier();
    }

    @Override
    public boolean hasSecondPhaseRebirth() {
        return GildedTyrantConfig.TWO_LIFE_REBIRTH.get();
    }

    @Override
    public boolean isSecondPhaseActive() {
        return isPhase2();
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
        if (getAttack() == GildedTyrantAttack.GREED_CORE_OVERDRIVE) {
            return false;
        }
        float actualAmount = getAttack() == GildedTyrantAttack.STAGGER ? amount * 1.25F : amount;
        boolean result = super.hurt(source, actualAmount);
        if (result && !this.level().isClientSide && !this.phase2Triggered && !GildedTyrantConfig.TWO_LIFE_REBIRTH.get()) {
            double ratio = GildedTyrantConfig.PHASE2_HEALTH_RATIO.get();
            if (this.getHealth() > 0.0F && this.getHealth() <= this.getMaxHealth() * ratio) {
                enterPhase2(false);
            }
        }
        return result;
    }

    @Override
    public void setHealth(float health) {
        if (!this.level().isClientSide
                && this.configAttributesApplied
                && !this.phase2Triggered
                && !this.bypassKillInProgress
                && !this.isRemoved()
                && GildedTyrantConfig.TWO_LIFE_REBIRTH.get()
                && health <= 0.0F) {
            triggerPhase2Rebirth();
            return;
        }
        super.setHealth(health);
    }

    @Override
    public void triggerPhase2Rebirth() {
        enterPhase2(true);
    }

    private void enterPhase2(boolean restoreFullHealth) {
        if (this.phase2Triggered) {
            return;
        }
        this.phase2Triggered = true;
        this.entityData.set(DATA_PHASE2, true);
        this.dead = false;
        this.deathTime = 0;
        if (restoreFullHealth) {
            super.setHealth(this.getMaxHealth());
        }
        this.clearFire();

        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance inst : this.getActiveEffects()) {
            if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(inst.getEffect());
            }
        }
        for (Holder<MobEffect> eff : harmful) {
            this.removeEffect(eff);
        }

        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / Math.max(1.0F, this.getMaxHealth()), 0.0F, 1.0F));
        Arrays.fill(this.skillCooldowns, 0);
        startAttack(GildedTyrantAttack.GREED_CORE_OVERDRIVE);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2.0D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.8D, getZ(), 45, 0.9D, 0.9D, 0.9D, 0.12D);
        }
        playBossSound(SoundEvents.PIGLIN_BRUTE_ANGRY, 2.0F, 0.65F);
        playBossSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 1.6F, 0.85F);
        LOGGER.debug("GildedTyrant [id={}] entered Phase 2 (rebirth={}, hp={}/{})", this.getId(), restoreFullHealth, this.getHealth(), this.getMaxHealth());
    }

    public void onObeliskShattered(UUID obeliskId) {
        if (!this.obeliskBatchActive) {
            return;
        }
        this.activeObelisks.remove(obeliskId);
        if (this.activeObelisks.isEmpty()) {
            this.obeliskBatchActive = false;
            if (this.isAlive() && getAttack() != GildedTyrantAttack.GREED_CORE_OVERDRIVE) {
                this.entityData.set(DATA_BEAM_MODE, 0);
                startAttack(GildedTyrantAttack.STAGGER);
                playBossSound(SoundEvents.ANVIL_LAND, 1.5F, 0.6F);
                playBossSound(SoundEvents.PIGLIN_BRUTE_HURT, 1.8F, 0.65F);
                LOGGER.debug("GildedTyrant [id={}] suffered obelisk backlash stagger", this.getId());
            }
        }
    }

    private void applyConfigAttributesOnce() {
        if (this.configAttributesApplied) {
            return;
        }
        setBaseAttribute(Attributes.MAX_HEALTH, GildedTyrantConfig.MAX_HEALTH.get());
        setBaseAttribute(Attributes.ATTACK_DAMAGE, GildedTyrantConfig.ATTACK_DAMAGE.get());
        setBaseAttribute(Attributes.ARMOR, GildedTyrantConfig.ARMOR.get());
        setBaseAttribute(Attributes.ARMOR_TOUGHNESS, GildedTyrantConfig.ARMOR_TOUGHNESS.get());
        setBaseAttribute(Attributes.MOVEMENT_SPEED, GildedTyrantConfig.MOVEMENT_SPEED.get());
        setBaseAttribute(Attributes.FOLLOW_RANGE, GildedTyrantConfig.FOLLOW_RANGE.get());
        super.setHealth(this.getMaxHealth());
        this.configAttributesApplied = true;
    }

    private void setBaseAttribute(Holder<Attribute> attr, double val) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst != null) {
            inst.setBaseValue(val);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            tickClientTimer();
            spawnClientAura();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        applyConfigAttributesOnce();

        if (this.tickCount % 10 == 0) {
            boolean shouldRapid = BossExpansionConfig.CRICKET_MODE_ENABLED.get() && !CricketBoss.hasNearbySurvivalPlayer(this);
            if (shouldRapid != isRapidCombat()) {
                this.entityData.set(DATA_RAPID_COMBAT, shouldRapid);
            }
            if (this.obeliskBatchActive) {
                this.activeObelisks.removeIf(uuid -> {
                    Entity e = serverLevel.getEntity(uuid);
                    return !(e instanceof GildedObeliskEntity) || !e.isAlive();
                });
                if (this.activeObelisks.isEmpty()) {
                    this.obeliskBatchActive = false;
                }
            }
        }

        int cdStep = isRapidCombat() ? 2 : 1;
        for (int i = 0; i < this.skillCooldowns.length; i++) {
            if (this.skillCooldowns[i] > 0) {
                this.skillCooldowns[i] = Math.max(0, this.skillCooldowns[i] - cdStep);
            }
        }

        GildedTyrantAttack current = getAttack();
        if (this.globalAttackDelay > 0 && current == GildedTyrantAttack.NONE) {
            this.globalAttackDelay = Math.max(0, this.globalAttackDelay - cdStep);
        }

        if (current != GildedTyrantAttack.NONE) {
            if (current != GildedTyrantAttack.MOLTEN_GOLD_RAY) {
                applyLockedYaw();
            }
            this.attackStepAccumulator += getActionSpeedMultiplier();
            while (this.attackStepAccumulator >= 1.0D && getAttack() == current) {
                this.attackStepAccumulator -= 1.0D;
                this.attackTick++;
                tickSkillTimeline(serverLevel, current);
                if (getAttack() == current && this.attackTick >= current.duration) {
                    finishCurrentAttack();
                    break;
                }
            }
        }

        tickPendingSpikes(serverLevel);
        tickActiveFissures(serverLevel);
        tickActiveShockRings(serverLevel);
        tickFallingAxeStrikes(serverLevel);

        this.bossEvent.setName(this.getDisplayName());
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / Math.max(1.0F, this.getMaxHealth()), 0.0F, 1.0F));
    }

    private void tickClientTimer() {
        int serial = this.entityData.get(DATA_ATTACK_SERIAL);
        GildedTyrantAttack current = getAttack();
        if (current == GildedTyrantAttack.NONE) {
            this.clientAttackAge = 0.0F;
            return;
        }
        if (serial != this.clientVfxSerial) {
            this.clientVfxSerial = serial;
            this.clientAttackAge = 0.0F;
        } else {
            this.clientAttackAge += getActionSpeedMultiplier();
        }
    }

    private void spawnClientAura() {
        if (isPhase2() && this.random.nextInt(2) == 0) {
            double ang = this.random.nextDouble() * Math.PI * 2.0D;
            double r = 0.7D + this.random.nextDouble() * 0.6D;
            this.level().addParticle(
                    this.random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.WAX_ON,
                    getX() + Math.cos(ang) * r,
                    getY() + 0.3D + this.random.nextDouble() * 2.4D,
                    getZ() + Math.sin(ang) * r,
                    0.0D, 0.04D, 0.0D
            );
        }
    }

    public void startAttack(GildedTyrantAttack next) {
        GildedTyrantAttack prev = getAttack();
        this.entityData.set(DATA_ATTACK, next.id);
        if (prev == GildedTyrantAttack.MOLTEN_GOLD_RAY || prev == GildedTyrantAttack.GOLDEN_CHAIN_SHACKLE || next == GildedTyrantAttack.NONE) {
            this.entityData.set(DATA_BEAM_MODE, 0);
        }
        if (next != GildedTyrantAttack.NONE) {
            this.entityData.set(DATA_ATTACK_SERIAL, this.entityData.get(DATA_ATTACK_SERIAL) + 1);
            this.getNavigation().stop();
            faceCurrentTarget();
            int cd = GildedTyrantConfig.getCooldown(next);
            if (cd > 0) {
                if (isRapidCombat()) {
                    cd = Math.max(15, (int) Math.round(cd * BossExpansionConfig.NO_PLAYER_COOLDOWN_SCALE.get()));
                }
                this.skillCooldowns[next.id] = cd;
                this.lastSkill = next;
            }
            LOGGER.debug("GildedTyrant [id={}] started skill {} (phase2={})", this.getId(), next, isPhase2());
        }
        this.attackTick = 0;
        this.attackStepAccumulator = 0.0D;
    }

    private void finishCurrentAttack() {
        GildedTyrantAttack finished = getAttack();
        if (finished == GildedTyrantAttack.WHIRLING_GREATAXE_BOOMERANG) {
            setAxeThrown(false);
        }
        startAttack(GildedTyrantAttack.NONE);
        int gap = isPhase2() ? 16 : 22;
        if (isRapidCombat()) {
            gap = Math.max(3, (int) Math.round(gap * BossExpansionConfig.NO_PLAYER_COOLDOWN_SCALE.get() * 0.5D));
        } else {
            gap += this.random.nextInt(8);
        }
        this.globalAttackDelay = gap;
    }

    private void faceCurrentTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            if (dx * dx + dz * dz > 1.0E-4D) {
                this.lockedYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            }
        } else {
            this.lockedYaw = this.yBodyRot;
        }
        applyLockedYaw();
    }

    private void applyLockedYaw() {
        this.setYRot(this.lockedYaw);
        this.yBodyRot = this.lockedYaw;
        this.yHeadRot = this.lockedYaw;
    }

    private Vec3 forward() {
        return Vec3.directionFromRotation(0.0F, this.lockedYaw);
    }

    public boolean canAffectVictim(LivingEntity entity) {
        if (entity == null || entity == this || !entity.isAlive() || entity instanceof ArmorStand || entity instanceof GildedObeliskEntity) {
            return false;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return !this.isAlliedTo(entity);
    }

    private boolean applyScaledAttack(LivingEntity victim, Vec3 origin, double multiplier, double baseHorizKb, double baseLiftY, boolean breakShield) {
        BossCombatEvents.applyAntiHealIfEligible(this, victim);
        if (breakShield) {
            if (victim instanceof Player player && player.isBlocking()) {
                ItemStack useItem = player.getUseItem();
                if (!useItem.isEmpty()) {
                    player.getCooldowns().addCooldown(useItem.getItem(), 60);
                }
                player.stopUsingItem();
                this.level().broadcastEntityEvent(player, (byte) 30);
            } else if (!(victim instanceof Player)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
        }

        float dmg = (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier);
        boolean hit = victim.hurt(this.damageSources().mobAttack(this), dmg);
        if (!hit) {
            return false;
        }

        double resist = Math.max(0.0D, 1.0D - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        double scale = GildedTyrantConfig.KNOCKBACK_SCALE.get();
        double kb = baseHorizKb * resist * scale;
        double lift = baseLiftY * resist * scale;
        if (Math.abs(kb) > 0.01D || Math.abs(lift) > 0.01D) {
            Vec3 away = victim.position().subtract(origin);
            Vec3 flat = new Vec3(away.x, 0.0D, away.z);
            flat = flat.lengthSqr() < 1.0E-4D ? forward() : flat.normalize();
            victim.push(flat.x * kb, lift, flat.z * kb);
            victim.hurtMarked = true;
        }
        return true;
    }

    private int strikeCone(ServerLevel level, double reach, double cosHalfAngle, double mult, double kb, double lift, boolean breakShield) {
        Vec3 look = forward();
        int count = 0;
        AABB box = this.getBoundingBox().inflate(reach + 1.2D, 2.0D, reach + 1.2D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::canAffectVictim)) {
            Vec3 diff = victim.position().subtract(position());
            Vec3 flat = new Vec3(diff.x, 0.0D, diff.z);
            double dist = flat.length() - victim.getBbWidth() * 0.5D;
            if (dist > reach) {
                continue;
            }
            if (flat.lengthSqr() > 0.4D && flat.normalize().dot(look) < cosHalfAngle) {
                continue;
            }
            if (applyScaledAttack(victim, position(), mult, kb, lift, breakShield)) {
                count++;
            }
        }
        return count;
    }

    private void tickSkillTimeline(ServerLevel level, GildedTyrantAttack attack) {
        LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) {
            target = null;
        }

        switch (attack) {
            case TYRANT_COMBO -> tickTyrantCombo(level, target);
            case BEARDED_HOOK_SWEEP -> tickBeardedHookSweep(level, target);
            case PAULDRON_BULL_RUSH -> tickPauldronBullRush(level, target);
            case GILDED_FISSURE_SLAM -> tickGildedFissureSlam(level, target);
            case GOLD_FEVER_ROAR -> tickGoldFeverRoar(level, target);
            case BASTION_ANVIL_LEAP -> tickBastionAnvilLeap(level, target);
            case GREED_CORE_OVERDRIVE -> tickGreedCoreOverdrive(level);
            case WHIRLING_GREATAXE_BOOMERANG -> tickWhirlingGreataxeBoomerang(level, target);
            case GOLDEN_CHAIN_SHACKLE -> tickGoldenChainShackle(level, target);
            case MOLTEN_GOLD_RAY -> tickMoltenGoldRay(level, target);
            case GILDED_OBELISK_SANCTUM -> tickGildedObeliskSanctum(level);
            case CRIMSON_WHIRLWIND_CLEAVE -> tickCrimsonWhirlwindCleave(level, target);
            case CATACLYSMIC_GOLD_RAIN -> tickCataclysmicGoldRain(level, target);
            default -> {
            }
        }
    }

    private void tickTyrantCombo(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t < 9 || (t >= 15 && t <= 22)) {
            faceCurrentTarget();
        }
        if (t == 8 || t == 22) {
            Vec3 step = forward().scale(t == 8 ? 0.38D : 0.48D);
            this.setDeltaMovement(step.x, this.getDeltaMovement().y, step.z);
            playBossSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, t == 8 ? 0.85F : 0.65F);
        }
        if (t == 11) {
            int hits = strikeCone(level, 4.2D, 0.65D, GildedTyrantConfig.COMBO_PUNCH_MULTIPLIER.get(), 0.35D, 0.10D, true);
            playBossSound(SoundEvents.ANVIL_PLACE, hits > 0 ? 1.1F : 0.7F, 1.25F);
        } else if (t == 25) {
            Vec3 front = position().add(forward().scale(2.4D));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y + 1.4D, front.z, 3, 0.6D, 0.2D, 0.6D, 0.0D);
            strikeCone(level, 5.2D, 0.45D, GildedTyrantConfig.COMBO_AXE_MULTIPLIER.get(), 0.55D, 0.16D, false);
            playBossSound(SoundEvents.TRIDENT_HIT_GROUND, 1.5F, 0.7F);
        }
    }

    private void tickBeardedHookSweep(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t <= 12) {
            faceCurrentTarget();
        }
        if (t == 16) {
            playBossSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.55F);
            Vec3 look = forward();
            AABB box = this.getBoundingBox().inflate(6.6D, 2.0D, 6.6D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::canAffectVictim)) {
                Vec3 diff = victim.position().subtract(position());
                Vec3 flat = new Vec3(diff.x, 0.0D, diff.z);
                double dist = flat.length() - victim.getBbWidth() * 0.5D;
                if (dist > 6.3D || (flat.lengthSqr() > 0.3D && flat.normalize().dot(look) < -0.34D)) {
                    continue;
                }
                double kb = dist > 3.4D ? -0.60D : 0.25D;
                applyScaledAttack(victim, position(), GildedTyrantConfig.HOOK_SWEEP_MULTIPLIER.get(), kb, 0.14D, false);
            }
        } else if (t == 27) {
            playBossSound(SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 1.25F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GILDED_BLACKSTONE.defaultBlockState()),
                    getX(), getY() + 0.1D, getZ(), 24, 1.4D, 0.1D, 1.4D, 0.12D);
            AABB stompBox = this.getBoundingBox().inflate(3.4D, 1.2D, 3.4D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, stompBox, this::canAffectVictim)) {
                if (horizontalDist(victim.position(), position()) <= 3.4D + victim.getBbWidth() * 0.5D) {
                    applyScaledAttack(victim, position(), GildedTyrantConfig.HOOK_STOMP_MULTIPLIER.get(), 0.45D, 0.20D, false);
                }
            }
        }
    }

    private void tickPauldronBullRush(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t == 1) {
            this.rushHalted = false;
            this.rushHitIds.clear();
            playBossSound(SoundEvents.PIGLIN_BRUTE_ANGRY, 1.5F, 0.75F);
        }
        if (t <= 13) {
            faceCurrentTarget();
            this.rushDir = forward();
            if (t % 4 == 0) {
                level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2D, getZ(), 3, 0.4D, 0.1D, 0.4D, 0.02D);
            }
        } else if (t <= 33 && !this.rushHalted) {
            Vec3 vel = this.rushDir.scale(0.82D);
            this.setDeltaMovement(vel.x, this.getDeltaMovement().y, vel.z);
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3D, getZ(), 3, 0.4D, 0.2D, 0.4D, 0.01D);

            AABB hitBox = this.getBoundingBox().inflate(0.95D, 0.4D, 0.95D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox, this::canAffectVictim)) {
                if (this.rushHitIds.add(victim.getId())) {
                    applyScaledAttack(victim, position(), GildedTyrantConfig.BULL_RUSH_MULTIPLIER.get(), 0.75D, 0.22D, true);
                    playBossSound(SoundEvents.ANVIL_LAND, 1.1F, 0.85F);
                    this.rushHalted = true;
                }
            }
            if (this.horizontalCollision) {
                this.rushHalted = true;
            }
        } else if (t == 37) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            strikeCone(level, 4.0D, 0.50D, GildedTyrantConfig.BULL_UPPERCUT_MULTIPLIER.get(), 0.40D, 0.28D, false);
            playBossSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.4F, 0.75F);
        }
    }

    private void tickGildedFissureSlam(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t <= 14) {
            faceCurrentTarget();
        }
        if (t == 19) {
            Vec3 dir = forward();
            Vec3 impact = position().add(dir.scale(2.5D));
            playBossSound(SoundEvents.GENERIC_EXPLODE.value(), 1.3F, 0.85F);
            playBossSound(SoundEvents.ANVIL_LAND, 1.2F, 0.65F);
            level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.3D, impact.z, 2, 0.3D, 0.1D, 0.3D, 0.0D);

            AABB slamBox = new AABB(impact.x - 3.6D, impact.y - 1.5D, impact.z - 3.6D, impact.x + 3.6D, impact.y + 2.5D, impact.z + 3.6D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, slamBox, this::canAffectVictim)) {
                if (horizontalDist(victim.position(), impact) <= 3.5D + victim.getBbWidth() * 0.5D) {
                    applyScaledAttack(victim, impact, GildedTyrantConfig.FISSURE_IMPACT_MULTIPLIER.get(), 0.55D, 0.22D, true);
                }
            }
            this.activeFissures.add(new FissureLine(impact, dir, 7, 2.2D, GildedTyrantConfig.FISSURE_WAVE_MULTIPLIER.get()));
        }
    }

    private void tickGoldFeverRoar(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t <= 12) {
            faceCurrentTarget();
        }
        if (t == 18) {
            playBossSound(SoundEvents.PIGLIN_BRUTE_ANGRY, 2.0F, 0.55F);
            playBossSound(SoundEvents.RAID_HORN.value(), 1.2F, 0.9F);
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.2D, getZ(), 36, 1.2D, 0.6D, 1.2D, 0.15D);

            boolean triggeredGoldFever = false;
            AABB scanBox = this.getBoundingBox().inflate(14.0D, 5.0D, 14.0D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, scanBox, this::canAffectVictim)) {
                double d = horizontalDist(victim.position(), position());
                if (d <= 10.0D + victim.getBbWidth() * 0.5D) {
                    applyScaledAttack(victim, position(), GildedTyrantConfig.ROAR_WAVE_MULTIPLIER.get(), 0.45D, 0.12D, false);
                }
                if (hasGoldenGearOrBuff(victim)) {
                    triggeredGoldFever = true;
                }
                Vec3 ground = snapToGround(level, victim.position());
                this.pendingSpikes.add(new DelayedSpike(ground, 20, GildedTyrantConfig.GOLD_SPIKE_MULTIPLIER.get(), 1.75F));
                for (int i = 0; i < 2; i++) {
                    double ang = this.random.nextDouble() * Math.PI * 2.0D;
                    double rad = 1.8D + this.random.nextDouble() * 1.8D;
                    Vec3 extra = snapToGround(level, victim.position().add(Math.cos(ang) * rad, 0.0D, Math.sin(ang) * rad));
                    this.pendingSpikes.add(new DelayedSpike(extra, 24 + i * 4, GildedTyrantConfig.GOLD_SPIKE_MULTIPLIER.get(), 1.65F));
                }
            }
            if (triggeredGoldFever) {
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
            }
        }
    }

    private boolean hasGoldenGearOrBuff(LivingEntity entity) {
        if (entity.getAbsorptionAmount() > 0.0F) {
            return true;
        }
        if (!(entity instanceof Player)) {
            return entity.getHealth() >= entity.getMaxHealth() * 0.5F;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key.getPath().contains("gold")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void tickBastionAnvilLeap(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t == 1) {
            this.leapLanded = false;
        }
        if (t <= 9) {
            faceCurrentTarget();
            if (target != null) {
                this.leapDest = target.position();
            } else {
                this.leapDest = position().add(forward().scale(8.0D));
            }
        } else if (t == 10) {
            Vec3 to = this.leapDest.subtract(position());
            Vec3 flat = new Vec3(to.x, 0.0D, to.z);
            double dist = Math.min(20.0D, flat.length());
            Vec3 horiz = dist > 0.01D ? flat.normalize().scale(dist * 0.072D) : Vec3.ZERO;
            this.setDeltaMovement(horiz.x, 0.92D, horiz.z);
            this.hasImpulse = true;
            playBossSound(SoundEvents.RAVAGER_STEP, 1.6F, 0.65F);
        } else if (t > 14 && !this.leapLanded) {
            if (t == 22) {
                Vec3 m = this.getDeltaMovement();
                this.setDeltaMovement(m.x * 0.5D, -1.15D, m.z * 0.5D);
            }
            if (this.onGround() || t == 32) {
                this.leapLanded = true;
                Vec3 center = position();
                playBossSound(SoundEvents.ANVIL_LAND, 1.8F, 0.6F);
                playBossSound(SoundEvents.GENERIC_EXPLODE.value(), 1.3F, 0.85F);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.2D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

                AABB innerBox = this.getBoundingBox().inflate(3.8D, 1.8D, 3.8D);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, innerBox, this::canAffectVictim)) {
                    if (horizontalDist(victim.position(), center) <= 3.6D + victim.getBbWidth() * 0.5D) {
                        applyScaledAttack(victim, center, GildedTyrantConfig.ANVIL_INNER_MULTIPLIER.get(), 0.65D, 0.25D, true);
                    }
                }
                this.activeRings.add(new ExpandingShockRing(center, 3.2D, 9.2D, 0.45D, GildedTyrantConfig.ANVIL_RING_MULTIPLIER.get()));
                this.entityData.set(DATA_SHOCK_RING_ORIGIN, new Vector3f((float) center.x, (float) (center.y + 0.12D), (float) center.z));
                this.entityData.set(DATA_SHOCK_RING_RADIUS, 3.2F);
            }
        }
    }

    private void tickGreedCoreOverdrive(ServerLevel level) {
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        if (this.attackTick < 28 && this.attackTick % 4 == 0) {
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.8D, getZ(), 6, 0.5D, 0.5D, 0.5D, 0.05D);
        } else if (this.attackTick == 28) {
            playBossSound(SoundEvents.GENERIC_EXPLODE.value(), 1.8F, 0.7F);
            playBossSound(SoundEvents.PIGLIN_BRUTE_ANGRY, 2.0F, 0.55F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            AABB box = this.getBoundingBox().inflate(8.2D, 3.0D, 8.2D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::canAffectVictim)) {
                if (horizontalDist(victim.position(), position()) <= 8.0D + victim.getBbWidth() * 0.5D) {
                    applyScaledAttack(victim, position(), GildedTyrantConfig.OVERDRIVE_BURST_MULTIPLIER.get(), 0.70D, 0.24D, false);
                }
            }
        }
    }

    private void tickWhirlingGreataxeBoomerang(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t <= 9) {
            faceCurrentTarget();
        }
        if (t == 10) {
            Vec3 start = position().add(0.0D, 2.1D, 0.0D).add(forward().scale(1.2D));
            Vec3 apex = target != null
                    ? target.position().add(0.0D, 1.1D, 0.0D).add(forward().scale(2.5D))
                    : position().add(forward().scale(16.0D)).add(0.0D, 1.5D, 0.0D);
            ThrownGreataxeEntity thrown = new ThrownGreataxeEntity(level, this, start, apex);
            level.addFreshEntity(thrown);
            setAxeThrown(true);
            playBossSound(SoundEvents.TRIDENT_THROW.value(), 1.6F, 0.65F);
        } else if (t > 12 && t <= 38) {
            faceCurrentTarget();
            if (target != null && horizontalDist(target.position(), position()) > 3.2D) {
                Vec3 chase = forward().scale(0.46D);
                this.setDeltaMovement(chase.x, this.getDeltaMovement().y, chase.z);
            }
            if (t == 24 || t == 36) {
                strikeCone(level, 4.2D, 0.55D, GildedTyrantConfig.BOOMERANG_BRAWL_MULTIPLIER.get(), 0.35D, 0.12D, t == 24);
                playBossSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.35F, t == 24 ? 0.9F : 0.75F);
            }
        } else if (t == 51) {
            setAxeThrown(false);
        }
    }

    private void tickGoldenChainShackle(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t == 1) {
            this.chainHookedId = -1;
        }
        if (t <= 11) {
            faceCurrentTarget();
            if (target != null) {
                Vec3 aim = target.getBoundingBox().getCenter();
                this.entityData.set(DATA_BEAM_TARGET, new Vector3f((float) aim.x, (float) aim.y, (float) aim.z));
            }
        } else if (t == 12) {
            playBossSound(SoundEvents.CHAIN_BREAK, 1.8F, 1.2F);
            Vec3 start = position().add(0.0D, 1.9D, 0.0D);
            Vec3 end = start.add(forward().scale(16.0D));
            if (target != null && horizontalDist(target.position(), position()) <= 16.5D) {
                end = target.getBoundingBox().getCenter();
            }
            this.entityData.set(DATA_BEAM_MODE, 1);
            this.entityData.set(DATA_BEAM_TARGET, new Vector3f((float) end.x, (float) end.y, (float) end.z));

            AABB chainBox = new AABB(start, end).inflate(1.1D);
            LivingEntity hooked = null;
            double nearest = Double.MAX_VALUE;
            for (LivingEntity cand : level.getEntitiesOfClass(LivingEntity.class, chainBox, this::canAffectVictim)) {
                if (distanceToSegment(cand.getBoundingBox().getCenter(), start, end) <= 1.15D + cand.getBbWidth() * 0.4D) {
                    double dSq = cand.distanceToSqr(this);
                    if (dSq < nearest) {
                        nearest = dSq;
                        hooked = cand;
                    }
                }
            }
            if (hooked != null) {
                this.chainHookedId = hooked.getId();
                applyScaledAttack(hooked, position(), GildedTyrantConfig.CHAIN_HOOK_MULTIPLIER.get(), -0.80D, 0.18D, false);
                playBossSound(SoundEvents.CHAIN_PLACE, 1.6F, 0.75F);
            }
        } else if (t > 12 && t <= 22) {
            if (this.chainHookedId >= 0) {
                Entity e = level.getEntity(this.chainHookedId);
                if (e instanceof LivingEntity hooked && hooked.isAlive()) {
                    Vec3 c = hooked.getBoundingBox().getCenter();
                    this.entityData.set(DATA_BEAM_TARGET, new Vector3f((float) c.x, (float) c.y, (float) c.z));
                    if (t == 17 && horizontalDist(hooked.position(), position()) > 3.2D) {
                        double resist = Math.max(0.0D, 1.0D - hooked.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                        Vec3 pull = position().add(forward().scale(2.2D)).subtract(hooked.position());
                        Vec3 pullFlat = new Vec3(pull.x, 0.0D, pull.z);
                        if (pullFlat.lengthSqr() > 1.0E-4D) {
                            Vec3 v = pullFlat.normalize().scale(0.75D * resist * GildedTyrantConfig.KNOCKBACK_SCALE.get());
                            hooked.push(v.x, 0.12D * resist, v.z);
                            hooked.hurtMarked = true;
                        }
                    }
                }
            } else if (t == 20) {
                this.entityData.set(DATA_BEAM_MODE, 0);
                finishCurrentAttack();
            }
        } else if (t == 23) {
            this.entityData.set(DATA_BEAM_MODE, 0);
            faceCurrentTarget();
        } else if (t == 29 && this.chainHookedId >= 0) {
            strikeCone(level, 4.5D, 0.50D, GildedTyrantConfig.CHAIN_CRUSH_MULTIPLIER.get(), 0.75D, 0.24D, true);
            playBossSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.1F);
            playBossSound(SoundEvents.ANVIL_LAND, 1.4F, 0.75F);
        }
    }

    private void tickMoltenGoldRay(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        Vec3 chestOrigin = position().add(0.0D, 1.95D, 0.0D).add(Vec3.directionFromRotation(0.0F, this.lockedYaw).scale(0.65D));
        if (t == 1) {
            faceCurrentTarget();
            if (target != null) {
                Vec3 diff = target.getBoundingBox().getCenter().subtract(chestOrigin);
                this.rayDir = diff.lengthSqr() > 1.0E-4D ? diff.normalize() : forward();
            } else {
                this.rayDir = forward();
            }
            this.entityData.set(DATA_BEAM_MODE, 2);
            playBossSound(SoundEvents.BEACON_ACTIVATE, 1.6F, 1.35F);
        }

        if (target != null) {
            Vec3 desired = target.getBoundingBox().getCenter().subtract(chestOrigin);
            if (desired.lengthSqr() > 1.0E-4D) {
                desired = desired.normalize();
                double lerpRate = t < 20 ? 0.22D : 0.055D;
                this.rayDir = this.rayDir.lerp(desired, lerpRate).normalize();
                this.lockedYaw = (float) (Mth.atan2(this.rayDir.z, this.rayDir.x) * (180.0D / Math.PI)) - 90.0F;
                applyLockedYaw();
            }
        }

        Vec3 maxEnd = chestOrigin.add(this.rayDir.scale(MOLTEN_RAY_RANGE));
        HitResult blockHit = level.clip(new ClipContext(chestOrigin, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 endPos = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        this.entityData.set(DATA_BEAM_TARGET, new Vector3f((float) endPos.x, (float) endPos.y, (float) endPos.z));

        if (t == 20) {
            this.entityData.set(DATA_BEAM_MODE, 3);
            playBossSound(SoundEvents.BLAZE_SHOOT, 1.8F, 0.6F);
            playBossSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5F, 0.8F);
        } else if (t > 20 && t <= 48 && t % 4 == 0) {
            level.sendParticles(ParticleTypes.FLAME, endPos.x, endPos.y, endPos.z, 6, 0.25D, 0.25D, 0.25D, 0.04D);
            AABB rayBounds = new AABB(chestOrigin, endPos).inflate(0.95D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, rayBounds, this::canAffectVictim)) {
                if (distanceToSegment(victim.getBoundingBox().getCenter(), chestOrigin, endPos) <= 0.90D + victim.getBbWidth() * 0.45D) {
                    if (applyScaledAttack(victim, chestOrigin, GildedTyrantConfig.MOLTEN_RAY_TICK_MULTIPLIER.get(), 0.15D, 0.04D, false)) {
                        victim.igniteForSeconds(3.0F);
                    }
                }
            }
        } else if (t == 49) {
            this.entityData.set(DATA_BEAM_MODE, 0);
        }
    }

    private void tickGildedObeliskSanctum(ServerLevel level) {
        if (this.attackTick == 18) {
            for (UUID oldId : this.activeObelisks) {
                Entity old = level.getEntity(oldId);
                if (old != null) {
                    old.discard();
                }
            }
            this.activeObelisks.clear();

            int count = 3;
            double baseAngle = this.random.nextDouble() * Math.PI * 2.0D;
            for (int i = 0; i < count; i++) {
                double ang = baseAngle + i * (Math.PI * 2.0D / count);
                double rad = 8.0D;
                Vec3 raw = position().add(Math.cos(ang) * rad, 0.0D, Math.sin(ang) * rad);
                Vec3 ground = snapToGround(level, raw);
                GildedObeliskEntity obelisk = new GildedObeliskEntity(level, this, ground);
                if (level.addFreshEntity(obelisk)) {
                    this.activeObelisks.add(obelisk.getUUID());
                    level.sendParticles(ParticleTypes.FLAME, ground.x, ground.y + 1.0D, ground.z, 18, 0.4D, 0.8D, 0.4D, 0.05D);
                }
            }
            this.obeliskBatchActive = !this.activeObelisks.isEmpty();
            playBossSound(SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 1.8F, 0.85F);
        }
    }

    private void tickCrimsonWhirlwindCleave(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        if (t >= 9 && t <= 36) {
            if (target != null) {
                Vec3 diff = target.position().subtract(position());
                Vec3 flat = new Vec3(diff.x, 0.0D, diff.z);
                if (flat.lengthSqr() > 0.25D) {
                    Vec3 move = flat.normalize().scale(0.34D);
                    this.setDeltaMovement(move.x, this.getDeltaMovement().y, move.z);
                }
            }
            if (t == 12 || t == 22 || t == 32) {
                playBossSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.6F);
                for (int i = 0; i < 12; i++) {
                    double a = i * (Math.PI / 6.0D);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 3.8D, getY() + 1.3D, getZ() + Math.sin(a) * 3.8D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                AABB spinBox = this.getBoundingBox().inflate(5.6D, 1.8D, 5.6D);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, spinBox, this::canAffectVictim)) {
                    if (horizontalDist(victim.position(), position()) <= 5.5D + victim.getBbWidth() * 0.5D) {
                        applyScaledAttack(victim, position(), GildedTyrantConfig.WHIRLWIND_SPIN_MULTIPLIER.get(), 0.25D, 0.08D, false);
                    }
                }
            }
        } else if (t == 38) {
            faceCurrentTarget();
        } else if (t == 44) {
            Vec3 front = position().add(forward().scale(2.6D));
            playBossSound(SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.8F);
            strikeCone(level, 6.0D, 0.42D, GildedTyrantConfig.WHIRLWIND_FINISHER_MULTIPLIER.get(), 0.60D, 0.22D, true);
            for (int branch = -1; branch <= 1; branch++) {
                Vec3 branchDir = Vec3.directionFromRotation(0.0F, this.lockedYaw + branch * 22.0F);
                this.activeFissures.add(new FissureLine(front, branchDir, 5, 2.0D, GildedTyrantConfig.FISSURE_WAVE_MULTIPLIER.get() * 0.85D));
            }
        }
    }

    private void tickCataclysmicGoldRain(ServerLevel level, LivingEntity target) {
        int t = this.attackTick;
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        if (t == 16) {
            playBossSound(SoundEvents.ANVIL_LAND, 2.0F, 0.55F);
            playBossSound(SoundEvents.PIGLIN_BRUTE_ANGRY, 2.0F, 0.55F);
            for (int i = 0; i < 8; i++) {
                Vec3 dir = Vec3.directionFromRotation(0.0F, this.lockedYaw + i * 45.0F);
                this.activeFissures.add(new FissureLine(position().add(dir.scale(2.0D)), dir, 7, 2.2D, GildedTyrantConfig.CATACLYSMIC_RAY_MULTIPLIER.get()));
            }
        } else if (t >= 22 && t <= 46 && t % 6 == 4) {
            if (target != null) {
                Vec3 g = snapToGround(level, target.position());
                this.pendingAxeDrops.add(new FallingAxeStrike(g, 16));
            }
            for (int k = 0; k < 3; k++) {
                double ang = this.random.nextDouble() * Math.PI * 2.0D;
                double rad = 3.5D + this.random.nextDouble() * 10.5D;
                Vec3 p = snapToGround(level, position().add(Math.cos(ang) * rad, 0.0D, Math.sin(ang) * rad));
                this.pendingAxeDrops.add(new FallingAxeStrike(p, 16));
            }
            playBossSound(SoundEvents.BELL_BLOCK, 1.5F, 0.75F);
        } else if (t == 56) {
            playBossSound(SoundEvents.GENERIC_EXPLODE.value(), 1.8F, 0.7F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.2D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            AABB box = this.getBoundingBox().inflate(5.8D, 2.5D, 5.8D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::canAffectVictim)) {
                if (horizontalDist(victim.position(), position()) <= 5.5D + victim.getBbWidth() * 0.5D) {
                    applyScaledAttack(victim, position(), GildedTyrantConfig.CATACLYSMIC_BURST_MULTIPLIER.get(), 0.65D, 0.24D, true);
                }
            }
        }
    }

    private void tickPendingSpikes(ServerLevel level) {
        if (this.pendingSpikes.isEmpty()) {
            return;
        }
        Iterator<DelayedSpike> it = this.pendingSpikes.iterator();
        while (it.hasNext()) {
            DelayedSpike sp = it.next();
            sp.delay--;
            if (sp.delay > 0) {
                if (sp.delay % 3 == 0) {
                    level.sendParticles(ParticleTypes.WAX_ON, sp.pos.x, sp.pos.y + 0.1D, sp.pos.z, 4, 0.45D, 0.05D, 0.45D, 0.01D);
                }
            } else {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState()),
                        sp.pos.x, sp.pos.y + 0.6D, sp.pos.z, 22, 0.4D, 0.6D, 0.4D, 0.15D);
                level.sendParticles(ParticleTypes.FLAME, sp.pos.x, sp.pos.y + 0.5D, sp.pos.z, 10, 0.35D, 0.5D, 0.35D, 0.04D);
                level.playSound(null, sp.pos.x, sp.pos.y, sp.pos.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.0F, 0.75F);

                AABB box = new AABB(sp.pos.x - sp.radius, sp.pos.y - 0.5D, sp.pos.z - sp.radius, sp.pos.x + sp.radius, sp.pos.y + 2.4D, sp.pos.z + sp.radius);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::canAffectVictim)) {
                    if (horizontalDist(victim.position(), sp.pos) <= sp.radius + victim.getBbWidth() * 0.4D) {
                        applyScaledAttack(victim, sp.pos, sp.mult, 0.30D, 0.22D, false);
                    }
                }
                it.remove();
            }
        }
    }

    private void tickActiveFissures(ServerLevel level) {
        if (this.activeFissures.isEmpty()) {
            return;
        }
        Iterator<FissureLine> it = this.activeFissures.iterator();
        while (it.hasNext()) {
            FissureLine line = it.next();
            line.tickCounter++;
            if (line.tickCounter % 3 != 0) {
                continue;
            }
            line.currentStep++;
            Vec3 pt = snapToGround(level, line.start.add(line.dir.scale(line.currentStep * line.stepDist)));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GILDED_BLACKSTONE.defaultBlockState()),
                    pt.x, pt.y + 0.35D, pt.z, 16, 0.45D, 0.25D, 0.45D, 0.14D);
            level.sendParticles(ParticleTypes.LAVA, pt.x, pt.y + 0.25D, pt.z, 3, 0.3D, 0.1D, 0.3D, 0.02D);
            level.playSound(null, pt.x, pt.y, pt.z, SoundEvents.BASALT_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);

            AABB box = new AABB(pt.x - 1.6D, pt.y - 0.8D, pt.z - 1.6D, pt.x + 1.6D, pt.y + 2.2D, pt.z + 1.6D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::canAffectVictim)) {
                if (horizontalDist(victim.position(), pt) <= 1.55D + victim.getBbWidth() * 0.4D && line.hitSet.add(victim.getId())) {
                    applyScaledAttack(victim, pt, line.mult, 0.40D, 0.18D, false);
                }
            }
            if (line.currentStep >= line.maxSteps) {
                it.remove();
            }
        }
    }

    private void tickActiveShockRings(ServerLevel level) {
        if (this.activeRings.isEmpty()) {
            if (this.entityData.get(DATA_SHOCK_RING_RADIUS) > 0.0F) {
                this.entityData.set(DATA_SHOCK_RING_RADIUS, 0.0F);
            }
            return;
        }
        Iterator<ExpandingShockRing> it = this.activeRings.iterator();
        while (it.hasNext()) {
            ExpandingShockRing ring = it.next();
            ring.radius += ring.speed;
            this.entityData.set(DATA_SHOCK_RING_RADIUS, (float) ring.radius);

            for (int i = 0; i < 16; i++) {
                double ang = i * (Math.PI / 8.0D);
                level.sendParticles(ParticleTypes.FLAME,
                        ring.center.x + Math.cos(ang) * ring.radius,
                        ring.center.y + 0.18D,
                        ring.center.z + Math.sin(ang) * ring.radius,
                        1, 0.05D, 0.02D, 0.05D, 0.0D);
            }

            AABB ringBox = new AABB(
                    ring.center.x - ring.radius - 1.0D, ring.center.y - 0.6D, ring.center.z - ring.radius - 1.0D,
                    ring.center.x + ring.radius + 1.0D, ring.center.y + 0.85D, ring.center.z + ring.radius + 1.0D
            );
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, ringBox, this::canAffectVictim)) {
                double d = horizontalDist(victim.position(), ring.center);
                if (Math.abs(d - ring.radius) <= 0.95D + victim.getBbWidth() * 0.4D && victim.getY() <= ring.center.y + 0.85D) {
                    if (ring.hitSet.add(victim.getId())) {
                        applyScaledAttack(victim, ring.center, ring.mult, 0.45D, 0.16D, false);
                    }
                }
            }
            if (ring.radius >= ring.maxRadius) {
                it.remove();
                this.entityData.set(DATA_SHOCK_RING_RADIUS, 0.0F);
            }
        }
    }

    private void tickFallingAxeStrikes(ServerLevel level) {
        if (this.pendingAxeDrops.isEmpty()) {
            return;
        }
        Iterator<FallingAxeStrike> it = this.pendingAxeDrops.iterator();
        while (it.hasNext()) {
            FallingAxeStrike drop = it.next();
            drop.timer--;
            if (drop.timer > 0) {
                for (int i = 0; i < 8; i++) {
                    double a = i * (Math.PI / 4.0D) + drop.timer * 0.2D;
                    level.sendParticles(ParticleTypes.WAX_ON,
                            drop.pos.x + Math.cos(a) * 2.6D,
                            drop.pos.y + 0.12D,
                            drop.pos.z + Math.sin(a) * 2.6D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                double fallY = drop.pos.y + drop.timer * 0.75D;
                level.sendParticles(ParticleTypes.FLAME, drop.pos.x, fallY, drop.pos.z, 3, 0.2D, 0.4D, 0.2D, 0.02D);
            } else {
                level.sendParticles(ParticleTypes.EXPLOSION, drop.pos.x, drop.pos.y + 0.3D, drop.pos.z, 2, 0.4D, 0.2D, 0.4D, 0.0D);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState()),
                        drop.pos.x, drop.pos.y + 0.4D, drop.pos.z, 26, 0.8D, 0.3D, 0.8D, 0.18D);
                level.playSound(null, drop.pos.x, drop.pos.y, drop.pos.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.1F, 0.8F);

                AABB box = new AABB(drop.pos.x - 3.0D, drop.pos.y - 0.8D, drop.pos.z - 3.0D, drop.pos.x + 3.0D, drop.pos.y + 2.8D, drop.pos.z + 3.0D);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, this::canAffectVictim)) {
                    if (horizontalDist(victim.position(), drop.pos) <= 2.9D + victim.getBbWidth() * 0.4D) {
                        applyScaledAttack(victim, drop.pos, GildedTyrantConfig.CATACLYSMIC_DROP_MULTIPLIER.get(), 0.50D, 0.20D, false);
                    }
                }
                it.remove();
            }
        }
    }

    private Vec3 snapToGround(ServerLevel level, Vec3 pos) {
        Vec3 from = new Vec3(pos.x, pos.y + 3.0D, pos.z);
        Vec3 to = new Vec3(pos.x, pos.y - 8.0D, pos.z);
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? pos : hit.getLocation();
    }

    private static double horizontalDist(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-5D) {
            return p.distanceTo(a);
        }
        double t = Math.max(0.0D, Math.min(1.0D, p.subtract(a).dot(ab) / lenSq));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private void playBossSound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, getX(), getY() + 1.6D, getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    public GildedTyrantAttack chooseNextSkill(double dist) {
        List<GildedTyrantAttack> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        if (!isPhase2()) {
            addCandidate(pool, weights, GildedTyrantAttack.TYRANT_COMBO, dist <= 4.8D, 14);
            addCandidate(pool, weights, GildedTyrantAttack.BEARDED_HOOK_SWEEP, dist >= 2.2D && dist <= 6.5D, 11);
            addCandidate(pool, weights, GildedTyrantAttack.PAULDRON_BULL_RUSH, dist >= 5.2D && dist <= 18.0D, 10);
            addCandidate(pool, weights, GildedTyrantAttack.GILDED_FISSURE_SLAM, dist >= 3.0D && dist <= 16.0D, 9);
            addCandidate(pool, weights, GildedTyrantAttack.GOLD_FEVER_ROAR, dist <= 12.0D, 7);
            addCandidate(pool, weights, GildedTyrantAttack.BASTION_ANVIL_LEAP, dist >= 6.8D && dist <= 22.0D, 9);
        } else {
            addCandidate(pool, weights, GildedTyrantAttack.WHIRLING_GREATAXE_BOOMERANG, dist >= 4.5D && dist <= 22.0D && !isAxeThrown(), 11);
            addCandidate(pool, weights, GildedTyrantAttack.GOLDEN_CHAIN_SHACKLE, dist >= 4.0D && dist <= 16.0D, 11);
            addCandidate(pool, weights, GildedTyrantAttack.MOLTEN_GOLD_RAY, dist >= 3.8D && dist <= 26.0D, 10);
            addCandidate(pool, weights, GildedTyrantAttack.GILDED_OBELISK_SANCTUM, dist <= 24.0D && !this.obeliskBatchActive, 8);
            addCandidate(pool, weights, GildedTyrantAttack.CRIMSON_WHIRLWIND_CLEAVE, dist <= 10.5D, 13);

            double hpRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
            boolean unlockUlt = hpRatio <= GildedTyrantConfig.CATACLYSMIC_RAIN_HEALTH_RATIO.get();
            addCandidate(pool, weights, GildedTyrantAttack.CATACLYSMIC_GOLD_RAIN, unlockUlt && dist <= 28.0D, 12);
        }

        if (pool.isEmpty()) {
            return GildedTyrantAttack.NONE;
        }
        int totalWeight = 0;
        for (int w : weights) {
            totalWeight += w;
        }
        int pick = this.random.nextInt(totalWeight);
        for (int i = 0; i < pool.size(); i++) {
            pick -= weights.get(i);
            if (pick < 0) {
                return pool.get(i);
            }
        }
        return pool.get(0);
    }

    private void addCandidate(List<GildedTyrantAttack> pool, List<Integer> weights, GildedTyrantAttack atk, boolean inRange, int weight) {
        if (!inRange || this.skillCooldowns[atk.id] > 0) {
            return;
        }
        int finalW = atk == this.lastSkill ? Math.max(1, weight / 3) : weight;
        pool.add(atk);
        weights.add(finalW);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(NBT_CONFIG_APPLIED, this.configAttributesApplied);
        tag.putBoolean(NBT_PHASE2, this.phase2Triggered);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.configAttributesApplied = tag.getBoolean(NBT_CONFIG_APPLIED);
        this.phase2Triggered = tag.getBoolean(NBT_PHASE2);
        this.entityData.set(DATA_PHASE2, this.phase2Triggered);
        if (this.phase2Triggered) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "locomotion", 4, state -> {
            if (getAttack() != GildedTyrantAttack.NONE) {
                return PlayState.STOP;
            }
            if (state.isMoving()) {
                return state.setAndContinue(isPhase2() ? ANIM_RUN : ANIM_WALK);
            }
            return state.setAndContinue(ANIM_IDLE);
        }));

        controllers.add(new AnimationController<>(this, "action", 2, state -> {
            GildedTyrantAttack current = getAttack();
            if (current == GildedTyrantAttack.NONE) {
                state.getController().forceAnimationReset();
                return PlayState.STOP;
            }
            RawAnimation raw = ACTION_ANIMS[current.id];
            return raw == null ? PlayState.STOP : state.setAndContinue(raw);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static final class TyrantCombatGoal extends Goal {
        private final GildedTyrantEntity tyrant;

        TyrantCombatGoal(GildedTyrantEntity tyrant) {
            this.tyrant = tyrant;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.tyrant.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.tyrant.getTarget();
            return (target != null && target.isAlive()) || this.tyrant.getAttack() != GildedTyrantAttack.NONE;
        }

        @Override
        public void stop() {
            this.tyrant.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.tyrant.getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            if (this.tyrant.getAttack() != GildedTyrantAttack.NONE) {
                return;
            }

            this.tyrant.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double dist = horizontalDist(this.tyrant.position(), target.position());

            if (this.tyrant.globalAttackDelay <= 0) {
                GildedTyrantAttack chosen = this.tyrant.chooseNextSkill(dist);
                if (chosen != GildedTyrantAttack.NONE) {
                    this.tyrant.startAttack(chosen);
                    return;
                }
            }

            if (dist > 3.4D) {
                double speedMod = this.tyrant.isPhase2() ? 1.22D : 1.05D;
                this.tyrant.getNavigation().moveTo(target, speedMod);
            } else {
                this.tyrant.getNavigation().stop();
            }
        }
    }
}
