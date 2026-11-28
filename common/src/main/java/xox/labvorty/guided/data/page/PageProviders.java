package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import xox.labvorty.guided.data.book.Content;
import xox.labvorty.guided.data.registry.ProviderRegistry;

import java.util.HashMap;
import java.util.Map;

public final class PageProviders {
    private static final Map<ResourceLocation, Provider> providers = new HashMap<>();

    static {
        register(ResourceLocation.parse("guided:hide"), (page, player, depth) -> null);

        register(ResourceLocation.parse("guided:alternative"), (page, player, depth) -> {
            JsonObject definition = page.getAsJsonObject("definition");
            JsonObject alternative = definition.getAsJsonObject("alternative");

            return resolve(alternative, player, depth + 1);
        });
    }

    private PageProviders() {
    }

    public static void register(ResourceLocation id, Provider provider) {
        if (providers.putIfAbsent(id, provider) != null) {
            throw new IllegalArgumentException(id.toString());
        }
    }

    public static boolean passes(JsonObject definition, LocalPlayer player) {
        try {
            if (!definition.has("condition")) {
                return true;
            }

            return ProviderRegistry.test(
                    definition.getAsJsonObject("condition"),
                    player
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static JsonObject resolve(JsonObject page, LocalPlayer player, int depth) {
        if (page == null || depth > 16) {
            return null;
        }

        try {
            JsonObject definition = page.getAsJsonObject("definition");

            if (passes(definition, player)) {
                return page;
            }

            ResourceLocation providerId = ResourceLocation.parse(
                    Content.string(definition, "provider_type", "guided:hide")
            );

            Provider provider = providers.get(providerId);

            if (provider == null) {
                return null;
            }

            return provider.resolve(page, player, depth);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @FunctionalInterface
    public interface Provider {
        JsonObject resolve(JsonObject page, LocalPlayer player, int depth);
    }
}