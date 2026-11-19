package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrarchAttack;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrarchEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ZephyrarchRenderer extends GeoEntityRenderer<ZephyrarchEntity, ZephyrarchRenderState> {
    public ZephyrarchRenderer(EntityRendererProvider.Context context) {
        super(context, new ZephyrarchModel());
        this.shadowRadius = 1.15F;
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public ZephyrarchRenderState createRenderState(ZephyrarchEntity entity, Void related) {
        return new ZephyrarchRenderState();
    }

    @Override
    public void extractRenderState(ZephyrarchEntity entity, ZephyrarchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.attack = entity.getAttack();
        state.partial = partialTick;
        state.dying = entity.isDeadOrDying();
    }

    @Override
    public void preRenderPass(RenderPassInfo<ZephyrarchRenderState> pass, SubmitNodeCollector collector) {
        ZephyrarchRenderState state = pass.renderState();
        if (state.attack == ZephyrarchAttack.NONE && !state.dying) {
            pass.addBoneUpdater((info, snapshots) -> snapshots.ifPresent("head", head -> {
                float clampedPitch = Mth.clamp(state.xRot, -25.0F, 25.0F);
                float clampedYaw = Mth.clamp(state.yRot, -45.0F, 45.0F);
                head.setRotX(clampedPitch * Mth.DEG_TO_RAD);
                head.setRotY(clampedYaw * Mth.DEG_TO_RAD);
                head.setRotZ(0.0F);
            }));
        }
    }

    @Override
    public void postRenderPass(RenderPassInfo<ZephyrarchRenderState> pass, SubmitNodeCollector collector) {
        super.postRenderPass(pass, collector);
        ZephyrarchRenderState state = pass.renderState();
        if (state.entity == null) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        ZephyrarchVfx.SubmitCollectorBuffer buf = ZephyrarchVfx.beginSubmit();
        ZephyrarchVfx.renderBossEffects(state.entity, state.partial, pass.poseStack(), buf, camera.rotation(), camera.position());
        buf.flush(collector);
    }

    @Override
    public RenderType getRenderType(ZephyrarchRenderState state, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRender(ZephyrarchEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(48.0D));
    }
}
