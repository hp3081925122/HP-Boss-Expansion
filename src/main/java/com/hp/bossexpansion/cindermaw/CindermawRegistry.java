package com.hp.bossexpansion.cindermaw;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.entity.CindermawEntity;
import com.hp.bossexpansion.cindermaw.entity.LavaBomb;
import com.hp.bossexpansion.cindermaw.entity.MagmaEffect;
import com.hp.bossexpansion.cindermaw.item.CindermawArmorMaterial;
import com.hp.bossexpansion.cindermaw.item.CindermawCoreItem;
import com.hp.bossexpansion.cindermaw.item.CindermawPauldronItem;
import com.hp.bossexpansion.zephyrarch.item.DescItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 集中注册熔渊炉心首领、投射物与专属神装
public final class CindermawRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BossExpansion.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BossExpansion.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CindermawEntity>> CINDERMAW = ENTITIES.register("cindermaw",
            () -> EntityType.Builder.of(CindermawEntity::new, MobCategory.MONSTER)
                    .sized(3.2F, 3.3F).fireImmune().clientTrackingRange(10).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "cindermaw"))));

    public static final DeferredHolder<EntityType<?>, EntityType<LavaBomb>> LAVA_BOMB = ENTITIES.register("lava_bomb",
            () -> EntityType.Builder.<LavaBomb>of(LavaBomb::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).fireImmune().clientTrackingRange(12).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "lava_bomb"))));

    public static final DeferredHolder<EntityType<?>, EntityType<MagmaEffect>> MAGMA_EFFECT = ENTITIES.register("magma_effect",
            () -> EntityType.Builder.<MagmaEffect>of(MagmaEffect::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).fireImmune().clientTrackingRange(12).updateInterval(5)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "magma_effect"))));

    public static final DeferredHolder<Item, Item> CINDERMAW_SPAWN_EGG = ITEMS.registerItem("cindermaw_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(CINDERMAW.get())));

    public static final DeferredHolder<Item, Item> CINDERMAW_CORE = ITEMS.registerItem("cindermaw_core",
            properties -> new CindermawCoreItem(properties.stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<Item, Item> CINDERMAW_HEART = ITEMS.registerItem("cindermaw_heart",
            properties -> new DescItem(properties.rarity(Rarity.EPIC).fireResistant(), "item.hp_boss_expansion.cindermaw_heart.desc"));

    public static final DeferredHolder<Item, Item> CINDERMAW_CHESTPLATE = ITEMS.registerItem("cindermaw_chestplate",
            properties -> new CindermawPauldronItem(properties.humanoidArmor(CindermawArmorMaterial.MATERIAL, ArmorType.CHESTPLATE).rarity(Rarity.EPIC).fireResistant()));

    private CindermawRegistry() {
    }
}
