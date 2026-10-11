package dev.bomdnouveau;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.arsconflux.api.glyph.GlyphPresentation;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.glyph.BomdProjectileGlyph;
import dev.bomdnouveau.glyph.BomdEffectGlyph;
import java.util.List;
import java.util.stream.Stream;

public final class GlyphCatalog {
    public static final List<BomdProjectileGlyph> PROJECTILES = List.of(
            new BomdProjectileGlyph(Ability.MISSILE), new BomdProjectileGlyph(Ability.PETAL),
            new BomdProjectileGlyph(Ability.COMET), new BomdProjectileGlyph(Ability.SPORE));
    public static final List<BomdEffectGlyph> EFFECTS = List.of(
            new BomdEffectGlyph(Ability.COMET_BURST), new BomdEffectGlyph(Ability.SPORE_BURST),
            new BomdEffectGlyph(Ability.OBSIDIAN_BURST), new BomdEffectGlyph(Ability.FIRE_WAVE),
            new BomdEffectGlyph(Ability.OBSIDIAN_SPIKES), new BomdEffectGlyph(Ability.EARTHDIVE));
    public static final List<AbstractSpellPart> ALL = Stream.concat(EFFECTS.stream(), PROJECTILES.stream())
            .map(g -> (AbstractSpellPart)g).toList();
    public static void register() {
        GlyphPresentation.register("bomd_nouveau", g -> g instanceof BomdProjectileGlyph ? "1" : "0");
        ALL.forEach(GlyphRegistry::registerSpell);
    }
    public static void configureCarriers() {
        for (var profile : CarrierRegistry.profiles()) {
            var glyph = profile.glyph();
            if (glyph != null) { profile.rememberOriginalAugments(glyph); glyph.compatibleAugments.addAll(PROJECTILES); }
        }
    }
    private GlyphCatalog() {}
}
