package com.hp.bossexpansion.zephyrarch.network;

import com.hp.bossexpansion.BossExpansion;
import com.hp.bossexpansion.cindermaw.network.PauldronNetwork;
import com.hp.bossexpansion.gildedtyrant.network.GildedWorldVfxPayload;
import com.hp.bossexpansion.gildedtyrant.network.GreataxeNetwork;
import com.hp.bossexpansion.zephyrarch.client.ZephyrWorldVfxClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public record ZephyrWorldVfxPayload(
        int kind,
        double x,
        double y,
        double z,
        float vx,
        float vy,
        float vz,
        float scale,
        int duration
) {
    public static final int KIND_STORM_EYE_MARK = 1;
    public static final int KIND_DOWNDRAFT_COLUMN = 2;
    public static final int KIND_CHARGE_BURST = 3;
    public static final int KIND_TWISTER_DISSIPATE = 4;
    public static final int KIND_WIND_DASH_TRAIL = 5;

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "main"),
            () -> PROTOCOL,
            v -> PROTOCOL.equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v),
            v -> PROTOCOL.equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v)
    );

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, ZephyrWorldVfxPayload.class,
                ZephyrWorldVfxPayload::encode,
                ZephyrWorldVfxPayload::decode,
                ZephyrWorldVfxPayload::handleClient,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        id = GauntletNetwork.register(CHANNEL, id);
        id = PauldronNetwork.register(CHANNEL, id);
        id = GildedWorldVfxPayload.register(CHANNEL, id);
        id = GreataxeNetwork.register(CHANNEL, id);
    }

    public static void encode(ZephyrWorldVfxPayload p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.kind);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeFloat(p.vx);
        buf.writeFloat(p.vy);
        buf.writeFloat(p.vz);
        buf.writeFloat(p.scale);
        buf.writeVarInt(p.duration);
    }

    public static ZephyrWorldVfxPayload decode(FriendlyByteBuf buf) {
        return new ZephyrWorldVfxPayload(
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt()
        );
    }

    private static void handleClient(ZephyrWorldVfxPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ZephyrWorldVfxClient.spawn(payload)));
        ctx.setPacketHandled(true);
    }

    public static void sendPoint(Entity anchor, int kind, Vec3 pos, float scale, int duration) {
        if (!anchor.level().isClientSide) {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> anchor),
                    new ZephyrWorldVfxPayload(kind, pos.x, pos.y, pos.z, 0.0F, 0.0F, 0.0F, scale, duration));
        }
    }

    public static void sendDirected(Entity anchor, int kind, Vec3 pos, Vec3 vec, float scale, int duration) {
        if (!anchor.level().isClientSide) {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> anchor),
                    new ZephyrWorldVfxPayload(kind, pos.x, pos.y, pos.z, (float) vec.x, (float) vec.y, (float) vec.z, scale, duration));
        }
    }
}
