package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class GildedTyrantRenderer extends GeoEntityRenderer<GildedTyrantEntity> {
    public GildedTyrantRenderer(EntityRendererProvider.Context context) {
        super(context, new GildedTyrantModel());
        this.shadowRadius = 1.2F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(GildedTyrantEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        GildedTyrantVfx.renderBossEffects(entity, partialTick, poseStack, bufferSource);
    }

    @Override
    public RenderType getRenderType(GildedTyrantEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public boolean shouldRender(GildedTyrantEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(36.0D));
    }
}
