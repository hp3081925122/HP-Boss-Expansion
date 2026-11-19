package com.hp.bossexpansion.zephyrarch.registry;

import com.hp.bossexpansion.BossExpansion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ZephyrarchSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BossExpansion.MOD_ID);

    public static final String[] NAMES = {
            "punch_swing", "punch_impact", "sweep_spin", "rush_charge", "rush_dash", "hammer_rise", "hammer_slam",
            "grab_catch", "grab_throw", "stomp_pull", "stomp_burst", "phase_shift", "wind_dash", "stagger",
            "bolt_shoot", "lance_charge", "lance_fire", "twister_spawn", "downdraft_impact", "blade_throw",
            "cage_form", "orb_shoot"
    };

    public static final RegistryObject<SoundEvent> PUNCH_SWING = reg("punch_swing");
    public static final RegistryObject<SoundEvent> PUNCH_IMPACT = reg("punch_impact");
    public static final RegistryObject<SoundEvent> SWEEP_SPIN = reg("sweep_spin");
    public static final RegistryObject<SoundEvent> RUSH_CHARGE = reg("rush_charge");
    public static final RegistryObject<SoundEvent> RUSH_DASH = reg("rush_dash");
    public static final RegistryObject<SoundEvent> HAMMER_RISE = reg("hammer_rise");
    public static final RegistryObject<SoundEvent> HAMMER_SLAM = reg("hammer_slam");
    public static final RegistryObject<SoundEvent> GRAB_CATCH = reg("grab_catch");
    public static final RegistryObject<SoundEvent> GRAB_THROW = reg("grab_throw");
    public static final RegistryObject<SoundEvent> STOMP_PULL = reg("stomp_pull");
    public static final RegistryObject<SoundEvent> STOMP_BURST = reg("stomp_burst");
    public static final RegistryObject<SoundEvent> PHASE_SHIFT = reg("phase_shift");
    public static final RegistryObject<SoundEvent> WIND_DASH = reg("wind_dash");
    public static final RegistryObject<SoundEvent> STAGGER = reg("stagger");
    public static final RegistryObject<SoundEvent> BOLT_SHOOT = reg("bolt_shoot");
    public static final RegistryObject<SoundEvent> LANCE_CHARGE = reg("lance_charge");
    public static final RegistryObject<SoundEvent> LANCE_FIRE = reg("lance_fire");
    public static final RegistryObject<SoundEvent> TWISTER_SPAWN = reg("twister_spawn");
    public static final RegistryObject<SoundEvent> DOWNDRAFT_IMPACT = reg("downdraft_impact");
    public static final RegistryObject<SoundEvent> BLADE_THROW = reg("blade_throw");
    public static final RegistryObject<SoundEvent> CAGE_FORM = reg("cage_form");
    public static final RegistryObject<SoundEvent> ORB_SHOOT = reg("orb_shoot");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, name)));
    }

    private ZephyrarchSounds() {
    }
}
