package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class ImagePage extends BuiltinPage {
    public ImagePage(JsonObject definition) {
        super("guided:image", definition, PageTypes.get(ResourceLocation.parse("guided:image")));
    }
}
