package com.hp.bossexpansion.cindermaw.client;

import net.minecraft.resources.ResourceLocation;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.item.CindermawArmorItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class CindermawArmorRenderer extends GeoArmorRenderer<CindermawArmorItem> {
    public CindermawArmorRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "armor/cindermaw_armor")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
