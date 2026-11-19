package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.zephyrarch.item.SovereignGauntletItem;
import net.minecraft.resources.Identifier;

public final class SovereignGauntletModel extends GeoModel<SovereignGauntletItem> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "sovereign_gauntlet");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/item/sovereign_gauntlet.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "sovereign_gauntlet");

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(SovereignGauntletItem animatable) {
        return ANIMATION;
    }
}
