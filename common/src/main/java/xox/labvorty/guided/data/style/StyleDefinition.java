package xox.labvorty.guided.data.style;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xox.labvorty.guided.data.book.Content;
import xox.labvorty.guided.data.page.BookPage;
import xox.labvorty.guided.data.page.PageRenderContext;

public interface StyleDefinition {
    void renderBackground(GuiGraphics graphics);

    void renderConnection(
            GuiGraphics graphics,
            Point start,
            Point end
    );

    void renderPoint(GuiGraphics graphics);

    default void renderConnection(
            GuiGraphics graphics,
            Point start,
            Point end,
            boolean reversed,
            boolean arrow
    ) {
        renderConnection(graphics, start, end);
    }

    default void renderPanel(
            GuiGraphics graphics,
            boolean browser,
            int x,
            int y,
            int width,
            int height
    ) {
        if (browser) {
            graphics.blitSprite(
                    ResourceLocation.parse("guided:core/frame"),
                    x, y, width, height
            );
        } else {
            graphics.blit(
                    ResourceLocation.parse("guided:textures/gui/sprites/core/book.png"),
                    x, y, width, height,
                    0, 0, 272, 180, 272, 180
            );
        }
    }

    default void renderButton(
            GuiGraphics graphics,
            String direction,
            boolean enabled,
            int x,
            int y
    ) {
        renderVanillaPageButton(
                graphics,
                direction,
                enabled,
                x,
                y,
                false
        );
    }

    default void renderButton(
            GuiGraphics graphics,
            String direction,
            boolean enabled,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        boolean hovered = mouseX >= x
                && mouseX < x + 20
                && mouseY >= y
                && mouseY < y + 16;

        if (!renderVanillaPageButton(
                graphics, direction, enabled, x, y, hovered
        )) {
            renderButton(graphics, direction, enabled, x, y);
        }
    }

    static boolean renderVanillaPageButton(
            GuiGraphics graphics,
            String direction,
            boolean enabled,
            int x,
            int y,
            boolean hovered
    ) {
        if (direction.equals("back")) {
            if (enabled) {
                String sprite = hovered
                        ? "guided:widgets/back_highlighted"
                        : "guided:widgets/back";

                graphics.blitSprite(
                        ResourceLocation.parse(sprite),
                        x + 2, y, 16, 16
                );
            }

            return true;
        }

        int textureX;

        switch (direction) {
            case "left" -> textureX = 0;
            case "right" -> textureX = 23;
            default -> {
                return false;
            }
        }

        if (enabled) {
            String sprite = "guided:textures/gui/sprites/widgets/navigation";

            if (hovered) {
                sprite += "_highlighted";
            }

            sprite += ".png";

            graphics.blit(
                    ResourceLocation.parse(sprite),
                    x - 1,
                    y + 2,
                    textureX,
                    0,
                    23,
                    13,
                    46,
                    13
            );
        }

        return true;
    }

    default void renderUnread(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.blitSprite(
                ResourceLocation.parse("guided:core/unread"),
                x, y, 4, 4
        );
    }

    default void renderTab(
            GuiGraphics graphics,
            int x,
            int y,
            boolean selected
    ) {
        String sprite = selected
                ? "guided:widgets/tab_selected"
                : "guided:widgets/tab";

        graphics.blitSprite(
                ResourceLocation.parse(sprite),
                x, y, 22, 30
        );
    }

    default void renderFrameOverlay(
            GuiGraphics graphics,
            String side,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.blitSprite(
                ResourceLocation.parse("guided:core/" + side + "_overlay"),
                x, y, width, height
        );
    }

    default void renderBookmark(
            GuiGraphics graphics,
            int x,
            int y,
            boolean selected,
            boolean complete
    ) {
        String sprite = "guided:widgets/bookmark";

        if (complete) {
            sprite += "_complete";
        }

        if (selected) {
            sprite += "_selected";
        }

        graphics.blitSprite(
                ResourceLocation.parse(sprite),
                x, y, 22, 30
        );
    }

    default void renderEntryBackground(
            GuiGraphics graphics,
            JsonObject definition
    ) {
        String background = Content.string(
                definition,
                "background",
                "default"
        );

        if (definition.has("background_texture")) {
            background = definition.get("background_texture").getAsString();
        }

        if (background.equals("default")) {
            renderPoint(graphics);
            return;
        }

        background = switch (background) {
            case "goal" -> resolveTexture(
                    "goal_square",
                    "guided:core/goal_square"
            );

            case "challenge" -> resolveTexture(
                    "challenge_square",
                    "guided:core/challenge_square"
            );

            case "square", "circle", "pentagon", "octagon",
                 "challenge_square", "challenge_circle",
                 "challenge_pentagon", "challenge_octagon",
                 "goal_square", "goal_circle",
                 "goal_pentagon", "goal_octagon" -> resolveTexture(
                    background,
                    "guided:core/" + background
            );

            default -> background;
        };

        ResourceLocation texture = ResourceLocation.parse(background);

        if (texture.getPath().endsWith(".png")) {
            int textureWidth = 24;
            int textureHeight = 24;

            if (definition.has("background_texture_width")) {
                textureWidth = definition.get("background_texture_width").getAsInt();
            }

            if (definition.has("background_texture_height")) {
                textureHeight = definition.get("background_texture_height").getAsInt();
            }

            textureWidth = Math.max(1, textureWidth);
            textureHeight = Math.max(1, textureHeight);

            graphics.blit(
                    texture,
                    -12, -12,
                    24, 24,
                    0, 0,
                    textureWidth,
                    textureHeight,
                    textureWidth,
                    textureHeight
            );
        } else {
            graphics.blitSprite(texture, -12, -12, 24, 24);
        }
    }

    default void renderEntryIcon(
            GuiGraphics graphics,
            JsonObject definition,
            int x,
            int y
    ) {
        if (definition.has("icon_texture")) {
            ResourceLocation texture = ResourceLocation.parse(
                    definition.get("icon_texture").getAsString()
            );

            graphics.blit(
                    texture,
                    x, y, 0, 0, 16, 16, 16, 16
            );
        } else {
            ResourceLocation itemId = ResourceLocation.tryParse(
                    Content.string(definition, "icon", "minecraft:book")
            );

            if (itemId != null) {
                ItemStack stack = new ItemStack(
                        BuiltInRegistries.ITEM.get(itemId)
                );

                graphics.renderItem(stack, x, y);
            }
        }
    }

    default void renderParallax(
            GuiGraphics graphics,
            ResourceLocation texture,
            int left,
            int top,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            double panX,
            double panY
    ) {
        int offsetX = Math.floorMod(
                (int) (panX * 0.25),
                textureWidth
        );

        int offsetY = Math.floorMod(
                (int) (panY * 0.25),
                textureHeight
        );

        for (
                int x = left - textureWidth + offsetX;
                x < left + width;
                x += textureWidth
        ) {
            for (
                    int y = top - textureHeight + offsetY;
                    y < top + height;
                    y += textureHeight
            ) {
                graphics.blit(
                        texture,
                        x, y, 0, 0,
                        textureWidth,
                        textureHeight,
                        textureWidth,
                        textureHeight
                );
            }
        }
    }

    default void renderPage(
            BookPage page,
            PageRenderContext context
    ) {
        page.render(context);
    }

    default void renderPageSprite(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.blitSprite(texture, x, y, width, height);
    }

    default String resolveTexture(String key, String fallback) {
        return fallback;
    }

    default int getBrowserCullingMargin() {
        return 32;
    }

    record Point(int x, int y) {}
}