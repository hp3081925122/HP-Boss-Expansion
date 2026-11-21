package com.hp.bossexpansion.cindermaw.network;

import com.hp.bossexpansion.cindermaw.client.PauldronClientController;
import com.hp.bossexpansion.cindermaw.item.CindermawPauldronItem;
import com.hp.bossexpansion.zephyrarch.network.ZephyrWorldVfxPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

// 熔渊重炮肩甲网络同步：上传按键蓄力与释放指令，并向周围客户端广播蓄力状态
public final class PauldronNetwork {
    private PauldronNetwork() {
    }

    public static int register(SimpleChannel channel, int startId) {
        int id = startId;
        channel.registerMessage(id++, HoldPayload.class,
                (p, buf) -> buf.writeBoolean(p.holding),
                buf -> new HoldPayload(buf.readBoolean()),
                PauldronNetwork::handleHoldServer,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, ChargeStatePayload.class,
                (p, buf) -> {
                    buf.writeVarInt(p.entityId);
                    buf.writeBoolean(p.charging);
                },
                buf -> new ChargeStatePayload(buf.readVarInt(), buf.readBoolean()),
                PauldronNetwork::handleStateClient,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        return id;
    }

    private static void handleHoldServer(HoldPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                CindermawPauldronItem.handleKeyHold(sp, payload.holding());
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleStateClient(ChargeStatePayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PauldronClientController.handleChargeState(payload)));
        ctx.setPacketHandled(true);
    }

    public static void sendHoldToServer(boolean holding) {
        ZephyrWorldVfxPayload.CHANNEL.sendToServer(new HoldPayload(holding));
    }

    public static void broadcastChargeState(Entity wearer, boolean charging) {
        if (!wearer.level().isClientSide) {
            ZephyrWorldVfxPayload.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> wearer),
                    new ChargeStatePayload(wearer.getId(), charging));
        }
    }

    public static void sendChargeStateToPlayer(ServerPlayer receiver, Entity wearer, boolean charging) {
        ZephyrWorldVfxPayload.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new ChargeStatePayload(wearer.getId(), charging));
    }

    public record HoldPayload(boolean holding) {
    }

    public record ChargeStatePayload(int entityId, boolean charging) {
    }
}
