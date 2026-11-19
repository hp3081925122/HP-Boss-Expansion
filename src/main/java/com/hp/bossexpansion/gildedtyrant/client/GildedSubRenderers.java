package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedObeliskEntity;
import com.hp.bossexpansion.gildedtyrant.entity.ThrownGreataxeEntity;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.35D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSpinDegrees(partialTick)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.translate(0.0D, -1.4D, 0.0D);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }

        @Override
        public RenderType getRenderType(ThrownGreataxeEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
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
            int ownerId = entity.getOwnerEntityId();
            if (ownerId >= 0) {
                Entity owner = entity.level().getEntity(ownerId);
                if (owner != null && owner.isAlive()) {
                    Vec3 selfPos = entity.getPosition(partialTick);
                    Vec3 ownerChest = owner.getPosition(partialTick).add(0.0D, 1.95D, 0.0D).subtract(selfPos);
                    Vec3 start = new Vec3(0.0D, 1.88D, 0.0D);
                    float time = entity.tickCount + partialTick;
                    GildedTyrantVfx.renderCrossBeam(poseStack, bufferSource, GildedTyrantVfx.CHAIN,
                            start, ownerChest, 0.16F, 0.9F, -time * 0.15F, 1.0F, 0.85F, 0.24F, 0.88F, time * 0.12F);
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
        public RenderType getRenderType(GildedGreataxeItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }

    private GildedSubRenderers() {
    }
}
