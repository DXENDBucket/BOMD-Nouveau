package dev.bomdnouveau.casting;

import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.platform.BomdConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Provider-owned deadlines; Core owns the active-cast transaction, not cooldown policy.
 * Persistent per actor, keyed by glyph, with a common server clock across dimensions. */
public final class BomdCooldowns {
    private static final String KEY = "bomd_nouveau_cooldowns";
    private static long now(LivingEntity actor) { return actor.level().getServer().overworld().getGameTime(); }
    private static boolean applies(LivingEntity actor) {
        return actor != null && actor.level() instanceof ServerLevel && BomdConfig.cooldownsEnabled()
                && (!(actor instanceof Player player) || !player.isCreative() || BomdConfig.creativeCooldown());
    }
    public static long remaining(LivingEntity actor, Ability ability) {
        if (!applies(actor) || BomdConfig.cooldownTicks(ability) == 0) return 0;
        return Math.max(0, actor.getPersistentData().getCompound(KEY).getLong(ability.id) - now(actor));
    }
    public static void released(LivingEntity actor, Ability ability) {
        if (!applies(actor)) return;
        int ticks = BomdConfig.cooldownTicks(ability);
        if (ticks == 0) return;
        var data = actor.getPersistentData();
        CompoundTag deadlines = data.getCompound(KEY);
        deadlines.putLong(ability.id, Math.max(deadlines.getLong(ability.id), now(actor) + ticks));
        data.put(KEY, deadlines);
    }
    public static void succeeded(LivingEntity actor, Ability ability) {
        if (!applies(actor)) return;
        // Within an active use, wait until all synchronous branches have dispatched.
        // Later projectile/ongoing successes commit immediately without gating that execution.
        if (!ActiveCasts.afterRelease(actor, CarrierRegistry.id("bomd_nouveau:cooldown/" + ability.id),
                () -> released(actor, ability))) released(actor, ability);
    }
    private BomdCooldowns() {}
}
