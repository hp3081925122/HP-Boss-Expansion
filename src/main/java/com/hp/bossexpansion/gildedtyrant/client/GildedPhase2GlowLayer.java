package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class GildedPhase2GlowLayer extends GeoRenderLayer<GildedTyrantEntity> {
    private static final RenderType OVERDRIVE_GLOW = RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/gilded_tyrant_glowmask.png")
    );

    public GildedPhase2GlowLayer(GeoRenderer<GildedTyrantEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack pose, GildedTyrantEntity entity, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource buffers, VertexConsumer buffer, float partial, int light, int overlay) {
        if (entity.isDeadOrDying()) {
            return;
        }
        float strength = entity.isPhase2() ? 0.72F : 0.0F;
        GildedTyrantAttack atk = entity.getAttack();
        if (atk == GildedTyrantAttack.GREED_CORE_OVERDRIVE || atk == GildedTyrantAttack.CATACLYSMIC_GOLD_RAIN || atk == GildedTyrantAttack.MOLTEN_GOLD_RAY) {
            strength = 1.0F;
        }
        if (strength <= 0.01F) {
            return;
        }
        float time = entity.tickCount + partial;
        float pulse = strength * (0.48F + 0.42F * Mth.sin(time * 0.22F));
        float r = Mth.clamp(pulse, 0.0F, 1.0F);
        float g = Mth.clamp(pulse * (185.0F / 255.0F), 0.0F, 1.0F);
        float b = Mth.clamp(pulse * (64.0F / 255.0F), 0.0F, 1.0F);
        getRenderer().reRender(model, pose, buffers, entity, OVERDRIVE_GLOW, buffers.getBuffer(OVERDRIVE_GLOW),
                partial, 0xF000F0, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
    }
}
