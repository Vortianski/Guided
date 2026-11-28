package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class LinkPage extends BuiltinPage {
    public LinkPage(JsonObject definition) {
        super("guided:link", definition, PageTypes.get(ResourceLocation.parse("guided:link")));
    }
}
