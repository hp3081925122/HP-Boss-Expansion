package com.hp.bossexpansion.cindermaw;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.entity.CindermawEntity;
import com.hp.bossexpansion.cindermaw.entity.LavaBomb;
import com.hp.bossexpansion.cindermaw.entity.MagmaEffect;
import com.hp.bossexpansion.cindermaw.item.CindermawArmorMaterial;
import com.hp.bossexpansion.cindermaw.item.CindermawCoreItem;
import com.hp.bossexpansion.cindermaw.item.CindermawPauldronItem;
import com.hp.bossexpansion.zephyrarch.item.DescItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 集中注册熔渊炉心首领、熔岩弹、地面特效实体与专属物品，公共注册类不引用任何客户端类
public final class CindermawRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BossExpansion.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BossExpansion.MOD_ID);

    public static final RegistryObject<EntityType<CindermawEntity>> CINDERMAW = ENTITIES.register("cindermaw",
            () -> EntityType.Builder.of(CindermawEntity::new, MobCategory.MONSTER)
                    .sized(3.2F, 3.3F).fireImmune().clientTrackingRange(10).updateInterval(2)
                    .build(BossExpansion.MOD_ID + ":cindermaw"));

    public static final RegistryObject<EntityType<LavaBomb>> LAVA_BOMB = ENTITIES.register("lava_bomb",
            () -> EntityType.Builder.<LavaBomb>of(LavaBomb::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).fireImmune().clientTrackingRange(12).updateInterval(1)
                    .build(BossExpansion.MOD_ID + ":lava_bomb"));

    public static final RegistryObject<EntityType<MagmaEffect>> MAGMA_EFFECT = ENTITIES.register("magma_effect",
            () -> EntityType.Builder.<MagmaEffect>of(MagmaEffect::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).fireImmune().clientTrackingRange(12).updateInterval(5)
                    .build(BossExpansion.MOD_ID + ":magma_effect"));

    public static final RegistryObject<Item> CINDERMAW_SPAWN_EGG = ITEMS.register("cindermaw_spawn_egg",
            () -> new ForgeSpawnEggItem(CINDERMAW, 0x252933, 0xE65A1B, new Item.Properties()));

    public static final RegistryObject<Item> CINDERMAW_CORE = ITEMS.register("cindermaw_core",
            () -> new CindermawCoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> CINDERMAW_HEART = ITEMS.register("cindermaw_heart",
            () -> new DescItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "item.hp_boss_expansion.cindermaw_heart.desc"));

    // 熔渊重炮肩甲：穿戴于胸甲槽的三维重装双肩火炮护甲
    public static final RegistryObject<Item> CINDERMAW_CHESTPLATE = ITEMS.register("cindermaw_chestplate",
            () -> new CindermawPauldronItem(CindermawArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    private CindermawRegistry() {
    }
}
