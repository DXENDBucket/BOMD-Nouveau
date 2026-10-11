package dev.bomdnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import dev.arsconflux.api.resource.ArsManaAccount;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.platform.BomdConfig;
import java.util.List;

public final class BomdResources {
    public static ResourceCharge quote(SpellContext context, Ability ability, List<AbstractAugment> augments) {
        long extras = augments.stream().filter(a -> a == AugmentAmplify.INSTANCE).count() * BomdConfig.amplifyCost();
        return new ResourceCharge(new ArsManaAccount(context.getCaster()),
                (int)Math.min(Integer.MAX_VALUE, BomdConfig.cost(ability) + extras));
    }
    private BomdResources() {}
}
