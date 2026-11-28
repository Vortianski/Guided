package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class ShapelessCraftingPage extends BuiltinPage {
    public ShapelessCraftingPage(JsonObject definition) {
        super("guided:shapeless_crafting", definition, PageTypes.get(ResourceLocation.parse("guided:shapeless_crafting")));
    }
}
