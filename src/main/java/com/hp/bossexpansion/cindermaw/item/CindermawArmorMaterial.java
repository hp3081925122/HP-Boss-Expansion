package com.hp.bossexpansion.cindermaw.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;

// 炉心盔甲材质：比下界合金略强，只用于胸甲。
public final class CindermawArmorMaterial implements ArmorMaterial {
    public static final CindermawArmorMaterial INSTANCE = new CindermawArmorMaterial();

    private CindermawArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 495;
            case CHESTPLATE -> 640;
            case LEGGINGS -> 615;
            case BOOTS -> 555;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 4;
            case CHESTPLATE -> 9;
            case LEGGINGS -> 7;
            case BOOTS -> 4;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 16;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    // 用熔渊之心修复
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(CindermawRegistry.CINDERMAW_HEART.get());
    }

    // 对应 textures/models/armor/cindermaw_layer_1.png
    @Override
    public String getName() {
        return BossExpansion.MOD_ID + ":cindermaw";
    }

    @Override
    public float getToughness() {
        return 3.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.15F;
    }
}
