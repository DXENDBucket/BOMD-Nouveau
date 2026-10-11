package dev.bomdnouveau.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.UUID;

/** Same short visual lease as Neo; Forge transports only server-authoritative endpoints. */
public record LaserVisualPayload(UUID token, ResourceLocation dimension, int casterId, UUID casterUuid,
                                 Vec3 origin, Vec3 end, Vec3 direction, boolean beam, int remaining) {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("bomd_nouveau","laser_visual"), () -> "1", "1"::equals,"1"::equals);
    public static void register() {
        CHANNEL.messageBuilder(LaserVisualPayload.class,0,NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LaserVisualPayload::encode).decoder(LaserVisualPayload::decode)
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.bomdnouveau.client.LaserVisuals.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
    }
    private static void encode(LaserVisualPayload p, FriendlyByteBuf b) {
        b.writeUUID(p.token); b.writeResourceLocation(p.dimension); b.writeVarInt(p.casterId); b.writeUUID(p.casterUuid);
        write(b,p.origin); write(b,p.end); write(b,p.direction); b.writeBoolean(p.beam); b.writeVarInt(p.remaining);
    }
    private static LaserVisualPayload decode(FriendlyByteBuf b) {
        return new LaserVisualPayload(b.readUUID(),b.readResourceLocation(),b.readVarInt(),b.readUUID(),
                read(b),read(b),read(b),b.readBoolean(),b.readVarInt());
    }
    private static void write(FriendlyByteBuf b,Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    private static Vec3 read(FriendlyByteBuf b) { return new Vec3(b.readDouble(),b.readDouble(),b.readDouble()); }
    public static void send(ServerLevel world,LaserVisualPayload packet) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                packet.origin.x,packet.origin.y,packet.origin.z,96,world.dimension())),packet);
    }
}
