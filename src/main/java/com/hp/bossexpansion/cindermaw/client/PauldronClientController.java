package com.hp.bossexpansion.cindermaw.client;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.CindermawParticles;
import com.hp.bossexpansion.cindermaw.CindermawRegistry;
import com.hp.bossexpansion.cindermaw.item.CindermawPauldronItem;
import com.hp.bossexpansion.cindermaw.network.PauldronNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 客户端主动技能按键监听与蓄力聚火粒子演出
public final class PauldronClientController {
    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "main")
    );
    public static final KeyMapping VOLCANIC_CANNON_KEY = new KeyMapping(
            "key.hp_boss_expansion.volcanic_cannon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    private static final Set<Integer> CHARGING_ENTITIES = ConcurrentHashMap.newKeySet();
    private static boolean clientHolding;

    private PauldronClientController() {
    }

    public static boolean isEntityCharging(int entityId) {
        return CHARGING_ENTITIES.contains(entityId);
    }

    public static void handleChargeState(PauldronNetwork.ChargeStatePayload payload) {
        if (payload.charging()) {
            CHARGING_ENTITIES.add(payload.entityId());
        } else {
            CHARGING_ENTITIES.remove(payload.entityId());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            clientHolding = false;
            CHARGING_ENTITIES.clear();
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearing = player.isAlive()
                && !player.isSpectator()
                && chest.is(CindermawRegistry.CINDERMAW_CHESTPLATE.get());
        boolean onCooldown = wearing && player.getCooldowns().isOnCooldown(chest);
        boolean wantHold = wearing && !onCooldown && mc.screen == null && VOLCANIC_CANNON_KEY.isDown();

        if (wantHold != clientHolding) {
            clientHolding = wantHold;
            ClientPacketDistributor.sendToServer(new PauldronNetwork.HoldPayload(wantHold));
        }

        CHARGING_ENTITIES.removeIf(id -> {
            Entity e = level.getEntity(id);
            if (!(e instanceof LivingEntity living) || !living.isAlive()) {
                return true;
            }
            spawnChargeParticles(level, living);
            return false;
        });
    }

    private static void spawnChargeParticles(ClientLevel level, LivingEntity wearer) {
        RandomSource rng = level.getRandom();
        for (boolean right : new boolean[]{true, false}) {
            Vec3 muzzle = CindermawPauldronItem.shoulderMuzzlePos(wearer, right);
            Vec3 from = muzzle.add((rng.nextDouble() - 0.5D) * 0.9D, (rng.nextDouble() - 0.3D) * 0.7D, (rng.nextDouble() - 0.5D) * 0.9D);
            Vec3 pull = muzzle.subtract(from).scale(0.24D);
            level.addParticle(CindermawParticles.EMBER.get(), from.x, from.y, from.z, pull.x, pull.y, pull.z);
            if (rng.nextFloat() < 0.55F) {
                level.addParticle(CindermawParticles.BLAZE_FLAME.get(), muzzle.x, muzzle.y, muzzle.z, 0.0D, 0.03D, 0.0D);
            }
        }
    }
}
