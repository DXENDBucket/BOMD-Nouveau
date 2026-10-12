package dev.bomdnouveau.casting;

import com.cerbon.bosses_of_mass_destruction.projectile.BaseThrownItemProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.*;
import dev.bomdnouveau.api.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** All carrier geometry comes from Core/Ars; only native payload semantics live here. */
public final class ProjectileCasting {
    public static boolean replace(Ability ability, EntityProjectileSpell carrier, CarrierProfile profile) {
        if (!(carrier.level() instanceof ServerLevel world)) return false;
        var resolver = ArsSpellAccess.resolver(carrier);
        var frame = CastContext.of(resolver.spellContext, null, null);
        if (frame.caster() == null) return true;
        var stats = resolver.getCastStats();
        var quote = BomdResources.quote(resolver.spellContext, ability, stats.getAugments());
        var selected = frame.withAccount(quote.account()).withIncomingDirection(TriggerGeometry.direction(carrier));
        var visual = NativePayloads.create(ability, world, selected, stats.getAmpMultiplier());
        var trajectory = new ArsTrajectory(visual, carrier);
        var continuation = resolver.clone();
        var driver = new Flight(ability, visual, trajectory, stats.getAmpMultiplier());
        var session = new ExecutionSession<>(world, selected, visual,
                Math.max(1, Math.min(1200, carrier.getExpirationTime() + 1)), driver,
                hit -> {
                    var child = continuation.clone();
                    CastContexts.bind(child.spellContext, CastContexts.current().withAccount(frame.account()));
                    child.onResolveEffect(world, hit);
                }, () -> dev.bomdnouveau.GlyphCatalog.PROJECTILES.stream().filter(g -> g.ability == ability).findFirst().orElseThrow().isEnabled(),
                ExecutionScope.DIRECT).billing(quote::trigger, profile.repeated());
        ExecutionSessions.start(session);
        return true;
    }
    private static final class Flight implements ExecutionDriver<BaseThrownItemProjectile> {
        private final Ability ability;
        private final BaseThrownItemProjectile visual;
        private final ArsTrajectory trajectory;
        private final double amplify;
        private Vec3 direction;
        Flight(Ability ability, BaseThrownItemProjectile visual, ArsTrajectory trajectory, double amplify) {
            this.ability = ability; this.visual = visual; this.trajectory = trajectory; this.amplify = amplify;
            direction = trajectory.incomingDirection();
        }
        @Override public Vec3 incomingDirection() { return direction; }
        @Override public boolean start(ExecutionSession<BaseThrownItemProjectile> session) {
            var state = (ManagedProjectile)visual;
            state.bomdNouveau$mode(1); state.bomdNouveau$trajectory(trajectory.carrier());
            state.bomdNouveau$damage(NativePayloads.damage(ability, amplify));
            boolean added = trajectory.start(session.world(), () -> {}, hit -> {
                if (hit.getType() == HitResult.Type.MISS || state.bomdNouveau$resolving()) return;
                direction = trajectory.incomingDirection();
                state.bomdNouveau$resolving(true);
                try {
                    if (!session.activate(() -> {
                        boolean continued=dev.arsconflux.api.projectile.ImpactTriggers.resolve(hit,
                                dev.arsconflux.api.projectile.ImpactTriggers.Policy.ENTITY_DAMAGE,
                                () -> NativePayloads.impact(ability, session.world(), session.context().withTarget(hit), hit, state.bomdNouveau$damage(), amplify));
                        if(continued) session.impact(hit);
                        return true;
                    })) session.finish(ExecutionSession.EndReason.DENIED);
                } finally { state.bomdNouveau$resolving(false); }
            });
            if (added) NativePayloads.launchSound(ability, session.world(), session.caster());
            return added;
        }
        @Override public boolean tick(ExecutionSession<BaseThrownItemProjectile> session) { return trajectory.tick(session.world()); }
        @Override public void close(ExecutionSession<BaseThrownItemProjectile> session, ExecutionSession.EndReason reason) {
            ((ManagedProjectile)visual).bomdNouveau$trajectory(null);
            trajectory.close();
        }
    }
    private ProjectileCasting() {}
}
