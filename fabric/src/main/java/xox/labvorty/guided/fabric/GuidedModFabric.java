package xox.labvorty.guided.fabric;

import net.fabricmc.api.ModInitializer;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.fabric.init.GuidedDataComponents;
import xox.labvorty.guided.fabric.init.GuidedItems;

public final class GuidedModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GuidedMod.init();
        GuidedDataComponents.register();
        GuidedItems.register();
    }
}
