package dev.bomdnouveau.mixin;

import com.cerbon.bosses_of_mass_destruction.projectile.comet.CometProjectile;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.bomdnouveau.casting.ManagedProjectile;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the native shoot-down mechanic inside the same paid impact transaction. */
@Mixin(value = CometProjectile.class, remap = false)
public abstract class ManagedCometHurtMixin {
    @Inject(method = "hurt", remap = true, at = @At("HEAD"), cancellable = true)
    private void bomdNouveau$shootDown(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        var visual = (CometProjectile)(Object)this;
        var state = (ManagedProjectile)this;
        if (state.bomdNouveau$mode() != 1) return;
        if (state.bomdNouveau$resolving()) { ci.setReturnValue(true); return; }
        if (visual.level() instanceof ServerLevel world && state.confluxTrajectory() != null) {
            var carrier = state.confluxTrajectory();
            var resolver = ArsSpellAccess.resolver(carrier);
            // Native explosions can hurt this visual again before resolution finishes.
            // Detach first so one shoot-down cannot recursively activate the payload.
            state.bomdNouveau$trajectory(null);
            try {
                if (resolver != null) resolver.onResolveEffect(world,
                        new BlockHitResult(visual.position(), Direction.UP, visual.blockPosition(), false));
            } finally {
                carrier.discard(); visual.discard();
            }
        }
        ci.setReturnValue(true);
    }
}
