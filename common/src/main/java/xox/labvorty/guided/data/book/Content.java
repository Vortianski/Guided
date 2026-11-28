package xox.labvorty.guided.data.book;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.data.registry.BookRegistry;

public final class Content {
    private static volatile Map<ResourceLocation,Book> books = Map.of();

    public static String string(JsonObject jsonObject, String string, String fallback) {
        return jsonObject.has(string) ? jsonObject.get(string).getAsString() : fallback;
    }

    public static int numeric(JsonObject jsonObject, String string, int fallback) {
        return jsonObject.has(string) ? jsonObject.get(string).getAsInt() : fallback;
    }

    public static Book get(ResourceLocation id) {
        return books.getOrDefault(id, new Book(List.of(), List.of(), new JsonObject(),false));
    }

    public static void load(Map<ResourceLocation,JsonElement> objects) {
        Map<ResourceLocation, Book> result = new HashMap<>();

        for (ResourceLocation resourceLocation : BookRegistry.getRegistry().keySet()) {
            boolean valid = true;
            List<Category> categories = new ArrayList<>();
            List<Entry> entries = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> pair : objects.entrySet()) {
                ResourceLocation id = pair.getKey();
                String prefix = resourceLocation.getPath() + "/";

                if (!id.getNamespace().equals(resourceLocation.getNamespace()) || !id.getPath().startsWith(prefix)) {
                    continue;
                }

                String path = id.getPath().substring(prefix.length());
                try {
                    JsonObject jsonObject = pair.getValue().getAsJsonObject().deepCopy();
                    if (path.startsWith("categories/")) {
                        Category category = new Category(path.substring(11),jsonObject);
                        category.priority();
                        category.name();
                        categories.add(category);
                    }

                    if (path.startsWith("entries/")) {
                        Entry entry = new Entry(path.substring(8),jsonObject);
                        if (Math.abs((long)entry.x()) > 4096 || Math.abs((long)entry.y()) > 4096) {
                            throw new IllegalArgumentException("coordinates");
                        }

                        entry.priority();
                        entry.name();
                        entry.category();

                        if (jsonObject.has("pages"))  {
                            for (JsonElement page : jsonObject.getAsJsonArray("pages")) {
                                Page parsed = Page.parse(page.getAsJsonObject());
                                if (parsed.definition() == null) {
                                    throw new IllegalArgumentException("Missing page definition");
                                }
                            }
                        }

                        entries.add(entry);
                    }
                }

                catch (RuntimeException e) {
                    valid = false;
                    GuidedMod.LOGGER.warn("Invalid content {}", id, e);
                }
            }

            if (categories.isEmpty() || entries.stream().anyMatch(e -> categories.stream().noneMatch(c -> c.id().equals(e.category())))) {
                valid = false;
            }

            categories.sort(Comparator.comparingInt(Category::priority).thenComparing(Category::id));
            entries.sort(Comparator.comparingInt(Entry::priority).thenComparing(Entry::id));
            result.put(
                    resourceLocation,
                    new Book(
                            List.copyOf(categories),
                            List.copyOf(entries),
                            objects.get(resourceLocation.withPath(resourceLocation.getPath() + "/book")).getAsJsonObject().deepCopy(),
                            valid
                    )
            );
        }

        books=Map.copyOf(result);
    }

    public record Book(List<Category> categories,List<Entry> entries, JsonObject json, boolean valid) {}
    public record Page(ResourceLocation type,JsonObject definition) {
        public static Page parse(JsonObject j) {
            return new Page(ResourceLocation.parse(j.get("type").getAsString()),j.getAsJsonObject("definition"));
        }
    }

    public record Category(String id,JsonObject json) {
        public int priority() {
            return numeric(json,"priority",0);
        }

        public String name() {
            return string(json,"name",id);
        }
    }

    public record Entry(String id,JsonObject json) {
        public String category() {
            return string(json,"category","default");
        }

        public int x() {
            return numeric(json,"x",0);
        }

        public int y() {
            return numeric(json,"y",0);
        }

        public String name() {
            return string(json,"name",id);
        }

        public int priority() {
            return numeric(json,"priority",0);
        }
    }
}
