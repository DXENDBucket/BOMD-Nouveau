package dev.bomdnouveau.mixin;
import com.cerbon.bosses_of_mass_destruction.projectile.*;
import dev.arsconflux.api.damage.NativeSpellDamage;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Binding is opt-in; no native Boss or ordinary projectile gains Ars attributes. */
@Mixin(value={MagicMissileProjectile.class,PetalBladeProjectile.class,SporeBallProjectile.class},remap=false)
public abstract class NativeArsDamageMixin {
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=true),require=0)
    private boolean nativeArs$entity(Entity target,DamageSource source,float amount,Operation<Boolean> original) {
        return NativeSpellDamage.hurt(NativeSpellDamage.context((Entity)(Object)this),target,source,amount,(s,a)->original.call(target,s,a));
    }
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=true),require=0)
    private boolean nativeArs$living(LivingEntity target,DamageSource source,float amount,Operation<Boolean> original) {
        return NativeSpellDamage.hurt(NativeSpellDamage.context((Entity)(Object)this),target,source,amount,(s,a)->original.call(target,s,a));
    }
}
