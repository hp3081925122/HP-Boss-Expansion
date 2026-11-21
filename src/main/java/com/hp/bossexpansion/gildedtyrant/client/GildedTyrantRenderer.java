package com.hp.bossexpansion.gildedtyrant.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.hp.bossexpansion.zephyrarch.client.ZephyrarchVfx;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// 鎏金暴君三维渲染器与实时刀光特效挂载
public final class GildedTyrantRenderer extends GeoEntityRenderer<GildedTyrantEntity, GildedTyrantRenderState> {
    public GildedTyrantRenderer(EntityRendererProvider.Context context) {
        super(context, new GildedTyrantModel());
        this.shadowRadius = 1.2F;
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.withRenderLayer(new GildedPhase2GlowLayer(this));
    }

    @Override
    public GildedTyrantRenderState createRenderState(GildedTyrantEntity entity, Void related) {
        return new GildedTyrantRenderState();
    }

    @Override
    public void extractRenderState(GildedTyrantEntity entity, GildedTyrantRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.attack = entity.getAttack();
        state.phase2 = entity.isPhase2();
        state.axeThrown = entity.isAxeThrown();
        state.dying = entity.isDeadOrDying();
        state.partial = partialTick;
        state.time = entity.tickCount + partialTick;
    }

    @Override
    public void preRenderPass(RenderPassInfo<GildedTyrantRenderState> pass, SubmitNodeCollector collector) {
        GildedTyrantRenderState state = pass.renderState();
        pass.addBoneUpdater((info, snapshots) -> {
            snapshots.ifPresent("held_greataxe", axeBone -> {
                if (state.axeThrown) {
                    axeBone.setScale(0.0F, 0.0F, 0.0F);
                }
            });
            if (state.attack == GildedTyrantAttack.NONE && !state.dying) {
                snapshots.ifPresent("head", head -> {
                    float pitch = Mth.clamp(state.xRot, -22.0F, 22.0F) * Mth.DEG_TO_RAD;
                    float yaw = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
                    head.setRotX(head.getRotX() + pitch);
                    head.setRotY(head.getRotY() + yaw);
                });
            }
        });
    }

    @Override
    public void postRenderPass(RenderPassInfo<GildedTyrantRenderState> pass, SubmitNodeCollector collector) {
        super.postRenderPass(pass, collector);
        GildedTyrantRenderState state = pass.renderState();
        if (state.entity == null) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        ZephyrarchVfx.SubmitCollectorBuffer buf = ZephyrarchVfx.beginSubmit();
        GildedTyrantVfx.renderBossEffects(state.entity, state.partial, pass.poseStack(), buf, camera.rotation(), camera.position());
        buf.flush(collector);
    }

    @Override
    public RenderType getRenderType(GildedTyrantRenderState state, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    @Override
    public boolean shouldRender(GildedTyrantEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(48.0D));
    }
}
