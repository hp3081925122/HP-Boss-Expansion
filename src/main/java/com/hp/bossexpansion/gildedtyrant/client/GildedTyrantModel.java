package com.hp.bossexpansion.gildedtyrant.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantAttack;
import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public final class GildedTyrantModel extends GeoModel<GildedTyrantEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "geo/gilded_tyrant.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "textures/entity/gilded_tyrant.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "animations/gilded_tyrant.animation.json");

    @Override
    public ResourceLocation getModelResource(GildedTyrantEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GildedTyrantEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GildedTyrantEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(GildedTyrantEntity animatable, long instanceId, AnimationState<GildedTyrantEntity> state) {
        super.setCustomAnimations(animatable, instanceId, state);
        getBone("held_greataxe").ifPresent(axeBone -> axeBone.setHidden(animatable.isAxeThrown()));

        if (animatable.getAttack() != GildedTyrantAttack.NONE || animatable.isDeadOrDying()) {
            return;
        }
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) {
            return;
        }
        getBone("head").ifPresent(head -> {
            float pitch = Mth.clamp(data.headPitch(), -22.0F, 22.0F) * Mth.DEG_TO_RAD;
            float yaw = Mth.clamp(data.netHeadYaw(), -40.0F, 40.0F) * Mth.DEG_TO_RAD;
            head.setRotX(head.getRotX() + pitch);
            head.setRotY(head.getRotY() + yaw);
        });
    }
}
