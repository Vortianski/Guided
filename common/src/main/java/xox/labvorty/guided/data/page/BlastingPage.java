package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class BlastingPage extends BuiltinPage {
    public BlastingPage(JsonObject definition) {
        super("guided:blasting", definition, PageTypes.get(ResourceLocation.parse("guided:blasting")));
    }
}
