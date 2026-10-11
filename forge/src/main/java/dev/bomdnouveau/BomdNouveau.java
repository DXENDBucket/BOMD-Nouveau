package dev.bomdnouveau;

import dev.bomdnouveau.platform.BomdConfig;
import dev.bomdnouveau.network.LaserVisualPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;

@Mod(BomdNouveau.MOD_ID)
public final class BomdNouveau {
    public static final String MOD_ID = "bomd_nouveau";
    public BomdNouveau() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BomdConfig.SPEC);
        GlyphCatalog.register();
        dev.bomdnouveau.casting.BomdActiveCastProvider.register();
        LaserVisualPayload.register();
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(GlyphCatalog::configureCarriers));
        var tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        tabs.register("glyphs", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.bomd_nouveau"))
                .icon(() -> GlyphCatalog.PROJECTILES.get(0).getGlyph().getDefaultInstance())
                .displayItems((parameters, output) -> GlyphCatalog.ALL.forEach(g -> output.accept(g.getGlyph()))).build());
        tabs.register(bus);
    }
}
