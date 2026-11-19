package com.hp.bossexpansion.gildedtyrant.entity;

public enum GildedTyrantAttack {
    NONE(0, null),
    TYRANT_COMBO(38, "tyrant_combo"),
    BEARDED_HOOK_SWEEP(42, "bearded_hook_sweep"),
    PAULDRON_BULL_RUSH(46, "pauldron_bull_rush"),
    GILDED_FISSURE_SLAM(46, "gilded_fissure_slam"),
    GOLD_FEVER_ROAR(44, "gold_fever_roar"),
    BASTION_ANVIL_LEAP(52, "bastion_anvil_leap"),
    GREED_CORE_OVERDRIVE(50, "greed_core_overdrive"),
    STAGGER(32, "stagger"),
    WHIRLING_GREATAXE_BOOMERANG(56, "whirling_greataxe_boomerang"),
    GOLDEN_CHAIN_SHACKLE(46, "golden_chain_shackle"),
    MOLTEN_GOLD_RAY(52, "molten_gold_ray"),
    GILDED_OBELISK_SANCTUM(42, "gilded_obelisk_sanctum"),
    CRIMSON_WHIRLWIND_CLEAVE(56, "crimson_whirlwind_cleave"),
    CATACLYSMIC_GOLD_RAIN(68, "cataclysmic_gold_rain");

    private static final GildedTyrantAttack[] BY_ID = values();

    public final int id;
    public final int duration;
    public final String animation;

    GildedTyrantAttack(int duration, String animation) {
        this.id = this.ordinal();
        this.duration = duration;
        this.animation = animation == null ? null : "animation.gilded_tyrant." + animation;
    }

    public static GildedTyrantAttack byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return NONE;
        }
        return BY_ID[id];
    }
}
