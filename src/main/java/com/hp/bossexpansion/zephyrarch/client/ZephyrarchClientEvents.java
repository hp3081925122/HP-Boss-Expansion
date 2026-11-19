package com.hp.bossexpansion.zephyrarch.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.zephyrarch.registry.ZephyrarchRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BossExpansion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ZephyrarchClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYRARCH.get(), ZephyrarchRenderer::new);
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYR_CHARGE.get(), ZephyrChargeRenderer::new);
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYR_BLADE.get(), ZephyrWindRenderers.Blade::new);
        event.registerEntityRenderer(ZephyrarchRegistry.ZEPHYR_TWISTER.get(), ZephyrWindRenderers.Twister::new);
    }

    public static IClientItemExtensions createGauntletClientExtensions() {
        return new IClientItemExtensions() {
            private SovereignGauntletRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new SovereignGauntletRenderer();
                }
                return this.renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return GauntletClientController.GAUNTLET_ARM_POSE;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                                   float partialTick, float equipProcess, float swingProcess) {
                return GauntletClientController.applyFirstPersonTransform(poseStack, player, arm, partialTick, equipProcess);
            }
        };
    }

    private ZephyrarchClientEvents() {
    }
}
