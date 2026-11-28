package xox.labvorty.guided.data.page;

import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;

public final class ExtraPages {
    public static void register() {
        PageTypes.register(
                ResourceLocation.parse("guided:link"),
                (graphics, font, definition, x, y, width, height) -> {
                }
        );

        PageTypes.register(
                ResourceLocation.parse("guided:empty"),
                (graphics, font, definition, x, y, width, height) -> {
                    if (!definition.has("image")) {
                        return;
                    }

                    ResourceLocation image = ResourceLocation.parse(
                            definition.get("image").getAsString()
                    );

                    int textureWidth = definition.has("texture_width")
                            ? definition.get("texture_width").getAsInt()
                            : 256;

                    int textureHeight = definition.has("texture_height")
                            ? definition.get("texture_height").getAsInt()
                            : 256;

                    graphics.blit(
                            image,
                            x,
                            y,
                            width,
                            height,
                            0,
                            0,
                            textureWidth,
                            textureHeight,
                            textureWidth,
                            textureHeight
                    );
                }
        );

        PageTypes.register(
                ResourceLocation.parse("guided:advancement"),
                (graphics, font, definition, x, y, width, height) -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();

                    if (connection == null) {
                        return;
                    }

                    ResourceLocation advancementId = ResourceLocation.parse(
                            definition.get("advancement").getAsString()
                    );

                    AdvancementHolder holder = connection.getAdvancements().get(advancementId);

                    if (holder == null) {
                        return;
                    }

                    holder.value().display().ifPresent(display -> {
                        graphics.renderItem(
                                display.getIcon(),
                                x + width / 2 - 8,
                                y + 4
                        );

                        JsonObject text = new JsonObject();

                        text.addProperty(
                                "text",
                                display.getTitle().getString()
                                        + "\n\n"
                                        + display.getDescription().getString()
                        );

                        PageTypes.get(ResourceLocation.parse("guided:text")).render(
                                graphics,
                                font,
                                text,
                                x,
                                y + 24,
                                width,
                                height - 24
                        );
                    });
                }
        );
    }
}