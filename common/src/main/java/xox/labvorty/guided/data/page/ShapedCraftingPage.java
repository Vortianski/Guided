package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class ShapedCraftingPage extends BuiltinPage {
    public ShapedCraftingPage(JsonObject definition) {
        super("guided:shaped_crafting", definition, PageTypes.get(ResourceLocation.parse("guided:shaped_crafting")));
    }
}
