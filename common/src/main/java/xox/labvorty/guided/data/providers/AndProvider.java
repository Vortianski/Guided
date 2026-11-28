package xox.labvorty.guided.data.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import xox.labvorty.guided.data.registry.ProviderRegistry;

public class AndProvider implements ConditionProvider {
    @Override
    public boolean test(JsonObject jsonObject, LocalPlayer localPlayer) {
        if (!jsonObject.has("conditions") || !jsonObject.get("conditions").isJsonArray()) {
            return false;
        }

        JsonArray conditions = jsonObject.getAsJsonArray("conditions");

        for (JsonElement element : conditions) {
            if (!element.isJsonObject()) {
                return false;
            }

            if (!ProviderRegistry.test(element.getAsJsonObject(), localPlayer)) {
                return false;
            }
        }

        return !conditions.isEmpty();
    }
}