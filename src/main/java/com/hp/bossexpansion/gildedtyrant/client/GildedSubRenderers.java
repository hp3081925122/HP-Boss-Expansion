package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedObeliskEntity;
import com.hp.bossexpansion.gildedtyrant.entity.ThrownGreataxeEntity;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class GildedSubRenderers {
    private static final ResourceLocation AXE_GEO = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "geo/gilded_greataxe.geo.json");
    private static final ResourceLocation AXE_TEX = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/item/gilded_greataxe.png");
    private static final ResourceLocation AXE_ANIM = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "animations/gilded_greataxe.animation.json");

    private static final ResourceLocation OBELISK_GEO = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "geo/gilded_obelisk.geo.json");
    private static final ResourceLocation OBELISK_TEX = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/gilded_obelisk.png");
    private static final ResourceLocation OBELISK_ANIM = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "animations/gilded_obelisk.animation.json");

    public static final class ThrownAxeModel extends GeoModel<ThrownGreataxeEntity> {
        @Override
        public ResourceLocation getModelResource(ThrownGreataxeEntity animatable) {
            return AXE_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(ThrownGreataxeEntity animatable) {
            return AXE_TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(ThrownGreataxeEntity animatable) {
            return AXE_ANIM;
        }
    }

    public static final class ThrownAxe extends GeoEntityRenderer<ThrownGreataxeEntity> {
        public ThrownAxe(EntityRendererProvider.Context context) {
            super(context, new ThrownAxeModel());
            this.shadowRadius = 0.6F;
            this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void render(ThrownGreataxeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            float spin = entity.getSpinDegrees(partialTick);
            poseStack.pushPose();
            // 贴合刀光圆盘微倾平面并绕巨斧真实重心水平回旋，主斧刃朝向旋转切割正前方
            poseStack.translate(0.0D, 0.36D, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(4.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-4.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            poseStack.translate(0.0D, -2.25D, 0.0D);
            super.render(entity, 0.0F, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();

            Quaternionf camRot = this.entityRenderDispatcher.cameraOrientation();
            Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
            GildedTyrantVfx.drawSlashDisc(poseStack, bufferSource, 0.35D, 2.25F, spin, 4.0F, -4.0F, 0.92F, true);
            GildedTyrantVfx.drawSlashDisc(poseStack, bufferSource, 0.38D, 1.78F, spin + 180.0F, -4.0F, 4.0F, 0.85F, false);
            GildedTyrantVfx.drawBillboardFlare(poseStack, bufferSource, camRot, new Vec3(0.0D, 0.36D, 0.0D),
                    0.95F, -spin * 0.6F, 1.0F, 0.90F, 0.38F);
            GildedTyrantVfx.drawRibbonTrail(entity.getPosition(partialTick).add(0.0D, 0.35D, 0.0D), 0.35D,
                    entity.clientTrail, 0.65F, poseStack, bufferSource, camPos);
        }

        @Override
        public RenderType getRenderType(ThrownGreataxeEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRender(ThrownGreataxeEntity entity, Frustum frustum, double x, double y, double z) {
            return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(16.0D));
        }
    }

    public static final class ObeliskModel extends GeoModel<GildedObeliskEntity> {
        @Override
        public ResourceLocation getModelResource(GildedObeliskEntity animatable) {
            return OBELISK_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(GildedObeliskEntity animatable) {
            return OBELISK_TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(GildedObeliskEntity animatable) {
            return OBELISK_ANIM;
        }
    }

    public static final class Obelisk extends GeoEntityRenderer<GildedObeliskEntity> {
        public Obelisk(EntityRendererProvider.Context context) {
            super(context, new ObeliskModel());
            this.shadowRadius = 0.55F;
            this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void render(GildedObeliskEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            float time = entity.tickCount + partialTick;
            float pulse = 0.80F + 0.20F * Mth.sin(time * 0.22F);
            Quaternionf camRot = this.entityRenderDispatcher.cameraOrientation();

            GildedTyrantVfx.drawRoyalRing(poseStack, bufferSource, 0.04D, 1.55F, time * 8.0F, 1.0F, 0.88F, 0.34F, 0.72F, 0.78F * pulse);
            GildedTyrantVfx.drawRoyalRing(poseStack, bufferSource, 0.06D, 0.92F, -time * 14.0F, 1.0F, 0.95F, 0.52F, 0.65F, 0.72F * pulse);
            GildedTyrantVfx.drawBillboardFlare(poseStack, bufferSource, camRot, new Vec3(0.0D, 1.90D, 0.0D),
                    0.68F * pulse, time * 12.0F, 1.0F, 0.90F * pulse, 0.38F * pulse);

            int ownerId = entity.getOwnerEntityId();
            if (ownerId >= 0) {
                Entity owner = entity.level().getEntity(ownerId);
                if (owner != null && owner.isAlive()) {
                    Vec3 selfPos = entity.getPosition(partialTick);
                    Vec3 ownerChest = owner.getPosition(partialTick).add(0.0D, 1.95D, 0.0D).subtract(selfPos);
                    Vec3 start = new Vec3(0.0D, 1.88D, 0.0D);
                    GildedTyrantVfx.renderCrossBeam(poseStack, bufferSource, GildedTyrantVfx.CHAIN,
                            start, ownerChest, 0.20F, 0.95F, -time * 0.18F, 1.0F, 0.88F, 0.30F, 0.94F, time * 0.10F);
                    GildedTyrantVfx.renderCrossBeam(poseStack, bufferSource, GildedTyrantVfx.RIBBON,
                            start, ownerChest, 0.12F, 0.55F, -time * 0.34F, 1.0F, 0.96F, 0.62F, 0.82F, -time * 0.28F);

                    float flow = ((time + 15.0F) % 30.0F) / 30.0F;
                    Vec3 orbPos = start.lerp(ownerChest, flow);
                    GildedTyrantVfx.drawBillboardFlare(poseStack, bufferSource, camRot, orbPos,
                            0.58F + 0.25F * Mth.sin(flow * (float) Math.PI), -time * 20.0F, 1.0F, 0.94F, 0.48F);
                }
            }
        }

        @Override
        public RenderType getRenderType(GildedObeliskEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRender(GildedObeliskEntity entity, Frustum frustum, double x, double y, double z) {
            return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(28.0D));
        }
    }

    public static final class GreataxeItemModel extends GeoModel<GildedGreataxeItem> {
        @Override
        public ResourceLocation getModelResource(GildedGreataxeItem animatable) {
            return AXE_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(GildedGreataxeItem animatable) {
            return AXE_TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(GildedGreataxeItem animatable) {
            return AXE_ANIM;
        }
    }

    public static final class GreataxeItem extends GeoItemRenderer<GildedGreataxeItem> {
        public GreataxeItem() {
            super(new GreataxeItemModel());
            this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void preRender(PoseStack poseStack, GildedGreataxeItem animatable, BakedGeoModel model,
                              @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
            if (!isReRender && GreataxeClientController.shouldApplyThirdPersonWhirlwindPose(this.renderPerspective)) {
                float sign = this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -1.0F : 1.0F;
                float gripY = 22.0F / 16.0F;
                poseStack.translate(0.0F, gripY, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-116.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(sign * 90.0F));
                poseStack.translate(0.0F, -gripY, 0.0F);
            }
        }

        @Override
        public RenderType getRenderType(GildedGreataxeItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }

    private GildedSubRenderers() {
    }
}
