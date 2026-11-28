package xox.labvorty.guided.fabric.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.items.GuidedBookItem;

public class GuidedItems {
    private static GuidedBookItem GUIDE_BOOK = register("guide_book", new GuidedBookItem());

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(GuidedMod.MOD_ID, name),
                item
        );
    }

    public static void register() {
        xox.labvorty.guided.init.GuidedItems.GUIDE_BOOK = () -> GUIDE_BOOK;
    }
}
