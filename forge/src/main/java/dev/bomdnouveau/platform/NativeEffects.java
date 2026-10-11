package dev.bomdnouveau.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class NativeEffects {
    public static void apply(LivingEntity target, LivingEntity owner, ResourceLocation id, int ticks, int amplifier) {
        var effect = BuiltInRegistries.MOB_EFFECT.getOptional(id);
        effect.ifPresent(value -> target.addEffect(new MobEffectInstance(value, ticks, amplifier), owner));
    }
    private NativeEffects() {}
}
