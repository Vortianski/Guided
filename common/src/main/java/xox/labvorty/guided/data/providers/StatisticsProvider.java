package xox.labvorty.guided.data.providers;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.StatType;
import xox.labvorty.guided.data.registry.ProviderRegistry;

public class StatisticsProvider implements ConditionProvider {
    @Override
    public boolean test(JsonObject jsonObject, LocalPlayer localPlayer) {
        if (jsonObject.has("statistic")
                && jsonObject.has("statistic_type")
                && jsonObject.has("comparison")
                && jsonObject.has("value")
        ) {
            ResourceLocation statistic = ResourceLocation.tryParse(jsonObject.get("statistic").getAsString());
            ResourceLocation statisticType = ResourceLocation.tryParse(jsonObject.get("statistic_type").getAsString());
            String comparison = jsonObject.get("comparison").getAsString();
            int requiredValue = jsonObject.get("value").getAsInt();

            if (statistic == null || statisticType == null) {
                return false;
            }

            StatType<?> statType = BuiltInRegistries.STAT_TYPE.get(statisticType);
            if (statType != null && statType.getRegistry().containsKey(statistic)) {
                return ProviderRegistry.testValues(
                        comparison,
                        getStatisticValue(statType, statistic, localPlayer),
                        requiredValue
                );
            }
        }

        return false;
    }

    private <T> int getStatisticValue(StatType<T> statType, ResourceLocation statisticId, LocalPlayer localPlayer) {
        T statistic = statType.getRegistry().get(statisticId);

        return localPlayer.getStats().getValue(statType.get(statistic));
    }
}