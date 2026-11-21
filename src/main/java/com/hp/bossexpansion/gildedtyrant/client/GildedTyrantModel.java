package com.hp.bossexpansion.gildedtyrant.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import net.minecraft.resources.Identifier;

// 鎏金暴君 GeckoLib 5 模型定义
public final class GildedTyrantModel extends GeoModel<GildedTyrantEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_tyrant");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/gilded_tyrant.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_tyrant");

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GildedTyrantEntity animatable) {
        return ANIMATION;
    }
}
