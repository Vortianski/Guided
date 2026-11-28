package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class RecipeListPage extends BuiltinPage {
    public RecipeListPage(JsonObject definition) {
        super("guided:recipe_list", definition, PageTypes.get(ResourceLocation.parse("guided:recipe_list")));
    }
}
