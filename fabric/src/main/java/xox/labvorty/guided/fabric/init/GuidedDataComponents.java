package xox.labvorty.guided.fabric.init;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import xox.labvorty.guided.GuidedMod;

public class GuidedDataComponents {
    private static DataComponentType<ResourceLocation> BOOK_ID = register(
            "book_id",
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> dataComponentType) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(GuidedMod.MOD_ID, name),
                dataComponentType
        );
    }

    public static void register() {
        xox.labvorty.guided.init.GuidedDataComponents.BOOK_ID = () -> BOOK_ID;
    }
}
