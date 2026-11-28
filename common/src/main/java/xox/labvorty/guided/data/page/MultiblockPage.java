package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class MultiblockPage extends BuiltinPage {
    public MultiblockPage(JsonObject definition) {
        super("guided:multiblock", definition, PageTypes.get(ResourceLocation.parse("guided:multiblock")));
    }
}
