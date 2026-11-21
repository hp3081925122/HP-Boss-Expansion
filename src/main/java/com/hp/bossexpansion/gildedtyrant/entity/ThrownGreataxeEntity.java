package com.hp.bossexpansion.gildedtyrant.entity;

import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.event.BossCombatEvents;
import com.hp.bossexpansion.gildedtyrant.config.GildedTyrantConfig;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ThrownGreataxeEntity extends Entity implements GeoEntity {
    public static final int TOTAL_FLIGHT_TICKS = 40;
    private static final RawAnimation ANIM_SPIN = RawAnimation.begin().thenLoop("animation.gilded_greataxe.spin");

    public final Deque<Vec3> clientTrail = new ArrayDeque<>(16);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int ownerId = -1;
    private Vec3 originPos = Vec3.ZERO;
    private Vec3 apexPos = Vec3.ZERO;
    private int ageTicks;
    private final Set<Integer> hitTargets = new HashSet<>();

    public ThrownGreataxeEntity(EntityType<? extends ThrownGreataxeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ThrownGreataxeEntity(Level level, GildedTyrantEntity owner, Vec3 start, Vec3 apex) {
        this(GildedTyrantRegistry.THROWN_GREATAXE.get(), level);
        this.ownerId = owner.getId();
        this.originPos = start;
        this.apexPos = apex;
        this.setPos(start.x, start.y, start.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            Vec3 cur = position().add(0.0D, 0.35D, 0.0D);
            if (this.clientTrail.size() >= 14) {
                this.clientTrail.removeLast();
            }
            this.clientTrail.addFirst(cur);
            for (int i = 0; i < 3; i++) {
                double ang = this.random.nextDouble() * Math.PI * 2.0D;
                double r = 0.85D + this.random.nextDouble() * 1.15D;
                this.level().addParticle(CindermawParticles.EMBER.get(),
                        getX() + Math.cos(ang) * r,
                        getY() + 0.35D + (this.random.nextDouble() - 0.5D) * 0.25D,
                        getZ() + Math.sin(ang) * r,
                        -Math.sin(ang) * 0.16D, 0.04D, Math.cos(ang) * 0.16D);
            }
            if (this.random.nextBoolean()) {
                this.level().addParticle(CindermawParticles.BLAZE_FLAME.get(),
                        getX() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        getY() + 0.35D,
                        getZ() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        0.0D, 0.03D, 0.0D);
            }
            return;
        }

        this.ageTicks++;
        Entity ownerEnt = this.level().getEntity(this.ownerId);
        if (!(ownerEnt instanceof GildedTyrantEntity owner) || !owner.isAlive() || this.ageTicks > TOTAL_FLIGHT_TICKS) {
            if (ownerEnt instanceof GildedTyrantEntity liveOwner) {
                liveOwner.setAxeThrown(false);
            }
            this.discard();
            return;
        }

        if (this.ageTicks == 17 || this.ageTicks == 25) {
            this.hitTargets.clear();
        }

        Vec3 nextPos;
        if (this.ageTicks <= 16) {
            double u = this.ageTicks / 16.0D;
            double ease = 1.0D - (1.0D - u) * (1.0D - u);
            Vec3 side = new Vec3(-(this.apexPos.z - this.originPos.z), 0.0D, this.apexPos.x - this.originPos.x);
            if (side.lengthSqr() > 1.0E-4D) {
                side = side.normalize().scale(Math.sin(u * Math.PI) * 2.2D);
            }
            nextPos = this.originPos.lerp(this.apexPos, ease).add(side);
        } else if (this.ageTicks <= 24) {
            nextPos = this.apexPos;
        } else {
            double u = (this.ageTicks - 24) / 16.0D;
            Vec3 shoulder = owner.position().add(0.0D, 2.2D, 0.0D);
            Vec3 side = new Vec3(shoulder.z - this.apexPos.z, 0.0D, -(shoulder.x - this.apexPos.x));
            if (side.lengthSqr() > 1.0E-4D) {
                side = side.normalize().scale(Math.sin(u * Math.PI) * 2.0D);
            }
            nextPos = this.apexPos.lerp(shoulder, u * u).add(side);
        }

        Vec3 delta = nextPos.subtract(position());
        this.setDeltaMovement(delta);
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        if (this.ageTicks % 4 == 1) {
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.65F);
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        AABB hitBox = this.getBoundingBox().inflate(1.6D, 0.9D, 1.6D);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, owner::canAffectVictim);
        for (LivingEntity victim : victims) {
            if (!this.hitTargets.add(victim.getId())) {
                continue;
            }
            BossCombatEvents.applyAntiHealIfEligible(owner, victim);
            float dmg = (float) (owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * GildedTyrantConfig.BOOMERANG_AXE_MULTIPLIER.get());
            if (victim.hurtServer(serverLevel, this.damageSources().mobProjectile(this, owner), dmg)) {
                double resist = Math.max(0.0D, 1.0D - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                double kb = 0.35D * resist * GildedTyrantConfig.KNOCKBACK_SCALE.get();
                if (kb > 0.01D) {
                    Vec3 away = victim.position().subtract(position());
                    Vec3 flat = new Vec3(away.x, 0.0D, away.z);
                    flat = flat.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : flat.normalize();
                    victim.push(flat.x * kb, 0.12D * resist, flat.z * kb);
                    victim.hurtMarked = true;
                }
            }
        }

        if (this.ageTicks >= TOTAL_FLIGHT_TICKS) {
            owner.setAxeThrown(false);
            this.level().playSound(null, owner.getX(), owner.getY() + 2.0D, owner.getZ(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.HOSTILE, 1.4F, 0.8F);
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.ageTicks = input.getIntOr("Age", 0);
        this.ownerId = input.getIntOr("OwnerId", -1);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", this.ageTicks);
        output.putInt("OwnerId", this.ownerId);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("spin", 0, state -> state.setAndContinue(ANIM_SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public float getSpinDegrees(float partialTick) {
        return (this.tickCount + partialTick) * 38.0F;
    }
}
