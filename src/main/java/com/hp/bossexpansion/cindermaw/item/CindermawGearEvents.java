package com.hp.bossexpansion.cindermaw.item;

import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.cindermaw.network.PauldronNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// 熔渊重炮肩甲事件：火焰与岩浆免疫、受击喷焰反击、近战炉心协射与蓄力状态同步
public final class CindermawGearEvents {
    private CindermawGearEvents() {
    }

    // 火焰类伤害直接取消，并持续熄灭身上的火焰
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!event.getSource().is(DamageTypeTags.IS_FIRE) || !CindermawPauldronItem.isWearing(wearer)) {
            return;
        }
        event.setCanceled(true);
        wearer.clearFire();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }

        // 穿戴者近战命中目标时，积攒炉心热量并触发肩炮预判协射（仅统计直接近战攻击，排除投射物递归）
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getSource().getDirectEntity() == attacker
                && CindermawPauldronItem.isWearing(attacker)
                && victim != attacker) {
            CindermawPauldronItem.onWearerMeleeHit(attacker, victim);
        }

        // 穿戴者被近身攻击时，35% 概率由胸前炉心喷发烈焰反击
        if (!CindermawPauldronItem.isWearing(victim)) {
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker) || attacker == victim || attacker.distanceTo(victim) > 4.0F) {
            return;
        }
        if (victim.getRandom().nextFloat() >= 0.35F) {
            return;
        }
        attacker.setSecondsOnFire(4);
        attacker.hurt(victim.damageSources().thorns(victim), 4.0F);
        if (victim.level() instanceof ServerLevel server) {
            Vec3 from = victim.position().add(0, victim.getBbHeight() * 0.65, 0);
            Vec3 dir = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0).subtract(from);
            if (dir.lengthSqr() > 1.0E-4D) {
                dir = dir.normalize();
            }
            for (int i = 0; i < 10; i++) {
                server.sendParticles(CindermawParticles.BLAZE_FLAME.get(), from.x, from.y, from.z, 0, dir.x, dir.y, dir.z, 0.15 + i * 0.03);
            }
            server.sendParticles(CindermawParticles.EMBER.get(), from.x, from.y, from.z, 8, 0.3, 0.3, 0.3, 0.08);
            server.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer sp) {
            if (sp.getRemainingFireTicks() > 0 && CindermawPauldronItem.isWearing(sp)) {
                sp.clearFire();
            }
            CindermawPauldronItem.tickServerPlayer(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CindermawPauldronItem.clearPlayerState(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            if (CindermawPauldronItem.isServerCharging(target)) {
                PauldronNetwork.sendChargeStateToPlayer(viewer, target, true);
            }
        }
    }
}
