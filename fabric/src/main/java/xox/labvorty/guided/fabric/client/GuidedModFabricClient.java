package xox.labvorty.guided.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import xox.labvorty.guided.fabric.init.GuidedClient;

public final class GuidedModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GuidedClient.register();
    }
}
