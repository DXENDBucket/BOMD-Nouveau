package dev.bomdnouveau.mixin;

import com.cerbon.bosses_of_mass_destruction.projectile.SporeBallProjectile;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.bomdnouveau.casting.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Includes the delayed explosion callback as well as direct entity hits. */
@Mixin(value = SporeBallProjectile.class, remap = false)
public abstract class SporeDamageMixin {
    /** Native landing state normally comes from client collision. An Ars impact payload
     * has no second local collision, so its synced mode supplies that visual state. */
    @Inject(method = "clientTick", at = @At("HEAD"))
    private void bomdNouveau$landedVisual(CallbackInfo ci) {
        if (((ManagedProjectile)this).bomdNouveau$mode() == 2) ((SporeBallProjectile)(Object)this).impacted = true;
    }
    @ModifyExpressionValue(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D", remap = true))
    private double bomdNouveau$damage(double original) {
        var state = (ManagedProjectile)this;
        return state.bomdNouveau$mode() == 0 ? original : state.bomdNouveau$damage();
    }
    @ModifyExpressionValue(method = "*", at = @At(value = "INVOKE", target = "Lcom/cerbon/bosses_of_mass_destruction/util/BMDUtils;shieldPiercing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", remap = false))
    private DamageSource bomdNouveau$piercing(DamageSource original) {
        return ((ManagedProjectile)this).bomdNouveau$mode() == 0 ? original : NativePayloads.source((SporeBallProjectile)(Object)this, true);
    }
    @ModifyExpressionValue(method = "entityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;thrown(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", remap = true))
    private DamageSource bomdNouveau$source(DamageSource original) {
        return ((ManagedProjectile)this).bomdNouveau$mode() == 0 ? original : NativePayloads.source((SporeBallProjectile)(Object)this, false);
    }
}
