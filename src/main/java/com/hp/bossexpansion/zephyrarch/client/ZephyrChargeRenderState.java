package com.hp.bossexpansion.zephyrarch.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.dataticket.OverridingDataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.hp.bossexpansion.zephyrarch.entity.ZephyrChargeEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ZephyrChargeRenderState extends EntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();
    public ZephyrChargeEntity entity;
    public float partial;
    public float scale = 1.0F;

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return geckolibData;
    }

    @Override
    public <D> void addGeckolibData(DataTicket<D> ticket, D data) {
        geckolibData.put(ticket, data);
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> ticket) {
        return geckolibData.containsKey(ticket);
    }

    @Override
    public <D> D getGeckolibData(DataTicket<D> ticket) {
        D stored = read(ticket);
        if (stored != null) {
            return stored;
        }
        return overridden(ticket);
    }

    @Override
    public <D> D getOrDefaultGeckolibData(DataTicket<D> ticket, D fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(DataTicket<D> ticket, Supplier<D> fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback.get();
    }

    @SuppressWarnings("unchecked")
    private <D> D read(DataTicket<D> ticket) {
        return (D) geckolibData.get(ticket);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <D> D overridden(DataTicket<D> ticket) {
        if (ticket instanceof OverridingDataTicket overriding && overriding.canExtractFrom(this)) {
            return (D) overriding.extractFrom(overriding.getOverriddenClass().cast(this));
        }
        return null;
    }
}
