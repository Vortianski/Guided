package xox.labvorty.guided.neoforge.init;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber
public class GuidedEvents {
    @SubscribeEvent
    public static void client(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

        });
    }

    @SubscribeEvent
    public static void common(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {

        });
    }
}
