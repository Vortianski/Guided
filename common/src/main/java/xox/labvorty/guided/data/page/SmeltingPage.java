package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class SmeltingPage extends BuiltinPage {
    public SmeltingPage(JsonObject definition) {
        super("guided:smelting", definition, PageTypes.get(ResourceLocation.parse("guided:smelting")));
    }
}
