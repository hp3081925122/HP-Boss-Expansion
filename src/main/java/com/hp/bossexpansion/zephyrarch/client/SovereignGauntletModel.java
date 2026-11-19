package com.hp.bossexpansion.zephyrarch.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.zephyrarch.item.SovereignGauntletItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SovereignGauntletModel extends GeoModel<SovereignGauntletItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "geo/sovereign_gauntlet.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/item/sovereign_gauntlet.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "animations/sovereign_gauntlet.animation.json");

    @Override
    public ResourceLocation getModelResource(SovereignGauntletItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SovereignGauntletItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SovereignGauntletItem animatable) {
        return ANIMATION;
    }
}
