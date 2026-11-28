package xox.labvorty.guided.data.providers;

import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import xox.labvorty.guided.mixins.client.ClientAdvancementsAccessor;

public class AdvancementProvider implements ConditionProvider {
    @Override
    public boolean test(JsonObject definition, LocalPlayer player) {
        if (player == null || !definition.has("advancement")) return false;
        ResourceLocation id = ResourceLocation.tryParse(definition.get("advancement").getAsString());
        if (id == null) return false;
        AdvancementHolder holder = player.connection.getAdvancements().get(id);
        if (holder == null) return false;
        AdvancementProgress progress = ((ClientAdvancementsAccessor) player.connection.getAdvancements()).guided$getProgress().get(holder);
        return progress != null && progress.isDone();
    }
}
