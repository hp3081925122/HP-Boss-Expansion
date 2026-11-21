package com.hp.bossexpansion;

import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.cindermaw.client.CinderFx;
import com.hp.bossexpansion.cindermaw.client.CindermawClientEvents;
import com.hp.bossexpansion.cindermaw.client.PauldronClientController;
import com.hp.bossexpansion.cindermaw.entity.CindermawEntity;
import com.hp.bossexpansion.cindermaw.item.CindermawArmorMaterial;
import com.hp.bossexpansion.cindermaw.item.CindermawGearEvents;
import com.hp.bossexpansion.cindermaw.network.PauldronNetwork;
import com.hp.bossexpansion.config.BossExpansionConfig;
import com.hp.bossexpansion.event.BossCombatEvents;
import com.hp.bossexpansion.gildedtyrant.client.GildedTyrantClientEvents;
import com.hp.bossexpansion.gildedtyrant.entity.GildedObeliskEntity;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.hp.bossexpansion.gildedtyrant.network.GildedWorldVfxPayload;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import com.hp.bossexpansion.zephyrarch.client.ZephyrarchClientEvents;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrarchEntity;
import com.hp.bossexpansion.zephyrarch.item.SovereignGauntletItem;
import com.hp.bossexpansion.zephyrarch.network.ZephyrWorldVfxPayload;
import com.hp.bossexpansion.zephyrarch.registry.ZephyrarchRegistry;
import com.hp.bossexpansion.zephyrarch.registry.ZephyrarchSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BossExpansion.MOD_ID)
public final class BossExpansion {
    public static final String MOD_ID = "hp_boss_expansion";

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hp_boss_expansion"))
            .icon(() -> CindermawRegistry.CINDERMAW_CORE.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(CindermawRegistry.CINDERMAW_CORE.get());
                output.accept(CindermawRegistry.CINDERMAW_HEART.get());
                output.accept(CindermawRegistry.CINDERMAW_CHESTPLATE.get());
                output.accept(CindermawRegistry.CINDERMAW_SPAWN_EGG.get());
                output.accept(ZephyrarchRegistry.TEMPEST_CREST.get());
                output.accept(ZephyrarchRegistry.ZEPHYR_CORE.get());
                output.accept(ZephyrarchRegistry.SOVEREIGN_GAUNTLET.get());
                output.accept(ZephyrarchRegistry.ZEPHYRARCH_SPAWN_EGG.get());
                output.accept(GildedTyrantRegistry.BASTION_EDICT.get());
                output.accept(GildedTyrantRegistry.GILDED_CORE.get());
                output.accept(GildedTyrantRegistry.GILDED_GREATAXE.get());
                output.accept(GildedTyrantRegistry.GILDED_TYRANT_SPAWN_EGG.get());
            })
            .build());

    public BossExpansion(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BossExpansionConfig.SPEC);

        CindermawRegistry.ENTITIES.register(modEventBus);
        CindermawRegistry.ITEMS.register(modEventBus);
        CindermawParticles.PARTICLES.register(modEventBus);
        CindermawArmorMaterial.MATERIALS.register(modEventBus);

        ZephyrarchRegistry.ENTITIES.register(modEventBus);
        ZephyrarchRegistry.ITEMS.register(modEventBus);
        ZephyrarchSounds.SOUNDS.register(modEventBus);

        GildedTyrantRegistry.ENTITIES.register(modEventBus);
        GildedTyrantRegistry.ITEMS.register(modEventBus);

        TABS.register(modEventBus);

        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::fillCreativeTabs);
        modEventBus.addListener(PauldronNetwork::register);
        modEventBus.addListener(ZephyrWorldVfxPayload::register);
        modEventBus.addListener(GildedWorldVfxPayload::register);

        NeoForge.EVENT_BUS.register(CindermawGearEvents.class);
        NeoForge.EVENT_BUS.register(SovereignGauntletItem.Events.class);
        NeoForge.EVENT_BUS.register(GildedGreataxeItem.Events.class);
        NeoForge.EVENT_BUS.register(BossCombatEvents.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(CindermawClientEvents.class);
            modEventBus.register(ZephyrarchClientEvents.class);
            modEventBus.register(GildedTyrantClientEvents.class);
            NeoForge.EVENT_BUS.register(CinderFx.class);
            NeoForge.EVENT_BUS.register(PauldronClientController.class);
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CindermawRegistry.CINDERMAW.get(), CindermawEntity.createAttributes().build());
        event.put(ZephyrarchRegistry.ZEPHYRARCH.get(), ZephyrarchEntity.createAttributes().build());
        event.put(GildedTyrantRegistry.GILDED_TYRANT.get(), GildedTyrantEntity.createAttributes().build());
        event.put(GildedTyrantRegistry.GILDED_OBELISK.get(), GildedObeliskEntity.createAttributes().build());
    }

    private void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CindermawRegistry.CINDERMAW_SPAWN_EGG.get());
            event.accept(ZephyrarchRegistry.ZEPHYRARCH_SPAWN_EGG.get());
            event.accept(GildedTyrantRegistry.GILDED_TYRANT_SPAWN_EGG.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(CindermawRegistry.CINDERMAW_CHESTPLATE.get());
            event.accept(ZephyrarchRegistry.SOVEREIGN_GAUNTLET.get());
            event.accept(GildedTyrantRegistry.GILDED_GREATAXE.get());
        }
    }
}
