package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hp.bossexpansion.zephyrarch.item.SovereignGauntletItem;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class SovereignGauntletRenderer extends GeoItemRenderer<SovereignGauntletItem> {
    public SovereignGauntletRenderer() {
        super(new SovereignGauntletModel());
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(GeoRenderState state, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
