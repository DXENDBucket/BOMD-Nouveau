package dev.bomdnouveau.casting;

import com.cerbon.bosses_of_mass_destruction.entity.BMDEntities;
import com.cerbon.bosses_of_mass_destruction.projectile.*;
import com.cerbon.bosses_of_mass_destruction.projectile.comet.CometProjectile;
import com.cerbon.bosses_of_mass_destruction.sound.BMDSounds;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.mixin.ProjectileHitAccess;
import dev.bomdnouveau.platform.BomdConfig;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.List;

/** LGPL v3. Native callback behavior adapted from BOMD VolleyAction / CometAction;
 * native entities retain their own impact, particles, sounds and delayed spore eruptions. */
public final class NativePayloads {
    public static double amplification() {
        var harm = EffectHarm.INSTANCE;
        return harm.CONFIG != null && harm.CONFIG.isLoaded() ? harm.AMP_VALUE.get() : 2;
    }
    /** Hard difficulty multiplies a living non-player attack against players by 1.5.
     * Use that fixed baseline; our managed damage types never apply difficulty a second time. */
    public static float damage(Ability ability, double amplify) {
        double base = ability == Ability.MISSILE ? BMDEntities.mobConfig.lichConfig.missile.damage
                : BMDEntities.mobConfig.voidBlossomConfig.attack;
        return (float)Math.max(0, base * 1.5 + amplification() * amplify);
    }
    public static BaseThrownItemProjectile create(Ability ability, ServerLevel world, CastContext context, double amplify) {
        LivingEntity owner = context.damageOwner() != null ? context.damageOwner() : context.caster();
        var payload = switch (ability.payload()) {
            case MISSILE -> new MagicMissileProjectile(owner, world, target -> {
                var config = BMDEntities.mobConfig.lichConfig.missile;
                var id = CarrierRegistry.id(config.mobEffectId);
                dev.bomdnouveau.platform.NativeEffects.apply(target, owner, id, config.mobEffectDuration, config.mobEffectAmplifier);
            }, List.of());
            case PETAL -> new PetalBladeProjectile(owner, world, target -> {}, List.of(), 0);
            case COMET -> new CometProjectile(owner, world, point -> {
                float strength = (float)Math.max(.1, BMDEntities.mobConfig.lichConfig.comet.explosionStrength + .5 * amplify);
                dev.arsconflux.api.damage.NativeSpellDamage.during(context, () -> world.explode(owner, point.x, point.y, point.z, strength,
                        BomdConfig.terrainDamage() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE));
            }, List.of());
            case SPORE -> new SporeBallProjectile(owner, world, hit -> hit.getEntity() != owner);
            default -> throw new IllegalArgumentException("Not a native projectile: " + ability);
        };
        return dev.arsconflux.api.damage.NativeSpellDamage.bind(payload,context);
    }
    /** Fresh payload per impact: repeated trajectories must not reuse a native 'already impacted' flag.
     * Landed spores are real native entities, so their flashing model survives the 30-tick delay. */
    public static void impact(Ability ability, ServerLevel world, CastContext context, HitResult hit, float damage, double amplify) {
        var payload = create(ability, world, context, amplify);
        payload.setPos(hit.getLocation()); payload.setDeltaMovement(Vec3.ZERO);
        var state = (ManagedProjectile)payload;
        state.bomdNouveau$mode(2); state.bomdNouveau$damage(damage);
        if (ability.payload() == Ability.SPORE) {
            if (!world.addFreshEntity(payload)) return;
            // Native entityHit only deals direct damage; the complete landed bloom additionally needs block impact.
            if (hit instanceof EntityHitResult) ((ProjectileHitAccess)payload).bomdNouveau$hit(hit);
            ((ProjectileHitAccess)payload).bomdNouveau$hit(new BlockHitResult(hit.getLocation(), Direction.UP, payload.blockPosition(), false));
        } else ((ProjectileHitAccess)payload).bomdNouveau$hit(hit);
    }
    public static DamageSource source(BaseThrownItemProjectile projectile, boolean piercing) {
        var key = ResourceKey.create(Registries.DAMAGE_TYPE, CarrierRegistry.id("bomd_nouveau:" + (piercing ? "shield_piercing" : "projectile")));
        return new DamageSource(projectile.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), projectile, projectile.getOwner());
    }
    public static void launchSound(Ability ability, ServerLevel world, Entity caster) {
        var sound = switch (ability.payload()) {
            case MISSILE -> BMDSounds.MISSILE_SHOOT.get();
            case PETAL -> BMDSounds.PETAL_BLADE.get();
            case COMET -> BMDSounds.COMET_SHOOT.get();
            case SPORE -> BMDSounds.SPORE_PREPARE.get();
            default -> throw new IllegalArgumentException();
        };
        world.playSound(null, caster.blockPosition(), sound, SoundSource.PLAYERS, 1, 1);
    }
    private NativePayloads() {}
}
