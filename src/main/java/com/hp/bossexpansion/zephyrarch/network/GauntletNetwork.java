package com.hp.bossexpansion.zephyrarch.network;

import com.hp.bossexpansion.zephyrarch.client.GauntletClientController;
import com.hp.bossexpansion.zephyrarch.item.SovereignGauntletItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class GauntletNetwork {
    public static final byte ACTION_CHARGE_START = 1;
    public static final byte ACTION_PUNCH_FIRE = 2;
    public static final byte ACTION_CHARGE_STOP = 3;
    public static final byte ACTION_WIND_DASH = 4;

    private GauntletNetwork() {
    }

    public static void register(SimpleChannel channel, int startId) {
        int id = startId;
        channel.registerMessage(id++, HoldPayload.class,
                (p, buf) -> buf.writeBoolean(p.holding),
                buf -> new HoldPayload(buf.readBoolean()),
                GauntletNetwork::handleHoldServer,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, ActionPayload.class,
                ActionPayload::encode,
                ActionPayload::decode,
                GauntletNetwork::handleActionClient,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static void handleHoldServer(HoldPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                SovereignGauntletItem.setHoldingAttack(sp, payload.holding());
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleActionClient(ActionPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GauntletClientController.handleAction(payload)));
        ctx.setPacketHandled(true);
    }

    public static void sendHoldToServer(boolean holding) {
        ZephyrWorldVfxPayload.CHANNEL.sendToServer(new HoldPayload(holding));
    }

    public static void broadcastAction(Entity player, byte action, InteractionHand hand, int maxChargeTicks, Vec3 pos, Vec3 dir) {
        if (!player.level().isClientSide) {
            byte handOrd = (byte) (hand == InteractionHand.OFF_HAND ? 1 : 0);
            ZephyrWorldVfxPayload.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new ActionPayload(
                    player.getId(),
                    action,
                    handOrd,
                    maxChargeTicks,
                    pos.x,
                    pos.y,
                    pos.z,
                    (float) dir.x,
                    (float) dir.y,
                    (float) dir.z
            ));
        }
    }

    public record HoldPayload(boolean holding) {
    }

    public record ActionPayload(
            int entityId,
            byte action,
            byte handOrd,
            int maxChargeTicks,
            double x,
            double y,
            double z,
            float dx,
            float dy,
            float dz
    ) {
        public static void encode(ActionPayload p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.entityId);
            buf.writeByte(p.action);
            buf.writeByte(p.handOrd);
            buf.writeVarInt(p.maxChargeTicks);
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
            buf.writeFloat(p.dx);
            buf.writeFloat(p.dy);
            buf.writeFloat(p.dz);
        }

        public static ActionPayload decode(FriendlyByteBuf buf) {
            return new ActionPayload(
                    buf.readVarInt(),
                    buf.readByte(),
                    buf.readByte(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            );
        }
    }
}
