package xox.labvorty.guided.data.page;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import xox.labvorty.guided.data.book.Content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PageTypes {
    private static final Map<ResourceLocation, Renderer> types = new HashMap<>();
    private static final Map<ResourceLocation, PageFactory> factories = new HashMap<>();

    static {
        for (String type : List.of("itemstack", "entity", "multiblock", "gallery", "image")) {
            String pageType = "guided:" + type;

            register(ResourceLocation.parse(pageType), (graphics, font, definition, x, y, width, height) -> {
                PreviewPages previewPages = new PreviewPages(new PreviewPages.View());
                previewPages.render(pageType, graphics, font, definition, x, y, width, height);
            });
        }

        for (String type : List.of("shaped_crafting", "shapeless_crafting", "smelting", "smoking", "blasting", "recipe_list")) {
            String pageType = "guided:" + type;

            register(ResourceLocation.parse(pageType), (graphics, font, definition, x, y, width, height) -> {
                RecipePages recipePages = new RecipePages();
                recipePages.render(pageType, graphics, font, definition, x, y, width, height);
            });
        }

        ExtraPages.register();

        register(
                ResourceLocation.parse("guided:text"),
                (graphics, font, definition, x, y, width, height) ->
                        renderText(graphics, font, definition, x, y, width, height, null)
        );

        register(
                ResourceLocation.parse("guided:list"),
                (graphics, font, definition, x, y, width, height) ->
                        renderList(graphics, font, definition, x, y, width, height, null)
        );

        register(ResourceLocation.parse("guided:text"), (PageFactory) TextPage::new);
        register(ResourceLocation.parse("guided:list"), (PageFactory) ListPage::new);
        register(ResourceLocation.parse("guided:image"), (PageFactory) ImagePage::new);
        register(ResourceLocation.parse("guided:gallery"), (PageFactory) GalleryPage::new);
        register(ResourceLocation.parse("guided:multiblock"), (PageFactory) MultiblockPage::new);
        register(ResourceLocation.parse("guided:entity"), (PageFactory) EntityPage::new);
        register(ResourceLocation.parse("guided:itemstack"), (PageFactory) ItemstackPage::new);
        register(ResourceLocation.parse("guided:advancement"), (PageFactory) AdvancementPage::new);
        register(ResourceLocation.parse("guided:link"), (PageFactory) LinkPage::new);
        register(ResourceLocation.parse("guided:empty"), (PageFactory) EmptyPage::new);
        register(ResourceLocation.parse("guided:shaped_crafting"), (PageFactory) ShapedCraftingPage::new);
        register(ResourceLocation.parse("guided:shapeless_crafting"), (PageFactory) ShapelessCraftingPage::new);
        register(ResourceLocation.parse("guided:smelting"), (PageFactory) SmeltingPage::new);
        register(ResourceLocation.parse("guided:smoking"), (PageFactory) SmokingPage::new);
        register(ResourceLocation.parse("guided:blasting"), (PageFactory) BlastingPage::new);
        register(ResourceLocation.parse("guided:recipe_list"), (PageFactory) RecipeListPage::new);
    }

    private PageTypes() {
    }

    public static void register(ResourceLocation id, PageFactory factory) {
        factories.put(id, factory);
    }

    public static void register(ResourceLocation id, Renderer renderer) {
        types.put(id, renderer);

        factories.put(id, definition -> context -> renderer.render(
                context.graphics(),
                context.font(),
                definition,
                context.x(),
                context.y(),
                context.width(),
                context.height()
        ));
    }

    public static void renderText(
            GuiGraphics graphics,
            Font font,
            JsonObject definition,
            int x,
            int y,
            int width,
            int height,
            LinkSink linkSink
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        String text = Component.translatable(
                Content.string(definition, "text", "")
        ).getString();

        Component component = FormattingRegistry.parse(text);

        int lineHeight = font.lineHeight;
        float scale = 1.0F;

        List<FormattedCharSequence> rows = font.split(component, width);

        if (rows.size() * lineHeight > height) {
            float low = 0.0001F;
            float high = 1.0F;

            for (int i = 0; i < 20; i++) {
                float candidate = (low + high) / 2;

                List<FormattedCharSequence> candidateRows = font.split(
                        component,
                        Math.max(1, (int) (width / candidate))
                );

                if (candidateRows.size() * lineHeight * candidate <= height) {
                    low = candidate;
                } else {
                    high = candidate;
                }
            }

            scale = low;

            rows = font.split(
                    component,
                    Math.max(1, (int) (width / scale))
            );
        }

        graphics.enableScissor(x, y, x + width, y + height);
        graphics.pose().pushPose();

        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1);

            int rowY = 0;

            for (FormattedCharSequence row : rows) {
                graphics.drawString(font, row, 0, rowY, 0xff382719, false);

                if (linkSink != null) {
                    final int currentRowY = rowY;
                    final float currentScale = scale;
                    final float[] offset = {0};

                    row.accept((index, style, codePoint) -> {
                        float advance = font.width(
                                new String(Character.toChars(codePoint))
                        );

                        if (style.getClickEvent() != null) {
                            linkSink.add(
                                    style,
                                    x + (int) (offset[0] * currentScale),
                                    y + (int) (currentRowY * currentScale),
                                    Math.max(1, (int) Math.ceil(advance * currentScale)),
                                    Math.max(1, (int) Math.ceil(lineHeight * currentScale))
                            );
                        }

                        offset[0] += advance;

                        return true;
                    });
                }

                rowY += lineHeight;
            }
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    public static void renderList(
            GuiGraphics graphics,
            Font font,
            JsonObject definition,
            int x,
            int y,
            int width,
            int height,
            LinkSink linkSink
    ) {
        JsonArray items = definition.getAsJsonArray("items");

        if (items == null) {
            return;
        }

        boolean ordered = definition.has("ordered")
                && definition.get("ordered").getAsBoolean();

        StringBuilder text = new StringBuilder();

        int index = 0;

        for (JsonElement item : items) {
            if (index > 0) {
                text.append("\n");
            }

            if (ordered) {
                text.append(index + 1).append(". ");
            } else {
                text.append("• ");
            }

            text.append(Component.translatable(item.getAsString()).getString());

            index++;
        }

        JsonObject copy = new JsonObject();
        copy.addProperty("text", text.toString());

        renderText(
                graphics,
                font,
                copy,
                x,
                y,
                width,
                height,
                linkSink
        );
    }

    public static BookPage create(ResourceLocation id, JsonObject definition) {
        PageFactory factory = factories.get(id);

        if (factory == null) {
            return context -> context.graphics().drawString(
                    context.font(),
                    "Unknown page: " + id,
                    context.x(),
                    context.y(),
                    0xffaa0000,
                    false
            );
        }

        return factory.create(definition);
    }

    public static Renderer get(ResourceLocation id) {
        return types.get(id);
    }

    @FunctionalInterface
    public interface LinkSink {
        void add(Style style, int x, int y, int width, int height);
    }

    @FunctionalInterface
    public interface Renderer {
        void render(
                GuiGraphics graphics,
                Font font,
                JsonObject definition,
                int x,
                int y,
                int width,
                int height
        );
    }

    @FunctionalInterface
    public interface PageFactory {
        BookPage create(JsonObject definition);
    }
}