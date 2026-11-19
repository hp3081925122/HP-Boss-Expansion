package com.hp.bossexpansion.gildedtyrant.item;

import com.hp.bossexpansion.cindermaw.entity.MagmaEffect;
import com.hp.bossexpansion.gildedtyrant.client.GildedTyrantClientEvents;
import com.hp.bossexpansion.gildedtyrant.network.GildedWorldVfxPayload;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public final class GildedGreataxeItem extends AxeItem implements GeoItem {
    private static final int CHARGE_TICKS = 14;
    private static final int COOLDOWN_TICKS = 70;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.gilded_greataxe.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GildedGreataxeItem(Tier tier, Properties properties) {
        super(tier, properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Player targetPlayer && targetPlayer.isBlocking()) {
            ItemStack useItem = targetPlayer.getUseItem();
            if (!useItem.isEmpty()) {
                targetPlayer.getCooldowns().addCooldown(useItem.getItem(), 80);
            }
            targetPlayer.stopUsingItem();
            attacker.level().broadcastEntityEvent(targetPlayer, (byte) 30);
        }
        if (attacker.level() instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
            AABB cleaveBox = target.getBoundingBox().inflate(2.2D, 0.8D, 2.2D);
            float baseDmg = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F;
            for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, cleaveBox,
                    e -> e != attacker && e != target && e.isAlive() && !attacker.isAlliedTo(e))) {
                if (attacker instanceof Player player) {
                    nearby.hurt(serverLevel.damageSources().playerAttack(player), baseDmg);
                } else {
                    nearby.hurt(serverLevel.damageSources().mobAttack(attacker), baseDmg);
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
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!(user instanceof Player player) || getUseDuration(stack, player) - remaining < CHARGE_TICKS) {
            return;
        }
        player.swing(player.getUsedItemHand(), true);
        if (level.isClientSide) {
            return;
        }
        float yaw = player.getYRot();
        Vec3 front = player.position().add(-Math.sin(Math.toRadians(yaw)) * 1.4D, 0.0D, Math.cos(Math.toRadians(yaw)) * 1.4D);
        HitResult hit = level.clip(new ClipContext(front.add(0.0D, 1.8D, 0.0D), front.add(0.0D, -3.5D, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return;
        }
        Vec3 at = hit.getLocation();
        float baseAttack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        MagmaEffect.shockwave(player, at, 3.0F, 0);
        MagmaEffect.fissure(player, at, yaw, 3, 9.0F, Math.max(8.0F, baseAttack * 0.95F));
        GildedWorldVfxPayload.sendDirected(player, GildedWorldVfxPayload.KIND_HEAVY_IMPACT, at, new Vec3(1.25D, yaw, 0.0D), 3.2F, 18);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.1F, 0.65F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.85F);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(2, player, slot);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(GildedTyrantRegistry.GILDED_CORE.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hp_boss_expansion.gilded_greataxe.desc1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.hp_boss_expansion.gilded_greataxe.desc2").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            consumer.accept(GildedTyrantClientEvents.createGreataxeRenderProvider());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(ANIM_IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
