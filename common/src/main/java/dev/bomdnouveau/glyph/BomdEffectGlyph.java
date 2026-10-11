package dev.bomdnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.casting.BomdResources;
import dev.bomdnouveau.casting.NativePayloads;
import dev.bomdnouveau.casting.GroundEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.Set;

public final class BomdEffectGlyph extends AbstractEffect implements TriggerPaidGlyph {
    public final Ability ability;
    public BomdEffectGlyph(Ability ability) {
        super(CarrierRegistry.id("bomd_nouveau:glyph_" + ability.id), ability.name);
        this.ability = ability;
        if (ability == Ability.EARTHDIVE) compatibleAugments = new java.util.HashSet<>();
    }
    @Override public void onResolve(HitResult hit, Level level, LivingEntity ignoredCaster, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(level instanceof ServerLevel world) || !isEnabled()) return;
        var frame = CastContext.of(context, hit, null);
        if (frame.caster() == null || !frame.caster().isAlive()) return;
        if (ability == Ability.GAUNTLET_LASER) {
            dev.bomdnouveau.casting.GauntletLaser.start(world, frame, stats.getAmpMultiplier(), firstCharge(context, stats.getAugments()));
            return;
        }
        firstCharge(context, stats.getAugments()).trigger(() -> {
            if (ability.groundEffect()) return GroundEffects.trigger(ability, world, frame, stats.getAmpMultiplier());
            NativePayloads.impact(ability.payload(), world, frame, hit, NativePayloads.damage(ability.payload(), stats.getAmpMultiplier()), stats.getAmpMultiplier());
            return true;
        });
    }
    @Override public ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) { return BomdResources.quote(context, ability, augments); }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE); }
    @Override public void addAugmentDescriptions(java.util.Map<AbstractAugment, String> descriptions) {
        if (ability == Ability.EARTHDIVE) return;
        descriptions.put(AugmentAmplify.INSTANCE, "Increases damage for spores or explosion strength for comets.");
        descriptions.put(AugmentDampen.INSTANCE, "Decreases damage for spores or explosion strength for comets.");
    }
    @Override public String getBookDescription() {
        return net.minecraft.network.chat.Component.translatable("bomd_nouveau.glyph_desc." + getRegistryName().getPath()).getString();
    }
}
