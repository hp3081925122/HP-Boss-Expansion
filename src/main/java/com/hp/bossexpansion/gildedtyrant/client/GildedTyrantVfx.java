package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class GildedTyrantVfx {
    public static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/effect/gilded_ring.png");
    public static final ResourceLocation CHAIN = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/effect/gilded_chain.png");
    public static final ResourceLocation FLARE = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/effect/gilded_flare.png");
    public static final ResourceLocation SLASH = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/effect/gilded_slash.png");

    private GildedTyrantVfx() {
    }

    private static void vtx(PoseStack.Pose pose, VertexConsumer vc, Vec3 p, float u, float v, float r, float g, float b, float a) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(Mth.clamp(r, 0.0F, 1.0F), Mth.clamp(g, 0.0F, 1.0F), Mth.clamp(b, 0.0F, 1.0F), Mth.clamp(a, 0.0F, 1.0F))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public static void quadTwoSided(PoseStack.Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                    float u0, float v0, float u1, float v1, float r, float g, float bl, float alpha) {
        vtx(pose, vc, a, u0, v0, r, g, bl, alpha);
        vtx(pose, vc, b, u1, v0, r, g, bl, alpha);
        vtx(pose, vc, c, u1, v1, r, g, bl, alpha);
        vtx(pose, vc, d, u0, v1, r, g, bl, alpha);
        vtx(pose, vc, d, u0, v1, r, g, bl, alpha);
        vtx(pose, vc, c, u1, v1, r, g, bl, alpha);
        vtx(pose, vc, b, u1, v0, r, g, bl, alpha);
        vtx(pose, vc, a, u0, v0, r, g, bl, alpha);
    }

    public static void renderCrossBeam(PoseStack ps, MultiBufferSource buffers, ResourceLocation tex,
                                       Vec3 localStart, Vec3 localEnd, float halfWidth, float uTileScale,
                                       float uScroll, float r, float g, float b, float alpha, float rollRad) {
        Vec3 diff = localEnd.subtract(localStart);
        double len = diff.length();
        if (len < 0.05D) {
            return;
        }
        Vec3 dir = diff.scale(1.0D / len);
        Vec3 upRef = Math.abs(dir.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side1 = dir.cross(upRef).normalize();
        Vec3 side2 = dir.cross(side1).normalize();

        double cos = Math.cos(rollRad);
        double sin = Math.sin(rollRad);
        Vec3 n1 = side1.scale(cos).add(side2.scale(sin)).scale(halfWidth);
        Vec3 n2 = side2.scale(cos).subtract(side1.scale(sin)).scale(halfWidth);

        float u0 = uScroll;
        float u1 = uScroll + (float) (len * uTileScale);
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(tex));
        PoseStack.Pose pose = ps.last();

        quadTwoSided(pose, vc, localStart.subtract(n1), localEnd.subtract(n1), localEnd.add(n1), localStart.add(n1), u0, 0.0F, u1, 1.0F, r, g, b, alpha);
        quadTwoSided(pose, vc, localStart.subtract(n2), localEnd.subtract(n2), localEnd.add(n2), localStart.add(n2), u0, 0.0F, u1, 1.0F, r, g, b, alpha);
    }

    public static void renderBossEffects(GildedTyrantEntity boss, float partialTick, PoseStack ps, MultiBufferSource buffers) {
        if (boss.isDeadOrDying()) {
            return;
        }
        float time = boss.tickCount + partialTick;
        Vec3 bossPos = boss.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, boss.yBodyRotO, boss.yBodyRot);

        float ringRadius = boss.getShockRingRadius();
        if (ringRadius > 0.1F) {
            Vec3 origin = boss.getShockRingOrigin().subtract(bossPos);
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(RING));
            PoseStack.Pose pose = ps.last();
            Vec3 a = origin.add(-ringRadius, 0.05D, -ringRadius);
            Vec3 b = origin.add(ringRadius, 0.05D, -ringRadius);
            Vec3 c = origin.add(ringRadius, 0.05D, ringRadius);
            Vec3 d = origin.add(-ringRadius, 0.05D, ringRadius);
            quadTwoSided(pose, vc, a, b, c, d, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.86F, 0.28F, 0.92F);
        }

        int beamMode = boss.getBeamMode();
        if (beamMode > 0) {
            Vec3 targetLocal = boss.getBeamTarget().subtract(bossPos);
            Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
            Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x);
            if (beamMode == 1) {
                Vec3 handStart = new Vec3(0.0D, 1.95D, 0.0D).subtract(right.scale(0.85D)).add(fwd.scale(0.5D));
                renderCrossBeam(ps, buffers, CHAIN, handStart, targetLocal, 0.22F, 1.2F, -time * 0.12F, 1.0F, 0.88F, 0.32F, 0.95F, 0.0F);
            } else if (beamMode == 2) {
                Vec3 chestStart = new Vec3(0.0D, 1.95D, 0.0D).add(fwd.scale(0.6D));
                renderCrossBeam(ps, buffers, FLARE, chestStart, targetLocal, 0.09F, 0.5F, -time * 0.2F, 1.0F, 0.92F, 0.45F, 0.78F, time * 0.2F);
            } else if (beamMode == 3) {
                Vec3 chestStart = new Vec3(0.0D, 1.95D, 0.0D).add(fwd.scale(0.6D));
                renderCrossBeam(ps, buffers, FLARE, chestStart, targetLocal, 0.52F, 0.35F, -time * 0.45F, 1.0F, 0.72F, 0.16F, 0.88F, time * 0.28F);
                renderCrossBeam(ps, buffers, FLARE, chestStart, targetLocal, 0.26F, 0.5F, -time * 0.65F, 1.0F, 0.98F, 0.82F, 0.98F, -time * 0.36F);
            }
        }

        GildedTyrantAttack attack = boss.getAttack();
        if (attack == GildedTyrantAttack.CRIMSON_WHIRLWIND_CLEAVE) {
            float age = boss.getClientAttackAge(partialTick);
            if (age >= 9.0F && age <= 37.0F) {
                VertexConsumer vc = buffers.getBuffer(RenderType.eyes(SLASH));
                ps.pushPose();
                ps.translate(0.0D, 1.35D, 0.0D);
                ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * 42.0F));
                PoseStack.Pose pose = ps.last();
                double r = 4.9D;
                quadTwoSided(pose, vc, new Vec3(-r, 0.0D, -r), new Vec3(r, 0.0D, -r), new Vec3(r, 0.0D, r), new Vec3(-r, 0.0D, r),
                        0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.84F, 0.25F, 0.88F);
                ps.popPose();
            }
        }
    }
}
