package com.hp.bossexpansion.gildedtyrant.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedObeliskEntity;
import com.hp.bossexpansion.gildedtyrant.entity.ThrownGreataxeEntity;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.hp.bossexpansion.zephyrarch.client.ZephyrarchVfx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

// 鎏金暴君附属实体与巨斧三维渲染器
public final class GildedSubRenderers {
    private static final Identifier AXE_MODEL = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_greataxe");
    private static final Identifier AXE_TEX = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/item/gilded_greataxe.png");
    private static final Identifier AXE_ANIM = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_greataxe");

    private static final Identifier OBELISK_MODEL = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_obelisk");
    private static final Identifier OBELISK_TEX = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/gilded_obelisk.png");
    private static final Identifier OBELISK_ANIM = Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_obelisk");

    public static final class ThrownAxeState extends EntityRenderState implements GeoRenderState {
        public ThrownGreataxeEntity entity;
        public float spin;
        public float partial;

    private final java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> geckolibData = new java.util.HashMap<>();

    @Override
    public java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> getDataMap() {
        return geckolibData;
    }

    @Override
    public <D> void addGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, D data) {
        geckolibData.put(ticket, data);
    }

    @Override
    public boolean hasGeckolibData(com.geckolib.constant.dataticket.DataTicket<?> ticket) {
        return geckolibData.containsKey(ticket);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <D> D getGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket) {
        Object stored = geckolibData.get(ticket);
        if (stored != null) {
            return (D) stored;
        }
        if (ticket instanceof com.geckolib.constant.dataticket.OverridingDataTicket overriding && overriding.canExtractFrom(this)) {
            return (D) overriding.extractFrom(overriding.getOverriddenClass().cast(this));
        }
        return null;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, D fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, java.util.function.Supplier<D> fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback.get();
    }
    }

    public static final class ThrownAxeModel extends GeoModel<ThrownGreataxeEntity> {
        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return AXE_MODEL;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return AXE_TEX;
        }

        @Override
        public Identifier getAnimationResource(ThrownGreataxeEntity animatable) {
            return AXE_ANIM;
        }
    }

    public static final class ThrownAxe extends GeoEntityRenderer<ThrownGreataxeEntity, ThrownAxeState> {
        public ThrownAxe(EntityRendererProvider.Context context) {
            super(context, new ThrownAxeModel());
            this.shadowRadius = 0.6F;
            this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public ThrownAxeState createRenderState(ThrownGreataxeEntity entity, Void related) {
            return new ThrownAxeState();
        }

        @Override
        public void extractRenderState(ThrownGreataxeEntity entity, ThrownAxeState state, float partialTick) {
            super.extractRenderState(entity, state, partialTick);
            state.entity = entity;
            state.spin = entity.getSpinDegrees(partialTick);
            state.partial = partialTick;
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<ThrownAxeState> pass) {
            PoseStack poseStack = pass.poseStack();
            float spin = pass.renderState().spin;
            // 贴合刀光圆盘微倾平面并绕巨斧真实重心水平回旋，主斧刃朝向旋转切割正前方
            poseStack.translate(0.0D, 0.36D, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(4.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-4.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            poseStack.translate(0.0D, -2.25D, 0.0D);
        }

        @Override
        public void postRenderPass(RenderPassInfo<ThrownAxeState> pass, SubmitNodeCollector collector) {
            super.postRenderPass(pass, collector);
            ThrownAxeState state = pass.renderState();
            if (state.entity == null) {
                return;
            }
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Quaternionf camRot = camera.rotation();
            Vec3 camPos = camera.position();
            float spin = state.spin;
            // 还原实体根空间矩阵，防止 adjustRenderPose 的斧身平铺旋转把刀光圆盘竖起扭曲
            PoseStack rootPose = new PoseStack();
            rootPose.last().pose().set(pass.getPreRenderMatrixState());

            ZephyrarchVfx.SubmitCollectorBuffer buf = ZephyrarchVfx.beginSubmit();
            GildedTyrantVfx.drawSlashDisc(rootPose, buf, 0.35D, 2.25F, spin, 4.0F, -4.0F, 0.92F, true);
            GildedTyrantVfx.drawSlashDisc(rootPose, buf, 0.38D, 1.78F, spin + 180.0F, -4.0F, 4.0F, 0.85F, false);
            GildedTyrantVfx.drawBillboardFlare(rootPose, buf, camRot, new Vec3(0.0D, 0.36D, 0.0D),
                    0.95F, -spin * 0.6F, 1.0F, 0.90F, 0.38F);
            GildedTyrantVfx.drawRibbonTrail(state.entity.getPosition(state.partial).add(0.0D, 0.35D, 0.0D), 0.35D,
                    state.entity.clientTrail, 0.65F, rootPose, buf, camPos);
            buf.flush(collector);
        }

        @Override
        public RenderType getRenderType(ThrownAxeState state, Identifier texture) {
            return RenderTypes.entityCutout(texture);
        }

        @Override
        public boolean shouldRender(ThrownGreataxeEntity entity, Frustum frustum, double x, double y, double z) {
            return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(16.0D));
        }
    }

    public static final class ObeliskState extends LivingEntityRenderState implements GeoRenderState {
        public GildedObeliskEntity entity;
        public float partial;
        public float time;

    private final java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> geckolibData = new java.util.HashMap<>();

    @Override
    public java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> getDataMap() {
        return geckolibData;
    }

    @Override
    public <D> void addGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, D data) {
        geckolibData.put(ticket, data);
    }

    @Override
    public boolean hasGeckolibData(com.geckolib.constant.dataticket.DataTicket<?> ticket) {
        return geckolibData.containsKey(ticket);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <D> D getGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket) {
        Object stored = geckolibData.get(ticket);
        if (stored != null) {
            return (D) stored;
        }
        if (ticket instanceof com.geckolib.constant.dataticket.OverridingDataTicket overriding && overriding.canExtractFrom(this)) {
            return (D) overriding.extractFrom(overriding.getOverriddenClass().cast(this));
        }
        return null;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, D fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback;
    }

    @Override
    public <D> D getOrDefaultGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> ticket, java.util.function.Supplier<D> fallback) {
        D value = getGeckolibData(ticket);
        return value != null ? value : fallback.get();
    }
    }

    public static final class ObeliskModel extends GeoModel<GildedObeliskEntity> {
        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return OBELISK_MODEL;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return OBELISK_TEX;
        }

        @Override
        public Identifier getAnimationResource(GildedObeliskEntity animatable) {
            return OBELISK_ANIM;
        }
    }

    public static final class Obelisk extends GeoEntityRenderer<GildedObeliskEntity, ObeliskState> {
        public Obelisk(EntityRendererProvider.Context context) {
            super(context, new ObeliskModel());
            this.shadowRadius = 0.55F;
            this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public ObeliskState createRenderState(GildedObeliskEntity entity, Void related) {
            return new ObeliskState();
        }

        @Override
        public void extractRenderState(GildedObeliskEntity entity, ObeliskState state, float partialTick) {
            super.extractRenderState(entity, state, partialTick);
            state.entity = entity;
            state.partial = partialTick;
            state.time = entity.tickCount + partialTick;
        }

        @Override
        public void postRenderPass(RenderPassInfo<ObeliskState> pass, SubmitNodeCollector collector) {
            super.postRenderPass(pass, collector);
            ObeliskState state = pass.renderState();
            if (state.entity == null) {
                return;
            }
            float time = state.time;
            float pulse = 0.80F + 0.20F * Mth.sin(time * 0.22F);
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Quaternionf camRot = camera.rotation();
            PoseStack poseStack = pass.poseStack();
            ZephyrarchVfx.SubmitCollectorBuffer buf = ZephyrarchVfx.beginSubmit();

            GildedTyrantVfx.drawRoyalRing(poseStack, buf, 0.04D, 1.55F, time * 8.0F, 1.0F, 0.88F, 0.34F, 0.72F, 0.78F * pulse);
            GildedTyrantVfx.drawRoyalRing(poseStack, buf, 0.06D, 0.92F, -time * 14.0F, 1.0F, 0.95F, 0.52F, 0.65F, 0.72F * pulse);
            GildedTyrantVfx.drawBillboardFlare(poseStack, buf, camRot, new Vec3(0.0D, 1.90D, 0.0D),
                    0.68F * pulse, time * 12.0F, 1.0F, 0.90F * pulse, 0.38F * pulse);

            int ownerId = state.entity.getOwnerEntityId();
            if (ownerId >= 0) {
                Entity owner = state.entity.level().getEntity(ownerId);
                if (owner != null && owner.isAlive()) {
                    Vec3 selfPos = state.entity.getPosition(state.partial);
                    Vec3 ownerChest = owner.getPosition(state.partial).add(0.0D, 1.95D, 0.0D).subtract(selfPos);
                    Vec3 start = new Vec3(0.0D, 1.88D, 0.0D);
                    GildedTyrantVfx.renderCrossBeam(poseStack, buf, GildedTyrantVfx.CHAIN,
                            start, ownerChest, 0.20F, 0.95F, -time * 0.18F, 1.0F, 0.88F, 0.30F, 0.94F, time * 0.10F);
                    GildedTyrantVfx.renderCrossBeam(poseStack, buf, GildedTyrantVfx.RIBBON,
                            start, ownerChest, 0.12F, 0.55F, -time * 0.34F, 1.0F, 0.96F, 0.62F, 0.82F, -time * 0.28F);

                    float flow = ((time + 15.0F) % 30.0F) / 30.0F;
                    Vec3 orbPos = start.lerp(ownerChest, flow);
                    GildedTyrantVfx.drawBillboardFlare(poseStack, buf, camRot, orbPos,
                            0.58F + 0.25F * Mth.sin(flow * (float) Math.PI), -time * 20.0F, 1.0F, 0.94F, 0.48F);
                }
            }
            buf.flush(collector);
        }

        @Override
        public RenderType getRenderType(ObeliskState state, Identifier texture) {
            return RenderTypes.entityCutout(texture);
        }

        @Override
        public boolean shouldRender(GildedObeliskEntity entity, Frustum frustum, double x, double y, double z) {
            return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(28.0D));
        }
    }

    public static final class GreataxeItemModel extends GeoModel<GildedGreataxeItem> {
        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return AXE_MODEL;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return AXE_TEX;
        }

        @Override
        public Identifier getAnimationResource(GildedGreataxeItem animatable) {
            return AXE_ANIM;
        }
    }

    public static final class GreataxeItem extends GeoItemRenderer<GildedGreataxeItem> {
        private static final DataTicket<Float> WHIRLWIND_SIGN = DataTicket.create("greataxe_whirlwind_sign", Float.class);

        public GreataxeItem() {
            super(new GreataxeItemModel());
            this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void captureDefaultRenderState(GildedGreataxeItem animatable, RenderData renderData, GeoRenderState state, float partialTick) {
            super.captureDefaultRenderState(animatable, renderData, state, partialTick);
            ItemDisplayContext ctx = renderData.renderPerspective();
            if ((ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                    && renderData.itemOwner() instanceof LivingEntity living
                    && GreataxeClientController.isEntitySpinningOnPerspective(living, ctx)) {
                float sign = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -1.0F : 1.0F;
                state.addGeckolibData(WHIRLWIND_SIGN, sign);
            }
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<GeoRenderState> pass) {
            super.adjustRenderPose(pass);
            Float sign = pass.renderState().getGeckolibData(WHIRLWIND_SIGN);
            if (sign != null) {
                PoseStack poseStack = pass.poseStack();
                float gripY = 22.0F / 16.0F;
                poseStack.translate(0.0F, gripY, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-116.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(sign * 90.0F));
                poseStack.translate(0.0F, -gripY, 0.0F);
            }
        }

        @Override
        public RenderType getRenderType(GeoRenderState state, Identifier texture) {
            return RenderTypes.entityCutout(texture);
        }
    }

    private GildedSubRenderers() {
    }
}
