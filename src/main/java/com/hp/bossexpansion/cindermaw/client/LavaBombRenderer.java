package com.hp.bossexpansion.cindermaw.client;

import com.hp.bossexpansion.cindermaw.entity.LavaBomb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LavaBombRenderer extends EntityRenderer<LavaBomb, LavaBombRenderer.State> {
    public LavaBombRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LavaBomb bomb, State state, float partial) {
        super.extractRenderState(bomb, state, partial);
        state.ready = bomb.ready();
        if (!state.ready) {
            return;
        }
        state.impacted = bomb.impacted();
        state.megabomb = bomb.isMegabomb();
        state.playerShell = bomb.isPlayerShell();
        state.age = bomb.age(partial);
        state.impactAge = bomb.impactAge(partial);
        state.flightTicks = Math.max(1, bomb.flightTicks());
        Vec3 here = new Vec3(Mth.lerp(partial, bomb.xo, bomb.getX()), Mth.lerp(partial, bomb.yo, bomb.getY()), Mth.lerp(partial, bomb.zo, bomb.getZ()));
        state.landing = bomb.landing().subtract(here).add(0, 0.04, 0);
    }

    @Override
    public boolean shouldRender(LavaBomb bomb, Frustum frustum, double x, double y, double z) {
        if (!bomb.ready()) {
            return false;
        }
        return super.shouldRender(bomb, frustum, x, y, z) || bomb.shouldRender(x, y, z) && frustum.isVisible(new AABB(bomb.landing(), bomb.landing()).inflate(5));
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.ready) {
            return;
        }
        boolean mega = state.megabomb;
        double scale = mega ? 1.85D : 1.0D;
        if (state.impacted) {
            float t = Math.min(1.0F, state.impactAge / LavaBomb.LINGER);
            VfxDraw.dome(collector, pose, state.landing, (0.65 + 2.1 * t) * scale, -state.age * 0.35F, 0xFFF0A8, 0xD83810, (1 - t) * 0.9F);
            VfxDraw.submit(collector, pose, VfxDraw.GLOW, (local, out) ->
                    VfxDraw.ring(out, local, state.landing.add(0, 0.02, 0), (0.7 + 2.2 * t) * scale, Math.max(0.15, 0.3 + 1.5 * t) * scale, 0.0, VfxDraw.RING, 0xFF8A30, 1 - t));
            return;
        }
        double warnRadius = mega ? 4.4D : (state.playerShell ? 1.85D : 2.3D);
        float progress = Math.min(1.0F, state.age / state.flightTicks);
        float blink = 0.6F + 0.4F * Mth.sin(state.age * (0.5F + progress * 1.2F));
        VfxDraw.submit(collector, pose, VfxDraw.GLOW, (local, out) -> {
            VfxDraw.flat(out, local, state.landing, warnRadius, VfxDraw.WARN, 0xFF4A1A, 0.5F + 0.5F * progress * blink);
            VfxDraw.flat(out, local, state.landing.add(0, 0.01, 0), warnRadius * (1 - progress) + 0.2, VfxDraw.WARN, 0xFFC070, 0.85F);
            VfxDraw.billboard(out, local, Vec3.ZERO, 0.56 * scale, state.age * 0.15, VfxDraw.DISC, mega ? 0xFFB040 : 0xFF8A30, 0.75F);
        });
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.age * 17));
        pose.mulPose(Axis.XP.rotationDegrees(state.age * 11));
        VfxDraw.submit(collector, pose, VfxDraw.SOFT, (local, out) -> VfxDraw.box(out, local, 0.3 * scale, VfxDraw.ROCK, 0xFFFFFF, 1.0F));
        float pulse = (float) ((0.36F + Mth.sin(state.age * 0.9F) * 0.03F) * scale);
        VfxDraw.submit(collector, pose, VfxDraw.GLOW, (local, out) -> VfxDraw.box(out, local, pulse, VfxDraw.CORE, mega ? 0xFFD060 : 0xFFA040, 0.90F));
        pose.popPose();
    }

    public static final class State extends EntityRenderState {
        public boolean ready;
        public boolean impacted;
        public boolean megabomb;
        public boolean playerShell;
        public float age;
        public float impactAge;
        public int flightTicks = 1;
        public Vec3 landing = Vec3.ZERO;
    }
}
