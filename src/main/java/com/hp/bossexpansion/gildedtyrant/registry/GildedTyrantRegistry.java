package com.hp.bossexpansion.gildedtyrant.registry;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedObeliskEntity;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.hp.bossexpansion.gildedtyrant.entity.ThrownGreataxeEntity;
import com.hp.bossexpansion.gildedtyrant.item.BastionEdictItem;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.hp.bossexpansion.zephyrarch.item.DescItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GildedTyrantRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BossExpansion.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BossExpansion.MOD_ID);

    public static final RegistryObject<EntityType<GildedTyrantEntity>> GILDED_TYRANT = ENTITIES.register("gilded_tyrant",
            () -> EntityType.Builder.of(GildedTyrantEntity::new, MobCategory.MONSTER)
                    .sized(1.85F, 3.35F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(BossExpansion.MOD_ID + ":gilded_tyrant"));

    public static final RegistryObject<EntityType<ThrownGreataxeEntity>> THROWN_GREATAXE = ENTITIES.register("thrown_greataxe",
            () -> EntityType.Builder.<ThrownGreataxeEntity>of(ThrownGreataxeEntity::new, MobCategory.MISC)
                    .sized(1.4F, 0.7F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(BossExpansion.MOD_ID + ":thrown_greataxe"));

    public static final RegistryObject<EntityType<GildedObeliskEntity>> GILDED_OBELISK = ENTITIES.register("gilded_obelisk",
            () -> EntityType.Builder.<GildedObeliskEntity>of(GildedObeliskEntity::new, MobCategory.MONSTER)
                    .sized(0.85F, 2.2F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(BossExpansion.MOD_ID + ":gilded_obelisk"));

    public static final RegistryObject<Item> GILDED_TYRANT_SPAWN_EGG = ITEMS.register("gilded_tyrant_spawn_egg",
            () -> new ForgeSpawnEggItem(GILDED_TYRANT, 0xC78B6B, 0xFAD241, new Item.Properties()));

    public static final RegistryObject<Item> BASTION_EDICT = ITEMS.register("bastion_edict",
            () -> new BastionEdictItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<Item> GILDED_CORE = ITEMS.register("gilded_core",
            () -> new DescItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "item.hp_boss_expansion.gilded_core.desc"));

    public static final RegistryObject<Item> GILDED_GREATAXE = ITEMS.register("gilded_greataxe",
            () -> new GildedGreataxeItem(
                    Tiers.NETHERITE,
                    7.0F,
                    -2.9F,
                    new Item.Properties()
                            .rarity(Rarity.EPIC)
                            .fireResistant()
            ));

    private GildedTyrantRegistry() {
    }
}
