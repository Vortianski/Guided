package xox.labvorty.guided.data.page;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xox.labvorty.guided.data.style.StyleDefinition;

import java.util.Set;
import java.util.function.BiFunction;

public final class RecipePages {
    private static final Set<String> RECIPE_TYPES = Set.of(
            "guided:shaped_crafting",
            "guided:shapeless_crafting",
            "guided:smelting",
            "guided:smoking",
            "guided:blasting",
            "guided:recipe_list"
    );

    public BiFunction<String, String, ResourceLocation> textureResolver =
            (key, fallback) -> ResourceLocation.parse(fallback);

    public int mouseX;
    public int mouseY;

    public ItemStack hovered;

    private StyleDefinition style;

    private float scale;
    private float originX;
    private float originY;

    private boolean tooltips;

    public void setStyle(StyleDefinition style) {
        this.style = style;
    }

    public void render(
            String pageType,
            GuiGraphics graphics,
            Font font,
            JsonObject definition,
            int x,
            int y,
            int width,
            int height
    ) {
        String type = pageType.substring("guided:".length());

        if (type.equals("recipe_list")) {
            renderRecipeList(graphics, font, definition, x, y, width, height);
            return;
        }

        boolean crafting = type.endsWith("crafting");

        scale = Math.min(width / 100.0F, height / 70.0F);

        originX = x + width / 2.0F - (crafting ? 50.0F : 58.5F) * scale;
        originY = y + height / 2.0F - 35.0F * scale;

        tooltips = !definition.has("show_tooltip")
                || definition.get("show_tooltip").getAsBoolean();

        graphics.pose().pushPose();

        try {
            graphics.pose().translate(originX, originY, 0);
            graphics.pose().scale(scale, scale, 1);

            ResourceLocation texture = textureResolver.apply(
                    type + "_texture",
                    "guided:page/recipes/" + type
            );

            if (style != null) {
                style.renderPageSprite(graphics, texture, 0, 0, 100, 70);
            } else {
                graphics.blitSprite(texture, 0, 0, 100, 70);
            }

            if (crafting) {
                if (type.equals("shaped_crafting")) {
                    renderShapedCrafting(graphics, definition);
                } else {
                    renderShapelessCrafting(graphics, definition);
                }
            } else {
                renderCooking(graphics, font, definition, type);
            }

            draw(graphics, definition.get("result"), 80, 24);

            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    private void renderRecipeList(
            GuiGraphics graphics,
            Font font,
            JsonObject definition,
            int x,
            int y,
            int width,
            int height
    ) {
        JsonArray recipes = definition.getAsJsonArray("recipes");

        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        String recipeType = definition.get("recipe_type").getAsString();

        if (!isRecipe(recipeType) || recipeType.equals("guided:recipe_list")) {
            return;
        }

        int interval = 60;

        if (definition.has("interval")) {
            interval = definition.get("interval").getAsInt();
        }

        interval = Math.max(1, interval);

        int selectedIndex = (int) (
                (Util.getMillis() / 50 / interval) % recipes.size()
        );

        JsonObject selectedRecipe = recipes.get(selectedIndex)
                .getAsJsonObject()
                .deepCopy();

        if (definition.has("show_tooltip")) {
            selectedRecipe.add(
                    "show_tooltip",
                    definition.get("show_tooltip")
            );
        }

        render(
                recipeType,
                graphics,
                font,
                selectedRecipe,
                x,
                y,
                width,
                height
        );
    }

    private void renderShapedCrafting(
            GuiGraphics graphics,
            JsonObject definition
    ) {
        JsonArray pattern = definition.getAsJsonArray("pattern");
        JsonObject key = definition.getAsJsonObject("key");

        for (int row = 0; row < Math.min(3, pattern.size()); row++) {
            String line = pattern.get(row).getAsString();

            for (int column = 0; column < Math.min(3, line.length()); column++) {
                String symbol = String.valueOf(line.charAt(column));

                if (key.has(symbol)) {
                    draw(
                            graphics,
                            key.get(symbol),
                            4 + column * 17,
                            8 + row * 17
                    );
                }
            }
        }
    }

    private void renderShapelessCrafting(
            GuiGraphics graphics,
            JsonObject definition
    ) {
        JsonArray ingredients = definition.getAsJsonArray("ingredients");

        for (int index = 0; index < Math.min(9, ingredients.size()); index++) {
            int column = index % 3;
            int row = index / 3;

            draw(
                    graphics,
                    ingredients.get(index),
                    4 + column * 17,
                    8 + row * 17
            );
        }
    }

    private void renderCooking(
            GuiGraphics graphics,
            Font font,
            JsonObject definition,
            String type
    ) {
        draw(graphics, definition.get("ingredient"), 21, 24);

        int cookingTime = 100;

        if (type.equals("smelting")) {
            cookingTime = 200;
        }

        if (definition.has("cooking_time")) {
            cookingTime = definition.get("cooking_time").getAsInt();
        }

        String time = cookingTime + "t";

        graphics.drawString(
                font,
                time,
                58 - font.width(time) / 2,
                56,
                0xff60482e,
                false
        );
    }

    private void draw(
            GuiGraphics graphics,
            JsonElement value,
            int x,
            int y
    ) {
        if (value == null || value.isJsonNull()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        ItemStack stack;

        if (value.isJsonPrimitive()) {
            ResourceLocation itemId = ResourceLocation.parse(value.getAsString());

            stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
        } else if (minecraft.level != null) {
            stack = ItemStack.CODEC.parse(
                    minecraft.level.registryAccess()
                            .createSerializationContext(JsonOps.INSTANCE),
                    value
            ).getOrThrow();
        } else {
            return;
        }

        if (tooltips
                && mouseX >= originX + x * scale
                && mouseX < originX + (x + 16) * scale
                && mouseY >= originY + y * scale
                && mouseY < originY + (y + 16) * scale) {
            hovered = stack;
        }

        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(minecraft.font, stack, x, y);
    }

    public static boolean isRecipe(String type) {
        return RECIPE_TYPES.contains(type);
    }
}