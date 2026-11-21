package com.hp.bossexpansion.cindermaw.item;

import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.cindermaw.client.CindermawClientEvents;
import com.hp.bossexpansion.cindermaw.client.PauldronClientController;
import com.hp.bossexpansion.cindermaw.entity.LavaBomb;
import com.hp.bossexpansion.cindermaw.entity.MagmaEffect;
import com.hp.bossexpansion.cindermaw.network.PauldronNetwork;
import com.hp.bossexpansion.config.BossExpansionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// 熔渊重炮肩甲：穿戴于胸甲槽的三维重装火炮，兼具火焰免疫、近战炉心协射、短按双肩四连齐射与长按超载熔渊巨炮
public final class CindermawPauldronItem extends ArmorItem implements GeoItem {
    public static final int OVERCHARGE_THRESHOLD_TICKS = 18;
    public static final int MAX_AUTO_RELEASE_TICKS = 32;
    private static final int SALVO_SHOT_COUNT = 4;
    private static final int SALVO_SHOT_INTERVAL = 3;
    private static final int COOP_HIT_ICD_TICKS = 6;
    private static final double MAX_AIM_RANGE = 36.0D;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.cindermaw_pauldron.idle");
    private static final RawAnimation ANIM_CHARGE = RawAnimation.begin().thenLoop("animation.cindermaw_pauldron.charge");
    private static final RawAnimation ANIM_FIRE_RIGHT = RawAnimation.begin().thenPlay("animation.cindermaw_pauldron.fire_right");
    private static final RawAnimation ANIM_FIRE_LEFT = RawAnimation.begin().thenPlay("animation.cindermaw_pauldron.fire_left");
    private static final RawAnimation ANIM_FIRE_SALVO = RawAnimation.begin().thenPlay("animation.cindermaw_pauldron.fire_salvo");
    private static final RawAnimation ANIM_FIRE_MEGABOMB = RawAnimation.begin().thenPlay("animation.cindermaw_pauldron.fire_megabomb");

    private static final Map<UUID, ServerCannonState> SERVER_STATES = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CindermawPauldronItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public static boolean isWearing(LivingEntity entity) {
        return entity != null && entity.getItemBySlot(EquipmentSlot.CHEST).is(CindermawRegistry.CINDERMAW_CHESTPLATE.get());
    }

    public static void clearPlayerState(UUID uuid) {
        SERVER_STATES.remove(uuid);
    }

    public static boolean isServerCharging(Player player) {
        ServerCannonState st = SERVER_STATES.get(player.getUUID());
        return st != null && st.charging;
    }

    // 计算穿戴者左/右肩炮炮口的世界坐标，用于发射起点与炮口火光粒子
    public static Vec3 shoulderMuzzlePos(LivingEntity wearer, boolean rightShoulder) {
        float yawRad = wearer.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double side = rightShoulder ? 0.48D : -0.48D;
        return wearer.position()
                .add(right.scale(side))
                .add(forward.scale(0.16D))
                .add(0.0D, wearer.getBbHeight() * 0.94D, 0.0D);
    }

