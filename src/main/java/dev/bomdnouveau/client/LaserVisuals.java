package dev.bomdnouveau.client;

import com.cerbon.bosses_of_mass_destruction.particle.BMDParticles;
import com.cerbon.bosses_of_mass_destruction.util.VanillaCopies;
import com.cerbon.cerbons_api.api.general.particle.ClientParticleBuilder;
import com.cerbon.cerbons_api.api.static_utilities.Vec3Colors;
import dev.bomdnouveau.network.LaserVisualPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Reuses GauntletLaserRenderer's actual beam helper/material and native sparkle builder.
 * No hidden Gauntlet, Boss renderer, entity casting state or client-side damage is involved. */
@EventBusSubscriber(modid = "bomd_nouveau", value = Dist.CLIENT)
public final class LaserVisuals {
    private record Lease(LaserVisualPayload data, Vec3 previousOrigin, Vec3 previousEnd, long updated, long expires) {}
    private static final LinkedHashMap<UUID, Lease> ACTIVE = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static final RenderType BEAM = RenderType.entityCutoutNoCull(
            ResourceLocation.fromNamespaceAndPath("bosses_of_mass_destruction", "textures/entity/gauntlet_beam.png"));
    private static ClientParticleBuilder sparkles;
    private static ClientParticleBuilder sparkles() {
        // Resolve the native particle only after registries exist, not during subscriber discovery.
        if (sparkles == null) sparkles = new ClientParticleBuilder(BMDParticles.SPARKLES.get())
                .brightness(BMDParticles.FULL_BRIGHT).color(Vec3Colors.LASER_RED).colorVariation(.2);
        return sparkles;
    }
    private static ClientLevel level() {
        var level = Minecraft.getInstance().level;
        if (level != world.get()) { ACTIVE.clear(); world = new WeakReference<>(level); }
        return level;
    }
    public static void accept(LaserVisualPayload data) {
        var level = level();
        if (level == null || !level.dimension().location().equals(data.dimension())) return;
        if (data.remaining() <= 0) { ACTIVE.remove(data.token()); return; }
        if (!Double.isFinite(data.origin().lengthSqr()) || !Double.isFinite(data.end().lengthSqr())
                || !Double.isFinite(data.direction().lengthSqr()) || data.direction().lengthSqr() < 1e-8
                || data.origin().distanceToSqr(data.end()) > 31 * 31) return;
        var previous = ACTIVE.get(data.token());
        if (ACTIVE.size() >= 256 && previous == null) ACTIVE.remove(ACTIVE.keySet().iterator().next());
        ACTIVE.put(data.token(), new Lease(data, previous == null ? data.origin() : previous.data.origin(),
                previous == null || !previous.data.beam() ? data.end() : previous.data.end(),
                level.getGameTime(), level.getGameTime() + Math.min(6, data.remaining())));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var level = level();
        if (level == null) return;
        ACTIVE.values().removeIf(l -> l.expires <= level.getGameTime());
        var player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) return;
        for (var lease : ACTIVE.values()) {
            var data = lease.data;
            if (player.distanceToSqr(data.origin()) > 96 * 96) continue;
            Vec3 look = data.direction().normalize();
            Vec3 basis = look.cross(Math.abs(look.y) > .99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            Vec3 second = look.cross(basis).normalize();
            for (int i = 0; i < 2; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                Vec3 ring = basis.scale(Math.cos(angle)).add(second.scale(Math.sin(angle)));
                sparkles().build(data.origin().add(ring).add(look.scale(.5)), ring.scale(-.07));
            }
            if (data.beam()) {
                var motion = data.end().subtract(lease.previousEnd).normalize().scale(.1);
                sparkles().build(data.origin().lerp(data.end(), level.random.nextDouble()), motion);
            }
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var level = level(); var client = Minecraft.getInstance();
        if (level == null || client.player == null || ACTIVE.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = client.renderBuffers().bufferSource();
        for (var lease : ACTIVE.values()) {
            if (!lease.data.beam() || lease.expires <= level.getGameTime()) continue;
            double blend = Math.min(1, Math.max(0, (level.getGameTime() - lease.updated + partial) / 2));
            var start = lease.previousOrigin.lerp(lease.data.origin(), blend);
            var end = lease.previousEnd.lerp(lease.data.end(), blend);
            if (!event.getFrustum().isVisible(new AABB(start, end).inflate(.5))) continue;
            var entity = level.getEntity(lease.data.casterId());
            LivingEntity actor = entity instanceof LivingEntity living && living.getUUID().equals(lease.data.casterUuid())
                    ? living : client.player;
            var poses = event.getPoseStack(); poses.pushPose();
            try {
                var camera = event.getCamera().getPosition();
                // The native helper starts at actor position + (eyeHeight - 1.7).
                // Translate its coordinate frame and endpoint together to use the real beam origin.
                double nativeEye = actor.getEyeHeight() - 1.7f;
                var nativeOrigin = VanillaCopies.fromLerpedPosition(actor, nativeEye, partial);
                var nativeEnd = nativeOrigin.add(end.subtract(start));
                poses.translate(start.x - camera.x, start.y - camera.y - nativeEye, start.z - camera.z);
                VanillaCopies.renderBeam(actor, nativeEnd, nativeEnd, partial, Vec3Colors.LASER_RED, poses, buffers, BEAM);
            } finally { poses.popPose(); }
        }
        buffers.endBatch(BEAM);
    }
    private LaserVisuals() {}
}
