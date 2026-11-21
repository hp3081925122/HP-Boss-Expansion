package com.hp.bossexpansion.gildedtyrant.item;

import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.gildedtyrant.client.GildedTyrantClientEvents;
import com.hp.bossexpansion.gildedtyrant.client.GreataxeClientController;
import com.hp.bossexpansion.gildedtyrant.config.GildedTyrantConfig;
import com.hp.bossexpansion.gildedtyrant.network.GreataxeNetwork;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import com.hp.bossexpansion.zephyrarch.registry.ZephyrarchSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GildedGreataxeItem extends Item implements GeoItem {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int SPIN_PERIOD_TICKS = 10;
    private static final int RELEASE_COOLDOWN_TICKS = 12;
    private static final double WHIRLWIND_RADIUS = 3.35D;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.gilded_greataxe.idle");

    private static final Map<UUID, ServerSpinState> SERVER_STATES = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GildedGreataxeItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Nullable
    public static InteractionHand findWhirlwindHand(Player player) {
        if (player.getMainHandItem().is(GildedTyrantRegistry.GILDED_GREATAXE.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(GildedTyrantRegistry.GILDED_GREATAXE.get()) && player.getMainHandItem().isEmpty()) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static void setWhirlwindHolding(ServerPlayer player, boolean holding, InteractionHand hand) {
        if (!holding) {
            stopServerWhirlwind(player, true);
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(GildedTyrantRegistry.GILDED_GREATAXE.get().getDefaultInstance())) {
            return;
        }
        InteractionHand resolved = player.getItemInHand(hand).is(GildedTyrantRegistry.GILDED_GREATAXE.get())
                ? hand
                : findWhirlwindHand(player);
        if (resolved == null) {
            stopServerWhirlwind(player, false);
            return;
        }

        ServerSpinState st = SERVER_STATES.computeIfAbsent(player.getUUID(), k -> new ServerSpinState());
        boolean wasActive = st.active;
        st.active = true;
        st.hand = resolved;
        if (!wasActive) {
            st.spinTicks = 0;
            if (player.level() instanceof ServerLevel serverLevel) {
                GeoItem.getOrAssignId(player.getItemInHand(resolved), serverLevel);
            }
            GreataxeNetwork.broadcastState(player, true, resolved);
            LOGGER.debug("[GildedGreataxe] Server whirlwind started for player {} on hand {}", player.getScoreboardName(), resolved);
        }
    }

    private static void stopServerWhirlwind(ServerPlayer player, boolean applyCooldown) {
        ServerSpinState st = SERVER_STATES.remove(player.getUUID());
        if (st != null && st.active) {
            GreataxeNetwork.broadcastState(player, false, st.hand);
            if (applyCooldown) {
                player.getCooldowns().addCooldown(GildedTyrantRegistry.GILDED_GREATAXE.get().getDefaultInstance(), RELEASE_COOLDOWN_TICKS);
            }
            LOGGER.debug("[GildedGreataxe] Server whirlwind stopped for player {} after {} ticks", player.getScoreboardName(), st.spinTicks);
        }
    }

    private static void disableBlockingShield(LivingEntity target, Level level) {
        if (target instanceof Player targetPlayer && targetPlayer.isBlocking()) {
            ItemStack useItem = targetPlayer.getUseItem();
            if (!useItem.isEmpty()) {
                targetPlayer.getCooldowns().addCooldown(useItem, 80);
            }
            targetPlayer.stopUsingItem();
            level.broadcastEntityEvent(targetPlayer, (byte) 30);
        }
    }

    private static boolean canSkillHit(Player attacker, LivingEntity candidate) {
        if (candidate == attacker || !candidate.isAlive() || candidate instanceof ArmorStand || attacker.isAlliedTo(candidate)) {
            return false;
        }
        if (candidate instanceof Player other && (other.isCreative() || other.isSpectator() || !attacker.canHarmPlayer(other))) {
            return false;
        }
        if (candidate instanceof OwnableEntity ownable) {
            EntityReference<LivingEntity> ref = ownable.getOwnerReference();
            if (ref != null && attacker.getUUID().equals(ref.getUUID())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        disableBlockingShield(target, attacker.level());
        if (attacker.level() instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
            AABB cleaveBox = target.getBoundingBox().inflate(2.2D, 0.8D, 2.2D);
            float baseDmg = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F;
            for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, cleaveBox,
                    e -> e != attacker && e != target && e.isAlive() && !attacker.isAlliedTo(e))) {
                if (attacker instanceof Player player) {
                    nearby.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), baseDmg);
                } else {
                    nearby.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(attacker), baseDmg);
                }
                double resist = Math.max(0.0D, 1.0D - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                Vec3 away = nearby.position().subtract(attacker.position());
                Vec3 flat = new Vec3(away.x, 0.0D, away.z);
                if (flat.lengthSqr() > 1.0E-4D && resist > 0.01D) {
                    Vec3 push = flat.normalize().scale(0.35D * resist);
                    nearby.push(push.x, 0.1D * resist, push.z);
                    nearby.hurtMarked = true;
                }
            }
        }
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
        } else if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ClientUseBridge.notifyUse(hand);
        }
        return InteractionResult.CONSUME;
    }

    private static void performWhirlwindRevolutionPulse(ServerLevel serverLevel, ServerPlayer player, ServerSpinState st, ItemStack stack) {
        double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double mult = GildedTyrantConfig.PLAYER_WHIRLWIND_MULTIPLIER.get();
        float spinDamage = Math.max(4.0F, (float) (baseAttack * mult));
        double kbScale = GildedTyrantConfig.KNOCKBACK_SCALE.get();

        Vec3 center = player.position();
        AABB hitBox = new AABB(
                center.x - WHIRLWIND_RADIUS, player.getY() - 0.45D, center.z - WHIRLWIND_RADIUS,
                center.x + WHIRLWIND_RADIUS, player.getY() + player.getBbHeight() + 0.75D, center.z + WHIRLWIND_RADIUS
        );
        double maxDistSq = (WHIRLWIND_RADIUS + 0.55D) * (WHIRLWIND_RADIUS + 0.55D);

        boolean hitAny = false;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> canSkillHit(player, e))) {
            double dx = victim.getX() - player.getX();
            double dz = victim.getZ() - player.getZ();
            if (dx * dx + dz * dz > maxDistSq) {
                continue;
            }
            disableBlockingShield(victim, serverLevel);
            if (victim.invulnerableTime <= 12) {
                victim.invulnerableTime = 0;
            }
            if (victim.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), spinDamage)) {
                hitAny = true;
            }
            double resist = Math.max(0.0D, 1.0D - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 flat = new Vec3(dx, 0.0D, dz);
            if (flat.lengthSqr() > 1.0E-4D && resist > 0.01D && kbScale > 0.01D) {
                Vec3 dir = flat.normalize();
                double push = 0.20D * resist * kbScale;
                victim.push(dir.x * push, 0.06D * resist * kbScale, dir.z * push);
                victim.hurtMarked = true;
            }
            serverLevel.sendParticles(CindermawParticles.EMBER.get(),
                    victim.getX(), victim.getY(0.55D), victim.getZ(),
                    4, 0.22D, 0.20D, 0.22D, 0.04D);
        }

        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                ZephyrarchSounds.SWEEP_SPIN.get(), SoundSource.PLAYERS, 0.95F, 0.92F + serverLevel.getRandom().nextFloat() * 0.14F);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 0.78F + serverLevel.getRandom().nextFloat() * 0.12F);
        if (hitAny) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.32F, 1.55F);
        }

        serverLevel.sendParticles(CindermawParticles.EMBER.get(),
                player.getX(), player.getY() + 0.95D, player.getZ(),
                6, 1.65D, 0.12D, 1.65D, 0.02D);

        if (st.spinTicks > 0 && st.spinTicks % (SPIN_PERIOD_TICKS * 2) == 3) {
            EquipmentSlot slot = st.hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hp_boss_expansion.gilded_greataxe.desc1").withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("item.hp_boss_expansion.gilded_greataxe.desc2").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            consumer.accept(GildedTyrantClientEvents.createGreataxeRenderProvider());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("idle", 0, state -> state.setAndContinue(ANIM_IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static final class ServerSpinState {
        boolean active;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        int spinTicks;
    }

    private static final class ClientUseBridge {
        private static void notifyUse(InteractionHand hand) {
            GreataxeClientController.notifyUseTriggered(hand);
        }
    }

    public static final class Events {
        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            SERVER_STATES.remove(event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
                ServerSpinState st = SERVER_STATES.get(target.getUUID());
                if (st != null && st.active) {
                    GreataxeNetwork.sendStateToPlayer(viewer, target, true, st.hand);
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            Player raw = event.getEntity();
            if (raw.level().isClientSide() || !(raw instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            ServerSpinState st = SERVER_STATES.get(sp.getUUID());
            if (st == null || !st.active) {
                return;
            }
            ItemStack stack = sp.getItemInHand(st.hand);
            if (!sp.isAlive() || sp.isSpectator() || !stack.is(GildedTyrantRegistry.GILDED_GREATAXE.get())) {
                stopServerWhirlwind(sp, false);
                return;
            }

            if (st.spinTicks % SPIN_PERIOD_TICKS == 3) {
                performWhirlwindRevolutionPulse(serverLevel, sp, st, stack);
            }
            st.spinTicks++;
        }
    }
}
