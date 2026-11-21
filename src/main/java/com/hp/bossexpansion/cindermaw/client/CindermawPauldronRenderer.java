package com.hp.bossexpansion.cindermaw.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.item.CindermawPauldronItem;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

// 熔渊重炮肩甲三维护甲渲染器，附带熔岩脉络与炉心自发光层
public final class CindermawPauldronRenderer extends GeoArmorRenderer<CindermawPauldronItem, CindermawPauldronRenderer.State> {
    public CindermawPauldronRenderer() {
        super(new Model());
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public State createRenderState(CindermawPauldronItem animatable, RenderData renderData) {
        return new State();
    }

    @Override
    public void captureDefaultRenderState(CindermawPauldronItem animatable, RenderData renderData, State state, float partialTick) {
        super.captureDefaultRenderState(animatable, renderData, state, partialTick);
        LivingEntity wearer = renderData.entity();
        state.addGeckolibData(CindermawPauldronItem.IS_CHARGING, wearer != null && PauldronClientController.isEntityCharging(wearer.getId()));
    }

    public static final class State extends HumanoidRenderState implements GeoRenderState {
    private final java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> geckolibData = new java.util.HashMap<>();

    @Override
    public java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> getDataMap() {
        return geckolibData;
    }

    @Override
    public <D> void addGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, D data) {
        geckolibData.put(ticket, data);
    }

    @Override
    public boolean hasGeckolibData(com.geckolib.constant.dataticket.DataTicket<?> ticket) {
        return geckolibData.containsKey(ticket);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <D> D getGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket) {
        Object stored = geckolibData.get(ticket);
        if (stored != null) {
            return (D) stored;
        }
        if (ticket instanceof com.geckolib.constant.dataticket.OverridingDataTicket overriding && overriding.canExtractFrom(this)) {
            return (D) overriding.extractFrom(overriding.getOverriddenClass().cast(this));
        }
        return null;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, D fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, java.util.function.Supplier<D> fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback.get();
    }
    }

    private static final class Model extends GeoModel<CindermawPauldronItem> {
        private static final Identifier MODEL = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "cindermaw_pauldron");
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/item/armor/cindermaw_pauldron.png");
        private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "cindermaw_pauldron");

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return MODEL;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return TEXTURE;
        }

        @Override
        public Identifier getAnimationResource(CindermawPauldronItem animatable) {
            return ANIMATION;
        }
    }
}
