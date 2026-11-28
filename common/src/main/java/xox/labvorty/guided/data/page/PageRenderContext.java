package xox.labvorty.guided.data.page;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xox.labvorty.guided.data.style.StyleDefinition;

import java.util.List;

public record PageRenderContext(
        GuiGraphics graphics,
        Font font,
        StyleDefinition style,
        int x,
        int y,
        int width,
        int height,
        int mouseX,
        int mouseY,
        float partialTick,
        Actions actions
) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    public GuiGraphics getGraphics() {
        return graphics;
    }

    public Font getFont() {
        return font;
    }

    public StyleDefinition getStyle() {
        return style;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public Actions getActions() {
        return actions;
    }

    public interface Actions {
        String texture(String key, String fallback);

        void openEntry(String id);

        void openCategory(String id);

        void playSound(ResourceLocation id);

        void openLink(String url);

        void tooltip(List<Component> lines);

        void itemTooltip(ItemStack stack);
    }
}