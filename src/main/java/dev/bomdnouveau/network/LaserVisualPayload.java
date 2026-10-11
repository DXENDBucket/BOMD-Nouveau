package dev.bomdnouveau.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Server-authoritative endpoints, visual-only short lease; no client resource or damage authority. */
public record LaserVisualPayload(UUID token, ResourceLocation dimension, int casterId, UUID casterUuid,
                                 Vec3 origin, Vec3 end, Vec3 direction, boolean beam, int remaining) implements CustomPacketPayload {
    public static final Type<LaserVisualPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("bomd_nouveau", "laser_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserVisualPayload> CODEC = StreamCodec.of((b, p) -> {
        b.writeUUID(p.token); b.writeResourceLocation(p.dimension); b.writeVarInt(p.casterId); b.writeUUID(p.casterUuid);
        write(b, p.origin); write(b, p.end); write(b, p.direction); b.writeBoolean(p.beam); b.writeVarInt(p.remaining);
    }, b -> new LaserVisualPayload(b.readUUID(), b.readResourceLocation(), b.readVarInt(), b.readUUID(),
            read(b), read(b), read(b), b.readBoolean(), b.readVarInt()));
    private static void write(RegistryFriendlyByteBuf b, Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    private static Vec3 read(RegistryFriendlyByteBuf b) { return new Vec3(b.readDouble(), b.readDouble(), b.readDouble()); }
    @Override public Type<LaserVisualPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (p, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) context.enqueueWork(() -> dev.bomdnouveau.client.LaserVisuals.accept(p));
        });
    }
    public static void send(ServerLevel world, LaserVisualPayload packet) {
        PacketDistributor.sendToPlayersNear(world, null, packet.origin.x, packet.origin.y, packet.origin.z, 96, packet);
    }
}
