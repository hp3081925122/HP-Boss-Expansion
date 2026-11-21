package com.hp.bossexpansion.gildedtyrant.network;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.gildedtyrant.client.GreataxeClientController;
import com.hp.bossexpansion.gildedtyrant.item.GildedGreataxeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GreataxeNetwork {
    private GreataxeNetwork() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(HoldPayload.TYPE, HoldPayload.STREAM_CODEC, GreataxeNetwork::handleHoldServer);
        registrar.playToClient(StatePayload.TYPE, StatePayload.STREAM_CODEC, GreataxeNetwork::handleStateClient);
    }

    private static void handleHoldServer(HoldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                InteractionHand hand = payload.handOrd() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                GildedGreataxeItem.setWhirlwindHolding(sp, payload.holding(), hand);
            }
        });
    }

    private static void handleStateClient(StatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            context.enqueueWork(() -> ClientBridge.accept(payload));
        }
    }

    public static void broadcastState(Entity player, boolean active, InteractionHand hand) {
        if (!player.level().isClientSide()) {
            byte handOrd = (byte) (hand == InteractionHand.OFF_HAND ? 1 : 0);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StatePayload(player.getId(), active, handOrd));
        }
    }

    public static void sendStateToPlayer(ServerPlayer receiver, Entity target, boolean active, InteractionHand hand) {
        byte handOrd = (byte) (hand == InteractionHand.OFF_HAND ? 1 : 0);
        PacketDistributor.sendToPlayer(receiver, new StatePayload(target.getId(), active, handOrd));
    }

    public record HoldPayload(boolean holding, byte handOrd) implements CustomPacketPayload {
        public static final Type<HoldPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "greataxe_hold")
        );

        public static final StreamCodec<FriendlyByteBuf, HoldPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.holding);
                    buf.writeByte(p.handOrd);
                },
                buf -> new HoldPayload(buf.readBoolean(), buf.readByte())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StatePayload(int entityId, boolean active, byte handOrd) implements CustomPacketPayload {
        public static final Type<StatePayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "greataxe_whirlwind_state")
        );

        public static final StreamCodec<FriendlyByteBuf, StatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entityId);
                    buf.writeBoolean(p.active);
                    buf.writeByte(p.handOrd);
                },
                buf -> new StatePayload(buf.readVarInt(), buf.readBoolean(), buf.readByte())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final class ClientBridge {
        private static void accept(StatePayload payload) {
            GreataxeClientController.handleStatePayload(payload);
        }
    }
}
