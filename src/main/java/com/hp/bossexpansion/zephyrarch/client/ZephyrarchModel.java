package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrarchEntity;
import net.minecraft.resources.Identifier;

public final class ZephyrarchModel extends GeoModel<ZephyrarchEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "zephyrarch");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/zephyrarch.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "zephyrarch");

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ZephyrarchEntity animatable) {
        return ANIMATION;
    }
}
