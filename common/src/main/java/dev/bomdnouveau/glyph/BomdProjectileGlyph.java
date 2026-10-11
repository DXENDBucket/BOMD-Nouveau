package dev.bomdnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.casting.BomdResources;
import dev.bomdnouveau.casting.ProjectileCasting;
import java.util.List;

public final class BomdProjectileGlyph extends AbstractAugment implements ProjectileForm {
    public final Ability ability;
    public BomdProjectileGlyph(Ability ability) {
        super(CarrierRegistry.id("bomd_nouveau:glyph_" + ability.id), ability.name);
        this.ability = ability;
    }
    @Override public boolean replace(EntityProjectileSpell carrier, CarrierProfile profile) {
        return ProjectileCasting.replace(ability, carrier, profile);
    }
    @Override public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) {
        return BomdResources.quote(context, ability, augments);
    }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override public String getBookDescription() {
        return net.minecraft.network.chat.Component.translatable("bomd_nouveau.glyph_desc." + getRegistryName().getPath()).getString();
    }
}
