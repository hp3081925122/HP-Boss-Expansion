package com.hp.bossexpansion.api;

import com.hp.bossexpansion.config.BossExpansionConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

    static boolean isValidCricketTarget(Mob self, LivingEntity target) {
        if (!BossExpansionConfig.CRICKET_MODE_ENABLED.get()) {
            return false;
        }
        if (target == null || target == self || !target.isAlive() || target instanceof Player) {
            return false;
        }
        return !self.isAlliedTo(target) && target.attackable();
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
