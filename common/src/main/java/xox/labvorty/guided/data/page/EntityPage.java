package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class EntityPage extends BuiltinPage {
    public EntityPage(JsonObject definition) {
        super("guided:entity", definition, PageTypes.get(ResourceLocation.parse("guided:entity")));
    }
}
