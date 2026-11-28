package xox.labvorty.guided.data.style;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class Styles {
    private static final Map<ResourceLocation, StyleDefinition> javaStyles = new HashMap<>();
    private static final Map<ResourceLocation, StyleDefinition> jsonStyles = new HashMap<>();

    private static final ResourceLocation DEFAULT_STYLE = ResourceLocation.parse("guided:default");

    static {
        register(DEFAULT_STYLE, new DefaultStyle(new JsonObject()));
    }

    public static void register(ResourceLocation id, StyleDefinition style) {
        javaStyles.put(id, style);
    }

    public static void load(Map<ResourceLocation, JsonElement> objects) {
        jsonStyles.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement element = entry.getValue();

            if (!id.getPath().startsWith("styles/")) {
                continue;
            }

            try {
                ResourceLocation styleId = id.withPath(
                        id.getPath().substring(7)
                );

                StyleDefinition style = new DefaultStyle(
                        element.getAsJsonObject()
                );

                jsonStyles.put(styleId, style);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static StyleDefinition get(ResourceLocation id) {
        if (jsonStyles.containsKey(id)) {
            return jsonStyles.get(id);
        }

        if (javaStyles.containsKey(id)) {
            return javaStyles.get(id);
        }

        return javaStyles.get(DEFAULT_STYLE);
    }
}