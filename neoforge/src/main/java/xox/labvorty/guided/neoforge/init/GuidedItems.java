package xox.labvorty.guided.neoforge.init;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.items.GuidedBookItem;

public class GuidedItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(GuidedMod.MOD_ID);
    private static final DeferredHolder<Item, GuidedBookItem> GUIDE_BOOK = ITEMS.register("guide_book", GuidedBookItem::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        xox.labvorty.guided.init.GuidedItems.GUIDE_BOOK = GUIDE_BOOK;
    }
}
