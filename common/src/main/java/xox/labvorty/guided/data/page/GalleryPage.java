package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class GalleryPage extends BuiltinPage {
    public GalleryPage(JsonObject definition) {
        super("guided:gallery", definition, PageTypes.get(ResourceLocation.parse("guided:gallery")));
    }
}
