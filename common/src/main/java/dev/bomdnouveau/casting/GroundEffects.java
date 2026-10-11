package dev.bomdnouveau.casting;

import com.cerbon.bosses_of_mass_destruction.entity.BMDEntities;
import com.cerbon.bosses_of_mass_destruction.entity.custom.obsidilith.RiftBurst;
import com.cerbon.bosses_of_mass_destruction.item.custom.WallTeleport;
import com.cerbon.bosses_of_mass_destruction.packet.custom.SendDeltaMovementS2CPacket;
import com.cerbon.bosses_of_mass_destruction.particle.BMDParticles;
import com.cerbon.bosses_of_mass_destruction.sound.BMDSounds;
import com.cerbon.bosses_of_mass_destruction.util.BMDUtils;
import com.cerbon.cerbons_api.api.general.event.TimedEvent;
import com.cerbon.cerbons_api.api.network.Dispatcher;
import com.cerbon.cerbons_api.api.static_utilities.*;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.bomdnouveau.api.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;

/** LGPL v3: geometry/timing adapted from BOMD BurstAction, WaveAction and SpikeAction.
 * Rift columns, surface search, indicator particles and sounds remain native BOMD.
 * No Boss or surrogate actor is spawned and no melee attribute is modified. */
public final class GroundEffects {
    public static boolean trigger(Ability ability, ServerLevel world, CastContext context, double amplify) {
        if (!active(world, context)) return false;
        if (ability == Ability.EARTHDIVE) return earthdive(world, context);
        Vec3 origin = context.origin();
        if (!world.hasChunkAt(BlockPos.containing(origin))) return false;
        float damage = (float)Math.max(0, BMDEntities.mobConfig.obsidilithConfig.attack * 1.5 + NativePayloads.amplification() * amplify);
        switch (ability) {
            case OBSIDIAN_BURST -> {
                prepare(world, origin, 0.7f);
                disk(world, context, origin, 7, 30, BMDParticles.OBSIDILITH_BURST_INDICATOR.get(),
                        BMDParticles.OBSIDILITH_BURST.get(), target -> damage(world, context, target, damage, 1.3, false, false));
                later(world, context, origin, 30, () -> sound(world, origin, BMDSounds.OBSIDILITH_BURST.get(), 1.2f, 1));
            }
            case FIRE_WAVE -> {
                var direction = direction(context).multiply(1, 0, 1).normalize();
                if (direction.lengthSqr() < 1.0e-8) direction = context.caster().getLookAngle().multiply(1, 0, 1).normalize();
                if (direction.lengthSqr() < 1.0e-8) return false;
                prepare(world, origin, 0.8f);
                var start = origin.add(direction.scale(4));
                var end = start.add(direction.scale(4 * 5 * 1.5));
                MathUtils.lineCallback(start, end, 5, (point, index) -> later(world, context, point, 20 + index * 8, () -> {
                    sound(world, point, BMDSounds.WAVE_INDICATOR.get(), 0.7f, 1);
                    disk(world, context, point, 4, 20, BMDParticles.OBSIDILITH_WAVE_INDICATOR.get(),
                            BMDParticles.OBSIDILITH_WAVE.get(), target -> damage(world, context, target, damage, 0.8, true, false));
                    later(world, context, point, 20, () -> sound(world, point, BMDSounds.OBSIDILITH_WAVE.get(), 1.2f, 1));
                }));
            }
            case OBSIDIAN_SPIKES -> {
                prepare(world, origin, 1.2f);
                var selected = context.target() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living ? living : null;
                for (int i = 0; i < 3; i++) {
                    var scheduler = CapabilityUtils.getLevelEventScheduler(world);
                    scheduler.addEvent(new TimedEvent(() -> {
                        if (selected != null && (!selected.isAlive() || selected.level() != world)) return;
                        // Keep native three-round pursuit, using bounded velocity prediction for any living target.
                        var velocity = selected == null ? Vec3.ZERO : selected.getDeltaMovement().scale(10);
                        if (velocity.lengthSqr() > 36) velocity = velocity.normalize().scale(6);
                        Vec3 point = selected == null ? origin : selected.position().add(velocity);
                        if (!world.hasChunkAt(BlockPos.containing(point))) return;
                        sound(world, point, BMDSounds.SPIKE_INDICATOR.get(), 1, 1);
                        disk(world, context, point, 2, 20, BMDParticles.OBSIDILITH_SPIKE_INDICATOR.get(),
                                BMDParticles.OBSIDILITH_SPIKE.get(), target -> damage(world, context, target, damage, 0, false, true));
                        later(world, context, point, 20, () -> sound(world, point, BMDSounds.SPIKE.get(), 1.2f, 1));
                    }, 30 + i * 30, 1, () -> !active(world, context)));
                }
            }
            default -> { return false; }
        }
        return true;
    }
    private static void disk(ServerLevel world, CastContext context, Vec3 origin, double radius, int delay,
                             ParticleOptions indicator, ParticleOptions column, Consumer<LivingEntity> impact) {
        java.util.function.Function<BlockPos, Boolean> open = pos -> active(world, context) && world.hasChunkAt(pos)
                && world.getBlockState(pos).canBeReplaced(new DirectionalPlaceContext(world, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
        var rifts = new RiftBurst(context.caster(), world, indicator, column, delay, CapabilityUtils.getLevelEventScheduler(world),
                target -> { if (active(world, context) && target.isAlive() && world.hasChunkAt(target.blockPosition())) impact.accept(target); },
                open, point -> {
                    var above = BlockPos.containing(point.add(0, 14, 0));
                    if (!world.hasChunkAt(above)) return null;
                    var ground = BMDUtils.findGroundBelow(world, above, pos -> true).above();
                    return ground.getY() + 28 >= above.getY() && open.apply(ground) ? ground : null;
                });
        for (var offset : MathUtils.buildBlockCircle(radius)) rifts.tryPlaceRift(origin.add(offset));
    }
    private static void damage(ServerLevel world, CastContext context, LivingEntity target, float amount,
                               double lift, boolean burn, boolean slow) {
        var owner = context.damageOwner() == null ? context.caster() : context.damageOwner();
        var key = ResourceKey.create(Registries.DAMAGE_TYPE, CarrierRegistry.id("bomd_nouveau:shield_piercing"));
        dev.arsconflux.api.damage.NativeSpellDamage.hurt(context,target,new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), owner),amount,target::hurt);
        if (lift > 0) {
            var motion = new Vec3(target.getDeltaMovement().x, lift, target.getDeltaMovement().z);
            target.setDeltaMovement(motion); target.hasImpulse = true;
            if (target instanceof ServerPlayer player) Dispatcher.sendToClient(new SendDeltaMovementS2CPacket(motion), player);
        }
        if (burn) target.setRemainingFireTicks(5); // Native WaveAction uses ticks, not five seconds.
        if (slow) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2), owner);
    }
    private static boolean earthdive(ServerLevel world, CastContext context) {
        // Beneficial actions may move the caster or an ally, never an unwilling target.
        if (context.target() instanceof EntityHitResult hit && (!(hit.getEntity() instanceof LivingEntity host)
                || host.isSpectator() || !(host == context.caster()
                || context.caster().isAlliedTo(host) || host.isAlliedTo(context.caster())))) return false;
        var selected = context.target() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living ? living : context.caster();
        if (!selected.isAlive() || selected.level() != world) return false;
        var direction = selected == context.caster() && context.target() instanceof EntityHitResult ? selected.getLookAngle() : direction(context);
        if (direction.lengthSqr() < 1.0e-8) return false;
        var eye = selected.getEyePosition();
        // Native wall search reads up to 23 blocks; never request an unloaded chunk.
        for (int step = 0; step <= 26; step++) {
            if (!world.hasChunkAt(BlockPos.containing(eye.add(direction.scale(step))))
                    || !world.hasChunkAt(BlockPos.containing(eye.add(0, -1, 0).add(direction.scale(step))))) return false;
        }
        boolean[] moved = {false};
        Consumer<BlockPos> teleport = pos -> {
            var destination = Vec3.atBottomCenterOf(pos);
            var bounds = selected.getBoundingBox().move(destination.subtract(selected.position()));
            if (!world.hasChunkAt(pos) || bounds.minY < world.getMinBuildHeight() || bounds.maxY > world.getMaxBuildHeight()
                    || !world.getWorldBorder().isWithinBounds(bounds) || !world.noCollision(selected, bounds)) return;
            ParticleUtils.spawnParticle(world, BMDParticles.EARTHDIVE_INDICATOR.get(), eye, direction.scale(.3), 8, .1);
            selected.stopRiding(); selected.teleportTo(destination.x, destination.y, destination.z); selected.resetFallDistance();
            sound(world, destination, BMDSounds.EARTHDIVE_SPEAR_THROW.get(), 1, 1);
            moved[0] = true;
        };
        var wall = new WallTeleport(world, selected);
        wall.tryTeleport(direction, eye, teleport);
        if (!moved[0]) wall.tryTeleport(direction, eye.add(0, -1, 0), teleport);
        return moved[0];
    }
    private static Vec3 direction(CastContext context) {
        if (context.incomingDirection().lengthSqr() > 1.0e-8) return context.incomingDirection().normalize();
        if (context.target() instanceof EntityHitResult hit && hit.getEntity() == context.caster()) return context.caster().getLookAngle();
        var towards = context.target() == null ? Vec3.ZERO : context.origin().subtract(context.caster().getEyePosition());
        return towards.lengthSqr() > 1.0e-8 ? towards.normalize() : context.caster().getLookAngle();
    }
    private static boolean active(ServerLevel world, CastContext context) {
        return context.caster() != null && context.caster().isAlive() && context.caster().level() == world;
    }
    private static void later(ServerLevel world, CastContext context, Vec3 point, int ticks, Runnable action) {
        CapabilityUtils.getLevelEventScheduler(world).addEvent(new TimedEvent(action, ticks, 1,
                () -> !active(world, context) || !world.hasChunkAt(BlockPos.containing(point))));
    }
    private static void prepare(ServerLevel world, Vec3 point, float pitch) { sound(world, point, BMDSounds.OBSIDILITH_PREPARE_ATTACK.get(), 3, pitch); }
    private static void sound(ServerLevel world, Vec3 point, SoundEvent sound, float volume, float pitch) {
        SoundUtils.playSound(world, point, sound, SoundSource.PLAYERS, volume, pitch, 64, null);
    }
    private GroundEffects() {}
}
