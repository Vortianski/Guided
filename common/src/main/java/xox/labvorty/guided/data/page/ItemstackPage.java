package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class ItemstackPage extends BuiltinPage {
    public ItemstackPage(JsonObject definition) {
        super("guided:itemstack", definition, PageTypes.get(ResourceLocation.parse("guided:itemstack")));
    }
}
