package xox.labvorty.guided.data.style;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import xox.labvorty.guided.data.book.Content;

public class DefaultStyle implements StyleDefinition {
    final JsonObject json;

    public DefaultStyle(JsonObject json) {
        this.json = json.deepCopy();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
    }

    @Override
    public void renderPanel(
            GuiGraphics graphics,
            boolean browser,
            int x,
            int y,
            int width,
            int height
    ) {
        if (browser) {
            graphics.blitSprite(
                    texture("browser_frame", "guided:core/frame"),
                    x, y, width, height
            );
        } else {
            graphics.blit(
                    texture("book", "guided:textures/gui/sprites/core/book.png"),
                    x, y, width, height,
                    0, 0, 272, 180, 272, 180
            );
        }
    }

    @Override
    public void renderButton(
            GuiGraphics graphics,
            String direction,
            boolean enabled,
            int x,
            int y
    ) {
        renderButton(
                graphics,
                direction,
                enabled,
                x,
                y,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE
        );
    }

    @Override
    public void renderButton(
            GuiGraphics graphics,
            String direction,
            boolean enabled,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        String key = direction + (enabled ? "" : "_disabled");

        boolean hovered = mouseX >= x
                && mouseX < x + 20
                && mouseY >= y
                && mouseY < y + 16;

        if (hovered && json.has(key + "_highlighted")) {
            key += "_highlighted";
        }

        if (!json.has(key) && StyleDefinition.renderVanillaPageButton(
                graphics, direction, enabled, x, y, hovered
        )) {
            return;
        }

        if (json.has(key)) {
            graphics.blit(
                    texture(key, ""),
                    x, y, 0, 0, 20, 16, 20, 16
            );
        }
    }

    @Override
    public void renderUnread(GuiGraphics graphics, int x, int y) {
        graphics.blitSprite(
                texture("unread", "guided:core/unread"),
                x, y, 4, 4
        );
    }

    @Override
    public void renderTab(
            GuiGraphics graphics,
            int x,
            int y,
            boolean selected
    ) {
        String key = selected ? "tab_selected" : "tab";

        graphics.blitSprite(
                texture(key, "guided:widgets/" + key),
                x, y, 22, 30
        );
    }

    @Override
    public void renderFrameOverlay(
            GuiGraphics graphics,
            String side,
            int x,
            int y,
            int width,
            int height
    ) {
        String key = side + "_overlay";

        graphics.blitSprite(
                texture(key, "guided:core/" + key),
                x, y, width, height
        );
    }

    @Override
    public void renderBookmark(
            GuiGraphics graphics,
            int x,
            int y,
            boolean selected,
            boolean complete
    ) {
        String key = "bookmark";

        if (complete) {
            key += "_complete";
        }

        if (selected) {
            key += "_selected";
        }

        graphics.blitSprite(
                texture(key, "guided:widgets/" + key),
                x, y, 22, 30
        );
    }

    @Override
    public void renderPoint(GuiGraphics graphics) {
        graphics.blitSprite(
                ResourceLocation.parse(
                        resolveTexture("square", "guided:core/square")
                ),
                -12, -12, 24, 24
        );
    }

    @Override
    public void renderConnection(
            GuiGraphics graphics,
            Point start,
            Point end
    ) {
        renderConnection(graphics, start, end, false, true);
    }

    @Override
    public void renderConnection(
            GuiGraphics graphics,
            Point start,
            Point end,
            boolean reversed,
            boolean arrow
    ) {
        int deltaX = end.x() - start.x();
        int deltaY = end.y() - start.y();

        if (deltaX == 0 && deltaY == 0) {
            return;
        }

        boolean horizontal = deltaY == 0
                || (deltaX != 0 && reversed);

        int directionX = horizontal ? Integer.signum(deltaX) : 0;
        int directionY = horizontal ? 0 : Integer.signum(deltaY);

        int arrowOffset = 19;

        Point lineEnd = end;

        if (arrow) {
            lineEnd = new Point(
                    end.x() - directionX * arrowOffset,
                    end.y() - directionY * arrowOffset
            );
        }

        if (deltaX == 0 || deltaY == 0) {
            line(graphics, start, lineEnd);
        } else {
            Point corner;

            if (reversed) {
                corner = new Point(start.x(), end.y());
            } else {
                corner = new Point(end.x(), start.y());
            }

            int available = Math.min(
                    Math.abs(deltaX),
                    Math.abs(deltaY)
            );

            if (arrow) {
                int distance = horizontal
                        ? Math.abs(deltaX)
                        : Math.abs(deltaY);

                available = Math.min(
                        available,
                        distance - arrowOffset
                );
            }

            int radius = Math.min(
                    available,
                    available >= 48 ? 48 : 16
            );

            Point entrance = new Point(
                    corner.x() + Integer.signum(start.x() - corner.x()) * radius,
                    corner.y() + Integer.signum(start.y() - corner.y()) * radius
            );

            Point exit = new Point(
                    corner.x() + Integer.signum(end.x() - corner.x()) * radius,
                    corner.y() + Integer.signum(end.y() - corner.y()) * radius
            );

            line(graphics, start, entrance);
            line(graphics, exit, lineEnd);

            String side = Math.min(entrance.x(), exit.x()) < corner.x()
                    ? "left"
                    : "right";

            String vertical = Math.min(entrance.y(), exit.y()) < corner.y()
                    ? "up"
                    : "down";

            String size = radius >= 48 ? "large" : "small";
            String key = size + "_" + side + "_" + vertical;

            graphics.blitSprite(
                    texture(
                            "connection_" + key,
                            "guided:core/connections/" + key
                    ),
                    corner.x() - radius,
                    corner.y() - radius,
                    radius * 2,
                    radius * 2
            );
        }

        if (arrow) {
            String direction;

            if (horizontal) {
                direction = deltaX > 0 ? "right" : "left";
            } else {
                direction = deltaY > 0 ? "down" : "up";
            }

            int centerX = end.x() - directionX * 15;
            int centerY = end.y() - directionY * 15;

            graphics.blitSprite(
                    texture(
                            "connection_arrow_" + direction,
                            "guided:core/connections/arrow_" + direction
                    ),
                    centerX - 4,
                    centerY - 4,
                    8,
                    8
            );
        }
    }

    @Override
    public String resolveTexture(String key, String fallback) {
        return Content.string(json, key, fallback);
    }

    private void line(
            GuiGraphics graphics,
            Point start,
            Point end
    ) {
        boolean horizontal = start.y() == end.y();

        int first;
        int last;

        if (horizontal) {
            first = Math.min(start.x(), end.x());
            last = Math.max(start.x(), end.x());
        } else {
            first = Math.min(start.y(), end.y());
            last = Math.max(start.y(), end.y());
        }

        String key = horizontal ? "horizontal" : "vertical";

        ResourceLocation sprite = texture(
                "connection_" + key,
                "guided:core/connections/" + key
        );

        for (int position = first; position < last; position += 16) {
            int length = Math.min(16, last - position);

            if (horizontal) {
                graphics.blitSprite(
                        sprite,
                        position,
                        start.y() - 2,
                        length,
                        4
                );
            } else {
                graphics.blitSprite(
                        sprite,
                        start.x() - 2,
                        position,
                        4,
                        length
                );
            }
        }
    }

    ResourceLocation texture(String key, String fallback) {
        return ResourceLocation.parse(
                Content.string(json, key, fallback)
        );
    }
}