package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.gildedtyrant.network.GildedWorldVfxPayload;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

public final class GildedTyrantClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        GildedWorldVfxPayload.setClientHandler(GildedWorldVfxClient::spawn);
        event.registerEntityRenderer(GildedTyrantRegistry.GILDED_TYRANT.get(), GildedTyrantRenderer::new);
        event.registerEntityRenderer(GildedTyrantRegistry.THROWN_GREATAXE.get(), GildedSubRenderers.ThrownAxe::new);
        event.registerEntityRenderer(GildedTyrantRegistry.GILDED_OBELISK.get(), GildedSubRenderers.Obelisk::new);
    }

    public static GeoRenderProvider createGreataxeRenderProvider() {
        return new GeoRenderProvider() {
            private GildedSubRenderers.GreataxeItem renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GildedSubRenderers.GreataxeItem();
                }
                return this.renderer;
            }
        };
    }

    private GildedTyrantClientEvents() {
    }
}
