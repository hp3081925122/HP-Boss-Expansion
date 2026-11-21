package com.hp.bossexpansion.gildedtyrant.client;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoItemRenderer;
import com.hp.bossexpansion.gildedtyrant.network.GildedWorldVfxPayload;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

// 鎏金暴君客户端事件注册
public final class GildedTyrantClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        GildedWorldVfxPayload.setClientHandler(GildedWorldVfxClient::spawn);
        event.registerEntityRenderer(GildedTyrantRegistry.GILDED_TYRANT.get(), GildedTyrantRenderer::new);
        event.registerEntityRenderer(GildedTyrantRegistry.THROWN_GREATAXE.get(), GildedSubRenderers.ThrownAxe::new);
        event.registerEntityRenderer(GildedTyrantRegistry.GILDED_OBELISK.get(), GildedSubRenderers.Obelisk::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        if (!event.isItemRegistered(GildedTyrantRegistry.GILDED_GREATAXE.get())) {
            event.registerItem(GreataxeClientController.ITEM_EXTENSIONS, GildedTyrantRegistry.GILDED_GREATAXE.get());
        }
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T entity, AvatarRenderState state) {
                state.setRenderData(GreataxeClientController.ENTITY_ID_KEY, entity.getId());
            }
        });
    }

    public static GeoRenderProvider createGreataxeRenderProvider() {
        return new GeoRenderProvider() {
            private GildedSubRenderers.GreataxeItem renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
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
