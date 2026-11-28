package xox.labvorty.guided.data.providers;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ItemProvider implements ConditionProvider {
    @Override
    public boolean test(JsonObject jsonObject, LocalPlayer localPlayer) {
        if (jsonObject.has("item")) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(jsonObject.get("item").getAsString()));
            if (!item.equals(Items.AIR)) {
                return localPlayer.getStats().getValue(Stats.ITEM_PICKED_UP.get(item)) > 0;
            }
        }

        return false;
    }
}
