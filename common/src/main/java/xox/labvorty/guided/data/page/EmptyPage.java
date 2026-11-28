package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class EmptyPage extends BuiltinPage {
    public EmptyPage(JsonObject definition) {
        super("guided:empty", definition, PageTypes.get(ResourceLocation.parse("guided:empty")));
    }
}
