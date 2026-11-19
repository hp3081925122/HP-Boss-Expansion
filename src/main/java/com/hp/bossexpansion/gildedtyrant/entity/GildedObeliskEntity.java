package com.hp.bossexpansion.gildedtyrant.entity;

import com.hp.bossexpansion.api.CricketBoss;
import com.hp.bossexpansion.event.BossCombatEvents;
import com.hp.bossexpansion.gildedtyrant.config.GildedTyrantConfig;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public final class GildedObeliskEntity extends Monster implements GeoEntity {
    public static final int MAX_LIFETIME_TICKS = 200;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.gilded_obelisk.idle");
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = SynchedEntityData.defineId(GildedObeliskEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUuid;
    private int lifeTicks;
    private boolean configApplied;

    public GildedObeliskEntity(EntityType<? extends GildedObeliskEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public GildedObeliskEntity(Level level, GildedTyrantEntity owner, Vec3 pos) {
        this(GildedTyrantRegistry.GILDED_OBELISK.get(), level);
        this.ownerUuid = owner.getUUID();
        this.entityData.set(DATA_OWNER_ID, owner.getId());
        this.setPos(pos.x, pos.y, pos.z);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_ID, -1);
    }

    @Override
    protected void registerGoals() {
    }

    public int getOwnerEntityId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, this.onGround() ? 0.0D : this.getDeltaMovement().y, 0.0D);
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.FLAME, getX() + (this.random.nextDouble() - 0.5D) * 0.6D, getY() + 1.85D, getZ() + (this.random.nextDouble() - 0.5D) * 0.6D, 0.0D, 0.03D, 0.0D);
            }
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        if (!this.configApplied) {
            AttributeInstance hpInst = this.getAttribute(Attributes.MAX_HEALTH);
            if (hpInst != null) {
                hpInst.setBaseValue(GildedTyrantConfig.OBELISK_MAX_HEALTH.get());
                this.setHealth(this.getMaxHealth());
            }
            this.configApplied = true;
        }

        this.lifeTicks++;
        GildedTyrantEntity owner = resolveOwner(serverLevel);
        if (owner == null || !owner.isAlive() || this.lifeTicks >= MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }
        if (this.entityData.get(DATA_OWNER_ID) != owner.getId()) {
            this.entityData.set(DATA_OWNER_ID, owner.getId());
        }

        if (this.lifeTicks % 30 == 15) {
            pulseArcBeam(serverLevel, owner);
        }
    }

    private void pulseArcBeam(ServerLevel level, GildedTyrantEntity owner) {
        Vec3 from = this.position().add(0.0D, 1.85D, 0.0D);
        Vec3 to = owner.position().add(0.0D, 1.95D, 0.0D);
        Vec3 diff = to.subtract(from);
        double len = diff.length();
        if (len > 0.2D) {
            int steps = Math.min(28, Math.max(4, (int) (len * 2.0D)));
            for (int i = 0; i <= steps; i++) {
                Vec3 pt = from.lerp(to, (double) i / steps);
                level.sendParticles(ParticleTypes.WAX_ON, pt.x, pt.y, pt.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            }
            AABB beamBounds = new AABB(from, to).inflate(0.85D);
            List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, beamBounds, owner::canAffectVictim);
            float dmg = (float) (owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * GildedTyrantConfig.OBELISK_ARC_MULTIPLIER.get());
            for (LivingEntity victim : list) {
                if (distancePointToSegment(victim.getBoundingBox().getCenter(), from, to) <= 0.95D + victim.getBbWidth() * 0.4D) {
                    BossCombatEvents.applyAntiHealIfEligible(owner, victim);
                    if (victim.hurt(this.damageSources().indirectMagic(this, owner), dmg)) {
                        double resist = Math.max(0.0D, 1.0D - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                        double kb = 0.28D * resist * GildedTyrantConfig.KNOCKBACK_SCALE.get();
                        if (kb > 0.01D) {
                            victim.push(0.0D, 0.10D * resist, 0.0D);
                            victim.hurtMarked = true;
                        }
                    }
                }
            }
        }

        if (CricketBoss.hasNearbySurvivalPlayer(owner)) {
            float healAmount = (float) (owner.getMaxHealth() * GildedTyrantConfig.OBELISK_HEAL_RATIO.get());
            if (healAmount > 0.0F) {
                owner.heal(healAmount);
            }
        } else {
            LivingEntity target = owner.getTarget();
            if (target != null && target.isAlive() && target.distanceToSqr(this) <= 256.0D) {
                BossCombatEvents.applyAntiHealIfEligible(owner, target);
                float zap = (float) (owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * GildedTyrantConfig.OBELISK_ARC_MULTIPLIER.get() * 0.65D);
                target.hurt(this.damageSources().indirectMagic(this, owner), zap);
            }
        }
        level.playSound(null, getX(), getY() + 1.5D, getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 0.9F, 1.35F);
    }

    private static double distancePointToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double lenSq = ab.lengthSqr();
        if (lenSq < 1.0E-5D) {
            return p.distanceTo(a);
        }
        double t = Math.max(0.0D, Math.min(1.0D, p.subtract(a).dot(ab) / lenSq));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.2D, getZ(), 2, 0.2D, 0.3D, 0.2D, 0.0D);
            serverLevel.playSound(null, getX(), getY() + 1.0D, getZ(), SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 1.2F, 0.9F);
            GildedTyrantEntity owner = resolveOwner(serverLevel);
            if (owner != null && owner.isAlive()) {
                owner.onObeliskShattered(this.getUUID());
            }
        }
    }

    private GildedTyrantEntity resolveOwner(ServerLevel level) {
        if (this.ownerUuid != null) {
            Entity e = level.getEntity(this.ownerUuid);
            if (e instanceof GildedTyrantEntity tyrant) {
                return tyrant;
            }
        }
        int id = this.entityData.get(DATA_OWNER_ID);
        if (id >= 0) {
            Entity e = level.getEntity(id);
            if (e instanceof GildedTyrantEntity tyrant) {
                this.ownerUuid = tyrant.getUUID();
                return tyrant;
            }
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putBoolean("ConfigApplied", this.configApplied);
        if (this.ownerUuid != null) {
            tag.putUUID("OwnerUUID", this.ownerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeTicks = tag.getInt("LifeTicks");
        this.configApplied = tag.getBoolean("ConfigApplied");
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUuid = tag.getUUID("OwnerUUID");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(ANIM_IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
