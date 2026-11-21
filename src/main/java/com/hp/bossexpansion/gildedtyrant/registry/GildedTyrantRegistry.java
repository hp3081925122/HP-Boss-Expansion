package com.hp.bossexpansion.gildedtyrant.registry;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedObeliskEntity;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.hp.bossexpansion.gildedtyrant.entity.ThrownGreataxeEntity;
import com.hp.bossexpansion.gildedtyrant.item.BastionEdictItem;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.hp.bossexpansion.zephyrarch.item.DescItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 鎏金暴君实体与物品注册表
public final class GildedTyrantRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, BossExpansion.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BossExpansion.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GildedTyrantEntity>> GILDED_TYRANT = ENTITIES.register("gilded_tyrant",
            () -> EntityType.Builder.of(GildedTyrantEntity::new, MobCategory.MONSTER)
                    .sized(1.85F, 3.35F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_tyrant"))));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownGreataxeEntity>> THROWN_GREATAXE = ENTITIES.register("thrown_greataxe",
            () -> EntityType.Builder.<ThrownGreataxeEntity>of(ThrownGreataxeEntity::new, MobCategory.MISC)
                    .sized(1.4F, 0.7F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "thrown_greataxe"))));

    public static final DeferredHolder<EntityType<?>, EntityType<GildedObeliskEntity>> GILDED_OBELISK = ENTITIES.register("gilded_obelisk",
            () -> EntityType.Builder.<GildedObeliskEntity>of(GildedObeliskEntity::new, MobCategory.MONSTER)
                    .sized(0.85F, 2.2F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_obelisk"))));

    public static final DeferredHolder<Item, Item> GILDED_TYRANT_SPAWN_EGG = ITEMS.registerItem("gilded_tyrant_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(GILDED_TYRANT.get())));

    public static final DeferredHolder<Item, Item> BASTION_EDICT = ITEMS.registerItem("bastion_edict",
            properties -> new BastionEdictItem(properties.stacksTo(16).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredHolder<Item, Item> GILDED_CORE = ITEMS.registerItem("gilded_core",
            properties -> new DescItem(properties.rarity(Rarity.EPIC).fireResistant(), "item.hp_boss_expansion.gilded_core.desc"));

    public static final DeferredHolder<Item, Item> GILDED_GREATAXE = ITEMS.registerItem("gilded_greataxe",
            properties -> new GildedGreataxeItem(
                    properties.axe(ToolMaterial.NETHERITE, 7.0F, -2.9F)
                            .repairable(GILDED_CORE.get())
                            .rarity(Rarity.EPIC)
                            .fireResistant()
            ));

    private GildedTyrantRegistry() {
    }
}
