package com.hp.bossexpansion.cindermaw.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

// 客户端注册只在客户端事件总线加载，服务端不会解析任何渲染类
@Mod.EventBusSubscriber(modid = BossExpansion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CindermawClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CindermawRegistry.CINDERMAW.get(), CindermawRenderer::new);
        event.registerEntityRenderer(CindermawRegistry.LAVA_BOMB.get(), LavaBombRenderer::new);
        event.registerEntityRenderer(CindermawRegistry.MAGMA_EFFECT.get(), MagmaEffectRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CindermawParticles.EMBER.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.EMBER));
        event.registerSpriteSet(CindermawParticles.CINDER_SMOKE.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.SMOKE));
        event.registerSpriteSet(CindermawParticles.BLAZE_FLAME.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.FLAME));
        event.registerSpriteSet(CindermawParticles.MAGMA_CHUNK.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.CHUNK));
        event.registerSpriteSet(CindermawParticles.HEAT_FLASH.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.FLASH));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PauldronClientController.VOLCANIC_CANNON_KEY);
    }

    public static IClientItemExtensions createPauldronClientExtensions() {
        return new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new CindermawPauldronRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        };
    }

    private CindermawClientEvents() {
    }
}
