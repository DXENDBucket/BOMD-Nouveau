package dev.bomdnouveau.mixin;

import dev.bomdnouveau.casting.ManagedProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrowableProjectile.class)
public abstract class ManagedMovementMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bomdNouveau$movement(CallbackInfo ci) {
        if (!((Object)this instanceof ManagedProjectile state) || state.bomdNouveau$mode() == 0) return;
        var projectile = (Projectile)(Object)this;
        projectile.baseTick();
        if (projectile.level().isClientSide && state.bomdNouveau$mode() == 1) {
            projectile.setPos(projectile.position().add(projectile.getDeltaMovement()));
            ProjectileUtil.rotateTowardsMovement(projectile, .2F);
        }
        if (state.bomdNouveau$mode() == 2 && projectile.tickCount > 100) projectile.discard();
        ci.cancel();
    }
}
