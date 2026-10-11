package dev.bomdnouveau.mixin;

import com.cerbon.bosses_of_mass_destruction.projectile.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.bomdnouveau.casting.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {MagicMissileProjectile.class, PetalBladeProjectile.class}, remap = false)
public abstract class NativeDamageMixin {
    @ModifyExpressionValue(method = "entityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D", remap = true))
    private double bomdNouveau$damage(double original) {
        var state = (ManagedProjectile)this;
        return state.bomdNouveau$mode() == 0 ? original : state.bomdNouveau$damage();
    }
    @ModifyExpressionValue(method = "entityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;thrown(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", remap = true))
    private DamageSource bomdNouveau$source(DamageSource original) {
        return ((ManagedProjectile)this).bomdNouveau$mode() == 0 ? original : NativePayloads.source((BaseThrownItemProjectile)(Object)this, false);
    }
}
