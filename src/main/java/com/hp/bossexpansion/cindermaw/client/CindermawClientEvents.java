package com.hp.bossexpansion.cindermaw.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.cindermaw.CindermawParticles;

// 客户端注册只在 Forge 客户端事件总线加载，服务器不会解析渲染类。
public final class CindermawClientEvents {
    @SubscribeEvent
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(VfxDraw.ADDITIVE);
    }

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
