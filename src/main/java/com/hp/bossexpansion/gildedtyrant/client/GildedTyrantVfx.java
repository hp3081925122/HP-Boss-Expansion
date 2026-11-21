package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.client.VfxDraw;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public final class GildedTyrantVfx {
    public static final Identifier RING = tex("gilded_ring");
    public static final Identifier RING_GLOW = tex("gilded_ring_glow");
    public static final Identifier CHAIN = tex("gilded_chain");
    public static final Identifier FLARE = tex("gilded_flare");
    public static final Identifier SLASH = tex("gilded_slash");
    public static final Identifier SLASH_GLOW = tex("gilded_slash_glow");
    public static final Identifier ARC_STRIP = tex("gilded_arc_strip");
    public static final Identifier ARC_STRIP_GLOW = tex("gilded_arc_strip_glow");
    public static final Identifier RIBBON = tex("gilded_ribbon");
    public static final Identifier RIBBON_GLOW = tex("gilded_ribbon_glow");

    private GildedTyrantVfx() {
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/effect/" + name + ".png");
    }

    public static RenderType additive(Identifier texture) {
        if (texture.equals(RING)) {
            return RenderTypes.eyes(RING_GLOW);
        }
        if (texture.equals(SLASH)) {
            return RenderTypes.eyes(SLASH_GLOW);
        }
        if (texture.equals(ARC_STRIP)) {
            return RenderTypes.eyes(ARC_STRIP_GLOW);
        }
        if (texture.equals(RIBBON)) {
            return RenderTypes.eyes(RIBBON_GLOW);
        }
        return RenderTypes.eyes(texture);
    }

    private static void vtx(PoseStack.Pose pose, VertexConsumer vc, Vec3 p, float u, float v, float r, float g, float b, float a) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(Mth.clamp(r, 0.0F, 1.0F), Mth.clamp(g, 0.0F, 1.0F), Mth.clamp(b, 0.0F, 1.0F), Mth.clamp(a, 0.0F, 1.0F))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
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

    public static void drawRoyalRing(PoseStack ps, MultiBufferSource buffers, double yOffset, float radius, float spinDeg,
                                     float tr, float tg, float tb, float alpha, float glowIntensity) {
        if (radius <= 0.02F || alpha <= 0.01F) {
            return;
        }
        ps.pushPose();
        ps.translate(0.0D, yOffset, 0.0D);
        ps.mulPose(Axis.YP.rotationDegrees(spinDeg));
        PoseStack.Pose pose = ps.last();

        Vec3 a = new Vec3(-radius, 0.0D, -radius);
        Vec3 b = new Vec3(radius, 0.0D, -radius);
        Vec3 c = new Vec3(radius, 0.0D, radius);
        Vec3 d = new Vec3(-radius, 0.0D, radius);

        quadTwoSided(pose, buffers.getBuffer(RenderTypes.entityTranslucent(RING)), a, b, c, d, 0.0F, 0.0F, 1.0F, 1.0F, tr, tg, tb, alpha);
        if (glowIntensity > 0.01F) {
            Vec3 ag = a.add(0.0D, 0.012D, 0.0D);
            Vec3 bg = b.add(0.0D, 0.012D, 0.0D);
            Vec3 cg = c.add(0.0D, 0.012D, 0.0D);
            Vec3 dg = d.add(0.0D, 0.012D, 0.0D);
            quadTwoSided(pose, buffers.getBuffer(additive(RING)), ag, bg, cg, dg, 0.0F, 0.0F, 1.0F, 1.0F,
                    tr * glowIntensity, tg * glowIntensity, tb * glowIntensity, 1.0F);
        }
        ps.popPose();
    }

    public static void drawDirectedRing(PoseStack ps, MultiBufferSource buffers, float yawDeg, float pitchDeg, double yOff, double forwardOff,
                                        float radius, float spinDeg, float alpha, boolean crimsonHot) {
        if (alpha <= 0.01F || radius <= 0.02F) {
            return;
        }
        ps.pushPose();
        ps.translate(0.0D, yOff, 0.0D);
        ps.mulPose(Axis.YP.rotationDegrees(-yawDeg));
        ps.translate(0.0D, 0.0D, forwardOff);
        ps.mulPose(Axis.XP.rotationDegrees(90.0F + pitchDeg));
        drawRoyalRing(ps, buffers, 0.0D, radius, spinDeg,
                1.0F, crimsonHot ? 0.72F : 0.88F, crimsonHot ? 0.22F : 0.36F, 0.88F * alpha, 0.85F * alpha);
        ps.popPose();
    }

    public static void drawBillboardFlare(PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot, Vec3 localPos,
                                          float size, float spinDeg, float r, float g, float b) {
        if (size <= 0.01F) {
            return;
        }
        ps.pushPose();
        ps.translate(localPos.x, localPos.y, localPos.z);
        ps.mulPose(cameraRot);
        ps.mulPose(Axis.ZP.rotationDegrees(spinDeg));
        PoseStack.Pose pose = ps.last();
        quadTwoSided(pose, buffers.getBuffer(additive(FLARE)),
                new Vec3(-size, size, 0.0D), new Vec3(size, size, 0.0D),
                new Vec3(size, -size, 0.0D), new Vec3(-size, -size, 0.0D),
                0.0F, 0.0F, 1.0F, 1.0F, r, g, b, 1.0F);
        ps.popPose();
    }

    public static void drawFunnelWall(PoseStack ps, MultiBufferSource buffers, double yBottom, double yTop,
                                      float rBottom, float rTop, float uScroll,
                                      float tr, float tg, float tb, float alpha, float glow) {
        if (alpha <= 0.01F) {
            return;
        }
        int segs = 20;
        PoseStack.Pose pose = ps.last();
        VertexConsumer vcBase = buffers.getBuffer(RenderTypes.entityTranslucent(RIBBON));
        for (int i = 0; i < segs; i++) {
            float f0 = i / (float) segs;
            float f1 = (i + 1) / (float) segs;
            double a0 = f0 * Math.PI * 2.0D;
            double a1 = f1 * Math.PI * 2.0D;
            float u0 = uScroll + f0 * 3.0F;
            float u1 = uScroll + f1 * 3.0F;

            Vec3 top0 = new Vec3(Math.cos(a0) * rTop, yTop, Math.sin(a0) * rTop);
            Vec3 top1 = new Vec3(Math.cos(a1) * rTop, yTop, Math.sin(a1) * rTop);
            Vec3 bot1 = new Vec3(Math.cos(a1) * rBottom, yBottom, Math.sin(a1) * rBottom);
            Vec3 bot0 = new Vec3(Math.cos(a0) * rBottom, yBottom, Math.sin(a0) * rBottom);

            quadTwoSided(pose, vcBase, top0, top1, bot1, bot0, u0, 0.0F, u1, 1.0F, tr, tg, tb, alpha);
        }
        if (glow > 0.01F) {
            VertexConsumer vcGlow = buffers.getBuffer(additive(RIBBON));
            for (int i = 0; i < segs; i++) {
                float f0 = i / (float) segs;
                float f1 = (i + 1) / (float) segs;
                double a0 = f0 * Math.PI * 2.0D;
                double a1 = f1 * Math.PI * 2.0D;
                float u0 = uScroll + f0 * 3.0F;
                float u1 = uScroll + f1 * 3.0F;

                Vec3 top0 = new Vec3(Math.cos(a0) * (rTop + 0.015F), yTop, Math.sin(a0) * (rTop + 0.015F));
                Vec3 top1 = new Vec3(Math.cos(a1) * (rTop + 0.015F), yTop, Math.sin(a1) * (rTop + 0.015F));
                Vec3 bot1 = new Vec3(Math.cos(a1) * (rBottom + 0.015F), yBottom, Math.sin(a1) * (rBottom + 0.015F));
                Vec3 bot0 = new Vec3(Math.cos(a0) * (rBottom + 0.015F), yBottom, Math.sin(a0) * (rBottom + 0.015F));

                quadTwoSided(pose, vcGlow, top0, top1, bot1, bot0, u0, 0.0F, u1, 1.0F, tr * glow, tg * glow, tb * glow, 1.0F);
            }
        }
    }

    public static void drawTiltedOrbitRibbon(PoseStack ps, MultiBufferSource buffers, double centerY, float radius, float halfHeight,
                                             float tiltXDeg, float tiltZDeg, float uOffset,
                                             float tr, float tg, float tb, float alpha, float glow) {
        ps.pushPose();
        ps.translate(0.0D, centerY, 0.0D);
        ps.mulPose(Axis.XP.rotationDegrees(tiltXDeg));
        ps.mulPose(Axis.ZP.rotationDegrees(tiltZDeg));
        drawFunnelWall(ps, buffers, -halfHeight, halfHeight, radius, radius, uOffset, tr, tg, tb, alpha, glow);
        ps.popPose();
    }

    public static void drawGildedSlashArc(PoseStack ps, MultiBufferSource buffers, float bodyYaw, double centerY, double forwardShift,
                                          double innerR, double outerR, float pitchXDeg, float tiltZDeg,
                                          float startDeg, float endDeg, float k, boolean crimsonBoost) {
        float sweepProgress = Mth.clamp(k * 3.20F, 0.0F, 1.0F);
        float fade = k < 0.45F ? 1.0F : Mth.clamp((1.0F - k) / 0.55F, 0.0F, 1.0F);
        float currentEnd = Mth.lerp(sweepProgress, startDeg, endDeg);
        float tailStart = Mth.lerp(Mth.clamp((k - 0.18F) * 1.15F, 0.0F, 0.65F), startDeg, endDeg);
        drawGildedSlashArcRange(ps, buffers, bodyYaw, centerY, forwardShift, innerR, outerR, pitchXDeg, tiltZDeg, tailStart, currentEnd, fade, crimsonBoost);
    }

    public static void drawGildedSlashArcRange(PoseStack ps, MultiBufferSource buffers, float bodyYaw, double centerY, double forwardShift,
                                               double innerR, double outerR, float pitchXDeg, float tiltZDeg,
                                               float tailStart, float currentEnd, float fade, boolean crimsonBoost) {
        if (fade <= 0.01F || Math.abs(currentEnd - tailStart) <= 2.0F) {
            return;
        }
        ps.pushPose();
        ps.translate(0.0D, centerY, 0.0D);
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        ps.translate(0.0D, 0.0D, forwardShift);
        ps.mulPose(Axis.ZP.rotationDegrees(tiltZDeg));
        ps.mulPose(Axis.XP.rotationDegrees(pitchXDeg));

        int segments = 20;
        float tr = 1.0F;
        float tg = crimsonBoost ? 0.78F : 0.90F;
        float tb = crimsonBoost ? 0.28F : 0.42F;

        PoseStack.Pose pose = ps.last();
        VertexConsumer vcBase = buffers.getBuffer(RenderTypes.entityTranslucent(ARC_STRIP));
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = Mth.lerp(t0, tailStart, currentEnd) * Mth.DEG_TO_RAD;
            float a1 = Mth.lerp(t1, tailStart, currentEnd) * Mth.DEG_TO_RAD;
            double w0 = 0.28D + 0.72D * Math.sin(Math.PI * Math.pow(t0, 0.72D));
            double w1 = 0.28D + 0.72D * Math.sin(Math.PI * Math.pow(t1, 0.72D));
            double rIn0 = outerR - (outerR - innerR) * w0;
            double rIn1 = outerR - (outerR - innerR) * w1;

            Vec3 in0 = new Vec3(Math.sin(a0) * rIn0, 0.0D, Math.cos(a0) * rIn0);
            Vec3 out0 = new Vec3(Math.sin(a0) * outerR, 0.0D, Math.cos(a0) * outerR);
            Vec3 out1 = new Vec3(Math.sin(a1) * outerR, 0.0D, Math.cos(a1) * outerR);
            Vec3 in1 = new Vec3(Math.sin(a1) * rIn1, 0.0D, Math.cos(a1) * rIn1);

            quadTwoSided(pose, vcBase, in0, in1, out1, out0, t0, 0.0F, t1, 1.0F, tr, tg, tb, 0.94F * fade);
        }

        VertexConsumer vcGlow = buffers.getBuffer(additive(ARC_STRIP));
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = Mth.lerp(t0, tailStart, currentEnd) * Mth.DEG_TO_RAD;
            float a1 = Mth.lerp(t1, tailStart, currentEnd) * Mth.DEG_TO_RAD;
            double w0 = 0.24D + 0.56D * Math.sin(Math.PI * Math.pow(t0, 0.72D));
            double w1 = 0.24D + 0.56D * Math.sin(Math.PI * Math.pow(t1, 0.72D));
            double rIn0 = outerR - (outerR - innerR) * w0;
            double rIn1 = outerR - (outerR - innerR) * w1;

            Vec3 in0 = new Vec3(Math.sin(a0) * rIn0, 0.015D, Math.cos(a0) * rIn0);
            Vec3 out0 = new Vec3(Math.sin(a0) * (outerR + 0.06D), 0.015D, Math.cos(a0) * (outerR + 0.06D));
            Vec3 out1 = new Vec3(Math.sin(a1) * (outerR + 0.06D), 0.015D, Math.cos(a1) * (outerR + 0.06D));
            Vec3 in1 = new Vec3(Math.sin(a1) * rIn1, 0.015D, Math.cos(a1) * rIn1);

            quadTwoSided(pose, vcGlow, in0, in1, out1, out0, t0, 0.0F, t1, 1.0F, fade, 0.88F * fade, 0.45F * fade, 1.0F);
        }
        ps.popPose();
    }

    public static void drawSlashDisc(PoseStack ps, MultiBufferSource buffers, double yOffset, float radius, float spinDeg,
                                     float tiltX, float tiltZ, float alpha, boolean crimson) {
        if (alpha <= 0.01F || radius <= 0.05F) {
            return;
        }
        ps.pushPose();
        ps.translate(0.0D, yOffset, 0.0D);
        ps.mulPose(Axis.XP.rotationDegrees(tiltX));
        ps.mulPose(Axis.ZP.rotationDegrees(tiltZ));
        ps.mulPose(Axis.YP.rotationDegrees(spinDeg));
        PoseStack.Pose pose = ps.last();

        Vec3 a = new Vec3(-radius, 0.0D, -radius);
        Vec3 b = new Vec3(radius, 0.0D, -radius);
        Vec3 c = new Vec3(radius, 0.0D, radius);
        Vec3 d = new Vec3(-radius, 0.0D, radius);

        float g = crimson ? 0.74F : 0.90F;
        float bl = crimson ? 0.25F : 0.42F;
        quadTwoSided(pose, buffers.getBuffer(RenderTypes.entityTranslucent(SLASH)), a, b, c, d, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, g, bl, 0.90F * alpha);
        quadTwoSided(pose, buffers.getBuffer(additive(SLASH)),
                a.add(0.0D, 0.015D, 0.0D), b.add(0.0D, 0.015D, 0.0D), c.add(0.0D, 0.015D, 0.0D), d.add(0.0D, 0.015D, 0.0D),
                0.0F, 0.0F, 1.0F, 1.0F, alpha, g * alpha, bl * alpha, 1.0F);
        ps.popPose();
    }

    public static void drawRibbonTrail(Vec3 headWorld, double localY, Iterable<Vec3> worldTrail, float baseWidth,
                                       PoseStack ps, MultiBufferSource buffers, Vec3 cameraPos) {
        List<Vec3> relList = new ArrayList<>(16);
        relList.add(Vec3.ZERO);
        for (Vec3 worldPt : worldTrail) {
            relList.add(worldPt.subtract(headWorld));
        }
        int count = relList.size() - 1;
        if (count < 1) {
            return;
        }
        ps.pushPose();
        ps.translate(0.0D, localY, 0.0D);
        PoseStack.Pose pose = ps.last();

        VertexConsumer vcBase = buffers.getBuffer(RenderTypes.entityTranslucent(RIBBON));
        for (int i = 0; i < count; i++) {
            Vec3 p0 = relList.get(i);
            Vec3 p1 = relList.get(i + 1);
            Vec3 seg = p1.subtract(p0);
            if (seg.lengthSqr() < 1.0E-5D) {
                continue;
            }
            float t0 = i / (float) count;
            float t1 = (i + 1) / (float) count;
            float w0 = baseWidth * (1.0F - t0 * 0.88F);
            float w1 = baseWidth * (1.0F - t1 * 0.88F);
            float alpha = (1.0F - t0) * 0.88F;

            Vec3 toCam = cameraPos.subtract(headWorld.add(p0));
            Vec3 side = seg.cross(toCam);
            side = side.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : side.normalize();

            quadTwoSided(pose, vcBase,
                    p0.add(side.scale(w0)), p1.add(side.scale(w1)),
                    p1.subtract(side.scale(w1)), p0.subtract(side.scale(w0)),
                    t0 * 2.0F, 0.0F, t1 * 2.0F, 1.0F, 1.0F, 0.84F, 0.32F, alpha);
        }

        VertexConsumer vcGlow = buffers.getBuffer(additive(RIBBON));
        for (int i = 0; i < count; i++) {
            Vec3 p0 = relList.get(i);
            Vec3 p1 = relList.get(i + 1);
            Vec3 seg = p1.subtract(p0);
            if (seg.lengthSqr() < 1.0E-5D) {
                continue;
            }
            float t0 = i / (float) count;
            float t1 = (i + 1) / (float) count;
            float w0 = baseWidth * 0.68F * (1.0F - t0 * 0.88F);
            float w1 = baseWidth * 0.68F * (1.0F - t1 * 0.88F);
            float g = (1.0F - t0) * 0.90F;

            Vec3 toCam = cameraPos.subtract(headWorld.add(p0));
            Vec3 side = seg.cross(toCam);
            side = side.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : side.normalize();

            quadTwoSided(pose, vcGlow,
                    p0.add(side.scale(w0)), p1.add(side.scale(w1)),
                    p1.subtract(side.scale(w1)), p0.subtract(side.scale(w0)),
                    t0 * 2.0F, 0.0F, t1 * 2.0F, 1.0F, g, 0.86F * g, 0.36F * g, 1.0F);
        }
        ps.popPose();
    }

    public static void drawGoldenPrismSpike(PoseStack ps, MultiBufferSource buffers, Vec3 baseOffset, float yawDeg, float tiltDeg,
                                            float baseRadius, float height, float alpha) {
        if (alpha <= 0.01F || height <= 0.05F) {
            return;
        }
        ps.pushPose();
        ps.translate(baseOffset.x, baseOffset.y, baseOffset.z);
        ps.mulPose(Axis.YP.rotationDegrees(yawDeg));
        ps.mulPose(Axis.XP.rotationDegrees(tiltDeg));

        VertexConsumer glow = buffers.getBuffer(VfxDraw.GLOW);
        float[] tr = VfxDraw.TRAIL;
        double midH = height * 0.32D;
        double midR = baseRadius * 1.18D;
        Vec3 tip = new Vec3(0.0D, height, 0.0D);

        int sides = 4;
        for (int i = 0; i < sides; i++) {
            double a0 = i * (Math.PI * 2.0D / sides) + Math.PI * 0.25D;
            double a1 = (i + 1) * (Math.PI * 2.0D / sides) + Math.PI * 0.25D;
            Vec3 b0 = new Vec3(Math.cos(a0) * baseRadius, 0.0D, Math.sin(a0) * baseRadius);
            Vec3 b1 = new Vec3(Math.cos(a1) * baseRadius, 0.0D, Math.sin(a1) * baseRadius);
            Vec3 m0 = new Vec3(Math.cos(a0) * midR, midH, Math.sin(a0) * midR);
            Vec3 m1 = new Vec3(Math.cos(a1) * midR, midH, Math.sin(a1) * midR);

            VfxDraw.quadGrad2(glow, ps, m0, m1, b1, b0, tr[0], tr[1], tr[2], tr[3], 0xFFD84C, alpha * 0.95F, 0xB8320A, alpha * 0.75F);
            VfxDraw.quadGrad2(glow, ps, tip, tip, m1, m0, tr[0], tr[1], tr[2], tr[3], 0xFFFDF0, alpha, 0xFFD84C, alpha * 0.95F);
        }
        ps.popPose();
    }

    public static void renderCrossBeam(PoseStack ps, MultiBufferSource buffers, Identifier tex,
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
        VertexConsumer vc = buffers.getBuffer(additive(tex));
        PoseStack.Pose pose = ps.last();
        float pr = r * alpha;
        float pg = g * alpha;
        float pb = b * alpha;

        quadTwoSided(pose, vc, localStart.subtract(n1), localEnd.subtract(n1), localEnd.add(n1), localStart.add(n1), u0, 0.0F, u1, 1.0F, pr, pg, pb, 1.0F);
        quadTwoSided(pose, vc, localStart.subtract(n2), localEnd.subtract(n2), localEnd.add(n2), localStart.add(n2), u0, 0.0F, u1, 1.0F, pr, pg, pb, 1.0F);
    }

    public static void renderBossEffects(GildedTyrantEntity boss, float partialTick, PoseStack ps, MultiBufferSource buffers,
                                         Quaternionf cameraRot, Vec3 cameraPos) {
        if (boss.isDeadOrDying()) {
            return;
        }
        float time = boss.tickCount + partialTick;
        Vec3 bossPos = boss.getPosition(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, boss.yBodyRotO, boss.yBodyRot);
        boolean phase2 = boss.isPhase2();
        double ground = findGroundOffset(boss);

        renderShockRing(boss, time, bossPos, ps, buffers);
        renderBeams(boss, time, bodyYaw, bossPos, ground, ps, buffers, cameraRot);

        GildedTyrantAttack attack = boss.getAttack();
        if (attack == GildedTyrantAttack.NONE) {
            return;
        }
        float age = boss.getClientAttackAge(partialTick);
        switch (attack) {
            case TYRANT_COMBO -> renderTyrantCombo(age, time, bodyYaw, phase2, ps, buffers, cameraRot);
            case BEARDED_HOOK_SWEEP -> renderBeardedHookSweep(age, time, bodyYaw, phase2, ground, ps, buffers, cameraRot);
            case PAULDRON_BULL_RUSH -> renderPauldronBullRush(age, time, bodyYaw, ground, boss.isRushHalted(), ps, buffers, cameraRot);
            case GILDED_FISSURE_SLAM -> renderGildedFissureSlam(age, time, bodyYaw, ground, ps, buffers, cameraRot);
            case GOLD_FEVER_ROAR -> renderGoldFeverRoar(age, time, ground, ps, buffers, cameraRot);
            case BASTION_ANVIL_LEAP -> renderBastionAnvilLeap(age, time, bodyYaw, boss.isLeapLanded(), ps, buffers, cameraRot);
            case GREED_CORE_OVERDRIVE -> renderGreedCoreOverdrive(age, time, ground, ps, buffers, cameraRot);
            case WHIRLING_GREATAXE_BOOMERANG -> renderWhirlingBoomerang(age, time, bodyYaw, ps, buffers, cameraRot);
            case GOLDEN_CHAIN_SHACKLE -> renderGoldenChainShackle(age, time, bodyYaw, ps, buffers, cameraRot);
            case GILDED_OBELISK_SANCTUM -> renderObeliskSanctum(age, time, ground, ps, buffers, cameraRot);
            case CRIMSON_WHIRLWIND_CLEAVE -> renderCrimsonWhirlwind(age, time, bodyYaw, ground, ps, buffers, cameraRot);
            case CATACLYSMIC_GOLD_RAIN -> renderCataclysmicGoldRain(age, time, bodyYaw, ground, ps, buffers, cameraRot);
            case STAGGER -> renderStagger(age, time, ps, buffers, cameraRot);
            default -> {
            }
        }
    }

    private static void renderShockRing(GildedTyrantEntity boss, float time, Vec3 bossPos, PoseStack ps, MultiBufferSource buffers) {
        float ringRadius = boss.getShockRingRadius();
        if (ringRadius <= 0.1F) {
            return;
        }
        Vec3 origin = boss.getShockRingOrigin().subtract(bossPos);
        float progress = Mth.clamp((ringRadius - 3.2F) / 6.0F, 0.0F, 1.0F);
        float fade = 1.0F - progress * 0.82F;

        ps.pushPose();
        ps.translate(origin.x, origin.y, origin.z);
        drawRoyalRing(ps, buffers, 0.04D, ringRadius, time * 14.0F, 1.0F, 0.84F, 0.28F, 0.92F * fade, 0.95F * fade);
        drawRoyalRing(ps, buffers, 0.06D, ringRadius * 0.88F, -time * 18.0F, 1.0F, 0.95F, 0.52F, 0.72F * fade, 0.75F * fade);
        VfxDraw.wall(buffers, ps, Vec3.ZERO, ringRadius * 0.95D, 1.15D * fade + 0.25D, -time * 0.18F, 0xFFBC24, 0.78F * fade);
        ps.popPose();
    }

    private static void renderBeams(GildedTyrantEntity boss, float time, float bodyYaw, Vec3 bossPos, double ground,
                                    PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        int beamMode = boss.getBeamMode();
        if (beamMode <= 0) {
            return;
        }
        Vec3 targetLocal = boss.getBeamTarget().subtract(bossPos);
        Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
        Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x);

        if (beamMode == 1) {
            Vec3 handStart = new Vec3(0.0D, 1.95D, 0.0D).add(right.scale(0.95D)).add(fwd.scale(0.55D));
            renderCrossBeam(ps, buffers, CHAIN, handStart, targetLocal, 0.26F, 1.15F, -time * 0.18F, 1.0F, 0.90F, 0.36F, 0.98F, 0.0F);
            renderCrossBeam(ps, buffers, RIBBON, handStart, targetLocal, 0.15F, 0.65F, -time * 0.36F, 1.0F, 0.96F, 0.68F, 0.85F, time * 0.28F);
            drawBillboardFlare(ps, buffers, cameraRot, handStart, 0.62F, time * 12.0F, 1.0F, 0.88F, 0.38F);
            drawBillboardFlare(ps, buffers, cameraRot, targetLocal, 0.85F, -time * 16.0F, 1.0F, 0.85F, 0.32F);

            ps.pushPose();
            ps.translate(targetLocal.x, targetLocal.y - 0.35D, targetLocal.z);
            drawRoyalRing(ps, buffers, 0.0D, 1.15F, time * 22.0F, 1.0F, 0.86F, 0.28F, 0.85F, 0.90F);
            drawTiltedOrbitRibbon(ps, buffers, 0.45D, 0.95F, 0.22F, 28.0F, -24.0F, -time * 0.25F, 1.0F, 0.84F, 0.28F, 0.78F, 0.85F);
            ps.popPose();
        } else if (beamMode == 2) {
            Vec3 chestStart = new Vec3(0.0D, 1.98D, 0.0D).add(fwd.scale(0.65D));
            float pulse = 0.75F + 0.25F * Mth.sin(time * 0.8F);
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.98D, 0.72D, 0.95F * pulse, time * 24.0F, 0.88F, false);
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.98D, 0.85D, 0.58F, -time * 32.0F, 0.92F, true);
            drawBillboardFlare(ps, buffers, cameraRot, chestStart, 0.85F * pulse, time * 14.0F, 1.0F, 0.92F, 0.46F);
            renderCrossBeam(ps, buffers, RIBBON, chestStart, targetLocal, 0.08F, 0.8F, -time * 0.32F, 1.0F, 0.92F, 0.48F, 0.78F, time * 0.2F);
            drawBillboardFlare(ps, buffers, cameraRot, targetLocal, 0.65F * pulse, -time * 16.0F, 1.0F, 0.78F, 0.28F);
        } else if (beamMode == 3) {
            Vec3 chestStart = new Vec3(0.0D, 1.98D, 0.0D).add(fwd.scale(0.65D));
            Vec3 diff = targetLocal.subtract(chestStart);
            double len = diff.length();

            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.98D, 0.75D, 1.45F, time * 36.0F, 0.95F, true);
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.98D, 1.05D, 0.95F, -time * 42.0F, 0.92F, false);
            drawBillboardFlare(ps, buffers, cameraRot, chestStart, 1.35F, time * 20.0F, 1.0F, 0.94F, 0.55F);

            renderCrossBeam(ps, buffers, RIBBON, chestStart, targetLocal, 0.66F, 0.38F, -time * 0.52F, 1.0F, 0.64F, 0.14F, 0.88F, time * 0.30F);
            renderCrossBeam(ps, buffers, RIBBON, chestStart, targetLocal, 0.38F, 0.55F, -time * 0.72F, 1.0F, 0.88F, 0.32F, 0.95F, -time * 0.38F);
            renderCrossBeam(ps, buffers, FLARE, chestStart, targetLocal, 0.18F, 0.25F, -time * 0.85F, 1.0F, 0.98F, 0.88F, 1.0F, time * 0.45F);

            if (len > 0.5D) {
                Vec3 dir = diff.scale(1.0D / len);
                float rayYaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0D / Math.PI)) - 90.0F;
                float rayPitch = (float) (-Math.asin(Mth.clamp(dir.y, -1.0D, 1.0D)) * (180.0D / Math.PI));
                if (len > 1.5D) {
                    int ringCount = Math.min(8, (int) (len / 2.8D));
                    for (int i = 1; i <= ringCount; i++) {
                        double d = ((i * 2.8D + (time * 0.65D) % 2.8D));
                        if (d < len - 0.5D) {
                            Vec3 pt = chestStart.add(dir.scale(d));
                            ps.pushPose();
                            ps.translate(pt.x, pt.y, pt.z);
                            ps.mulPose(Axis.YP.rotationDegrees(-rayYaw));
                            ps.mulPose(Axis.XP.rotationDegrees(90.0F + rayPitch));
                            drawRoyalRing(ps, buffers, 0.0D, 0.88F, time * 28.0F + i * 45.0F, 1.0F, 0.86F, 0.32F, 0.75F, 0.80F);
                            ps.popPose();
                        }
                    }
                }
                ps.pushPose();
                ps.translate(targetLocal.x, targetLocal.y, targetLocal.z);
                ps.mulPose(Axis.YP.rotationDegrees(-rayYaw));
                ps.mulPose(Axis.XP.rotationDegrees(90.0F + rayPitch));
                float impPulse = 0.88F + 0.16F * Mth.sin(time * 1.2F);
                drawRoyalRing(ps, buffers, -0.05D, 1.48F * impPulse, -time * 34.0F, 1.0F, 0.80F, 0.22F, 0.92F, 0.95F);
                drawRoyalRing(ps, buffers, -0.18D, 0.95F * impPulse, time * 40.0F, 1.0F, 0.95F, 0.52F, 0.88F, 0.92F);
                ps.popPose();
            }

            Vec3 targetWorld = boss.getBeamTarget();
            Double targetGroundY = findNearbyGroundY(boss.level(), targetWorld);
            if (targetGroundY != null) {
                ps.pushPose();
                ps.translate(targetLocal.x, targetGroundY - bossPos.y + 0.04D, targetLocal.z);
                VfxDraw.dome(buffers, ps, Vec3.ZERO, 1.25D + 0.16D * Mth.sin(time * 0.9F), -time * 0.35F, 0xFFFCE0, 0xD83208, 0.85F);
                drawRoyalRing(ps, buffers, 0.03D, 1.55F, -time * 30.0F, 1.0F, 0.84F, 0.26F, 0.88F, 0.92F);
                ps.popPose();
            }
            drawBillboardFlare(ps, buffers, cameraRot, targetLocal, 1.75F, -time * 24.0F, 1.0F, 0.92F, 0.48F);
        }
    }

    private static void renderTyrantCombo(float age, float time, float bodyYaw, boolean phase2,
                                          PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
        Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x);

        if (age >= 3.0F && age < 10.5F) {
            float k = (age - 3.0F) / 7.5F;
            Vec3 fist = fwd.scale(-0.3D).add(right.scale(1.25D)).add(0.0D, 2.0D, 0.0D);
            drawBillboardFlare(ps, buffers, cameraRot, fist, 0.45F + 0.65F * k, time * 14.0F, 1.0F, 0.88F * k, 0.35F * k);
        }
        if (age >= 9.5F && age <= 16.0F) {
            float k = (age - 9.5F) / 6.5F;
            float fade = 1.0F - k;
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.65D, 1.2D + 2.5D * k, 0.85F + 1.55F * k, age * 28.0F, fade, true);
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.65D, 0.9D + 1.6D * k, 0.55F + 0.95F * k, -age * 34.0F, fade * 0.85F, false);
            if (k < 0.55F) {
                float fk = 1.0F - k / 0.55F;
                drawBillboardFlare(ps, buffers, cameraRot, fwd.scale(2.1D).add(0.0D, 1.65D, 0.0D), 1.35F * fk, age * 18.0F, 1.0F, 0.92F * fk, 0.45F * fk);
            }
        }

        if (age >= 15.0F && age < 22.0F) {
            float k = (age - 15.0F) / 7.0F;
            Vec3 axeCharge = fwd.scale(-0.4D).subtract(right.scale(1.45D)).add(0.0D, 3.45D, 0.0D);
            drawBillboardFlare(ps, buffers, cameraRot, axeCharge, 0.55F + 0.95F * k, -time * 16.0F, 1.0F, 0.90F, 0.38F);
        }
        if (age >= 22.0F && age <= 32.0F) {
            float k = (age - 22.0F) / 10.0F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 2.22D, 0.55D, 1.85D, 5.15D, 6.0F, 24.0F, 92.0F, -88.0F, k, phase2);
            if (age >= 23.5F && age <= 29.5F) {
                float rk = (age - 23.5F) / 6.0F;
                drawDirectedRing(ps, buffers, bodyYaw, 10.0F, 1.55D, 1.6D + 2.6D * rk, 1.05F + 1.65F * rk, -age * 26.0F, 1.0F - rk, true);
            }
        }
    }

    private static void renderBeardedHookSweep(float age, float time, float bodyYaw, boolean phase2, double ground,
                                               PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age < 15.0F) {
            float k = age / 15.0F;
            drawGildedSlashArcRange(ps, buffers, bodyYaw, ground + 0.05D, 0.0D, 0.45D, 6.3D, 0.0F, 0.0F,
                    -110.0F, 110.0F, 0.24F + 0.46F * k, false);
            double sweepR = 6.3D * (1.05D - 0.55D * k);
            drawGildedSlashArcRange(ps, buffers, bodyYaw, ground + 0.07D, 0.0D, Math.max(0.2D, sweepR - 0.85D), sweepR, 0.0F, 0.0F,
                    -110.0F, 110.0F, 0.40F * k, true);
            Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
            Vec3 leftBack = fwd.scale(-0.9D).subtract(new Vec3(-fwd.z, 0.0D, fwd.x).scale(1.85D)).add(0.0D, 2.2D, 0.0D);
            drawBillboardFlare(ps, buffers, cameraRot, leftBack, 0.5F + 0.9F * k, time * 15.0F, 1.0F, 0.88F, 0.35F);
        } else if (age <= 24.5F) {
            float k = (age - 15.0F) / 9.5F;
            float fade = 1.0F - k;
            drawGildedSlashArcRange(ps, buffers, bodyYaw, 1.55D, 0.2D, 2.1D, 6.25D, 0.0F, 4.0F,
                    Mth.lerp(Mth.clamp(k * 1.1F, 0.0F, 0.55F), 118.0F, -112.0F),
                    Mth.lerp(Mth.clamp(k * 2.8F, 0.0F, 1.0F), 118.0F, -112.0F),
                    fade, true);
            float pullR = 6.1F * (1.0F - 0.48F * Mth.clamp(k * 1.4F, 0.0F, 1.0F));
            drawFunnelWall(ps, buffers, ground + 0.15D, ground + 1.85D * fade + 0.3D, pullR * 0.92F, pullR, -time * 0.35F,
                    1.0F, 0.82F, 0.25F, 0.62F * fade, 0.65F * fade);
            drawRoyalRing(ps, buffers, ground + 0.05D, pullR, time * 24.0F, 1.0F, 0.88F, 0.35F, 0.78F * fade, 0.82F * fade);
        }

        if (age >= 26.0F && age <= 36.0F) {
            float k = (age - 26.0F) / 10.0F;
            float ease = 1.0F - (float) Math.pow(1.0F - k, 3.0D);
            float fade = 1.0F - k;
            float stompR = 0.9F + 2.85F * ease;
            drawRoyalRing(ps, buffers, ground + 0.06D, stompR, -age * 22.0F, 1.0F, 0.85F, 0.28F, 0.92F * fade, 0.95F * fade);
            VfxDraw.wall(buffers, ps, new Vec3(0.0D, ground, 0.0D), stompR * 0.92D, 1.05D * fade, -time * 0.22F, 0xFFBE28, 0.75F * fade);
            VfxDraw.dome(buffers, ps, new Vec3(0.0D, ground, 0.0D), stompR * 0.75D, -time * 0.25F, 0xFFFBE0, 0xC82808, 0.58F * fade);
        }
    }

    private static void renderPauldronBullRush(float age, float time, float bodyYaw, double ground, boolean rushHalted,
                                               PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age <= 13.0F) {
            float k = Mth.clamp(age / 13.0F, 0.0F, 1.0F);
            float pulse = age > 9.0F ? (0.78F + 0.22F * Mth.sin(age * 3.0F)) : 1.0F;
            float length = 15.5F * Mth.clamp(age / 5.0F, 0.25F, 1.0F);
            float halfW = 1.65F;

            ps.pushPose();
            ps.translate(0.0D, ground + 0.05D, 0.0D);
            ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            PoseStack.Pose pose = ps.last();
            float uOff = -time * 0.26F;
            float uEnd = uOff + length * 0.35F;
            Vec3 a = new Vec3(-halfW, 0.0D, 0.5D);
            Vec3 b = new Vec3(halfW, 0.0D, 0.5D);
            Vec3 c = new Vec3(halfW, 0.0D, length);
            Vec3 d = new Vec3(-halfW, 0.0D, length);

            quadTwoSided(pose, buffers.getBuffer(RenderTypes.entityTranslucent(RIBBON)),
                    a, b, c, d, uOff, 0.0F, uEnd, 1.0F, 1.0F, 0.84F, 0.28F, (0.35F + 0.48F * k) * pulse);
            quadTwoSided(pose, buffers.getBuffer(additive(RIBBON)),
                    a.add(0.0D, 0.012D, 0.0D), b.add(0.0D, 0.012D, 0.0D), c.add(0.0D, 0.012D, 0.0D), d.add(0.0D, 0.012D, 0.0D),
                    uOff, 0.0F, uEnd, 1.0F, k * pulse, 0.82F * k * pulse, 0.28F * k, 1.0F);
            ps.popPose();

            Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
            Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x);
            drawBillboardFlare(ps, buffers, cameraRot, fwd.scale(0.5D).add(right.scale(1.1D)).add(0.0D, 2.25D, 0.0D),
                    0.55F + 0.85F * k, time * 16.0F, 1.0F, 0.88F, 0.32F);
        } else if (age <= 33.5F && !rushHalted) {
            float k = (age - 13.0F) / 20.5F;
            float fade = 1.0F - k * 0.45F;
            ps.pushPose();
            ps.translate(0.0D, 1.68D, 0.0D);
            ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            ps.mulPose(Axis.XP.rotationDegrees(90.0F));
            drawFunnelWall(ps, buffers, -2.6D, 1.55D, 1.85F, 0.35F, -age * 0.55F, 1.0F, 0.78F, 0.22F, 0.48F * fade, 0.58F * fade);
            drawFunnelWall(ps, buffers, -1.8D, 1.65D, 1.12F, 0.18F, age * 0.68F, 1.0F, 0.95F, 0.56F, 0.55F * fade, 0.68F * fade);
            ps.popPose();
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.68D, -0.5D - (age % 5.0F) * 0.65D,
                    1.25F + (age % 5.0F) * 0.28F, age * 30.0F, (1.0F - (age % 5.0F) / 5.0F) * 0.85F, true);
        }

        if (age >= 35.0F && age <= 44.0F) {
            float k = (age - 35.0F) / 9.0F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 1.55D, 0.55D, 1.45D, 4.15D, 0.0F, 88.0F, -78.0F, 85.0F, k, true);
            drawDirectedRing(ps, buffers, bodyYaw, -35.0F, 1.85D, 1.2D + 1.8D * k, 0.95F + 1.45F * k, age * 26.0F, 1.0F - k, false);
        }
    }

    private static void renderGildedFissureSlam(float age, float time, float bodyYaw, double ground,
                                                PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
        if (age >= 4.0F && age < 18.5F) {
            float k = (age - 4.0F) / 14.5F;
            Vec3 overhead = fwd.scale(-0.3D).add(0.0D, 3.85D + 0.45D * k, 0.0D);
            drawBillboardFlare(ps, buffers, cameraRot, overhead, 0.65F + 1.25F * k, time * 16.0F, 1.0F, 0.90F, 0.36F);
            ps.pushPose();
            ps.translate(fwd.x * 2.5D, ground, fwd.z * 2.5D);
            drawRoyalRing(ps, buffers, 0.04D, 3.5F, -time * 10.0F, 1.0F, 0.78F, 0.22F, 0.35F + 0.45F * k, 0.65F * k);
            drawRoyalRing(ps, buffers, 0.06D, 3.5F * (1.05F - 0.65F * k), time * 20.0F, 1.0F, 0.94F, 0.48F, 0.55F * k, 0.80F * k);
            ps.popPose();
        }
        if (age >= 16.5F && age <= 26.0F) {
            float k = (age - 16.5F) / 9.5F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 2.05D, 0.55D, 1.95D, 5.45D, 0.0F, -88.0F, -98.0F, 82.0F, k, true);
        }
    }

    private static void renderGoldFeverRoar(float age, float time, double ground,
                                            PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age < 17.5F) {
            float k = age / 17.5F;
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 2.15D, 0.0D), 0.45F + 1.15F * k, time * 14.0F, 1.0F, 0.90F, 0.38F);
        } else if (age <= 36.0F) {
            float k = (age - 17.5F) / 18.5F;
            float ease = 1.0F - (float) Math.pow(1.0F - k, 3.0D);
            float fade = 1.0F - k;
            float roarR = 1.2F + 9.2F * ease;

            VfxDraw.dome(buffers, ps, new Vec3(0.0D, ground + 0.2D, 0.0D), roarR * 0.92D, -time * 0.24F, 0xFFFCE0, 0xD8380C, 0.68F * fade);
            VfxDraw.wall(buffers, ps, new Vec3(0.0D, ground, 0.0D), roarR * 0.88D, 1.35D * fade, -time * 0.22F, 0xFFBC24, 0.72F * fade);
            if (k < 0.45F) {
                float fk = 1.0F - k / 0.45F;
                drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 2.25D, 0.0D), 2.5F * fk, -age * 16.0F, 1.0F, 0.94F * fk, 0.52F * fk);
            }
        }
    }

    private static void renderBastionAnvilLeap(float age, float time, float bodyYaw, boolean leapLanded,
                                               PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age >= 10.0F && age <= 24.0F && !leapLanded) {
            float k = Mth.clamp((age - 10.0F) / 14.0F, 0.0F, 1.0F);
            drawFunnelWall(ps, buffers, 0.3D, 3.2D, 0.65F, 1.55F, age * 0.38F, 1.0F, 0.84F, 0.26F, 0.42F * (0.4F + 0.6F * k), 0.52F);
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 3.85D, 0.0D), 1.05F + 0.65F * k, -time * 18.0F, 1.0F, 0.92F, 0.45F);
        }
        if (age >= 21.5F && age <= 31.0F && (!leapLanded || age <= 29.0F)) {
            float k = (age - 21.5F) / 9.5F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 2.1D, 0.5D, 1.9D, 5.5D, 0.0F, -88.0F, -98.0F, 82.0F, k, true);
        }
    }

    private static void renderGreedCoreOverdrive(float age, float time, double ground,
                                                 PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age < 27.5F) {
            float k = Mth.clamp(age / 27.5F, 0.0F, 1.0F);
            VfxDraw.column(buffers, ps, new Vec3(0.0D, ground, 0.0D), new Vec3(0.0D, 1.2D + 3.2D * k * k, 0.0D),
                    0.55D + 0.55D * k, 0.25D + 0.35D * k, 3, -time * 0.32F, 1.6F, 0xFFC428, 0.65F * k);
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 1.98D, 0.0D), 0.55F + 2.15F * k * k, -time * 14.0F, 1.0F, 0.92F, 0.45F);
        } else {
            float k = Mth.clamp((age - 27.5F) / 22.5F, 0.0F, 1.0F);
            float ease = 1.0F - (float) Math.pow(1.0F - k, 3.0D);
            float fade = 1.0F - k;
            float waveR = 1.2F + 7.6F * ease;

            VfxDraw.column(buffers, ps, new Vec3(0.0D, ground, 0.0D), new Vec3(0.0D, 11.5D * (0.4F + 0.6F * ease), 0.0D),
                    1.35D + 0.85D * ease, 2.45D + 1.35D * ease, 4, -time * 0.25F, 2.4F, 0xFFC830, 0.92F * fade);
            VfxDraw.dome(buffers, ps, new Vec3(0.0D, ground, 0.0D), waveR, -time * 0.22F, 0xFFFDF0, 0xCC2206, 0.86F * fade);
            VfxDraw.wall(buffers, ps, new Vec3(0.0D, ground, 0.0D), waveR * 0.95D, 2.4D * fade + 0.4D, -time * 0.20F, 0xFFBC22, 0.85F * fade);
            drawRoyalRing(ps, buffers, ground + 0.08D, waveR * 1.04F, time * 20.0F, 1.0F, 0.88F, 0.32F, 0.95F * fade, fade);
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 2.1D, 0.0D), 4.2F * fade + 0.6F, time * 10.0F, fade, 0.92F * fade, 0.48F * fade);
        }
    }

    private static void renderWhirlingBoomerang(float age, float time, float bodyYaw,
                                                PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
        if (age >= 7.5F && age <= 14.5F) {
            float k = (age - 7.5F) / 7.0F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 1.95D, 0.4D, 1.6D, 4.6D, 0.0F, 5.0F, 95.0F, -95.0F, k, true);
        }
        if (age >= 22.0F && age <= 28.0F) {
            float k = (age - 22.0F) / 6.0F;
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.68D, 1.1D + 2.3D * k, 0.85F + 1.35F * k, age * 28.0F, 1.0F - k, true);
        }
        if (age >= 34.0F && age <= 40.0F) {
            float k = (age - 34.0F) / 6.0F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 1.72D, 0.45D, 1.4D, 3.9D, 0.0F, 12.0F, 82.0F, -82.0F, k, false);
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.68D, 1.1D + 2.1D * k, 0.80F + 1.25F * k, -age * 28.0F, 1.0F - k, false);
        }
        if (age >= 49.0F && age <= 54.5F) {
            float fk = 1.0F - (age - 49.0F) / 5.5F;
            Vec3 catchPt = fwd.scale(0.6D).subtract(new Vec3(-fwd.z, 0.0D, fwd.x).scale(1.1D)).add(0.0D, 2.25D, 0.0D);
            drawBillboardFlare(ps, buffers, cameraRot, catchPt, 1.45F * fk, time * 20.0F, 1.0F, 0.92F * fk, 0.42F * fk);
        }
    }

    private static void renderGoldenChainShackle(float age, float time, float bodyYaw,
                                                 PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
        if (age <= 11.0F) {
            float k = age / 11.0F;
            drawDirectedRing(ps, buffers, bodyYaw, 0.0F, 1.95D, 0.95D, 0.85F * (1.2F - 0.4F * k), -time * 24.0F, 0.80F * k, false);
        }
        if (age >= 21.0F && age < 27.5F) {
            float k = (age - 21.0F) / 6.5F;
            drawBillboardFlare(ps, buffers, cameraRot, fwd.scale(-0.2D).add(0.0D, 3.85D, 0.0D),
                    0.55F + 1.15F * k, time * 16.0F, 1.0F, 0.88F, 0.34F);
        }
        if (age >= 26.5F && age <= 36.0F) {
            float k = (age - 26.5F) / 9.5F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 2.0D, 0.5D, 1.85D, 5.25D, 0.0F, -88.0F, -96.0F, 84.0F, k, true);
        }
    }

    private static void renderObeliskSanctum(float age, float time, double ground,
                                             PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age < 17.5F) {
            float k = age / 17.5F;
            drawRoyalRing(ps, buffers, ground + 0.05D, 3.2F + 4.8F * k, time * 12.0F, 1.0F, 0.88F, 0.35F, 0.48F * k, 0.65F * k);
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 3.65D, 0.0D), 0.55F + 1.15F * k, -time * 14.0F, 1.0F, 0.92F, 0.45F);
        } else if (age <= 32.0F) {
            float k = (age - 17.5F) / 14.5F;
            float fade = 1.0F - k;
            float r = 1.5F + 7.2F * (1.0F - (float) Math.pow(1.0F - k, 3.0D));
            drawRoyalRing(ps, buffers, ground + 0.06D, r, -age * 16.0F, 1.0F, 0.90F, 0.38F, 0.90F * fade, 0.95F * fade);
            VfxDraw.wall(buffers, ps, new Vec3(0.0D, ground, 0.0D), r * 0.38D, 1.8D * fade, -time * 0.22F, 0xFFD038, 0.72F * fade);
        }
    }

    private static void renderCrimsonWhirlwind(float age, float time, float bodyYaw, double ground,
                                               PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age < 8.5F) {
            float k = age / 8.5F;
            drawRoyalRing(ps, buffers, ground + 0.05D, 5.4F * (1.08F - 0.35F * k), -time * 18.0F, 1.0F, 0.76F, 0.22F, 0.58F * k, 0.72F * k);
        } else if (age <= 37.2F) {
            float env = age < 11.0F ? (age - 8.5F) / 2.5F : (age > 34.0F ? (37.2F - age) / 3.2F : 1.0F);
            env = Mth.clamp(env, 0.0F, 1.0F);

            drawSlashDisc(ps, buffers, 1.42D, 5.35F, age * 42.0F, 0.0F, 0.0F, env, true);
            drawSlashDisc(ps, buffers, 1.62D, 4.75F, age * 42.0F + 175.0F, 4.0F, -4.0F, env * 0.92F, false);
            drawSlashDisc(ps, buffers, 1.18D, 3.85F, -age * 28.0F, -3.0F, 3.0F, env * 0.75F, true);

            drawFunnelWall(ps, buffers, ground + 0.1D, ground + 2.75D, 3.65F, 5.25F, -time * 0.42F,
                    1.0F, 0.72F, 0.18F, 0.58F * env, 0.68F * env);
            VfxDraw.wall(buffers, ps, new Vec3(0.0D, ground, 0.0D), 4.85D, 1.65D * env, -time * 0.28F, 0xFF9818, 0.62F * env);
            drawRoyalRing(ps, buffers, ground + 0.05D, 5.45F, time * 26.0F, 1.0F, 0.80F, 0.24F, 0.78F * env, 0.85F * env);
        }

        if (age >= 38.0F && age < 43.0F) {
            float k = (age - 38.0F) / 5.0F;
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 3.95D, 0.0D), 0.75F + 1.35F * k, -time * 18.0F, 1.0F, 0.88F, 0.32F);
        }
        if (age >= 41.5F && age <= 51.0F) {
            float k = (age - 41.5F) / 9.5F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 2.05D, 0.55D, 2.0D, 5.85D, 0.0F, -88.0F, -100.0F, 85.0F, k, true);
        }
    }

    private static void renderCataclysmicGoldRain(float age, float time, float bodyYaw, double ground,
                                                  PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, bodyYaw);
        Vec3 right = new Vec3(-fwd.z, 0.0D, fwd.x);

        if (age >= 4.0F && age < 15.5F) {
            float k = (age - 4.0F) / 11.5F;
            drawRoyalRing(ps, buffers, ground + 0.05D, 2.5F + 5.5F * k, time * 14.0F, 1.0F, 0.86F, 0.28F, 0.55F * k, 0.75F * k);
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 3.95D, 0.0D), 0.65F + 1.35F * k, time * 16.0F, 1.0F, 0.90F, 0.36F);
        }
        if (age >= 13.5F && age <= 21.5F) {
            float k = (age - 13.5F) / 8.0F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 2.05D, 0.55D, 1.9D, 5.55D, 0.0F, -88.0F, -98.0F, 82.0F, k, true);
        }

        if (age >= 18.0F && age <= 54.0F) {
            float env = age < 22.0F ? (age - 18.0F) / 4.0F : (age > 50.0F ? (54.0F - age) / 4.0F : 1.0F);
            env = Mth.clamp(env, 0.0F, 1.0F);
            drawRoyalRing(ps, buffers, ground + 0.04D, 8.2F, time * 8.0F, 1.0F, 0.84F, 0.26F, 0.65F * env, 0.75F * env);
            drawRoyalRing(ps, buffers, ground + 0.06D, 4.6F, -time * 14.0F, 1.0F, 0.94F, 0.48F, 0.72F * env, 0.82F * env);
            drawFunnelWall(ps, buffers, ground + 0.1D, ground + 3.2D, 1.55F, 2.85F, -time * 0.28F, 1.0F, 0.86F, 0.30F, 0.52F * env, 0.62F * env);

            Vec3 skyFist = right.scale(0.85D).add(0.0D, 3.75D, 0.0D);
            float bellPhase = ((age - 22.0F) % 6.0F + 6.0F) % 6.0F;
            float bellPulse = age >= 21.5F && age <= 49.0F ? Math.max(0.0F, 1.0F - bellPhase / 3.8F) : 0.0F;
            drawBillboardFlare(ps, buffers, cameraRot, skyFist, (0.85F + 1.15F * bellPulse) * env, -time * 18.0F, 1.0F, 0.94F, 0.48F);
            if (bellPulse > 0.05F) {
                VfxDraw.column(buffers, ps, skyFist, new Vec3(0.0D, 12.0D, 0.0D), 0.28D, 0.75D, 3, -time * 0.42F, 2.2F, 0xFFE055, 0.85F * bellPulse * env);
            }
        }

        if (age >= 53.5F && age <= 64.0F) {
            float k = (age - 53.5F) / 10.5F;
            drawGildedSlashArc(ps, buffers, bodyYaw, 2.1D, 0.55D, 2.0D, 6.0D, 0.0F, -88.0F, -100.0F, 85.0F, k, true);
        }
    }

    private static void renderStagger(float age, float time, PoseStack ps, MultiBufferSource buffers, Quaternionf cameraRot) {
        if (age < 10.0F) {
            float bk = 1.0F - age / 10.0F;
            drawBillboardFlare(ps, buffers, cameraRot, new Vec3(0.0D, 1.98D, 0.0D), 1.9F * bk, time * 24.0F, 1.0F, 0.55F * bk, 0.18F * bk);
        }
    }

    private static Double findNearbyGroundY(Level level, Vec3 pos) {
        BlockPos base = BlockPos.containing(pos.x, pos.y + 0.1D, pos.z);
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos check = base.below(dy);
            BlockState state = level.getBlockState(check);
            if (!state.getCollisionShape(level, check).isEmpty()) {
                double topY = check.getY() + state.getCollisionShape(level, check).max(Direction.Axis.Y);
                if (Math.abs(pos.y - topY) <= 0.65D) {
                    return topY;
                }
            }
        }
        return null;
    }

    public static double findGroundOffset(GildedTyrantEntity boss) {
        if (boss.onGround()) {
            return 0.0D;
        }
        Level level = boss.level();
        BlockPos base = boss.blockPosition();
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos check = base.below(dy);
            BlockState state = level.getBlockState(check);
            if (!state.getCollisionShape(level, check).isEmpty()) {
                double topY = check.getY() + state.getCollisionShape(level, check).max(Direction.Axis.Y);
                return topY - boss.getY();
            }
        }
        return 0.0D;
    }
}
