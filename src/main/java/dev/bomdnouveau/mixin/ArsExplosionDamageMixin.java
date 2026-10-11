package dev.bomdnouveau.mixin;
import dev.bomdnouveau.platform.ArsDamage;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LivingEntity.class)
public abstract class ArsExplosionDamageMixin {
    @WrapMethod(method="hurt")
    private boolean nativeArs$explosion(DamageSource source,float amount,Operation<Boolean> original) {
        var target=(LivingEntity)(Object)this;
        return ArsDamage.hurt(ArsDamage.explosion(),target,source,amount,(s,a)->original.call(s,a));
    }
}
