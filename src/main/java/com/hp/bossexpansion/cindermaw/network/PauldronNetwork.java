package com.hp.bossexpansion.cindermaw.network;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.client.PauldronClientController;
import com.hp.bossexpansion.cindermaw.item.CindermawPauldronItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 熔渊重炮肩甲网络同步：上传按键蓄力/释放指令，向周围客户端广播蓄力状态
public final class PauldronNetwork {
    private PauldronNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToServer(HoldPayload.TYPE, HoldPayload.STREAM_CODEC, PauldronNetwork::handleHoldServer);
        registrar.playToClient(ChargeStatePayload.TYPE, ChargeStatePayload.STREAM_CODEC, PauldronNetwork::handleStateClient);
    }

    private static void handleHoldServer(HoldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                CindermawPauldronItem.handleKeyHold(sp, payload.holding());
            }
        });
    }

    private static void handleStateClient(ChargeStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            context.enqueueWork(() -> ClientBridge.accept(payload));
        }
    }

    public static void broadcastChargeState(Entity wearer, boolean charging) {
        if (!wearer.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(wearer, new ChargeStatePayload(wearer.getId(), charging));
        }
    }

    public static void sendChargeStateToPlayer(ServerPlayer receiver, Entity wearer, boolean charging) {
        PacketDistributor.sendToPlayer(receiver, new ChargeStatePayload(wearer.getId(), charging));
    }

    public record HoldPayload(boolean holding) implements CustomPacketPayload {
        public static final Type<HoldPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "pauldron_hold")
        );

        public static final StreamCodec<FriendlyByteBuf, HoldPayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBoolean(p.holding),
                buf -> new HoldPayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChargeStatePayload(int entityId, boolean charging) implements CustomPacketPayload {
        public static final Type<ChargeStatePayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(BossExpansion.MOD_ID, "pauldron_charge_state")
        );

        public static final StreamCodec<FriendlyByteBuf, ChargeStatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeVarInt(p.entityId);
                    buf.writeBoolean(p.charging);
                },
                buf -> new ChargeStatePayload(buf.readVarInt(), buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final class ClientBridge {
        private static void accept(ChargeStatePayload payload) {
            PauldronClientController.handleChargeState(payload);
        }
    }
}
