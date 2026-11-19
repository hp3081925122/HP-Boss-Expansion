package com.hp.bossexpansion.cindermaw.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.cindermaw.CindermawParticles;

// 客户端注册只在 Forge 客户端事件总线加载，服务器不会解析渲染类。
@Mod.EventBusSubscriber(modid = BossExpansion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CindermawClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CindermawRegistry.CINDERMAW.get(), CindermawRenderer::new);
        event.registerEntityRenderer(CindermawRegistry.LAVA_BOMB.get(), LavaBombRenderer::new);
        event.registerEntityRenderer(CindermawRegistry.MAGMA_EFFECT.get(), MagmaEffectRenderer::new);
    }

    // 五种像素粒子分别绑定行为样式
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CindermawParticles.EMBER.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.EMBER));
        event.registerSpriteSet(CindermawParticles.CINDER_SMOKE.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.SMOKE));
        event.registerSpriteSet(CindermawParticles.BLAZE_FLAME.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.FLAME));
        event.registerSpriteSet(CindermawParticles.MAGMA_CHUNK.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.CHUNK));
        event.registerSpriteSet(CindermawParticles.HEAT_FLASH.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.FLASH));
    }

    private CindermawClientEvents() {
    }
}
