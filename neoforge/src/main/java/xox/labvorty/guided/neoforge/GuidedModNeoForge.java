package xox.labvorty.guided.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.neoforge.init.GuidedDataComponents;
import xox.labvorty.guided.neoforge.init.GuidedItems;

@Mod(GuidedMod.MOD_ID)
public final class GuidedModNeoForge {
    public GuidedModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        GuidedMod.init();
        GuidedItems.register(modEventBus);
        GuidedDataComponents.register(modEventBus);
    }
}
