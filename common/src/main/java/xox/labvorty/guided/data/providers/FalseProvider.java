package xox.labvorty.guided.data.providers;

import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

public class FalseProvider implements ConditionProvider {
    @Override
    public boolean test(JsonObject jsonObject, LocalPlayer localPlayer) {
        return false;
    }
}
