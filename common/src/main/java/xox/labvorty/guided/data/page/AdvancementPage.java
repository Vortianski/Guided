package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class AdvancementPage extends BuiltinPage {
    public AdvancementPage(JsonObject definition) {
        super("guided:advancement", definition, PageTypes.get(ResourceLocation.parse("guided:advancement")));
    }
}
