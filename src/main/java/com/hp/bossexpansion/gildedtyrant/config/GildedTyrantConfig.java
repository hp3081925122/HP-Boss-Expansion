package com.hp.bossexpansion.gildedtyrant.config;

import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GildedTyrantConfig {
    public static ModConfigSpec.DoubleValue MAX_HEALTH;
    public static ModConfigSpec.DoubleValue ATTACK_DAMAGE;
    public static ModConfigSpec.DoubleValue ARMOR;
    public static ModConfigSpec.DoubleValue ARMOR_TOUGHNESS;
    public static ModConfigSpec.DoubleValue MOVEMENT_SPEED;
    public static ModConfigSpec.DoubleValue FOLLOW_RANGE;

    public static ModConfigSpec.BooleanValue TWO_LIFE_REBIRTH;
    public static ModConfigSpec.DoubleValue PHASE2_HEALTH_RATIO;
    public static ModConfigSpec.DoubleValue CATACLYSMIC_RAIN_HEALTH_RATIO;
    public static ModConfigSpec.DoubleValue OBELISK_MAX_HEALTH;
    public static ModConfigSpec.DoubleValue OBELISK_HEAL_RATIO;
    public static ModConfigSpec.DoubleValue KNOCKBACK_SCALE;

    public static ModConfigSpec.DoubleValue COMBO_PUNCH_MULTIPLIER;
    public static ModConfigSpec.DoubleValue COMBO_AXE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue HOOK_SWEEP_MULTIPLIER;
    public static ModConfigSpec.DoubleValue HOOK_STOMP_MULTIPLIER;
    public static ModConfigSpec.DoubleValue BULL_RUSH_MULTIPLIER;
    public static ModConfigSpec.DoubleValue BULL_UPPERCUT_MULTIPLIER;
    public static ModConfigSpec.DoubleValue FISSURE_IMPACT_MULTIPLIER;
    public static ModConfigSpec.DoubleValue FISSURE_WAVE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue ROAR_WAVE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue GOLD_SPIKE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue ANVIL_INNER_MULTIPLIER;
    public static ModConfigSpec.DoubleValue ANVIL_RING_MULTIPLIER;
    public static ModConfigSpec.DoubleValue OVERDRIVE_BURST_MULTIPLIER;

    public static ModConfigSpec.DoubleValue BOOMERANG_AXE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue BOOMERANG_BRAWL_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CHAIN_HOOK_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CHAIN_CRUSH_MULTIPLIER;
    public static ModConfigSpec.DoubleValue MOLTEN_RAY_TICK_MULTIPLIER;
    public static ModConfigSpec.DoubleValue OBELISK_ARC_MULTIPLIER;
    public static ModConfigSpec.DoubleValue WHIRLWIND_SPIN_MULTIPLIER;
    public static ModConfigSpec.DoubleValue WHIRLWIND_FINISHER_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CATACLYSMIC_RAY_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CATACLYSMIC_DROP_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CATACLYSMIC_BURST_MULTIPLIER;
    public static ModConfigSpec.DoubleValue PLAYER_WHIRLWIND_MULTIPLIER;

    private static final ModConfigSpec.IntValue[] COOLDOWNS = new ModConfigSpec.IntValue[GildedTyrantAttack.values().length];

    public static void register(ModConfigSpec.Builder builder) {
        builder.comment("鎏金暴君基础属性配置 (仅在实体首次生成时通过NBT标记记录并应用一次，不会覆盖后续指令或KubeJS修改) / Gilded Tyrant base attribute settings (applied once on initial spawn via NBT flag)")
                .push("attributes");
        MAX_HEALTH = builder
                .comment("鎏金暴君基础最大生命值 / Base max health of Gilded Tyrant")
                .defineInRange("maxHealth", 360.0D, 20.0D, 20000.0D);
        ATTACK_DAMAGE = builder
                .comment("鎏金暴君基础攻击力 (所有技能伤害均以此基础攻击力乘以对应倍率计算) / Base attack damage of Gilded Tyrant (all skill damages scale with this value)")
                .defineInRange("attackDamage", 14.0D, 1.0D, 2000.0D);
        ARMOR = builder
                .comment("鎏金暴君基础护甲值 / Base armor of Gilded Tyrant")
                .defineInRange("armor", 12.0D, 0.0D, 100.0D);
        ARMOR_TOUGHNESS = builder
                .comment("鎏金暴君基础盔甲韧性 / Base armor toughness of Gilded Tyrant")
                .defineInRange("armorToughness", 6.0D, 0.0D, 100.0D);
        MOVEMENT_SPEED = builder
                .comment("鎏金暴君基础移动速度 / Base movement speed of Gilded Tyrant")
                .defineInRange("movementSpeed", 0.28D, 0.05D, 1.0D);
        FOLLOW_RANGE = builder
                .comment("鎏金暴君索敌追踪距离 / Follow range of Gilded Tyrant")
                .defineInRange("followRange", 44.0D, 16.0D, 128.0D);
        builder.pop();

        builder.comment("鎏金暴君阶段与技能倍率配置 / Phase mechanics and skill damage multipliers")
                .push("combat");
        TWO_LIFE_REBIRTH = builder
                .comment("是否采用两条命重生进入二阶段 (开启时第一管血归零回满进入二阶段；关闭时按 phase2HealthRatio 百分比血量转二阶段) / Enable two-life rebirth for Phase 2 (when false, Phase 2 triggers at phase2HealthRatio)")
                .define("twoLifeRebirth", true);
        PHASE2_HEALTH_RATIO = builder
                .comment("当 twoLifeRebirth 关闭时触发二阶段的生命值百分比阈值 (0.50 = 50%血量) / Health percentage threshold for Phase 2 when twoLifeRebirth is false")
                .defineInRange("phase2HealthRatio", 0.50D, 0.10D, 0.95D);
        CATACLYSMIC_RAIN_HEALTH_RATIO = builder
                .comment("二阶段解锁终极技能「黄金审判·裂狱天陨」的生命值百分比阈值 (0.75 = 75%血量以下) / Phase 2 health percentage threshold to unlock Cataclysmic Gold Rain")
                .defineInRange("cataclysmicRainHealthRatio", 0.75D, 0.05D, 1.00D);
        OBELISK_MAX_HEALTH = builder
                .comment("贪婪金碑生命值 / Max health of summoned Gilded Obelisks")
                .defineInRange("obeliskMaxHealth", 24.0D, 4.0D, 1000.0D);
        OBELISK_HEAL_RATIO = builder
                .comment("每座存活贪婪金碑每次脉冲恢复首领最大生命值的比例 / Max health heal ratio per pulse from each active Gilded Obelisk")
                .defineInRange("obeliskHealRatio", 0.015D, 0.0D, 0.20D);
        KNOCKBACK_SCALE = builder
                .comment("技能击退与拉拽全局缩放系数 (实际位移还会受目标击退抗性减免) / Global knockback scale for skills (also reduced by target knockback resistance)")
                .defineInRange("knockbackScale", 1.00D, 0.0D, 3.0D);

        COMBO_PUNCH_MULTIPLIER = mult(builder, "comboPunchMultiplier", "暴君拳斧二连-破盾直拳 / Tyrant Combo punch", 0.85D);
        COMBO_AXE_MULTIPLIER = mult(builder, "comboAxeMultiplier", "暴君拳斧二连-斜劈巨斧 / Tyrant Combo axe cleave", 1.25D);
        HOOK_SWEEP_MULTIPLIER = mult(builder, "hookSweepMultiplier", "胡须斧横扫钩拽-横扫 / Bearded Hook Sweep slash", 1.10D);
        HOOK_STOMP_MULTIPLIER = mult(builder, "hookStompMultiplier", "胡须斧横扫钩拽-震踏 / Bearded Hook Sweep stomp", 0.65D);
        BULL_RUSH_MULTIPLIER = mult(builder, "bullRushMultiplier", "黑石刺肩蛮牛冲撞-路径 / Pauldron Bull Rush path", 1.30D);
        BULL_UPPERCUT_MULTIPLIER = mult(builder, "bullUppercutMultiplier", "黑石刺肩蛮牛冲撞-挑拳 / Pauldron Bull Rush uppercut", 0.80D);
        FISSURE_IMPACT_MULTIPLIER = mult(builder, "fissureImpactMultiplier", "裂金碎地劈-落点 / Gilded Fissure Slam impact", 1.50D);
        FISSURE_WAVE_MULTIPLIER = mult(builder, "fissureWaveMultiplier", "裂金碎地劈-地裂波 / Gilded Fissure Slam wave", 1.05D);
        ROAR_WAVE_MULTIPLIER = mult(builder, "roarWaveMultiplier", "贪婪战吼-震波 / Gold Fever Roar shockwave", 0.60D);
        GOLD_SPIKE_MULTIPLIER = mult(builder, "goldSpikeMultiplier", "贪婪战吼-下界金笋刺 / Gold Fever Roar gold spike", 0.95D);
        ANVIL_INNER_MULTIPLIER = mult(builder, "anvilInnerMultiplier", "堡垒重砧跳劈-内圈 / Bastion Anvil Leap inner slam", 1.45D);
        ANVIL_RING_MULTIPLIER = mult(builder, "anvilRingMultiplier", "堡垒重砧跳劈-外圈震环 / Bastion Anvil Leap shock ring", 0.85D);
        OVERDRIVE_BURST_MULTIPLIER = mult(builder, "overdriveBurstMultiplier", "贪婪双核超载爆发 / Greed Core Overdrive burst", 0.80D);

        BOOMERANG_AXE_MULTIPLIER = mult(builder, "boomerangAxeMultiplier", "裂狱回旋飞斧每次切割 / Whirling Greataxe Boomerang hit", 1.15D);
        BOOMERANG_BRAWL_MULTIPLIER = mult(builder, "boomerangBrawlMultiplier", "裂狱回旋飞斧期间追猎重拳 / Brawl punch while axe airborne", 0.90D);
        CHAIN_HOOK_MULTIPLIER = mult(builder, "chainHookMultiplier", "黄金锁链擒拿-钩爪 / Golden Chain Shackle hook", 0.70D);
        CHAIN_CRUSH_MULTIPLIER = mult(builder, "chainCrushMultiplier", "黄金锁链擒拿-崩拳 / Golden Chain Shackle crush punch", 1.55D);
        MOLTEN_RAY_TICK_MULTIPLIER = mult(builder, "moltenRayTickMultiplier", "熔金贯日炮单次脉冲 / Molten Gold Ray per pulse", 0.45D);
        OBELISK_ARC_MULTIPLIER = mult(builder, "obeliskArcMultiplier", "贪婪金碑电弧切割 / Gilded Obelisk arc beam", 0.75D);
        WHIRLWIND_SPIN_MULTIPLIER = mult(builder, "whirlwindSpinMultiplier", "裂狱狂岚每圈回旋斩 / Crimson Whirlwind Cleave per spin", 0.85D);
        WHIRLWIND_FINISHER_MULTIPLIER = mult(builder, "whirlwindFinisherMultiplier", "裂狱狂岚终结裂地劈 / Crimson Whirlwind Cleave finisher", 1.40D);
        CATACLYSMIC_RAY_MULTIPLIER = mult(builder, "cataclysmicRayMultiplier", "黄金审判-米字黑石地裂 / Cataclysmic Gold Rain fissure", 1.20D);
        CATACLYSMIC_DROP_MULTIPLIER = mult(builder, "cataclysmicDropMultiplier", "黄金审判-天陨金斧 / Cataclysmic Gold Rain falling axe", 1.35D);
        CATACLYSMIC_BURST_MULTIPLIER = mult(builder, "cataclysmicBurstMultiplier", "黄金审判-拔斧终结爆破 / Cataclysmic Gold Rain final burst", 1.50D);
        PLAYER_WHIRLWIND_MULTIPLIER = mult(builder, "playerWhirlwindMultiplier", "玩家手持鎏金裂狱巨斧长按右键每圈旋风斩 / Player Gilded Greataxe right-click whirlwind per spin", 0.85D);
        builder.pop();

        builder.comment("鎏金暴君技能冷却 (刻，20刻=1秒) / Gilded Tyrant skill cooldowns in ticks")
                .push("cooldowns");
        cd(builder, GildedTyrantAttack.TYRANT_COMBO, "tyrantCombo", 50);
        cd(builder, GildedTyrantAttack.BEARDED_HOOK_SWEEP, "beardedHookSweep", 90);
        cd(builder, GildedTyrantAttack.PAULDRON_BULL_RUSH, "pauldronBullRush", 130);
        cd(builder, GildedTyrantAttack.GILDED_FISSURE_SLAM, "gildedFissureSlam", 140);
        cd(builder, GildedTyrantAttack.GOLD_FEVER_ROAR, "goldFeverRoar", 220);
        cd(builder, GildedTyrantAttack.BASTION_ANVIL_LEAP, "bastionAnvilLeap", 200);
        cd(builder, GildedTyrantAttack.WHIRLING_GREATAXE_BOOMERANG, "whirlingGreataxeBoomerang", 180);
        cd(builder, GildedTyrantAttack.GOLDEN_CHAIN_SHACKLE, "goldenChainShackle", 160);
        cd(builder, GildedTyrantAttack.MOLTEN_GOLD_RAY, "moltenGoldRay", 200);
        cd(builder, GildedTyrantAttack.GILDED_OBELISK_SANCTUM, "gildedObeliskSanctum", 320);
        cd(builder, GildedTyrantAttack.CRIMSON_WHIRLWIND_CLEAVE, "crimsonWhirlwindCleave", 170);
        cd(builder, GildedTyrantAttack.CATACLYSMIC_GOLD_RAIN, "cataclysmicGoldRain", 360);
        builder.pop();
    }

    private static ModConfigSpec.DoubleValue mult(ModConfigSpec.Builder b, String key, String name, double def) {
        return b.comment(name + " 伤害倍率 (乘以基础攻击力) / damage multiplier (times base attack damage)")
                .defineInRange(key, def, 0.0D, 20.0D);
    }

    private static void cd(ModConfigSpec.Builder b, GildedTyrantAttack attack, String key, int def) {
        COOLDOWNS[attack.id] = b.defineInRange(key, def, 0, 12000);
    }

    public static int getCooldown(GildedTyrantAttack attack) {
        ModConfigSpec.IntValue v = COOLDOWNS[attack.id];
        return v == null ? 0 : v.get();
    }

    private GildedTyrantConfig() {
    }
}
