package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.cindermaw.client.CinderFx;
import com.hp.bossexpansion.cindermaw.client.VfxDraw;
import com.hp.bossexpansion.gildedtyrant.network.GildedWorldVfxPayload;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = BossExpansion.MOD_ID, value = Dist.CLIENT)
public final class GildedWorldVfxClient {
    private static final Identifier AXE_MODEL_ID = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_greataxe");
    private static final Identifier AXE_TEX = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/item/gilded_greataxe.png");
    private static final Identifier AXE_GLOW_TEX = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/item/gilded_greataxe_glowmask.png");
    private static final List<ActiveEffect> EFFECTS = new ArrayList<>();
    private static ItemStack cachedGreataxeStack;

    private GildedWorldVfxClient() {
    }

    public static void spawn(GildedWorldVfxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Vec3 pos = new Vec3(p.x(), p.y(), p.z());
        Vec3 vec = new Vec3(p.vx(), p.vy(), p.vz());
        if (p.kind() == GildedWorldVfxPayload.KIND_HEAVY_IMPACT) {
            EFFECTS.removeIf(existing -> existing.kind == GildedWorldVfxPayload.KIND_LEAP_WARN
                    && existing.pos.distanceToSqr(pos) <= 64.0D);
        }
        if (EFFECTS.size() > 180) {
            EFFECTS.removeFirst();
        }
        ActiveEffect fx = new ActiveEffect(p.kind(), pos, vec, p.scale(), Math.max(1, p.duration()));
        EFFECTS.add(fx);
        onEffectSpawned(level, fx);
    }

