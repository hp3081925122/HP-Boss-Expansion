package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrChargeEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class ZephyrChargeRenderer extends GeoEntityRenderer<ZephyrChargeEntity, ZephyrChargeRenderState> {
    public ZephyrChargeRenderer(EntityRendererProvider.Context context) {
        super(context, new ZephyrChargeModel());
        this.shadowRadius = 0.25F;
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public ZephyrChargeRenderState createRenderState(ZephyrChargeEntity entity, Void related) {
        return new ZephyrChargeRenderState();
    }

    @Override
    public void extractRenderState(ZephyrChargeEntity entity, ZephyrChargeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.partial = partialTick;
        state.scale = entity.getScale();
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<ZephyrChargeRenderState> pass, float widthScale, float heightScale) {
        float s = pass.renderState().scale;
        super.scaleModelForRender(pass, widthScale * s, heightScale * s);
    }

    @Override
    public void postRenderPass(RenderPassInfo<ZephyrChargeRenderState> pass, SubmitNodeCollector collector) {
        super.postRenderPass(pass, collector);
        ZephyrChargeRenderState state = pass.renderState();
        if (state.entity == null) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        ZephyrarchVfx.SubmitCollectorBuffer buf = ZephyrarchVfx.beginSubmit();
        ZephyrarchVfx.renderChargeTrail(state.entity, state.partial, pass.poseStack(), buf, camera.rotation(), camera.position());
        buf.flush(collector);
    }

    @Override
    public RenderType getRenderType(ZephyrChargeRenderState state, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRender(ZephyrChargeEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(6.0D));
    }
}
