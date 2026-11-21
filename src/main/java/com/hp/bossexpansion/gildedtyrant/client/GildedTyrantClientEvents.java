package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
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
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = BossExpansion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GildedTyrantClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GildedTyrantRegistry.GILDED_TYRANT.get(), GildedTyrantRenderer::new);
        event.registerEntityRenderer(GildedTyrantRegistry.THROWN_GREATAXE.get(), GildedSubRenderers.ThrownAxe::new);
        event.registerEntityRenderer(GildedTyrantRegistry.GILDED_OBELISK.get(), GildedSubRenderers.Obelisk::new);
    }

    public static IClientItemExtensions createGreataxeClientExtensions() {
        return new IClientItemExtensions() {
            private GildedSubRenderers.GreataxeItem renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GildedSubRenderers.GreataxeItem();
                }
                return this.renderer;
            }

            @Override
            @Nullable
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return GreataxeClientController.resolveArmPose(entityLiving, hand);
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                                   float partialTick, float equipProcess, float swingProcess) {
                return GreataxeClientController.applyFirstPersonTransform(poseStack, player, arm, partialTick, equipProcess, swingProcess);
            }
        };
    }

    private GildedTyrantClientEvents() {
    }
}
