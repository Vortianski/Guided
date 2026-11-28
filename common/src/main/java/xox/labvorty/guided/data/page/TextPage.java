package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class TextPage extends BuiltinPage {
    public TextPage(JsonObject definition) {
        super("guided:text", definition, PageTypes.get(ResourceLocation.parse("guided:text")));
    }
}
