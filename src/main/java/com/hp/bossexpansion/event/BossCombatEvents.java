package com.hp.bossexpansion.event;

import com.hp.bossexpansion.api.CricketBoss;
import com.hp.bossexpansion.config.BossExpansionConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.UUID;

public final class BossCombatEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String NBT_ANTI_HEAL_UNTIL = "hp_boss_anti_heal_until";

    private BossCombatEvents() {
    }

    public static void applyAntiHealIfEligible(Entity attacker, LivingEntity victim) {
        if (!BossExpansionConfig.CRICKET_MODE_ENABLED.get()) {
            return;
        }
        if (!(attacker instanceof CricketBoss) || victim instanceof Player || !victim.isAlive() || victim == attacker) {
            return;
        }
        if (victim.level().isClientSide) {
            return;
        }
        int duration = BossExpansionConfig.ANTI_HEAL_DURATION_TICKS.get();
        long until = victim.level().getGameTime() + duration;
        victim.getPersistentData().putLong(NBT_ANTI_HEAL_UNTIL, until);
        LOGGER.debug("CricketBoss [id={}] applied anti-heal to non-player entity {} for {} ticks", attacker.getId(), victim.getName().getString(), duration);
    }

    public static boolean isNonPlayerSourcedDamage(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Entity culprit = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (culprit == null && direct == null) {
            return false;
        }
        return !isPlayerOrOwnedByPlayer(culprit) && !isPlayerOrOwnedByPlayer(direct);
    }

    private static boolean isPlayerOrOwnedByPlayer(Entity entity) {
        Entity current = entity;
        for (int depth = 0; depth < 6 && current != null; depth++) {
            if (current instanceof Player) {
                return true;
            }
            if (current instanceof OwnableEntity ownable) {
                LivingEntity owner = ownable.getOwner();
                if (owner != null) {
                    current = owner;
                    continue;
                }
                UUID ownerId = ownable.getOwnerUUID();
                if (ownerId != null && current.level() instanceof ServerLevel serverLevel) {
                    if (serverLevel.getServer().getPlayerList().getPlayer(ownerId) != null) {
                        return true;
                    }
                    current = serverLevel.getEntity(ownerId);
                    continue;
                }
                return false;
            }
            if (current instanceof TraceableEntity traceable) {
                current = traceable.getOwner();
                continue;
            }
            return false;
        }
        return false;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof CricketBoss boss) || entity.level().isClientSide) {
            return;
        }
        if (!BossExpansionConfig.CRICKET_MODE_ENABLED.get()) {
            return;
        }
        if (isNonPlayerSourcedDamage(event.getSource())) {
            float cap = boss.getNonPlayerDamageCap();
            if (event.getAmount() > cap * 6.0F) {
                event.setAmount(cap * 6.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        if (victim instanceof CricketBoss boss && BossExpansionConfig.CRICKET_MODE_ENABLED.get()) {
            if (isNonPlayerSourcedDamage(event.getSource())) {
                float cap = boss.getNonPlayerDamageCap();
                if (event.getAmount() > cap) {
                    LOGGER.debug("CricketBoss [id={}] cricket damage cap clamped non-player damage {} -> {}", victim.getId(), event.getAmount(), cap);
                    event.setAmount(cap);
                }
            }
        }
        if (!(victim instanceof Player)) {
            Entity culprit = event.getSource().getEntity();
            if (culprit instanceof CricketBoss) {
                applyAntiHealIfEligible(culprit, victim);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof CricketBoss boss) || entity.level().isClientSide) {
            return;
        }
        if (boss.hasSecondPhaseRebirth()
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !boss.isSecondPhaseActive()) {
            event.setCanceled(true);
            boss.triggerPhase2Rebirth();
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        boolean hasNbtLock = BossExpansionConfig.CRICKET_MODE_ENABLED.get()
                && entity.getPersistentData().getLong(NBT_ANTI_HEAL_UNTIL) > entity.level().getGameTime();
        if (hasNbtLock) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }
}
