package com.hp.bossexpansion.zephyrarch.registry;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrBladeEntity;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrChargeEntity;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrTwisterEntity;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrarchEntity;
import com.hp.bossexpansion.zephyrarch.item.DescItem;
import com.hp.bossexpansion.zephyrarch.item.SovereignGauntletItem;
import com.hp.bossexpansion.zephyrarch.item.TempestCrestItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ZephyrarchRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BossExpansion.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BossExpansion.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ZephyrarchEntity>> ZEPHYRARCH = ENTITIES.register("zephyrarch",
            () -> EntityType.Builder.of(ZephyrarchEntity::new, MobCategory.MONSTER)
                    .sized(1.8F, 3.4F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(BossExpansion.MOD_ID + ":zephyrarch"));

    public static final DeferredHolder<EntityType<?>, EntityType<ZephyrChargeEntity>> ZEPHYR_CHARGE = ENTITIES.register("zephyr_charge",
            () -> EntityType.Builder.<ZephyrChargeEntity>of(ZephyrChargeEntity::new, MobCategory.MISC)
                    .sized(0.55F, 0.55F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(BossExpansion.MOD_ID + ":zephyr_charge"));

    public static final DeferredHolder<EntityType<?>, EntityType<ZephyrBladeEntity>> ZEPHYR_BLADE = ENTITIES.register("zephyr_blade",
            () -> EntityType.Builder.<ZephyrBladeEntity>of(ZephyrBladeEntity::new, MobCategory.MISC)
                    .sized(1.2F, 0.4F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(BossExpansion.MOD_ID + ":zephyr_blade"));

    public static final DeferredHolder<EntityType<?>, EntityType<ZephyrTwisterEntity>> ZEPHYR_TWISTER = ENTITIES.register("zephyr_twister",
            () -> EntityType.Builder.<ZephyrTwisterEntity>of(ZephyrTwisterEntity::new, MobCategory.MISC)
                    .sized(1.4F, 2.8F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(BossExpansion.MOD_ID + ":zephyr_twister"));

    public static final DeferredHolder<Item, Item> ZEPHYRARCH_SPAWN_EGG = ITEMS.register("zephyrarch_spawn_egg",
            () -> new DeferredSpawnEggItem(ZEPHYRARCH, 0x576C9D, 0x8AE6FF, new Item.Properties()));

    public static final DeferredHolder<Item, Item> TEMPEST_CREST = ITEMS.register("tempest_crest",
            () -> new TempestCrestItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, Item> ZEPHYR_CORE = ITEMS.register("zephyr_core",
            () -> new DescItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "item.hp_boss_expansion.zephyr_core.desc"));

    public static final DeferredHolder<Item, Item> SOVEREIGN_GAUNTLET = ITEMS.register("sovereign_gauntlet",
            () -> new SovereignGauntletItem(
                    Tiers.NETHERITE,
                    new Item.Properties()
                            .rarity(Rarity.EPIC)
                            .fireResistant()
                            .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.0F))
            ));

    private ZephyrarchRegistry() {
    }
}
