package dev.bomdnouveau.casting;

import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.platform.BomdConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Provider-owned deadlines; Core owns the active-cast transaction, not cooldown policy.
 * Persistent per actor, keyed by glyph, with a common server clock across dimensions. */
public final class BomdCooldowns {
    private static final String KEY = "bomd_nouveau_cooldowns";
    private static final dev.arsconflux.api.casting.PersistentCooldowns.Namespace STORE =
            dev.arsconflux.api.casting.PersistentCooldowns.register(CarrierRegistry.id("bomd_nouveau:cooldowns"), KEY);
    /** Eagerly register legacy storage before the first player clone or cast. */
    public static void register() {}
    private static boolean applies(LivingEntity actor) {
        return actor != null && actor.level() instanceof ServerLevel && BomdConfig.cooldownsEnabled() && ActiveCasts.cooldownsEnabled(actor)
                && (!(actor instanceof Player player) || !player.isCreative() || BomdConfig.creativeCooldown());
    }
    public static long remaining(LivingEntity actor, Ability ability) {
        if (!applies(actor) || BomdConfig.cooldownTicks(ability) == 0) return 0;
        return STORE.remaining(actor, ability.id);
    }
    public static void released(LivingEntity actor, Ability ability) {
        if (!applies(actor)) return;
        int ticks = BomdConfig.cooldownTicks(ability);
        if (ticks == 0) return;
        STORE.extend(actor, ability.id, ticks);
    }
    public static void succeeded(LivingEntity actor, Ability ability) {
        if (!applies(actor)) return;
        STORE.commit(actor, ability.id, "success", BomdConfig.cooldownTicks(ability));
    }
    private BomdCooldowns() {}
}
