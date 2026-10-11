package dev.bomdnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.bomdnouveau.api.Ability;
import dev.bomdnouveau.glyph.BomdEffectGlyph;
import dev.bomdnouveau.glyph.BomdProjectileGlyph;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/** Only new active uses are gated. Hit callbacks, scatter branches and ongoing beams are untouched. */
public final class BomdActiveCastProvider implements ActiveCastProvider {
    public static void register() { ActiveCasts.register(CarrierRegistry.id("bomd_nouveau:active_casting"), new BomdActiveCastProvider()); }
    private static EnumSet<Ability> abilities(List<Spell> recipes) {
        var result = EnumSet.noneOf(Ability.class);
        for (var recipe : recipes) for (var part : ArsSpellAccess.parts(recipe)) {
            if (part instanceof BomdEffectGlyph glyph) result.add(glyph.ability);
            else if (part instanceof BomdProjectileGlyph glyph) result.add(glyph.ability);
        }
        return result;
    }
    @Override public boolean supports(Spell spell) { return !abilities(List.of(spell)).isEmpty(); }
    @Override public boolean allowed(ActiveCastRequest request) {
        for (var ability : abilities(List.of(request.spell()))) {
            long remaining = BomdCooldowns.remaining(request.caster(), ability);
            if (remaining == 0) continue;
            if (request.caster() instanceof ServerPlayer player) player.displayClientMessage(
                    Component.translatable("bomd_nouveau.cast.cooldown",
                            Component.translatable("bomd_nouveau.glyph_name.glyph_" + ability.id),
                            String.format(Locale.ROOT, "%.1f", remaining / 20.0)).withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }
    @Override public void released(ActiveCastRequest request, List<Spell> recipes) {
        for (var ability : abilities(recipes)) if (ability != Ability.EARTHDIVE)
            BomdCooldowns.released(request.caster(), ability);
    }
}
