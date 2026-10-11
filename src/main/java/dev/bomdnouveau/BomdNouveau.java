package dev.bomdnouveau;

import dev.bomdnouveau.platform.BomdConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BomdNouveau.MOD_ID)
public final class BomdNouveau {
    public static final String MOD_ID = "bomd_nouveau";
    public BomdNouveau(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, BomdConfig.SPEC);
        GlyphCatalog.register();
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(GlyphCatalog::configureCarriers));
        var tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        tabs.register("glyphs", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.bomd_nouveau"))
                .icon(() -> GlyphCatalog.PROJECTILES.get(0).getGlyph().getDefaultInstance())
                .displayItems((parameters, output) -> GlyphCatalog.ALL.forEach(g -> output.accept(g.getGlyph()))).build());
        tabs.register(bus);
    }
}
