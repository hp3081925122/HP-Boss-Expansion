package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoItemRenderer;
import com.hp.bossexpansion.zephyrarch.registry.ZephyrarchRegistry;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

public final class ZephyrarchClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYRARCH.get(), ZephyrarchRenderer::new);
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYR_CHARGE.get(), ZephyrChargeRenderer::new);
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYR_BLADE.get(), ZephyrWindRenderers.Blade::new);
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYR_TWISTER.get(), ZephyrWindRenderers.Twister::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        if (!event.isItemRegistered(ZephyrarchRegistry.SOVEREIGN_GAUNTLET.get())) {
            event.registerItem(GauntletClientController.ITEM_EXTENSIONS, ZephyrarchRegistry.SOVEREIGN_GAUNTLET.get());
        }
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T entity, AvatarRenderState state) {
                state.setRenderData(GauntletClientController.ENTITY_ID_KEY, entity.getId());
            }
        });
    }

    public static GeoRenderProvider createGauntletRenderProvider() {
        return new GeoRenderProvider() {
            private SovereignGauntletRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new SovereignGauntletRenderer();
                }
                return this.renderer;
            }
        };
    }

    private ZephyrarchClientEvents() {
    }
}
