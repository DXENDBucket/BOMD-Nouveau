package dev.bomdnouveau.platform;

import dev.bomdnouveau.api.Ability;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.EnumMap;
import java.util.Map;

public final class BomdConfig {
    public static final ModConfigSpec SPEC;
    private static final Map<Ability, ModConfigSpec.IntValue> COSTS = new EnumMap<>(Ability.class);
    public static final ModConfigSpec.IntValue AMPLIFY_COST;
    public static final ModConfigSpec.BooleanValue TERRAIN_DAMAGE;
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
        TERRAIN_DAMAGE = b.comment("Allow native comet explosions to affect blocks. False keeps entity damage and visuals.")
                .define("comet_terrain_damage", false);
        SPEC = b.build();
    }
    public static int cost(Ability ability) { return SPEC.isLoaded() ? COSTS.get(ability).get() : ability.cost; }
    public static int amplifyCost() { return SPEC.isLoaded() ? AMPLIFY_COST.get() : 10; }
    public static boolean terrainDamage() { return SPEC.isLoaded() && TERRAIN_DAMAGE.get(); }
    private BomdConfig() {}
}
