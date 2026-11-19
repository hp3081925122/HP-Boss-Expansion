package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrChargeEntity;
import net.minecraft.resources.Identifier;

public final class ZephyrChargeModel extends GeoModel<ZephyrChargeEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "zephyr_charge");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/zephyr_charge.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "zephyr_charge");

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ZephyrChargeEntity animatable) {
        return ANIMATION;
    }
}
