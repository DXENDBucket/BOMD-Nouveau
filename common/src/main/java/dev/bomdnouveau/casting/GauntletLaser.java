package dev.bomdnouveau.casting;

import com.cerbon.bosses_of_mass_destruction.entity.BMDEntities;
import com.cerbon.bosses_of_mass_destruction.sound.BMDSounds;
import com.cerbon.bosses_of_mass_destruction.util.BMDUtils;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.network.LaserVisualPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

/** LGPL v3: native LaserAction timing/line hits, detached from the Boss actor.
 * Native visual helpers are used by the client; Core owns lifetime and resource transactions. */
public final class GauntletLaser implements ExecutionDriver<GauntletLaser.Pose> {
    public static final int CHARGE_TICKS = 25, ACTIVE_TICKS = 60, LAG_TICKS = 8;
    public static final double RANGE = 30;
    public record Pose(LivingEntity anchor, Vec3 point, Vec3 direction) {
        Vec3 origin() { return anchor == null ? point : anchor.getEyePosition(); }
        Vec3 facing() { return anchor == null ? direction : anchor.getLookAngle(); }
        boolean valid(ServerLevel world, LivingEntity caster) {
            return anchor == null || anchor.isAlive() && !anchor.isRemoved() && !anchor.isSpectator()
                    && anchor.level() == world && allied(caster, anchor);
        }
    }
    private final float damage;
    private final ResourceCharge charge;
    private final ArrayDeque<Vec3> history = new ArrayDeque<>();
    private int age, prepaid;
    private Vec3 lastOrigin, lastEnd, lastDirection;
    private GauntletLaser(float damage, ResourceCharge charge) { this.damage = damage; this.charge = charge; }
    public static boolean allied(LivingEntity caster, LivingEntity host) {
        return caster == host || caster.isAlliedTo(host) || host.isAlliedTo(caster);
    }
    public static boolean start(ServerLevel world, CastContext frame, double amplify, ResourceCharge charge) {
        LivingEntity anchor = null;
        if (frame.target() instanceof EntityHitResult hit) {
            if (!(hit.getEntity() instanceof LivingEntity living) || !allied(frame.caster(), living)) return false;
            anchor = living;
        }
        var direction = frame.incomingDirection().lengthSqr() > 1e-8 ? frame.incomingDirection() : frame.caster().getLookAngle();
        var pose = new Pose(anchor, frame.origin(), direction.normalize());
        float damage = (float)Math.max(0, BMDEntities.mobConfig.gauntletConfig.attack * .75 * 1.5
                + NativePayloads.amplification() * amplify);
        var driver = new GauntletLaser(damage, charge);
        var session = new ExecutionSession<>(world, frame.withExecutor(anchor == null ? frame.caster() : anchor)
                .withAccount(charge.account()), pose, CHARGE_TICKS + ACTIVE_TICKS, driver, hit -> {},
                () -> pose.valid(world, frame.caster()) && dev.bomdnouveau.GlyphCatalog.EFFECTS.stream()
                        .filter(g -> g.ability == Ability.GAUNTLET_LASER).findFirst().orElseThrow().isEnabled(), ExecutionScope.DIRECT);
        return ExecutionSessions.start(session);
    }
    @Override public boolean start(ExecutionSession<Pose> session) {
        var pose = session.parameters();
        if (!pose.valid(session.world(), session.caster()) || charge.affordableCount(1) == 0) return false;
        lastOrigin = pose.origin(); lastDirection = pose.facing().normalize(); lastEnd = lastOrigin;
        if (!loaded(session.world(), lastOrigin, lastOrigin) || lastDirection.lengthSqr() < 1e-8) return false;
        session.world().playSound(null, lastOrigin.x, lastOrigin.y, lastOrigin.z,
                BMDSounds.GAUNTLET_LASER_CHARGE.get(), SoundSource.PLAYERS, 3, 1);
        send(session, false, 6);
        return true;
    }
    @Override public boolean tick(ExecutionSession<Pose> session) {
        var pose = session.parameters();
        if (!pose.valid(session.world(), session.caster())) return true;
        lastOrigin = pose.origin(); lastDirection = pose.facing().normalize();
        if (!loaded(session.world(), lastOrigin, lastOrigin)) return true;
        age++;
        if (age < CHARGE_TICKS) { if (age % 2 == 0) send(session, false, 6); return false; }
        history.addLast(lastDirection);
        boolean damaging = history.size() >= LAG_TICKS;
        lastDirection = damaging ? history.removeFirst() : history.getFirst();
        var far = lastOrigin.add(lastDirection.scale(RANGE));
        if (!loaded(session.world(), lastOrigin, far)) return true;
        lastEnd = session.world().clip(new ClipContext(lastOrigin, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, pose.anchor() == null ? session.caster() : pose.anchor())).getLocation();
        // Native visuals appear before the first damage sample. Both belong to paid active time.
        // No post-depletion hit can occur: the next active second is prepaid before damage/visuals.
        if (prepaid == 0) {
            if (!charge.trigger(() -> true)) return true;
            prepaid = 20;
        }
        prepaid--;
        if (age % 2 == 0) send(session, age > CHARGE_TICKS, 6);
        if (!damaging) return false;
        var owner = session.context().damageOwner() == null ? session.caster() : session.context().damageOwner();
        var key = ResourceKey.create(Registries.DAMAGE_TYPE, dev.arsconflux.api.projectile.CarrierRegistry.id("bomd_nouveau:gauntlet_laser"));
        var source = new DamageSource(session.world().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), owner);
        for (var entity : BMDUtils.findEntitiesInLine(session.world(), lastOrigin, lastEnd, session.caster())) {
            if (entity instanceof LivingEntity victim && victim.isAlive() && !victim.isSpectator()
                    && victim != pose.anchor() && !allied(session.caster(), victim)) dev.bomdnouveau.platform.ArsDamage.hurt(session.context(),victim,source,damage,victim::hurt);
        }
        return false;
    }
    private static boolean loaded(ServerLevel world, Vec3 start, Vec3 end) {
        if (!Double.isFinite(start.lengthSqr()) || !Double.isFinite(end.lengthSqr())) return false;
        int steps = Math.max(1, (int)Math.ceil(start.distanceTo(end)));
        for (int i = 0; i <= steps; i++) if (!world.hasChunkAt(BlockPos.containing(start.lerp(end, (double)i / steps)))) return false;
        return true;
    }
    private void send(ExecutionSession<Pose> session, boolean beam, int lease) {
        LaserVisualPayload.send(session.world(), new LaserVisualPayload(session.id(), session.world().dimension().location(),
                session.caster().getId(), session.caster().getUUID(), lastOrigin, lastEnd, lastDirection, beam, lease));
    }
    @Override public void close(ExecutionSession<Pose> session, ExecutionSession.EndReason reason) {
        history.clear();
        if (lastOrigin != null) send(session, false, 0);
    }
}
