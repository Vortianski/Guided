package xox.labvorty.guided.data.registry.reloaders;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.data.book.BookDefinition;
import xox.labvorty.guided.data.registry.BookRegistry;

import java.util.Map;

public class BookRegistryReloadListener extends SimpleJsonResourceReloadListener {
    public BookRegistryReloadListener() {
        super(new Gson(), "guided");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller
    ) {
        int amount = 0;
        int broken = 0;

        BookRegistry.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            String path = resourceLocation.getPath();

            if (!path.endsWith("/book")) {
                continue;
            }

            String bookName = path.substring(0, path.length() - "/book".length());

            if (bookName.isEmpty() || bookName.contains("/")) {
                continue;
            }

            ResourceLocation bookId = resourceLocation.withPath(bookName);

            try {
                JsonObject jsonObject = entry.getValue().getAsJsonObject();
                String name = jsonObject.get("name").getAsString();

                ResourceLocation style = jsonObject.has("style")
                        ? ResourceLocation.parse(jsonObject.get("style").getAsString())
                        : ResourceLocation.parse("guided:default");

                ResourceLocation creativeTab = jsonObject.has("creative_tab")
                        ? ResourceLocation.parse(jsonObject.get("creative_tab").getAsString())
                        : null;

                boolean navigation = jsonObject.has("navigation")
                        && jsonObject.get("navigation").getAsBoolean();

                ResourceLocation itemModel = jsonObject.has("item_model")
                        ? ResourceLocation.parse(jsonObject.get("item_model").getAsString())
                        : ResourceLocation.parse("guided:item/error_book");

                BookRegistry.register(
                        bookId,
                        new BookDefinition(
                                name,
                                style,
                                creativeTab,
                                navigation,
                                itemModel
                        )
                );

                amount++;
            } catch (RuntimeException exception) {
                broken++;

                GuidedMod.LOGGER.error("Failed to load book {}", bookId, exception);
            }
        }

        xox.labvorty.guided.data.book.Content.load(objects);
        xox.labvorty.guided.data.style.Styles.load(objects);
        GuidedMod.LOGGER.info("Loaded {} books! Ignored {} broken book instances!", amount, broken);
    }

    public void loadImmediately(ResourceManager resourceManager) {
        ProfilerFiller profiler = InactiveProfiler.INSTANCE;

        Map<ResourceLocation, JsonElement> objects = prepare(resourceManager, profiler);
        apply(objects, resourceManager, profiler);
    }
}