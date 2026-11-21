package com.hp.bossexpansion.gildedtyrant.item;

import com.hp.bossexpansion.gildedtyrant.entity.GildedTyrantEntity;
import com.hp.bossexpansion.gildedtyrant.registry.GildedTyrantRegistry;
import com.hp.bossexpansion.zephyrarch.item.DescItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 堡垒金敕：召唤鎏金暴君的消耗信物
public final class BastionEdictItem extends DescItem {
    public BastionEdictItem(Properties properties) {
        super(properties, "item.hp_boss_expansion.bastion_edict.desc");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(4.0D);
        BlockPos spawnPos = BlockPos.containing(player.getX() + forward.x, player.getY() + 0.5D, player.getZ() + forward.z);

        GildedTyrantEntity boss = GildedTyrantRegistry.GILDED_TYRANT.get().create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (boss == null) {
            return InteractionResult.FAIL;
        }

        boss.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        if (!serverLevel.noCollision(boss)) {
            boss.snapTo(player.getX(), player.getY() + 1.0D, player.getZ(), player.getYRot() + 180.0F, 0.0F);
            if (!serverLevel.noCollision(boss)) {
                player.sendOverlayMessage(Component.translatable("message.hp_boss_expansion.tyrant_summon_blocked").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
        }

        serverLevel.addFreshEntity(boss);
        serverLevel.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY() + 1.8D, boss.getZ(), 40, 0.8D, 0.9D, 0.8D, 0.08D);
        serverLevel.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.8F, 0.65F);
        serverLevel.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.PIGLIN_BRUTE_ANGRY, SoundSource.HOSTILE, 2.0F, 0.6F);
        player.sendOverlayMessage(Component.translatable("message.hp_boss_expansion.tyrant_summoned").withStyle(ChatFormatting.GOLD));

        player.getCooldowns().addCooldown(stack, 60);
        stack.consume(1, player);
        return InteractionResult.CONSUME;
    }
}
