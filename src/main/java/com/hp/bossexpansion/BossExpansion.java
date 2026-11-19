package com.hp.bossexpansion;

import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.cindermaw.entity.CindermawEntity;
import com.hp.bossexpansion.config.BossExpansionConfig;
import com.hp.bossexpansion.event.BossCombatEvents;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrarchEntity;
import com.hp.bossexpansion.zephyrarch.item.SovereignGauntletItem;
import com.hp.bossexpansion.zephyrarch.network.ZephyrWorldVfxPayload;
import com.hp.bossexpansion.zephyrarch.registry.ZephyrarchRegistry;
import com.hp.bossexpansion.zephyrarch.registry.ZephyrarchSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(BossExpansion.MOD_ID)
public final class BossExpansion {
    public static final String MOD_ID = "hp_boss_expansion";

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hp_boss_expansion"))
            .icon(() -> CindermawRegistry.CINDERMAW_CORE.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(CindermawRegistry.CINDERMAW_CORE.get());
                output.accept(CindermawRegistry.CINDERMAW_HEART.get());
                output.accept(CindermawRegistry.CINDERMAW_HAMMER.get());
                output.accept(CindermawRegistry.CINDERMAW_HELMET.get());
                output.accept(CindermawRegistry.CINDERMAW_CHESTPLATE.get());
                output.accept(CindermawRegistry.CINDERMAW_LEGGINGS.get());
                output.accept(CindermawRegistry.CINDERMAW_BOOTS.get());
                output.accept(CindermawRegistry.CINDERMAW_SPAWN_EGG.get());
                output.accept(ZephyrarchRegistry.TEMPEST_CREST.get());
                output.accept(ZephyrarchRegistry.ZEPHYR_CORE.get());
                output.accept(ZephyrarchRegistry.SOVEREIGN_GAUNTLET.get());
                output.accept(ZephyrarchRegistry.ZEPHYRARCH_SPAWN_EGG.get());
            })
            .build());

    public BossExpansion(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, BossExpansionConfig.SPEC);

        var bus = context.getModEventBus();
        CindermawRegistry.ENTITIES.register(bus);
        CindermawRegistry.ITEMS.register(bus);
        CindermawParticles.PARTICLES.register(bus);

        ZephyrarchRegistry.ENTITIES.register(bus);
        ZephyrarchRegistry.ITEMS.register(bus);
        ZephyrarchSounds.SOUNDS.register(bus);

        TABS.register(bus);

        ZephyrWorldVfxPayload.register();

        bus.addListener(this::registerAttributes);
        bus.addListener(this::fillCreativeTabs);

        MinecraftForge.EVENT_BUS.register(SovereignGauntletItem.Events.class);
        MinecraftForge.EVENT_BUS.register(BossCombatEvents.class);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CindermawRegistry.CINDERMAW.get(), CindermawEntity.createAttributes().build());
        event.put(ZephyrarchRegistry.ZEPHYRARCH.get(), ZephyrarchEntity.createAttributes().build());
    }

    private void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CindermawRegistry.CINDERMAW_SPAWN_EGG);
            event.accept(ZephyrarchRegistry.ZEPHYRARCH_SPAWN_EGG);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(CindermawRegistry.CINDERMAW_HAMMER);
            event.accept(CindermawRegistry.CINDERMAW_HELMET);
            event.accept(CindermawRegistry.CINDERMAW_CHESTPLATE);
            event.accept(CindermawRegistry.CINDERMAW_LEGGINGS);
            event.accept(CindermawRegistry.CINDERMAW_BOOTS);
            event.accept(ZephyrarchRegistry.SOVEREIGN_GAUNTLET);
        }
    }
}
