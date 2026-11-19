package com.hp.bossexpansion.config;

import com.hp.bossexpansion.zephyrarch.config.ZephyrarchConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BossExpansionConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue CINDERMAW_MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue CINDERMAW_ATTACK_DAMAGE;
    public static final ModConfigSpec.DoubleValue CINDERMAW_ARMOR;
    public static final ModConfigSpec.DoubleValue CINDERMAW_MOVEMENT_SPEED;
    public static final ModConfigSpec.DoubleValue CINDERMAW_FOLLOW_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("熔渊炉心基础属性配置 (仅在实体首次生成时应用一次，不会覆盖后续指令或KubeJS修改) / Cindermaw base attribute settings (applied once on initial spawn, preserving subsequent command/KubeJS edits)")
                .push("cindermaw");
        CINDERMAW_MAX_HEALTH = builder
                .comment("熔渊炉心基础最大生命值 / Base max health of Cindermaw")
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
