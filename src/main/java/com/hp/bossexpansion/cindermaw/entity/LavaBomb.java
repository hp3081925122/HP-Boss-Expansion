package com.hp.bossexpansion.cindermaw.entity;

import com.hp.bossexpansion.cindermaw.client.CinderFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// 火山齐射的熔岩弹：发射时确定抛物线，落点提前显示预警，命中后爆炸并留下熔岩池
public final class LavaBomb extends Projectile {
    private static final EntityDataAccessor<CompoundTag> FLIGHT = SynchedEntityData.defineId(LavaBomb.class, SyncedTag.COMPOUND);
    public static final int LINGER = 4;
    public int clientFxAge = Integer.MIN_VALUE;
    public boolean clientImpactDone;

    public LavaBomb(EntityType<? extends LavaBomb> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FLIGHT, new CompoundTag());
    }

    public void launch(CindermawEntity owner, Vec3 start, Vec3 end, int ticks) {
        setOwner(owner);
        CompoundTag d = new CompoundTag();
        d.putDouble("SX", start.x);
        d.putDouble("SY", start.y);
        d.putDouble("SZ", start.z);
        d.putDouble("EX", end.x);
        d.putDouble("EY", end.y);
        d.putDouble("EZ", end.z);
        d.putInt("Start", (int) owner.level().getGameTime());
        d.putInt("Ticks", ticks);
        entityData.set(FLIGHT, d);
        setPos(start.x, start.y, start.z);
    }

    // 穿戴熔渊重炮肩甲发射的弹道，支持低弧线与超载巨炮模式
    public void launchPlayerShell(LivingEntity owner, Vec3 start, Vec3 end, int ticks, float damage, float radius, float arcScale, boolean megabomb) {
        setOwner(owner);
        CompoundTag d = new CompoundTag();
        d.putDouble("SX", start.x);
        d.putDouble("SY", start.y);
        d.putDouble("SZ", start.z);
        d.putDouble("EX", end.x);
        d.putDouble("EY", end.y);
        d.putDouble("EZ", end.z);
        d.putInt("Start", (int) owner.level().getGameTime());
        d.putInt("Ticks", Math.max(4, ticks));
        d.putBoolean("PlayerShell", true);
        d.putFloat("Dmg", damage);
        d.putFloat("Rad", radius);
        d.putFloat("Arc", arcScale);
        d.putBoolean("Mega", megabomb);
        entityData.set(FLIGHT, d);
        setPos(start.x, start.y, start.z);
    }

    public boolean isMegabomb() {
        return entityData.get(FLIGHT).getBooleanOr("Mega", false);
    }

    public boolean isPlayerShell() {
        return entityData.get(FLIGHT).getBooleanOr("PlayerShell", false);
    }

    public boolean ready() {
        return !entityData.get(FLIGHT).isEmpty();
    }

    public int flightTicks() {
        return entityData.get(FLIGHT).getIntOr("Ticks", 0);
    }

    public float age(float partial) {
        return (int) level().getGameTime() - entityData.get(FLIGHT).getIntOr("Start", 0) + partial;
    }

    public boolean impacted() {
        return entityData.get(FLIGHT).getBooleanOr("Hit", false);
    }

    public float impactAge(float partial) {
        return (int) level().getGameTime() - entityData.get(FLIGHT).getIntOr("HitTime", 0) + partial;
    }

    public Vec3 landing() {
        CompoundTag d = entityData.get(FLIGHT);
        return new Vec3(d.getDoubleOr("EX", 0.0), d.getDoubleOr("EY", 0.0), d.getDoubleOr("EZ", 0.0));
    }

    public Vec3 sample(float age) {
        CompoundTag d = entityData.get(FLIGHT);
        Vec3 start = new Vec3(d.getDoubleOr("SX", 0.0), d.getDoubleOr("SY", 0.0), d.getDoubleOr("SZ", 0.0));
        double t = Math.max(0, Math.min(1, age / Math.max(1, flightTicks())));
        double arcScale = d.getFloat("Arc").orElse(1.0F);
        double arc = (6.0 + start.distanceTo(landing()) * 0.35) * arcScale;
        return start.add(landing().subtract(start).scale(t)).add(0, arc * 4 * t * (1 - t), 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!ready()) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }
        if (level().isClientSide()) {
            CinderFx.bombTick(this);
        }
        if (impacted()) {
            if (!level().isClientSide() && impactAge(0) >= LINGER) {
                discard();
            }
            return;
        }
        Vec3 next = sample(age(0) + 1);
        if (!level().isClientSide()) {
            var hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
            if (hit.getType() != HitResult.Type.MISS || age(0) >= flightTicks()) {
                Vec3 contact = landing();
                if (hit.getType() != HitResult.Type.MISS) {
                    contact = hit.getLocation();
                }
                explode(contact);
                return;
            }
        }
        setDeltaMovement(next.subtract(position()));
        setPos(next.x, next.y, next.z);
    }

    private void explode(Vec3 contact) {
        setPos(contact.x, contact.y, contact.z);
        CompoundTag prev = entityData.get(FLIGHT);
        Vec3 start = new Vec3(prev.getDoubleOr("SX", 0.0), prev.getDoubleOr("SY", 0.0), prev.getDoubleOr("SZ", 0.0));
        CompoundTag d = prev.copy();
        d.putBoolean("Hit", true);
        d.putInt("HitTime", (int) level().getGameTime());
        d.putDouble("EX", contact.x);
        d.putDouble("EY", contact.y);
        d.putDouble("EZ", contact.z);
        entityData.set(FLIGHT, d);

        if (d.getBooleanOr("PlayerShell", false)) {
            explodePlayerShell(contact, start, d);
            return;
        }

        level().playSound(null, contact.x, contact.y, contact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.6F, 1.1F);
        level().playSound(null, contact.x, contact.y, contact.z, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.5F, 0.6F);
        if (!(getOwner() instanceof CindermawEntity owner) || !owner.isAlive()) {
            return;
        }
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(contact, contact).inflate(2.6))) {
            Vec3 center = victim.getBoundingBox().getCenter();
            if (!owner.canHarm(victim) || center.distanceToSqr(contact) > 2.4 * 2.4 || !owner.clearPath(contact.add(0, 0.3, 0), victim)) {
                continue;
            }
            if (victim.hurtOrSimulate(damageSources().mobProjectile(this, owner), owner.attackDamage() * 0.7F)) {
                victim.setRemainingFireTicks(3 * 20);
                CindermawEntity.shove(victim, victim.position().subtract(contact), 0.5, 0.3);
            }
        }
        MagmaEffect.pool(owner, owner.groundAt(contact), 1.6F);
    }

    private void explodePlayerShell(Vec3 contact, Vec3 start, CompoundTag d) {
        boolean mega = d.getBooleanOr("Mega", false);
        float damage = d.getFloat("Dmg").orElse(6.0F);
        float radius = Math.max(1.8F, d.getFloat("Rad").orElse(2.5F));
        level().playSound(null, contact.x, contact.y, contact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, mega ? 2.4F : 1.5F, mega ? 0.72F : 1.08F);
        level().playSound(null, contact.x, contact.y, contact.z, SoundEvents.LAVA_POP, SoundSource.PLAYERS, mega ? 2.0F : 1.3F, 0.55F);
        if (!(getOwner() instanceof LivingEntity caster) || !caster.isAlive()) {
            return;
        }
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(contact, contact).inflate(radius + 0.6D))) {
            Vec3 center = victim.getBoundingBox().getCenter();
            double reach = radius + victim.getBbWidth() * 0.45D;
            if (!MagmaEffect.canHarmTarget(caster, victim) || center.distanceToSqr(contact) > reach * reach) {
                continue;
            }
            victim.invulnerableTime = 0;
            if (victim.hurtOrSimulate(damageSources().mobProjectile(this, caster), damage)) {
                victim.setRemainingFireTicks((mega ? 6 : 4) * 20);
                double resist = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                double kbScale = Math.max(0.0D, 1.0D - resist);
                Vec3 push = victim.position().subtract(contact);
                Vec3 horiz = new Vec3(push.x, 0.0D, push.z);
                horiz = horiz.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : horiz.normalize();
                double hStrength = (mega ? 0.65D : 0.35D) * kbScale;
                double vStrength = (mega ? 0.36D : 0.20D) * kbScale;
                victim.setDeltaMovement(victim.getDeltaMovement().add(horiz.x * hStrength, vStrength, horiz.z * hStrength));
                victim.hurtMarked = true;
            }
        }
        Vec3 ground = MagmaEffect.findGroundAt(caster, contact);
        if (mega) {
            MagmaEffect.shockwave(caster, ground, 5.2F, 1);
            Vec3 travel = contact.subtract(start);
            float yaw = (travel.horizontalDistanceSqr() > 1.0E-3D)
                    ? (float) (Mth.atan2(-travel.x, travel.z) * Mth.RAD_TO_DEG)
                    : caster.getYRot();
            MagmaEffect.fissure(caster, ground, yaw, 3, 8.0F, damage * 0.60F);
            MagmaEffect.playerPool(caster, ground, 3.0F, damage * 0.24F, 80);
        } else {
            MagmaEffect.playerPool(caster, ground, 1.7F, damage * 0.20F, 48);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
