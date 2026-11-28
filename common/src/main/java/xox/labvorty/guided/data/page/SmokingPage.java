package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class SmokingPage extends BuiltinPage {
    public SmokingPage(JsonObject definition) {
        super("guided:smoking", definition, PageTypes.get(ResourceLocation.parse("guided:smoking")));
    }
}
