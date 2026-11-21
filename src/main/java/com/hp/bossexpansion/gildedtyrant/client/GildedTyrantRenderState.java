package com.hp.bossexpansion.gildedtyrant.client;

import com.geckolib.renderer.base.GeoRenderState;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

// 鎏金暴君渲染状态对象
public final class GildedTyrantRenderState extends LivingEntityRenderState implements GeoRenderState {
    public GildedTyrantEntity entity;
    public GildedTyrantAttack attack = GildedTyrantAttack.NONE;
    public boolean phase2;
    public boolean axeThrown;
    public boolean dying;
    public float partial;
    public float time;

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
