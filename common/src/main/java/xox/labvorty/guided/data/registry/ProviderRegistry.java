package xox.labvorty.guided.data.registry;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import xox.labvorty.guided.data.providers.*;

import java.util.HashMap;
import java.util.Map;

public class ProviderRegistry {
    private static final ConditionProvider FALSE_PROVIDER = new FalseProvider();
    private static final Map<ResourceLocation, ConditionProvider> registry = new HashMap<>();

    public static void register(ResourceLocation resourceLocation, ConditionProvider conditionProvider) {
        if (registry.putIfAbsent(resourceLocation, conditionProvider) != null) {
            throw new IllegalArgumentException("Condition provider already registered: " + resourceLocation);
        }
    }

    public static boolean test(JsonObject jsonObject, LocalPlayer localPlayer) {
        if (!jsonObject.has("type") || !jsonObject.get("type").isJsonPrimitive()) {
            return false;
        }

        ResourceLocation resourceLocation = ResourceLocation.tryParse(
                jsonObject.get("type").getAsString()
        );

        if (resourceLocation == null) {
            return false;
        }

        return get(resourceLocation).test(jsonObject, localPlayer);
    }

    public static boolean testValues(String comparison, int currentValue, int requiredValue) {
        return switch (comparison) {
            case ">" -> currentValue > requiredValue;
            case ">=" -> currentValue >= requiredValue;
            case "<" -> currentValue < requiredValue;
            case "<=" -> currentValue <= requiredValue;
            case "==" -> currentValue == requiredValue;
            case "!=" -> currentValue != requiredValue;
            default -> false;
        };
    }

    public static boolean testValues(String comparison, double currentValue, double requiredValue) {
        return switch (comparison) {
            case ">" -> currentValue > requiredValue;
            case ">=" -> currentValue >= requiredValue;
            case "<" -> currentValue < requiredValue;
            case "<=" -> currentValue <= requiredValue;
            case "==" -> currentValue == requiredValue;
            case "!=" -> currentValue != requiredValue;
            default -> false;
        };
    }

    public static ConditionProvider get(ResourceLocation resourceLocation) {
        return registry.getOrDefault(resourceLocation, FALSE_PROVIDER);
    }

    static {
        register(ResourceLocation.parse("guided:true"), new TrueProvider());
        register(ResourceLocation.parse("guided:false"), new FalseProvider());
        register(ResourceLocation.parse("guided:and"), new AndProvider());
        register(ResourceLocation.parse("guided:or"), new OrProvider());
        register(ResourceLocation.parse("guided:not"), new NotProvider());
        register(ResourceLocation.parse("guided:advancement"), new AdvancementProvider());
        register(ResourceLocation.parse("guided:item"), new ItemProvider());
        register(ResourceLocation.parse("guided:stat"), new StatisticsProvider());
    }
}
