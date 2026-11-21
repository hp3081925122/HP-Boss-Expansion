package com.hp.bossexpansion.gildedtyrant.network;

import com.hp.bossexpansion.gildedtyrant.client.GreataxeClientController;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import com.hp.bossexpansion.zephyrarch.network.ZephyrWorldVfxPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class GreataxeNetwork {
    private GreataxeNetwork() {
    }

    public static int register(SimpleChannel channel, int startId) {
        int id = startId;
        channel.registerMessage(id++, HoldPayload.class,
                (p, buf) -> {
                    buf.writeBoolean(p.holding);
                    buf.writeByte(p.handOrd);
                },
                buf -> new HoldPayload(buf.readBoolean(), buf.readByte()),
                GreataxeNetwork::handleHoldServer,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, StatePayload.class,
                (p, buf) -> {
                    buf.writeVarInt(p.entityId);
                    buf.writeBoolean(p.active);
                    buf.writeByte(p.handOrd);
                },
                buf -> new StatePayload(buf.readVarInt(), buf.readBoolean(), buf.readByte()),
                GreataxeNetwork::handleStateClient,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        return id;
    }

    private static void handleHoldServer(HoldPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                InteractionHand hand = payload.handOrd() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                GildedGreataxeItem.setWhirlwindHolding(sp, payload.holding(), hand);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleStateClient(StatePayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GreataxeClientController.handleStatePayload(payload)));
        ctx.setPacketHandled(true);
    }

    public static void sendHoldToServer(boolean holding, byte handOrd) {
        ZephyrWorldVfxPayload.CHANNEL.sendToServer(new HoldPayload(holding, handOrd));
    }

    public static void broadcastState(Entity player, boolean active, InteractionHand hand) {
        if (!player.level().isClientSide) {
            byte handOrd = (byte) (hand == InteractionHand.OFF_HAND ? 1 : 0);
            ZephyrWorldVfxPayload.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new StatePayload(player.getId(), active, handOrd));
        }
    }

    public static void sendStateToPlayer(ServerPlayer receiver, Entity target, boolean active, InteractionHand hand) {
        byte handOrd = (byte) (hand == InteractionHand.OFF_HAND ? 1 : 0);
        ZephyrWorldVfxPayload.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new StatePayload(target.getId(), active, handOrd));
    }

    public record HoldPayload(boolean holding, byte handOrd) {
    }

    public record StatePayload(int entityId, boolean active, byte handOrd) {
    }
}
