package com.hp.bossexpansion.config;

import com.hp.bossexpansion.zephyrarch.config.ZephyrarchConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BossExpansionConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue CRICKET_MODE_ENABLED;
    public static final ModConfigSpec.DoubleValue NON_PLAYER_DAMAGE_CAP_RATIO;
    public static final ModConfigSpec.IntValue ANTI_HEAL_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue SURVIVAL_PLAYER_CHECK_RANGE;
    public static final ModConfigSpec.DoubleValue NO_PLAYER_COOLDOWN_SCALE;
    public static final ModConfigSpec.DoubleValue NO_PLAYER_ACTION_SPEED;

    public static final ModConfigSpec.DoubleValue CINDERMAW_MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue CINDERMAW_ATTACK_DAMAGE;
    public static final ModConfigSpec.DoubleValue CINDERMAW_ARMOR;
    public static final ModConfigSpec.DoubleValue CINDERMAW_MOVEMENT_SPEED;
    public static final ModConfigSpec.DoubleValue CINDERMAW_FOLLOW_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("通用斗蛐蛐模式配置 (适用于本模组所有首领：非玩家限伤、对非玩家禁疗、周围无生存玩家时高频出招) / Global Cricket (Mob Arena) mode settings (applies to all bosses in this mod)")
                .push("cricketMode");
        CRICKET_MODE_ENABLED = builder
                .comment("是否开启斗蛐蛐模式 (默认开启) / Enable Cricket Mode (enabled by default)")
                .define("enabled", true);
        NON_PLAYER_DAMAGE_CAP_RATIO = builder
                .comment("来自非玩家且主人不是玩家的有源伤害单次上限占最大生命值的比例 (0.04 = 单次最多扣除4%最大生命值；无源伤害不受限制) / Max single-hit damage ratio of max health from non-player / non-player-owned sources (0.04 = 4% max HP; sourceless damage is unaffected)")
                .defineInRange("nonPlayerDamageCapRatio", 0.04D, 0.005D, 1.00D);
        ANTI_HEAL_DURATION_TICKS = builder
                .comment("首领攻击非玩家生物时施加禁疗效果的持续时间 (刻，20刻=1秒) / Duration in ticks of the anti-heal effect applied when hitting non-player entities")
                .defineInRange("antiHealDurationTicks", 120, 20, 6000);
        SURVIVAL_PLAYER_CHECK_RANGE = builder
                .comment("检测周围是否存在生存/冒险模式玩家的半径 (格)，范围内无此类玩家时加快攻击频率 / Radius in blocks to check for survival/adventure players; attack frequency increases when none are present")
                .defineInRange("survivalPlayerCheckRange", 48.0D, 8.0D, 128.0D);
        NO_PLAYER_COOLDOWN_SCALE = builder
                .comment("周围没有生存模式玩家时的技能冷却与攻击间隔缩放系数 (越小越快，0.40 = 原冷却的40%) / Skill cooldown and attack interval scale when no survival player is nearby (lower is faster, 0.40 = 40% cooldown)")
                .defineInRange("noPlayerCooldownScale", 0.40D, 0.10D, 1.00D);
        NO_PLAYER_ACTION_SPEED = builder
                .comment("周围没有生存模式玩家时的攻击动作推进速率倍率 (1.45 = 1.45倍出招速度) / Attack action speed multiplier when no survival player is nearby (1.45 = 1.45x speed)")
                .defineInRange("noPlayerActionSpeed", 1.45D, 1.00D, 3.00D);
        builder.pop();

        builder.comment("熔渊炉心基础属性配置 (仅在实体首次生成时应用一次，不会覆盖后续指令或KubeJS修改) / Cindermaw base attribute settings (applied once on initial spawn, preserving subsequent command/KubeJS edits)")
                .push("cindermaw");
        CINDERMAW_MAX_HEALTH = builder
                .comment("熔渊炉心基础最大生命值 (单管血量；第一管血归零后回满血进入二阶段) / Base max health of Cindermaw (per phase; fatal blow in Phase 1 restores full HP and enters Phase 2)")
                .defineInRange("maxHealth", 340.0D, 20.0D, 20000.0D);
        CINDERMAW_ATTACK_DAMAGE = builder
                .comment("熔渊炉心基础攻击力 / Base attack damage of Cindermaw")
                .defineInRange("attackDamage", 14.0D, 1.0D, 2000.0D);
        CINDERMAW_ARMOR = builder
                .comment("熔渊炉心基础护甲值 / Base armor of Cindermaw")
                .defineInRange("armor", 10.0D, 0.0D, 100.0D);
        CINDERMAW_MOVEMENT_SPEED = builder
                .comment("熔渊炉心基础移动速度 / Base movement speed of Cindermaw")
                .defineInRange("movementSpeed", 0.24D, 0.05D, 1.0D);
        CINDERMAW_FOLLOW_RANGE = builder
                .comment("熔渊炉心索敌追踪距离 / Follow range of Cindermaw")
                .defineInRange("followRange", 40.0D, 16.0D, 128.0D);
        builder.pop();

        builder.comment("罡风君主配置 / Zephyrarch settings")
                .push("zephyrarch");
        ZephyrarchConfig.register(builder);
        builder.pop();

        SPEC = builder.build();
    }

    private BossExpansionConfig() {
    }
}
