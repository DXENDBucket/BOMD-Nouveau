package dev.bomdnouveau.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Version-local effect holder API; native missile status semantics stay shared. */
public final class NativeEffects {
    public static void apply(LivingEntity target, LivingEntity owner, ResourceLocation id, int ticks, int amplifier) {
        BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(effect ->
                target.addEffect(new MobEffectInstance(effect, ticks, amplifier), owner));
    }
    private NativeEffects() {}
}
