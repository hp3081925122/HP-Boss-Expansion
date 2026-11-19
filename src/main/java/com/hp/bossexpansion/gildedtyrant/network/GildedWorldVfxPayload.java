package com.hp.bossexpansion.gildedtyrant.network;

import com.hp.bossexpansion.BossExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

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
) implements CustomPacketPayload {

    public static final int KIND_FISSURE_NODE = 1;
    public static final int KIND_GOLD_SPIKE = 2;
    public static final int KIND_FALLING_AXE = 3;
    public static final int KIND_HEAVY_IMPACT = 4;
    public static final int KIND_LEAP_WARN = 5;
    public static final int KIND_OBELISK_SUMMON = 6;

    public static final Type<GildedWorldVfxPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BossExpansion.MOD_ID, "gilded_world_vfx")
    );

    public static final StreamCodec<FriendlyByteBuf, GildedWorldVfxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.kind);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.vx);
                buf.writeFloat(p.vy);
                buf.writeFloat(p.vz);
                buf.writeFloat(p.scale);
                buf.writeVarInt(p.duration);
            },
            buf -> new GildedWorldVfxPayload(
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readVarInt()
            )
    );

    private static volatile Consumer<GildedWorldVfxPayload> clientHandler;

    public static void setClientHandler(Consumer<GildedWorldVfxPayload> handler) {
        clientHandler = handler;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToClient(TYPE, STREAM_CODEC, GildedWorldVfxPayload::handleClient);
    }

    private static void handleClient(GildedWorldVfxPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Consumer<GildedWorldVfxPayload> handler = clientHandler;
            if (handler != null) {
                context.enqueueWork(() -> handler.accept(payload));
            }
        }
    }

    public static void sendPoint(Entity anchor, int kind, Vec3 pos, float scale, int duration) {
        if (!anchor.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntity(anchor,
                    new GildedWorldVfxPayload(kind, pos.x, pos.y, pos.z, 0.0F, 0.0F, 0.0F, scale, duration));
        }
    }

    public static void sendDirected(Entity anchor, int kind, Vec3 pos, Vec3 vec, float scale, int duration) {
        if (!anchor.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntity(anchor,
                    new GildedWorldVfxPayload(kind, pos.x, pos.y, pos.z, (float) vec.x, (float) vec.y, (float) vec.z, scale, duration));
        }
    }
}
