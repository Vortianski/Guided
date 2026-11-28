package xox.labvorty.guided.data.providers;

import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;

public interface ConditionProvider {
    boolean test(JsonObject jsonObject, LocalPlayer localPlayer);
}
