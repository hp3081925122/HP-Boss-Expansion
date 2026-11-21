package com.hp.bossexpansion.gildedtyrant.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// 鎏金暴君二阶段与超载脉动发光层
public final class GildedPhase2GlowLayer extends GeoRenderLayer<GildedTyrantEntity, Void, GildedTyrantRenderState> {
    private static final RenderType OVERDRIVE_GLOW = RenderTypes.eyes(
            Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/gilded_tyrant_glowmask.png")
    );

    public GildedPhase2GlowLayer(GeoRenderer<GildedTyrantEntity, Void, GildedTyrantRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void submitRenderTask(RenderPassInfo<GildedTyrantRenderState> pass, SubmitNodeCollector collector) {
        GildedTyrantRenderState state = pass.renderState();
        if (state.dying) {
            return;
        }
        float strength = state.phase2 ? 0.72F : 0.0F;
        GildedTyrantAttack atk = state.attack;
        if (atk == GildedTyrantAttack.GREED_CORE_OVERDRIVE || atk == GildedTyrantAttack.CATACLYSMIC_GOLD_RAIN || atk == GildedTyrantAttack.MOLTEN_GOLD_RAY) {
            strength = 1.0F;
        }
        if (strength <= 0.01F) {
            return;
        }
        float pulse = strength * (0.48F + 0.42F * Mth.sin(state.time * 0.22F));
        int r = Mth.clamp(Math.round(pulse * 255.0F), 0, 255);
        int g = Mth.clamp(Math.round(pulse * 185.0F), 0, 255);
        int b = Mth.clamp(Math.round(pulse * 64.0F), 0, 255);
        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
        GeoRenderState renderState = (GeoRenderState) state;
        int previous = pass.renderColor();
        renderState.addGeckolibData(DataTickets.RENDER_COLOR, color);
        getRenderer().submitRenderTasks(pass, collector.order(1), OVERDRIVE_GLOW);
        renderState.addGeckolibData(DataTickets.RENDER_COLOR, previous);
    }
}
