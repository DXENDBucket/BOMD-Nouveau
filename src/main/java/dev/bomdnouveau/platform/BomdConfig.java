package dev.bomdnouveau.platform;

import dev.bomdnouveau.api.Ability;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.EnumMap;
import java.util.Map;

public final class BomdConfig {
    public static final ModConfigSpec SPEC;
    private static final Map<Ability, ModConfigSpec.IntValue> COSTS = new EnumMap<>(Ability.class);
    private static final Map<Ability, ModConfigSpec.DoubleValue> COOLDOWNS = new EnumMap<>(Ability.class);
    public static final ModConfigSpec.IntValue AMPLIFY_COST;
    public static final ModConfigSpec.BooleanValue TERRAIN_DAMAGE;
    public static final ModConfigSpec.BooleanValue COOLDOWNS_ENABLED, CREATIVE_COOLDOWN;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("mana");
        for (var ability : Ability.values()) COSTS.put(ability, b.comment(ability == Ability.GAUNTLET_LASER
                ? "Ars mana per prepaid second of active laser; charge-up is free. At most three payments per beam."
                : "Ars mana per actual projectile / effect activation.")
                .defineInRange(ability.id, ability.cost, 0, 100000));
        AMPLIFY_COST = b.comment("Additional payload mana per Amplify. Ordinary Ars method/augment costs remain separate.")
                .defineInRange("per_amplify", 10, 0, 100000);
        b.pop();
        b.push("cooldowns");
        COOLDOWNS_ENABLED = b.comment("Shared per-caster/per-glyph cooldowns through Conflux active casting. Existing projectiles and repeated triggers continue.")
                .define("enabled", true);
        CREATIVE_COOLDOWN = b.comment("Apply cooldown checks and writes in creative mode. False bypasses both.")
                .define("creativeCooldown", false);
        b.push("seconds");
        for (var ability : Ability.values()) COOLDOWNS.put(ability, b.comment(ability == Ability.EARTHDIVE
                ? "Seconds from successful teleport; failure adds no cooldown."
                : "Seconds from acknowledged active release; duplicate occurrences are committed once.")
                .defineInRange(ability.id, ability.cooldownTicks / 20.0, 0, 3600));
        b.pop(); b.pop();
        TERRAIN_DAMAGE = b.comment("Allow native comet explosions to affect blocks. False keeps entity damage and visuals.")
                .define("comet_terrain_damage", false);
        SPEC = b.build();
    }
    public static int cost(Ability ability) { return SPEC.isLoaded() ? COSTS.get(ability).get() : ability.cost; }
    public static int amplifyCost() { return SPEC.isLoaded() ? AMPLIFY_COST.get() : 10; }
    public static boolean terrainDamage() { return SPEC.isLoaded() && TERRAIN_DAMAGE.get(); }
    public static boolean cooldownsEnabled() { return !SPEC.isLoaded() || COOLDOWNS_ENABLED.get(); }
    public static boolean creativeCooldown() { return SPEC.isLoaded() && CREATIVE_COOLDOWN.get(); }
    public static int cooldownTicks(Ability ability) { return SPEC.isLoaded() ? (int)Math.round(COOLDOWNS.get(ability).get() * 20) : ability.cooldownTicks; }
    private BomdConfig() {}
}