    // 处理客户端发来的按键按下/松开状态
    public static void handleKeyHold(ServerPlayer player, boolean holding) {
        ServerCannonState st = SERVER_STATES.computeIfAbsent(player.getUUID(), k -> new ServerCannonState());
        if (holding) {
            if (!player.isAlive() || player.isSpectator() || !isWearing(player)) {
                return;
            }
            if (player.getCooldowns().isOnCooldown(CindermawRegistry.CINDERMAW_CHESTPLATE.get()) || st.salvoRemaining > 0) {
                return;
            }
            if (!st.charging) {
                st.charging = true;
                st.chargeTicks = 0;
                PauldronNetwork.broadcastChargeState(player, true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.7F, 1.35F);
            }
            return;
        }

        if (!st.charging) {
            return;
        }
        int held = st.chargeTicks;
        stopCharging(player, st);
        if (!player.isAlive() || player.isSpectator() || !isWearing(player)) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(CindermawRegistry.CINDERMAW_CHESTPLATE.get())) {
            return;
        }
        if (held >= OVERCHARGE_THRESHOLD_TICKS) {
            fireOverchargedMegabomb(player);
        } else {
            startVolcanicSalvo(player, st);
        }
    }

    private static void stopCharging(ServerPlayer player, ServerCannonState st) {
        if (st.charging) {
            st.charging = false;
            st.chargeTicks = 0;
            PauldronNetwork.broadcastChargeState(player, false);
        }
    }

    // 服务端每刻推进蓄力提示与四连齐射队列
    public static void tickServerPlayer(ServerPlayer player) {
        ServerCannonState st = SERVER_STATES.get(player.getUUID());
        if (st == null) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !isWearing(player)) {
            if (st.charging) {
                stopCharging(player, st);
            }
            st.salvoRemaining = 0;
            return;
        }

        if (st.charging) {
            st.chargeTicks++;
            if (st.chargeTicks == OVERCHARGE_THRESHOLD_TICKS && player.serverLevel() != null) {
                ServerLevel sl = player.serverLevel();
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.9F, 1.35F);
                Vec3 chest = player.position().add(0.0D, player.getBbHeight() * 0.78D, 0.0D);
                sl.sendParticles(CindermawParticles.HEAT_FLASH.get(), chest.x, chest.y, chest.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (st.chargeTicks >= MAX_AUTO_RELEASE_TICKS) {
                stopCharging(player, st);
                fireOverchargedMegabomb(player);
            }
        }

        if (st.salvoRemaining > 0) {
            if (st.salvoDelay > 0) {
                st.salvoDelay--;
            } else {
                boolean right = st.nextRightShoulder;
                st.nextRightShoulder = !right;
                st.salvoRemaining--;
                st.salvoDelay = SALVO_SHOT_INTERVAL;
                fireSingleSalvoRound(player, right, SALVO_SHOT_COUNT - st.salvoRemaining - 1);
            }
        }
    }

    // 近战命中叠满炉心热量后，由肩炮自动发射一发预判协射熔岩弹
    public static void onWearerMeleeHit(ServerPlayer player, LivingEntity target) {
        if (!isWearing(player) || !target.isAlive() || target == player) {
            return;
        }
        ServerCannonState st = SERVER_STATES.computeIfAbsent(player.getUUID(), k -> new ServerCannonState());
        long now = player.level().getGameTime();
        if (now - st.lastCoopTick < COOP_HIT_ICD_TICKS) {
            return;
        }
        st.lastCoopTick = now;
        st.coopStacks++;
        int needed = Math.max(1, BossExpansionConfig.PAULDRON_COOP_HITS.get());
        if (st.coopStacks < needed) {
            return;
        }
        st.coopStacks = 0;
        boolean right = st.nextRightShoulder;
        st.nextRightShoulder = !right;

        ServerLevel level = player.serverLevel();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof CindermawPauldronItem pauldron) {
            long id = GeoItem.getOrAssignId(chest, level);
            pauldron.triggerArmorAnim(player, id, "action", right ? "fire_right" : "fire_left");
        }

        Vec3 start = shoulderMuzzlePos(player, right);
        Vec3 predicted = target.position().add(target.getDeltaMovement().scale(6.0D));
        Vec3 landing = MagmaEffect.findGroundAt(player, predicted);
        double dist = start.distanceTo(landing);
        int flightTicks = Mth.clamp((int) Math.round(dist * 0.95D), 7, 18);
        float baseAtk = Math.max(4.0F, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float dmg = (float) (baseAtk * BossExpansionConfig.PAULDRON_COOP_DAMAGE_MULT.get());

        spawnShell(level, player, start, landing, flightTicks, dmg, 2.5F, 0.30F, false);
        spawnMuzzleBlast(level, start, player.getLookAngle(), false);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.85F);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.85F, 0.72F);
    }

    private static void startVolcanicSalvo(ServerPlayer player, ServerCannonState st) {
        ServerLevel level = player.serverLevel();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof CindermawPauldronItem pauldron) {
            long id = GeoItem.getOrAssignId(chest, level);
            pauldron.triggerArmorAnim(player, id, "action", "fire_salvo");
        }
        st.salvoRemaining = SALVO_SHOT_COUNT;
        st.salvoDelay = 0;
        st.nextRightShoulder = true;
        int cd = BossExpansionConfig.PAULDRON_SALVO_COOLDOWN_TICKS.get();
        player.getCooldowns().addCooldown(CindermawRegistry.CINDERMAW_CHESTPLATE.get(), cd);
    }

    private static void fireSingleSalvoRound(ServerPlayer player, boolean rightShoulder, int shotIndex) {
        ServerLevel level = player.serverLevel();
        Vec3 start = shoulderMuzzlePos(player, rightShoulder);
        Vec3 centerTarget = raycastAimTarget(player, MAX_AIM_RANGE);
        double spread = 0.85D;
        double ox = (player.getRandom().nextDouble() - 0.5D) * 2.0D * spread + (rightShoulder ? 0.35D : -0.35D);
        double oz = (player.getRandom().nextDouble() - 0.5D) * 2.0D * spread;
        Vec3 landing = MagmaEffect.findGroundAt(player, centerTarget.add(ox, 0.0D, oz));

        double dist = start.distanceTo(landing);
        int flightTicks = Mth.clamp((int) Math.round(dist * 0.85D) + shotIndex, 8, 22);
        float baseAtk = Math.max(6.0F, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float dmg = (float) (baseAtk * BossExpansionConfig.PAULDRON_SALVO_DAMAGE_MULT.get());

        spawnShell(level, player, start, landing, flightTicks, dmg, 2.6F, 0.32F, false);
        spawnMuzzleBlast(level, start, player.getLookAngle(), false);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.1F, 0.90F + shotIndex * 0.05F);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.55F, 1.45F);
    }

    private static void fireOverchargedMegabomb(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof CindermawPauldronItem pauldron) {
            long id = GeoItem.getOrAssignId(chest, level);
            pauldron.triggerArmorAnim(player, id, "action", "fire_megabomb");
        }

        Vec3 rightMuzzle = shoulderMuzzlePos(player, true);
        Vec3 leftMuzzle = shoulderMuzzlePos(player, false);
        Vec3 centerStart = rightMuzzle.add(leftMuzzle).scale(0.5D).add(0.0D, 0.18D, 0.0D);
        Vec3 aim = raycastAimTarget(player, MAX_AIM_RANGE);
        Vec3 landing = MagmaEffect.findGroundAt(player, aim);

        double dist = centerStart.distanceTo(landing);
        int flightTicks = Mth.clamp((int) Math.round(dist * 0.92D), 10, 24);
        float baseAtk = Math.max(8.0F, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float dmg = (float) (baseAtk * BossExpansionConfig.PAULDRON_MEGABOMB_DAMAGE_MULT.get());

        spawnShell(level, player, centerStart, landing, flightTicks, dmg, 4.6F, 0.36F, true);
        spawnMuzzleBlast(level, rightMuzzle, player.getLookAngle(), true);
        spawnMuzzleBlast(level, leftMuzzle, player.getLookAngle(), true);

        level.playSound(null, centerStart.x, centerStart.y, centerStart.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4F, 0.78F);
        level.playSound(null, centerStart.x, centerStart.y, centerStart.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.4F, 0.62F);

        // 重炮齐轰给予穿戴者轻微后坐力反馈
        Vec3 look = player.getLookAngle();
        Vec3 recoil = new Vec3(-look.x, 0.0D, -look.z);
        if (recoil.lengthSqr() > 1.0E-4D) {
            recoil = recoil.normalize().scale(0.28D);
            player.setDeltaMovement(player.getDeltaMovement().add(recoil.x, 0.08D, recoil.z));
            player.hurtMarked = true;
        }

        int cd = BossExpansionConfig.PAULDRON_MEGABOMB_COOLDOWN_TICKS.get();
        player.getCooldowns().addCooldown(CindermawRegistry.CINDERMAW_CHESTPLATE.get(), cd);
    }

    private static void spawnShell(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 end, int ticks, float dmg, float radius, float arcScale, boolean mega) {
        LavaBomb bomb = CindermawRegistry.LAVA_BOMB.get().create(level);
        if (bomb == null) {
            return;
        }
        bomb.launchPlayerShell(owner, start, end, ticks, dmg, radius, arcScale, mega);
        level.addFreshEntity(bomb);
    }

    private static void spawnMuzzleBlast(ServerLevel level, Vec3 muzzle, Vec3 lookDir, boolean mega) {
        level.sendParticles(CindermawParticles.HEAT_FLASH.get(), muzzle.x, muzzle.y, muzzle.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(CindermawParticles.EMBER.get(), muzzle.x, muzzle.y, muzzle.z, mega ? 16 : 8, 0.16D, 0.16D, 0.16D, 0.12D);
        level.sendParticles(CindermawParticles.BLAZE_FLAME.get(),
                muzzle.x + lookDir.x * 0.2D, muzzle.y + 0.12D, muzzle.z + lookDir.z * 0.2D,
                mega ? 10 : 5, 0.12D, 0.12D, 0.12D, 0.06D);
        level.sendParticles(CindermawParticles.CINDER_SMOKE.get(), muzzle.x, muzzle.y + 0.2D, muzzle.z, mega ? 6 : 3, 0.15D, 0.15D, 0.15D, 0.03D);
    }

    // 沿玩家准星射线锁定实体或地面落点
    private static Vec3 raycastAimTarget(ServerPlayer player, double maxRange) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxRange));
        var blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistSqr = maxRange * maxRange;
        Vec3 hitPos = end;
        if (blockHit.getType() != HitResult.Type.MISS) {
            hitPos = blockHit.getLocation();
            maxDistSqr = eye.distanceToSqr(hitPos);
        }
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(1.5D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eye,
                hitPos,
                searchBox,
                e -> e instanceof LivingEntity living && MagmaEffect.canHarmTarget(player, living),
                0.35F
        );
        if (entityHit != null && eye.distanceToSqr(entityHit.getLocation()) <= maxDistSqr) {
            return entityHit.getEntity().position();
        }
        return hitPos;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component keyName = Component.keybind("key.hp_boss_expansion.volcanic_cannon").withStyle(ChatFormatting.WHITE);
        tooltip.add(Component.translatable("item.hp_boss_expansion.cindermaw_chestplate.desc1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.hp_boss_expansion.cindermaw_chestplate.desc2", keyName).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.hp_boss_expansion.cindermaw_chestplate.desc3", keyName).withStyle(ChatFormatting.RED));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            consumer.accept(CindermawClientEvents.createPauldronRenderProvider());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 3, state -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                Entity wearer = state.getData(DataTickets.ENTITY);
                if (wearer != null && ClientChargeBridge.isCharging(wearer.getId())) {
                    return state.setAndContinue(ANIM_CHARGE);
                }
            }
            return state.setAndContinue(ANIM_IDLE);
        }));
        controllers.add(new AnimationController<>(this, "action", 1, state -> PlayState.STOP)
                .triggerableAnim("fire_right", ANIM_FIRE_RIGHT)
                .triggerableAnim("fire_left", ANIM_FIRE_LEFT)
                .triggerableAnim("fire_salvo", ANIM_FIRE_SALVO)
                .triggerableAnim("fire_megabomb", ANIM_FIRE_MEGABOMB));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static final class ClientChargeBridge {
        private static boolean isCharging(int entityId) {
            return PauldronClientController.isEntityCharging(entityId);
        }
    }

    private static final class ServerCannonState {
        boolean charging;
        int chargeTicks;
        int salvoRemaining;
        int salvoDelay;
        boolean nextRightShoulder = true;
        int coopStacks;
        long lastCoopTick = -100L;
    }
}
