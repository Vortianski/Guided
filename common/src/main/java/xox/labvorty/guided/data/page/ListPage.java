package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class ListPage extends BuiltinPage {
    public ListPage(JsonObject definition) {
        super("guided:list", definition, PageTypes.get(ResourceLocation.parse("guided:list")));
    }
}
