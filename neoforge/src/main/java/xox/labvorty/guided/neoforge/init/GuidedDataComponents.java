package xox.labvorty.guided.neoforge.init;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xox.labvorty.guided.GuidedMod;

public class GuidedDataComponents {
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GuidedMod.MOD_ID);
    private static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> BOOK_ID = DATA_COMPONENTS.register(
            "book_id",
            () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        xox.labvorty.guided.init.GuidedDataComponents.BOOK_ID = BOOK_ID;
    }
}