    private static void onEffectSpawned(ClientLevel level, ActiveEffect fx) {
        RandomSource random = level.getRandom();
        switch (fx.kind) {
            case GildedWorldVfxPayload.KIND_FISSURE_NODE -> {
                CinderFx.shake(fx.pos, 0.30F, 13.5F);
                burst(level, CindermawParticles.EMBER.get(), fx.pos.add(0.0D, 0.2D, 0.0D), 9, 0.26D, 0.28D, random);
                burst(level, CindermawParticles.BLAZE_FLAME.get(), fx.pos.add(0.0D, 0.15D, 0.0D), 4, 0.12D, 0.18D, random);
                debris(level, fx.pos, 3, 0.18D, random);
            }
            case GildedWorldVfxPayload.KIND_HEAVY_IMPACT -> {
                float power = Math.max(0.6F, (float) fx.vec.x);
                CinderFx.shake(fx.pos, Math.min(1.55F, 0.50F * power), 14.0F + 5.5F * power);
                level.addParticle(CindermawParticles.HEAT_FLASH.get(), fx.pos.x, fx.pos.y + 0.45D, fx.pos.z, Math.min(5.0D, fx.scale * 0.72D), 0.0D, 0.0D);
                burst(level, CindermawParticles.EMBER.get(), fx.pos.add(0.0D, 0.25D, 0.0D), (int) (16 * power), 0.36D + 0.10D * power, 0.22D, random);
                burst(level, CindermawParticles.BLAZE_FLAME.get(), fx.pos.add(0.0D, 0.20D, 0.0D), (int) (7 * power), 0.22D + 0.06D * power, 0.10D, random);
                debris(level, fx.pos, (int) (6 * power), 0.24D + 0.05D * power, random);
            }
            case GildedWorldVfxPayload.KIND_OBELISK_SUMMON -> {
                boolean shattered = fx.vec.x > 0.5D;
                CinderFx.shake(fx.pos, shattered ? 0.52F : 0.35F, 15.0F);
                level.addParticle(CindermawParticles.HEAT_FLASH.get(), fx.pos.x, fx.pos.y + 1.1D, fx.pos.z, 2.2D, 0.0D, 0.0D);
                burst(level, CindermawParticles.EMBER.get(), fx.pos.add(0.0D, 0.9D, 0.0D), shattered ? 24 : 14, 0.32D, 0.20D, random);
                if (shattered) {
                    debris(level, fx.pos.add(0.0D, 0.5D, 0.0D), 8, 0.25D, random);
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            EFFECTS.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        RandomSource random = level.getRandom();
        Iterator<ActiveEffect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            ActiveEffect fx = it.next();
            fx.age++;
            tickEffectParticles(level, fx, random);
            if (fx.age >= fx.duration) {
                it.remove();
            }
        }
    }

    private static void tickEffectParticles(ClientLevel level, ActiveEffect fx, RandomSource random) {
        if (fx.kind == GildedWorldVfxPayload.KIND_GOLD_SPIKE) {
            int warn = Math.max(1, Mth.floor(fx.vec.x));
            if (fx.age < warn && fx.age % 2 == 0) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double r = random.nextDouble() * fx.scale * 0.8D;
                level.addParticle(CindermawParticles.EMBER.get(),
                        fx.pos.x + Math.cos(ang) * r, fx.pos.y + 0.08D, fx.pos.z + Math.sin(ang) * r,
                        0.0D, 0.08D + random.nextDouble() * 0.06D, 0.0D);
            } else if (fx.age == warn) {
                CinderFx.shake(fx.pos, 0.45F, 14.0F);
                level.addParticle(CindermawParticles.HEAT_FLASH.get(), fx.pos.x, fx.pos.y + 0.5D, fx.pos.z, 1.9D, 0.0D, 0.0D);
                burst(level, CindermawParticles.EMBER.get(), fx.pos.add(0.0D, 0.3D, 0.0D), 16, 0.30D, 0.28D, random);
                debris(level, fx.pos, 5, 0.20D, random);
            }
        } else if (fx.kind == GildedWorldVfxPayload.KIND_FALLING_AXE) {
            int warn = Math.max(1, Mth.floor(fx.vec.x));
            if (fx.age < warn) {
                float k = fx.age / (float) warn;
                double skyY = fx.pos.y + Mth.lerp(k * k, 18.5D, 0.8D);
                for (int i = 0; i < 2; i++) {
                    level.addParticle(CindermawParticles.EMBER.get(),
                            fx.pos.x + (random.nextDouble() - 0.5D) * 0.8D,
                            skyY + random.nextDouble() * 1.4D,
                            fx.pos.z + (random.nextDouble() - 0.5D) * 0.8D,
                            (random.nextDouble() - 0.5D) * 0.08D, 0.12D, (random.nextDouble() - 0.5D) * 0.08D);
                }
            } else if (fx.age == warn) {
                CinderFx.shake(fx.pos, 0.78F, 22.0F);
                level.addParticle(CindermawParticles.HEAT_FLASH.get(), fx.pos.x, fx.pos.y + 0.65D, fx.pos.z, 3.2D, 0.0D, 0.0D);
                burst(level, CindermawParticles.EMBER.get(), fx.pos.add(0.0D, 0.35D, 0.0D), 24, 0.45D, 0.28D, random);
                burst(level, CindermawParticles.BLAZE_FLAME.get(), fx.pos.add(0.0D, 0.25D, 0.0D), 10, 0.26D, 0.14D, random);
                debris(level, fx.pos, 8, 0.28D, random);
            }
        }
    }

    private static void burst(ClientLevel level, SimpleParticleType type, Vec3 at, int count, double speed, double lift, RandomSource random) {
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D);
            if (dir.lengthSqr() < 1.0E-4D) {
                dir = new Vec3(0.0D, 1.0D, 0.0D);
            }
            dir = dir.normalize().scale(speed * (0.45D + random.nextDouble() * 0.55D));
            level.addParticle(type, at.x, at.y, at.z, dir.x, dir.y + lift, dir.z);
        }
    }

    private static void debris(ClientLevel level, Vec3 at, int count, double speed, RandomSource random) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double s = speed * (0.45D + random.nextDouble() * 0.55D);
            level.addParticle(CindermawParticles.MAGMA_CHUNK.get(),
                    at.x + (random.nextDouble() - 0.5D) * 0.6D,
                    at.y + 0.12D,
                    at.z + (random.nextDouble() - 0.5D) * 0.6D,
                    Math.cos(angle) * s, 0.32D + random.nextDouble() * 0.40D, Math.sin(angle) * s);
        }
    }

    private static double snapToGround(Level level, double x, double y, double z) {
        BlockPos base = BlockPos.containing(x, y + 1.2D, z);
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos check = base.below(dy);
            BlockState state = level.getBlockState(check);
            if (!state.getCollisionShape(level, check).isEmpty()) {
                return check.getY() + state.getCollisionShape(level, check).max(Direction.Axis.Y);
            }
        }
        return y;
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (EFFECTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Quaternionf camRot = camera.rotation();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean renderedFallingAxe = false;

        ps.pushPose();
        ps.translate(-camPos.x, -camPos.y, -camPos.z);

        for (ActiveEffect fx : EFFECTS) {
            float age = fx.age + partialTick;
            if (age > fx.duration) {
                continue;
            }
            ps.pushPose();
            ps.translate(fx.pos.x, fx.pos.y, fx.pos.z);
            switch (fx.kind) {
                case GildedWorldVfxPayload.KIND_FISSURE_NODE -> renderFissureNode(fx, age, ps, buffers, camRot);
                case GildedWorldVfxPayload.KIND_GOLD_SPIKE -> renderGoldSpike(fx, age, ps, buffers, camRot);
                case GildedWorldVfxPayload.KIND_FALLING_AXE -> {
                    renderFallingAxe(mc, fx, age, ps, buffers, camRot);
                    renderedFallingAxe = true;
                }
                case GildedWorldVfxPayload.KIND_HEAVY_IMPACT -> renderHeavyImpact(fx, age, ps, buffers, camRot);
                case GildedWorldVfxPayload.KIND_LEAP_WARN -> renderLeapWarn(fx, age, ps, buffers);
                case GildedWorldVfxPayload.KIND_OBELISK_SUMMON -> renderObeliskSummon(fx, age, ps, buffers, camRot);
                default -> {
                }
            }
            ps.popPose();
        }

        ps.popPose();

        if (renderedFallingAxe) {
            buffers.endBatch(RenderTypes.entityCutout(AXE_TEX));
            buffers.endBatch(RenderTypes.eyes(AXE_GLOW_TEX));
        }
        buffers.endBatch(VfxDraw.SOFT);
        buffers.endBatch(VfxDraw.GLOW);
        buffers.endBatch(VfxDraw.FLAME);
        buffers.endBatch(RenderTypes.entityTranslucent(GildedTyrantVfx.RING));
        buffers.endBatch(RenderTypes.entityTranslucent(GildedTyrantVfx.RIBBON));
        buffers.endBatch(RenderTypes.entityTranslucent(GildedTyrantVfx.SLASH));
        buffers.endBatch(RenderTypes.entityTranslucent(GildedTyrantVfx.ARC_STRIP));
        buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.RING));
        buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.RIBBON));
        buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.SLASH));
        buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.ARC_STRIP));
        buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.FLARE));
        buffers.endBatch(GildedTyrantVfx.additive(GildedTyrantVfx.CHAIN));
    }

    private static void drawRadialCracks(PoseStack ps, MultiBufferSource buffers, int count, double length, double width, float baseYawDeg, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        VertexConsumer glow = buffers.getBuffer(VfxDraw.GLOW);
        float[] cr = VfxDraw.CRACK;
        for (int i = 0; i < count; i++) {
            double ang = Math.toRadians(baseYawDeg + i * (360.0D / count));
            Vec3 dir = new Vec3(Math.cos(ang), 0.0D, Math.sin(ang));
            Vec3 side = new Vec3(-dir.z * width, 0.0D, dir.x * width);
            Vec3 a = dir.scale(0.18D).add(0.0D, 0.035D, 0.0D);
            Vec3 b = dir.scale(length).add(0.0D, 0.035D, 0.0D);
            VfxDraw.quad2(glow, ps, a.add(side), b.add(side.scale(0.35D)), b.subtract(side.scale(0.35D)), a.subtract(side),
                    cr[0], cr[1], cr[2], cr[3], 0xFFD448, alpha);
        }
    }

    private static void renderFissureNode(ActiveEffect fx, float age, PoseStack ps, MultiBufferSource buffers, Quaternionf camRot) {
        float k = Mth.clamp(age / fx.duration, 0.0F, 1.0F);
        float fade = k < 0.55F ? 1.0F : (1.0F - k) / 0.45F;
        float r = fx.scale;

        VfxDraw.flat(buffers, VfxDraw.SOFT, ps, new Vec3(0.0D, 0.02D, 0.0D), r * 1.1D, VfxDraw.SCORCH, 0x220B04, 0.78F * fade);

        float dirYaw = (float) (Mth.atan2(fx.vec.z, fx.vec.x) * (180.0D / Math.PI));
        VertexConsumer glow = buffers.getBuffer(VfxDraw.GLOW);
        float[] cr = VfxDraw.CRACK;
        double ang = Math.toRadians(dirYaw);
        Vec3 along = new Vec3(Math.cos(ang), 0.0D, Math.sin(ang));
        Vec3 perp = new Vec3(-along.z, 0.0D, along.x);
        Vec3 p0 = along.scale(-r * 0.85D).add(0.0D, 0.04D, 0.0D);
        Vec3 p1 = along.scale(r * 0.95D).add(0.0D, 0.04D, 0.0D);
        VfxDraw.quad2(glow, ps, p0.add(perp.scale(0.42D)), p1.add(perp.scale(0.38D)), p1.subtract(perp.scale(0.38D)), p0.subtract(perp.scale(0.42D)),
                cr[0], cr[1], cr[2], cr[3], 0xFFE268, fade);
        drawRadialCracks(ps, buffers, 4, r * 0.95D, 0.22D, dirYaw + 38.0F, 0.82F * fade);

        float spikeRise = age < 3.5F ? (age / 3.5F) : 1.0F;
        spikeRise = 1.0F - (1.0F - spikeRise) * (1.0F - spikeRise);
        float sink = k > 0.68F ? (k - 0.68F) / 0.32F : 0.0F;
        float hScale = spikeRise * (1.0F - sink * 0.75F);
        GildedTyrantVfx.drawGoldenPrismSpike(ps, buffers, Vec3.ZERO, -dirYaw, 6.0F, 0.32F, 1.95F * hScale, fade);
        GildedTyrantVfx.drawGoldenPrismSpike(ps, buffers, perp.scale(0.35D), -dirYaw + 75.0F, 24.0F, 0.22F, 1.25F * hScale, fade * 0.92F);
        GildedTyrantVfx.drawGoldenPrismSpike(ps, buffers, perp.scale(-0.35D), -dirYaw - 80.0F, 22.0F, 0.22F, 1.18F * hScale, fade * 0.92F);

        if (age < 12.0F) {
            float gk = age / 12.0F;
            double gh = 3.4D * Math.sin(Math.PI * gk);
            VfxDraw.column(buffers, ps, Vec3.ZERO, new Vec3(0.0D, gh, 0.0D), 0.52D, 0.18D, 3, -age * 0.35F, 1.2F, 0xFFC428, (1.0F - gk * 0.4F) * 0.92F);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.05D, 0.45F + r * 0.88F * gk, age * 22.0F, 1.0F, 0.88F, 0.32F, 0.88F * (1.0F - gk), 0.92F * (1.0F - gk));
            GildedTyrantVfx.drawBillboardFlare(ps, buffers, camRot, new Vec3(0.0D, 0.55D, 0.0D), 1.05F * (1.0F - gk), age * 18.0F, 1.0F, 0.90F * (1.0F - gk), 0.38F * (1.0F - gk));
        }
    }

    private static void renderGoldSpike(ActiveEffect fx, float age, PoseStack ps, MultiBufferSource buffers, Quaternionf camRot) {
        float warnTicks = Math.max(1.0F, (float) fx.vec.x);
        float radius = fx.scale;
        if (age < warnTicks) {
            float k = Mth.clamp(age / warnTicks, 0.0F, 1.0F);
            float fadeIn = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
            float pulse = k > 0.65F ? (0.76F + 0.24F * Mth.sin(age * 2.8F)) : 1.0F;
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.04D, radius, -age * 12.0F, 1.0F, 0.84F, 0.26F, (0.35F + 0.50F * k) * fadeIn, 0.72F * k * pulse);
            float shrinkR = radius * (1.08F - 0.78F * k);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.06D, shrinkR, age * 24.0F, 1.0F, 0.94F, 0.48F, (0.42F + 0.50F * k) * fadeIn, 0.88F * k * pulse);
            GildedTyrantVfx.drawFunnelWall(ps, buffers, 0.03D, 0.35D + 1.45D * k * k, 0.22F, radius * (0.65F - 0.25F * k), -age * 0.36F,
                    1.0F, 0.88F, 0.34F, 0.58F * k, 0.65F * k);
        } else {
            float imp = age - warnTicks;
            float dur = Math.max(1.0F, fx.duration - warnTicks);
            float k = Mth.clamp(imp / dur, 0.0F, 1.0F);
            float fade = k < 0.55F ? 1.0F : (1.0F - k) / 0.45F;
            float thrust = Mth.clamp(imp / 2.6F, 0.0F, 1.0F);
            thrust = 1.0F - (float) Math.pow(1.0F - thrust, 3.0D);

            VfxDraw.flat(buffers, VfxDraw.SOFT, ps, new Vec3(0.0D, 0.02D, 0.0D), radius * 0.95D, VfxDraw.SCORCH, 0x240C04, 0.72F * fade);
            drawRadialCracks(ps, buffers, 5, radius * 0.95D, 0.22D, 18.0F, 0.85F * fade);

            GildedTyrantVfx.drawGoldenPrismSpike(ps, buffers, Vec3.ZERO, 22.0F, 0.0F, 0.42F, 3.15F * thrust, fade);
            for (int i = 0; i < 4; i++) {
                float yaw = i * 90.0F + 35.0F;
                double rad = Math.toRadians(yaw);
                Vec3 off = new Vec3(-Math.sin(rad) * 0.38D, 0.0D, Math.cos(rad) * 0.38D);
                GildedTyrantVfx.drawGoldenPrismSpike(ps, buffers, off, yaw, 26.0F, 0.28F, 1.95F * thrust, fade * 0.95F);
            }

            float ringK = Mth.clamp(imp / 10.0F, 0.0F, 1.0F);
            if (ringK < 1.0F) {
                float rf = 1.0F - ringK;
                float shockR = 0.45F + radius * 1.15F * (1.0F - (1.0F - ringK) * (1.0F - ringK));
                GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.06D, shockR, imp * 26.0F, 1.0F, 0.88F, 0.34F, 0.92F * rf, 0.95F * rf);
                VfxDraw.wall(buffers, ps, Vec3.ZERO, shockR * 0.85D, 1.05D * rf, -imp * 0.25F, 0xFFCA32, 0.72F * rf);
                GildedTyrantVfx.drawBillboardFlare(ps, buffers, camRot, new Vec3(0.0D, 0.85D, 0.0D), 1.45F * rf, -imp * 18.0F, 1.0F, 0.92F * rf, 0.45F * rf);
            }
        }
    }

    private static ItemStack getGreataxeStack() {
        if (cachedGreataxeStack == null || cachedGreataxeStack.isEmpty()) {
            cachedGreataxeStack = GildedTyrantRegistry.GILDED_GREATAXE.get().getDefaultInstance();
        }
        return cachedGreataxeStack;
    }

    private static void renderGiantGreataxeModel(Minecraft mc, PoseStack ps, MultiBufferSource buffers, double embedY, float yawDeg, float scale) {
        BakedGeoModel model = GeckoLibResources.getBakedModels().getModel(AXE_MODEL_ID);
        if (model == null || model.isMissingno()) {
            return;
        }
        ps.pushPose();
        ps.translate(0.0D, embedY, 0.0D);
        ps.mulPose(Axis.YP.rotationDegrees(yawDeg));
        ps.mulPose(Axis.XP.rotationDegrees(174.0F));
        ps.scale(scale, scale, scale);
        ps.translate(0.0D, -3.55D, 0.0D);
        VertexConsumer baseOut = buffers.getBuffer(RenderTypes.entityCutout(AXE_TEX));
        for (GeoBone bone : model.topLevelBones()) {
            renderBakedBoneStatic(bone, ps, baseOut);
        }
        VertexConsumer glowOut = buffers.getBuffer(RenderTypes.eyes(AXE_GLOW_TEX));
        for (GeoBone bone : model.topLevelBones()) {
            renderBakedBoneStatic(bone, ps, glowOut);
        }
        ps.popPose();
    }

    private static void renderBakedBoneStatic(GeoBone bone, PoseStack ps, VertexConsumer vc) {
        ps.pushPose();
        bone.translateToPivotPoint(ps);
        if (bone.baseRotZ() != 0.0F) {
            ps.mulPose(Axis.ZP.rotation(bone.baseRotZ()));
        }
        if (bone.baseRotY() != 0.0F) {
            ps.mulPose(Axis.YP.rotation(bone.baseRotY()));
        }
        if (bone.baseRotX() != 0.0F) {
            ps.mulPose(Axis.XP.rotation(bone.baseRotX()));
        }
        bone.translateAwayFromPivotPoint(ps);
        if (bone instanceof CuboidGeoBone cuboid) {
            for (GeoCube cube : cuboid.cubes) {
                cube.render(ps, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            }
        }
        for (GeoBone child : bone.children()) {
            renderBakedBoneStatic(child, ps, vc);
        }
        ps.popPose();
    }

    private static void renderFallingAxe(Minecraft mc, ActiveEffect fx, float age, PoseStack ps, MultiBufferSource buffers, Quaternionf camRot) {
        float warnTicks = Math.max(1.0F, (float) fx.vec.x);
        float yaw = (float) fx.vec.y;
        float radius = fx.scale;

        if (age < warnTicks) {
            float k = Mth.clamp(age / warnTicks, 0.0F, 1.0F);
            float fadeIn = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
            float pulse = k > 0.65F ? (0.75F + 0.25F * Mth.sin(age * 3.0F)) : 1.0F;

            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.04D, radius, -age * 12.0F, 1.0F, 0.82F, 0.24F, (0.38F + 0.52F * k) * fadeIn, 0.78F * k * pulse);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.06D, radius * (1.12F - 0.82F * k), age * 26.0F, 1.0F, 0.94F, 0.48F, (0.45F + 0.50F * k) * fadeIn, 0.92F * k * pulse);
            VfxDraw.column(buffers, ps, Vec3.ZERO, new Vec3(0.0D, 18.0D, 0.0D), 0.16D + 0.14D * k, 0.08D, 2, -age * 0.45F, 3.2F, 0xFFE060, 0.48F * k);

            double fallY = Mth.lerp(k * k, 18.5D, 0.22D);
            renderGiantGreataxeModel(mc, ps, buffers, fallY, yaw, 1.65F);

            GildedTyrantVfx.drawFunnelWall(ps, buffers, fallY + 2.1D, fallY + 6.4D, 0.28F, 1.05F, age * 0.52F,
                    1.0F, 0.86F, 0.28F, 0.45F * fadeIn, 0.58F * fadeIn);
            GildedTyrantVfx.drawBillboardFlare(ps, buffers, camRot, new Vec3(0.0D, fallY + 0.55D, 0.0D), 1.05F + 0.45F * k, age * 22.0F, 1.0F, 0.92F, 0.42F);
        } else {
            float imp = age - warnTicks;
            float dur = Math.max(1.0F, fx.duration - warnTicks);
            float k = Mth.clamp(imp / dur, 0.0F, 1.0F);
            float fade = 1.0F - k;
            float ease = 1.0F - (float) Math.pow(1.0F - k, 3.0D);

            VfxDraw.flat(buffers, VfxDraw.SOFT, ps, new Vec3(0.0D, 0.02D, 0.0D), radius * 1.05D, VfxDraw.SCORCH, 0x240A04, 0.82F * fade);
            drawRadialCracks(ps, buffers, 6, radius * 1.08D, 0.28D, yaw, 0.92F * fade);

            if (k < 0.82F) {
                float sinkK = k < 0.55F ? 0.0F : (k - 0.55F) / 0.27F;
                double embedY = 0.22D - sinkK * 2.8D;
                float axeScale = 1.65F * (1.0F - sinkK * 0.25F);
                renderGiantGreataxeModel(mc, ps, buffers, embedY, yaw, axeScale);
            }

            VfxDraw.column(buffers, ps, Vec3.ZERO, new Vec3(0.0D, 8.5D * (1.0D - k * 0.35D), 0.0D),
                    0.85D + 0.55D * ease, 1.65D + 0.75D * ease, 3, -imp * 0.34F, 2.2F, 0xFFC82C, 0.92F * fade);
            VfxDraw.dome(buffers, ps, Vec3.ZERO, 0.65D + radius * 0.92D * ease, -imp * 0.26F, 0xFFFDF0, 0xCC2408, 0.82F * fade);
            VfxDraw.wall(buffers, ps, Vec3.ZERO, radius * (0.35D + 0.82D * ease), 1.65D * fade + 0.25D, -imp * 0.24F, 0xFFBC24, 0.82F * fade);

            float shockR = radius * (0.40F + 0.88F * ease);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.06D, shockR, imp * 24.0F, 1.0F, 0.88F, 0.32F, 0.95F * fade, fade);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.08D, shockR * 0.72F, -imp * 30.0F, 1.0F, 0.95F, 0.52F, 0.82F * fade, 0.88F * fade);
            if (k < 0.5F) {
                float fk = 1.0F - k / 0.5F;
                GildedTyrantVfx.drawBillboardFlare(ps, buffers, camRot, new Vec3(0.0D, 1.15D, 0.0D), 2.35F * fk, -imp * 16.0F, 1.0F, 0.94F * fk, 0.48F * fk);
            }
        }
    }

    private static void renderHeavyImpact(ActiveEffect fx, float age, PoseStack ps, MultiBufferSource buffers, Quaternionf camRot) {
        float k = Mth.clamp(age / fx.duration, 0.0F, 1.0F);
        float ease = 1.0F - (float) Math.pow(1.0F - k, 3.0D);
        float fade = 1.0F - k;
        float power = Math.max(0.6F, (float) fx.vec.x);
        float yaw = (float) fx.vec.y;
        float maxR = fx.scale;
        float curR = 0.55F + maxR * ease;

        VfxDraw.flat(buffers, VfxDraw.SOFT, ps, new Vec3(0.0D, 0.02D, 0.0D), maxR * 0.82D, VfxDraw.SCORCH, 0x220A04, 0.76F * fade);
        drawRadialCracks(ps, buffers, power >= 1.5F ? 8 : 6, maxR * 0.92D, 0.22D + 0.06D * power, yaw, 0.88F * fade);

        VfxDraw.dome(buffers, ps, Vec3.ZERO, curR * 0.78D, -age * 0.25F, 0xFFFDF0, 0xC82408, Math.min(0.92F, 0.65F * power) * fade);
        VfxDraw.wall(buffers, ps, Vec3.ZERO, curR * 0.92D, (0.85D + 0.55D * power) * fade, -age * 0.22F, 0xFFBC24, 0.80F * fade);

        GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.06D, curR, age * 22.0F, 1.0F, 0.86F, 0.28F, 0.94F * fade, fade);
        GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.08D, curR * 0.74F, -age * 28.0F, 1.0F, 0.95F, 0.52F, 0.80F * fade, 0.85F * fade);

        if (power >= 1.25F) {
            ps.pushPose();
            ps.translate(0.0D, 0.65D, 0.0D);
            ps.mulPose(Axis.XP.rotationDegrees(22.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(-18.0F));
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.0D, curR * 0.82F, -age * 24.0F, 1.0F, 0.88F, 0.34F, 0.72F * fade, 0.78F * fade);
            ps.popPose();
        }
        if (power >= 1.45F) {
            VfxDraw.column(buffers, ps, Vec3.ZERO, new Vec3(0.0D, (4.2D + 2.2D * power) * (0.45D + 0.55D * ease), 0.0D),
                    0.75D + 0.35D * power, 1.45D + 0.55D * power, 3, -age * 0.30F, 2.0F, 0xFFC82E, 0.88F * fade);
        }
        if (k < 0.52F) {
            float fk = 1.0F - k / 0.52F;
            GildedTyrantVfx.drawBillboardFlare(ps, buffers, camRot, new Vec3(0.0D, 0.85D, 0.0D),
                    (1.35F + 0.85F * power) * fk, age * 16.0F, 1.0F, 0.92F * fk, 0.45F * fk);
        }
    }

    private static void renderLeapWarn(ActiveEffect fx, float age, PoseStack ps, MultiBufferSource buffers) {
        float k = Mth.clamp(age / fx.duration, 0.0F, 1.0F);
        float fadeIn = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        float fadeOut = k > 0.85F ? (1.0F - k) / 0.15F : 1.0F;
        float env = fadeIn * fadeOut;
        float innerR = fx.scale;

        GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.04D, innerR, -age * 16.0F, 1.0F, 0.80F, 0.22F, 0.78F * env, 0.85F * env);
        GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.06D, innerR * (1.15F - 0.82F * k), age * 28.0F, 1.0F, 0.94F, 0.48F, (0.45F + 0.50F * k) * env, 0.95F * k * env);
        GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.03D, 9.0F, age * 6.0F, 1.0F, 0.84F, 0.28F, 0.26F * env, 0.28F * env);
        GildedTyrantVfx.drawFunnelWall(ps, buffers, 0.05D, 0.45D + 2.8D * k * k, innerR * 0.42F, innerR * 0.92F, -age * 0.42F,
                1.0F, 0.84F, 0.26F, 0.58F * k * env, 0.68F * k * env);
    }

    private static void renderObeliskSummon(ActiveEffect fx, float age, PoseStack ps, MultiBufferSource buffers, Quaternionf camRot) {
        float k = Mth.clamp(age / fx.duration, 0.0F, 1.0F);
        float ease = 1.0F - (float) Math.pow(1.0F - k, 3.0D);
        float fade = 1.0F - k;
        boolean shattered = fx.vec.x > 0.5D;

        if (!shattered) {
            VfxDraw.column(buffers, ps, Vec3.ZERO, new Vec3(0.0D, 6.8D * (1.0D - k * 0.3D), 0.0D),
                    0.65D + 0.45D * ease, 1.15D + 0.55D * ease, 3, -age * 0.32F, 2.0F, 0xFFD438, 0.90F * fade);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 0.05D, 0.8F + fx.scale * ease, age * 22.0F, 1.0F, 0.90F, 0.38F, 0.92F * fade, 0.95F * fade);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 1.85D, 0.5F + fx.scale * 0.65F * ease, -age * 28.0F, 1.0F, 0.95F, 0.55F, 0.78F * fade, 0.85F * fade);
            GildedTyrantVfx.drawBillboardFlare(ps, buffers, camRot, new Vec3(0.0D, 1.85D, 0.0D), 1.85F * fade, age * 18.0F, 1.0F, 0.92F * fade, 0.45F * fade);
        } else {
            VfxDraw.dome(buffers, ps, new Vec3(0.0D, 0.9D, 0.0D), 0.5D + fx.scale * 0.95D * ease, -age * 0.30F, 0xFFF8D0, 0xC81E08, 0.85F * fade);
            GildedTyrantVfx.drawRoyalRing(ps, buffers, 1.05D, 0.6F + fx.scale * 1.15F * ease, -age * 30.0F, 1.0F, 0.72F, 0.18F, 0.90F * fade, 0.95F * fade);
            GildedTyrantVfx.drawBillboardFlare(ps, buffers, camRot, new Vec3(0.0D, 1.25D, 0.0D), 2.25F * fade, -age * 22.0F, 1.0F, 0.75F * fade, 0.25F * fade);
        }
    }

    private static final class ActiveEffect {
        final int kind;
        final Vec3 pos;
        final Vec3 vec;
        final float scale;
        final int duration;
        int age;

        ActiveEffect(int kind, Vec3 pos, Vec3 vec, float scale, int duration) {
            this.kind = kind;
            this.pos = pos;
            this.vec = vec;
            this.scale = scale;
            this.duration = duration;
        }
    }
}
