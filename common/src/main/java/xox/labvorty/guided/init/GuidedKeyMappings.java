package xox.labvorty.guided.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class GuidedKeyMappings {
    public static final KeyMapping TOGGLE_READ = new KeyMapping(
            "guided.key_mapping.toggle_read",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "guided.key_category.guided"
    );
    public static final KeyMapping RELOAD = new KeyMapping(
            "guided.key_mapping.reload",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "guided.key_category.guided"
    );
}
