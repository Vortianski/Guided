package xox.labvorty.guided.data.providers;

import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import xox.labvorty.guided.data.registry.ProviderRegistry;

public class NotProvider implements ConditionProvider {
    @Override
    public boolean test(JsonObject jsonObject, LocalPlayer localPlayer) {
        if (jsonObject.has("condition")) {
            return !ProviderRegistry.test(jsonObject.get("condition").getAsJsonObject(), localPlayer);
        }

        return false;
    }
}
