package com.hp.bossexpansion.cindermaw.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.cindermaw.CindermawParticles;

import java.util.List;

// 炉心胸甲效果：免疫火焰与岩浆；被近身攻击时概率喷火反击。
@Mod.EventBusSubscriber(modid = BossExpansion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CindermawGearEvents {
    private CindermawGearEvents() {
    }

    public static boolean hasFullArmorSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(CindermawRegistry.CINDERMAW_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(CindermawRegistry.CINDERMAW_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(CindermawRegistry.CINDERMAW_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(CindermawRegistry.CINDERMAW_BOOTS.get());
    }

    // 火焰类伤害直接取消，并熄灭身上的火
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!event.getSource().is(DamageTypeTags.IS_FIRE) || !wearer.getItemBySlot(EquipmentSlot.CHEST).is(CindermawRegistry.CINDERMAW_CHESTPLATE.get())) {
            return;
        }
        event.setCanceled(true);
        wearer.clearFire();
    }

    // 被近战命中时 35% 概率反喷火焰：点燃并灼伤攻击者
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide || !wearer.getItemBySlot(EquipmentSlot.CHEST).is(CindermawRegistry.CINDERMAW_CHESTPLATE.get())) {
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker) || attacker == wearer || attacker.distanceTo(wearer) > 4.0F) {
            return;
        }
        boolean fullSet = hasFullArmorSet(wearer);
        float chance = fullSet ? 0.65F : 0.35F;
        if (wearer.getRandom().nextFloat() >= chance) {
            return;
        }
        attacker.setSecondsOnFire(fullSet ? 6 : 4);
        attacker.hurt(wearer.damageSources().thorns(wearer), fullSet ? 6.0F : 3.0F);
        if (wearer.level() instanceof ServerLevel server) {
            // 火焰从胸口喷向攻击者
            Vec3 from = wearer.position().add(0, wearer.getBbHeight() * 0.65, 0);
            Vec3 dir = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0).subtract(from).normalize();
            for (int i = 0; i < (fullSet ? 16 : 10); i++) {
                server.sendParticles(CindermawParticles.BLAZE_FLAME.get(), from.x, from.y, from.z, 0, dir.x, dir.y, dir.z, 0.15 + i * 0.03);
            }
            server.sendParticles(CindermawParticles.EMBER.get(), from.x, from.y, from.z, fullSet ? 14 : 8, 0.35, 0.35, 0.35, 0.09);
            server.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.85F, 0.8F);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide || wearer.tickCount % 20 != 0 || !hasFullArmorSet(wearer)) {
            return;
        }
        if (!(wearer.level() instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(CindermawParticles.EMBER.get(), wearer.getX(), wearer.getY() + 1.0, wearer.getZ(), 3, 0.35, 0.45, 0.35, 0.02);
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, wearer.getBoundingBox().inflate(3.5),
                target -> target != wearer && target.isAlive() && !target.isAlliedTo(wearer)
                        && (target == wearer.getLastHurtByMob() || target == wearer.getLastHurtMob()
                        || (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == wearer)));
        for (LivingEntity target : targets) {
            target.setSecondsOnFire(3);
            target.hurt(wearer.damageSources().onFire(), 2.0F);
            server.sendParticles(CindermawParticles.BLAZE_FLAME.get(), target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 4, 0.2, 0.2, 0.2, 0.03);
        }
    }
}
