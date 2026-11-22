package com.hp.bossexpansion.gildedtyrant.registry;

import com.hp.bossexpansion.BossExpansion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 鎏金暴君与贪婪金碑自定义音效注册表
public final class GildedTyrantSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, BossExpansion.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> OBELISK_HIT = reg("obelisk_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> OBELISK_SHATTER = reg("obelisk_shatter");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, name)));
    }

    private GildedTyrantSounds() {
    }
}
