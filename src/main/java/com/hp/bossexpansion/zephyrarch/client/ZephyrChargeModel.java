package com.hp.bossexpansion.zephyrarch.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrChargeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ZephyrChargeModel extends GeoModel<ZephyrChargeEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "geo/zephyr_charge.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/zephyr_charge.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "animations/zephyr_charge.animation.json");

    @Override
    public ResourceLocation getModelResource(ZephyrChargeEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZephyrChargeEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ZephyrChargeEntity animatable) {
        return ANIMATION;
    }
}
