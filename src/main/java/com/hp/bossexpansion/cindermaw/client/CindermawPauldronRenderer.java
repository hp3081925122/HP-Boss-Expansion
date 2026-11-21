package com.hp.bossexpansion.cindermaw.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.item.CindermawPauldronItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// 熔渊重炮肩甲三维护甲渲染器，附带熔岩脉络与炉心自发光层
public final class CindermawPauldronRenderer extends GeoArmorRenderer<CindermawPauldronItem> {
    public CindermawPauldronRenderer() {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    private static final class Model extends GeoModel<CindermawPauldronItem> {
        private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(
                BossExpansion.MOD_ID, "geo/cindermaw_pauldron.geo.json");
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
                BossExpansion.MOD_ID, "textures/item/armor/cindermaw_pauldron.png");
        private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
                BossExpansion.MOD_ID, "animations/cindermaw_pauldron.animation.json");

        @Override
        public ResourceLocation getModelResource(CindermawPauldronItem animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(CindermawPauldronItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(CindermawPauldronItem animatable) {
            return ANIMATION;
        }
    }
}
