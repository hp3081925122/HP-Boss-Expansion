package com.hp.bossexpansion.gildedtyrant.network;

import com.hp.bossexpansion.gildedtyrant.client.GildedWorldVfxClient;
import com.hp.bossexpansion.zephyrarch.network.ZephyrWorldVfxPayload;
import net.minecraft.network.FriendlyByteBuf;
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

public record GildedWorldVfxPayload(
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
    public static final int KIND_FISSURE_NODE = 1;
    public static final int KIND_GOLD_SPIKE = 2;
    public static final int KIND_FALLING_AXE = 3;
    public static final int KIND_HEAVY_IMPACT = 4;
    public static final int KIND_LEAP_WARN = 5;
    public static final int KIND_OBELISK_SUMMON = 6;

    public static int register(SimpleChannel channel, int startId) {
        int id = startId;
        channel.registerMessage(id++, GildedWorldVfxPayload.class,
                GildedWorldVfxPayload::encode,
                GildedWorldVfxPayload::decode,
                GildedWorldVfxPayload::handleClient,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        return id;
    }

    public static void encode(GildedWorldVfxPayload p, FriendlyByteBuf buf) {
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

    public static GildedWorldVfxPayload decode(FriendlyByteBuf buf) {
        return new GildedWorldVfxPayload(
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

    private static void handleClient(GildedWorldVfxPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GildedWorldVfxClient.spawn(payload)));
        ctx.setPacketHandled(true);
    }

    public static void sendPoint(Entity anchor, int kind, Vec3 pos, float scale, int duration) {
        if (!anchor.level().isClientSide) {
            ZephyrWorldVfxPayload.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> anchor),
                    new GildedWorldVfxPayload(kind, pos.x, pos.y, pos.z, 0.0F, 0.0F, 0.0F, scale, duration));
        }
    }

    public static void sendDirected(Entity anchor, int kind, Vec3 pos, Vec3 vec, float scale, int duration) {
        if (!anchor.level().isClientSide) {
            ZephyrWorldVfxPayload.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> anchor),
                    new GildedWorldVfxPayload(kind, pos.x, pos.y, pos.z, (float) vec.x, (float) vec.y, (float) vec.z, scale, duration));
        }
    }
}
