package com.hp.bossexpansion.gildedtyrant.registry;

import com.hp.bossexpansion.BossExpansion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 鎏金暴君与贪婪金碑自定义音效注册表
public final class GildedTyrantSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BossExpansion.MOD_ID);

    public static final RegistryObject<SoundEvent> OBELISK_HIT = reg("obelisk_hit");
    public static final RegistryObject<SoundEvent> OBELISK_SHATTER = reg("obelisk_shatter");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, name)));
    }

    private GildedTyrantSounds() {
    }
}
