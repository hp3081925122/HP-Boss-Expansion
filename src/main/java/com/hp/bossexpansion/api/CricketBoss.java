package com.hp.bossexpansion.api;

import com.hp.bossexpansion.config.BossExpansionConfig;
import com.hp.bossexpansion.gildedtyrant.entity.GildedObeliskEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public interface CricketBoss {
    default float getNonPlayerDamageCap() {
        if (this instanceof LivingEntity living) {
            return Math.max(1.0F, (float) (living.getMaxHealth() * BossExpansionConfig.NON_PLAYER_DAMAGE_CAP_RATIO.get()));
        }
        return 1.0F;
    }

    default boolean hasSecondPhaseRebirth() {
        return false;
    }

    default boolean isSecondPhaseActive() {
        return true;
    }

    default void triggerPhase2Rebirth() {
    }

    default boolean isOwnedSummon(LivingEntity candidate) {
        return false;
    }

    static boolean isValidCricketTarget(Mob self, LivingEntity target) {
        if (!BossExpansionConfig.CRICKET_MODE_ENABLED.get()) {
            return false;
        }
        if (target == null || target == self || !target.isAlive() || target instanceof Player || target instanceof ArmorStand) {
            return false;
        }
        // 排除同类首领与自身召唤物，防止首领锁死自身金碑或同类空挥
        if (target.getType() == self.getType()) {
            return false;
        }
        if (self instanceof CricketBoss boss && boss.isOwnedSummon(target)) {
            return false;
        }
        if (target instanceof GildedObeliskEntity obelisk && obelisk.isOwnedBy(self)) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && (ownable.getOwner() == self || self.getUUID().equals(ownable.getOwnerUUID()))) {
            return false;
        }
        return !self.isAlliedTo(target) && !target.isAlliedTo(self) && target.attackable() && self.canAttack(target);
    }

    static boolean hasNearbySurvivalPlayer(LivingEntity self) {
        double range = BossExpansionConfig.SURVIVAL_PLAYER_CHECK_RANGE.get();
        for (Player player : self.level().players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(self) <= range * range) {
                return true;
            }
        }
        return false;
    }
}
