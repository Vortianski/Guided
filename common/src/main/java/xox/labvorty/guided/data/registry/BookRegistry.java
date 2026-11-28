package xox.labvorty.guided.data.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import xox.labvorty.guided.data.book.BookDefinition;

import java.util.HashMap;
import java.util.Map;

public class BookRegistry {
    private static final BookDefinition DEFAULT = new BookDefinition(
            "guided.book.error",
            ResourceLocation.parse("guided:default"),
            null,
            false,
            ResourceLocation.parse("guided:item/error_book")
    );
    private static final Map<ResourceLocation, BookDefinition> registry = new HashMap<>();

    public static void register(ResourceLocation resourceLocation, BookDefinition bookDefinition) {
        if (registry.putIfAbsent(resourceLocation, bookDefinition) != null) {
            throw new IllegalArgumentException("Book already registered: " + resourceLocation);
        }
    }

    public static BookDefinition get(ResourceLocation resourceLocation) {
        return registry.getOrDefault(resourceLocation, DEFAULT);
    }

    public static Map<ResourceLocation, BookDefinition> getRegistry() {
        return registry;
    }

    public static void clear() {
        registry.clear();
    }
}
