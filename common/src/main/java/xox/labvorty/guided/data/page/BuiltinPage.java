package xox.labvorty.guided.data.page;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import xox.labvorty.guided.data.book.Content;
import xox.labvorty.guided.data.style.StyleDefinition;

abstract class BuiltinPage implements BookPage {
    private final String type;
    private final JsonObject definition;
    private final PageTypes.Renderer renderer;
    private final Minecraft minecraft = Minecraft.getInstance();
    private PageRenderContext context;
    private Font font;
    private final PreviewPages.View view = new PreviewPages.View();
    private final PreviewPages previewRenderer = new PreviewPages(view);
    private final RecipePages recipeRenderer = new RecipePages();
    private int selectedImage;
    private final List<Action> actions = new ArrayList<>();
    private final List<GalleryButton> galleryButtons = new ArrayList<>();
    private PreviewArea previewArea;
    private PreviewArea draggingPreview;
    private int dragButton = -1;
    private List<Component> customTooltip;
    private PageTypes.LinkSink linkSink;

    BuiltinPage(String type, JsonObject definition, PageTypes.Renderer renderer) {
        this.type = type;
        this.definition = definition;
        this.renderer = renderer;
    }

    @Override
    public void render(PageRenderContext renderContext) {
        context = renderContext;
        font = renderContext.font();
        actions.clear();
        galleryButtons.clear();
        previewArea = null;
        customTooltip = null;
        linkSink = (textStyle,linkX,linkY,linkWidth,linkHeight)->{
            ClickEvent click = textStyle.getClickEvent();
            if (click != null && click.getValue().startsWith("guided-entry:")) actions.add(new Action(linkX, linkY, linkWidth, linkHeight, ()->context.actions().openEntry(click.getValue().substring(13))));
        };
        try {
            draw(renderContext.graphics(), renderContext.style(), renderContext.x(), renderContext.y(), renderContext.width(), renderContext.height(), renderContext.mouseX(), renderContext.mouseY());
            if (customTooltip != null) renderContext.actions().tooltip(customTooltip);
        } finally {
            linkSink = null;
            recipeRenderer.hovered = null;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            for (Action action : actions) if (x >= action.x() && x < action.x() + action.w() && y >= action.y() && y < action.y() + action.h()) {
                action.run().run();
                return true;
            }
            for (GalleryButton galleryButton : galleryButtons) if (x >= galleryButton.x() && x < galleryButton.x() + 20 && y >= galleryButton.y() && y < galleryButton.y() + 16) {
                if (galleryButton.enabled()) selectedImage = galleryButton.next();
                return true;
            }
        }
        if (previewArea != null && previewArea.contains(x, y) && (button == 0 || button == 1 && previewArea.canDrag())) {
            draggingPreview = previewArea;
            dragButton = button;
            view.touch();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        if (draggingPreview == null || button != dragButton) return false;
        if (button == 1 && draggingPreview.canDrag()) view.pan(deltaX, deltaY); else if (button == 0 && Set.of("guided:itemstack", "guided:entity", "guided:multiblock").contains(type)) view.drag(deltaX, deltaY);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button != dragButton) return false;
        draggingPreview = null;
        dragButton = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (previewArea == null || !previewArea.contains(x, y) || !previewArea.canScale()) return false;
        view.scroll(vertical);
        return true;
    }

    @Override
    public void onClose() {
        draggingPreview = null;
        dragButton = -1;
        actions.clear();
        galleryButtons.clear();
        previewArea = null;
    }

    private String texture(String key, String fallback) {
        return context.actions().texture(key, fallback);
    }

    private String translated(String key) {
        return Component.translatable(key).getString();
    }

    private void recipeTooltip(ItemStack stack) {
        if (Screen.hasShiftDown()) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (lines.stream().noneMatch((line)->line.getString().equals(id))) lines.add(Component.literal(id).withStyle(ChatFormatting.DARK_GRAY));
            context.actions().tooltip(lines);
        } else context.actions().itemTooltip(stack);
    }

    private void draw(GuiGraphics graphics, StyleDefinition style, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (type.equals("guided:link")) {
            JsonArray buttons = definition.getAsJsonArray("buttons");
            int count = buttons == null ? 0 : Math.min(3, buttons.size());
            int buttonTop = y + height - count * 24;
            if (definition.has("text") && buttonTop > y) PageTypes.renderText(graphics, font, definition, x, y, width, Math.max(1, buttonTop - y - 4), linkSink);
            if (buttons != null) for (int n = 0; n < count; n++) {
                JsonObject buttonDefinition = buttons.get(n).getAsJsonObject();
                int buttonY = buttonTop + n * 24 + 4;
                if (buttonY + 20 > y + height) break;
                style.renderPageSprite(graphics, ResourceLocation.parse(Content.string(buttonDefinition, "texture", "guided:widgets/link_button")), x, buttonY, width, 20);
                renderLabel(graphics, translated(Content.string(buttonDefinition, "text", "Link")), x + 5, buttonY + 6, width - 10);
                String url = Content.string(buttonDefinition, "url", "");
                actions.add(new Action(x, buttonY, width, 20, ()->{
                    if (url.startsWith("https://") || url.startsWith("http://")) context.actions().openLink(url);
                }));
            }
            return;
        }
        if (type.equals("guided:advancement")) {
            ClientPacketListener connection = minecraft.getConnection();
            if (connection == null) return;
            AdvancementHolder holder = connection.getAdvancements().get(ResourceLocation.parse(definition.get("advancement").getAsString()));
            if (holder == null) return;
            holder.value().display().ifPresent((display)->{
                int advancementX = x + width / 2 - 12;
                int advancementY = y + 2;
                style.renderPageSprite(graphics, ResourceLocation.parse(texture("advancement_frame", "guided:page/advancement_frame")), advancementX, advancementY, 24, 24);
                graphics.renderItem(display.getIcon(), advancementX + 4, advancementY + 4);
                if (mouseX >= advancementX && mouseX < advancementX + 24 && mouseY >= advancementY && mouseY < advancementY + 24) customTooltip = List.of(display.getTitle(), display.getDescription());
            });
            if (definition.has("text")) PageTypes.renderText(graphics, font, definition, x, y + 30, width, height - 30, linkSink);
            return;
        }
        if (type.equals("guided:empty")) {
            renderer.render(graphics, font, definition, x, y, width, height);
            return;
        }
        if (type.equals("guided:text") || type.equals("guided:list")) {
            if (type.equals("guided:text")) PageTypes.renderText(graphics, font, definition, x, y, width, height, linkSink); else PageTypes.renderList(graphics, font, definition, x, y, width, height, linkSink);
            return;
        }
        boolean recipe = RecipePages.isRecipe(type);
        boolean gallery = type.equals("guided:gallery");
        JsonObject imageDefinition = definition;
        if (gallery && definition.has("images") && !definition.getAsJsonArray("images").isEmpty()) {
            JsonArray images = definition.getAsJsonArray("images");
            int selected = Math.max(0, Math.min(images.size() - 1, selectedImage));
            imageDefinition = PreviewPages.galleryImage(definition, images.get(selected));
        }
        boolean item = type.equals("guided:itemstack");
        boolean huge = item && definition.has("huge") && definition.get("huge").getAsBoolean();
        int captionHeight = Math.min(40, Math.max(12, height / 4));
        List<Integer> layers = type.equals("guided:multiblock") ? PreviewPages.slices(definition) : List.<Integer>of();
        int controls = gallery ? 20 : 0;
        int nameHeight = huge ? 12 : 0;
        int frameHeight = height - captionHeight - controls - nameHeight - 4;
        if (frameHeight < 10 || width < 10) return;
        boolean imagePage = gallery || type.equals("guided:image");
        boolean showFrame = !recipe && (!imagePage || !imageDefinition.has("show_frame") || imageDefinition.get("show_frame").getAsBoolean());
        int previewX = x + 4;
        int previewY = y + 4;
        int previewWidth = width - 8;
        int previewHeight = frameHeight - 8;
        if (imagePage) {
            int padding = showFrame ? 4 : 0;
            int[] fitted = PreviewPages.fitImage(imageDefinition, width - 2 * padding, frameHeight - 2 * padding);
            previewWidth = fitted[0];
            previewHeight = fitted[1];
            previewX = x + (width - previewWidth) / 2;
            previewY = y + (frameHeight - previewHeight) / 2;
            if (showFrame) style.renderPageSprite(graphics, ResourceLocation.parse(texture("preview_frame", "guided:page/preview_frame")), previewX - padding, previewY - padding, previewWidth + 2 * padding, previewHeight + 2 * padding);
        } else if (showFrame) {
            style.renderPageSprite(graphics, ResourceLocation.parse(texture("preview_frame", "guided:page/preview_frame")), x, y, width, frameHeight);
        }
        PreviewPages.View view = this.view;
        view.configure(definition);
        if (view.slice > layers.size()) view.slice = 0;
        boolean canScale = definition.has("can_scale") ? definition.get("can_scale").getAsBoolean() : type.equals("guided:multiblock") || type.equals("guided:entity");
        PreviewArea area = new PreviewArea(previewX, previewY, previewWidth, previewHeight, type, !recipe && canScale, !recipe && (definition.has("can_drag") ? definition.get("can_drag").getAsBoolean() : type.equals("guided:multiblock")), view);
        previewArea = area;
        boolean model = item || type.equals("guided:entity") || type.equals("guided:multiblock");
        view.blockTransform = null;
        if (draggingPreview != null && draggingPreview.view() == view) view.touch();
        view.tick(model && PreviewPages.rotates(definition), definition.has("rotation_speed") ? definition.get("rotation_speed").getAsFloat() : 30);
        graphics.enableScissor(previewX, previewY, previewX + previewWidth, previewY + previewHeight);
        try {
            if (gallery) {
                JsonArray images = definition.getAsJsonArray("images");
                if (images != null && !images.isEmpty()) {
                    String key = "gallery";
                    int selected = Math.max(0, Math.min(images.size() - 1, selectedImage));
                    previewRenderer.drawImage(graphics, imageDefinition, imageDefinition.get("image").getAsString(), previewX, previewY, previewWidth, previewHeight);
                    int buttonY = y + frameHeight + 1;
                    galleryButtons.add(new GalleryButton(x, buttonY, key, selected - 1, selected > 0));
                    galleryButtons.add(new GalleryButton(x + width - 20, buttonY, key, selected + 1, selected + 1 < images.size()));
                }
            } else {
                if (recipe) {
                    recipeRenderer.setStyle(style);
                    recipeRenderer.textureResolver = (key,fallback)->ResourceLocation.parse(texture(key, fallback));
                    recipeRenderer.mouseX = mouseX;
                    recipeRenderer.mouseY = mouseY;
                    recipeRenderer.hovered = null;
                }
                if (recipe) {
                    recipeRenderer.render(type, graphics, font, definition, previewX, previewY, previewWidth, previewHeight);
                } else {
                    previewRenderer.render(type, graphics, font, definition, previewX, previewY, previewWidth, previewHeight);
                }
                if (recipe && recipeRenderer.hovered != null) recipeTooltip(recipeRenderer.hovered);
            }
        } finally {
            graphics.flush();
            graphics.disableScissor();
        }
        if (gallery) {
            for (GalleryButton button : galleryButtons) {
                if (button.y() == y + frameHeight + 1 && (button.x() == x || button.x() == x + width - 20)) {
                    style.renderButton(graphics, button.x() == x ? "left" : "right", button.enabled(), button.x(), button.y(), mouseX, mouseY);
                }
            }
        }
        boolean sliceHovered = false;
        if (!layers.isEmpty()) {
            int buttonX = x + width - 10;
            int buttonY = y + frameHeight - 10;
            sliceHovered = mouseX >= buttonX && mouseX < buttonX + 8 && mouseY >= buttonY && mouseY < buttonY + 8;
            String key = sliceHovered ? "slice_button_hovered" : "slice_button";
            style.renderPageSprite(graphics, ResourceLocation.parse(texture(key, "guided:widgets/" + key)), buttonX, buttonY, 8, 8);
            String label = view.slice == 0 ? Component.translatable("guided.slice.all").getString() : Component.translatable("guided.slice.layer", layers.get(view.slice - 1)).getString();
            if (sliceHovered) customTooltip = List.of(Component.literal(label));
            actions.add(new Action(buttonX, buttonY, 8, 8, ()->view.slice = (view.slice + 1) % (layers.size() + 1)));
        }
        if (item && minecraft.level != null) {
            ItemStack stack = ItemStack.CODEC.parse(minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), definition.get("stack")).getOrThrow();
            if (huge) renderLabel(graphics, stack.getHoverName().getString(), x, y + frameHeight + 2, width);
            if (definition.has("show_tooltip") && definition.get("show_tooltip").getAsBoolean() && mouseX >= previewX && mouseX < previewX + previewWidth && mouseY >= previewY && mouseY < previewY + previewHeight) {
                context.actions().itemTooltip(stack);
            }
        }
        if (!sliceHovered && area.contains(mouseX, mouseY) && type.equals("guided:multiblock") && view.blockTransform != null) {
            JsonObject block = PreviewPages.hoveredBlock(view, definition, mouseX, mouseY);
            if (block != null) {
                if (block.has("tooltip")) customTooltip = tooltipLines(block.get("tooltip")); else customTooltip = List.of(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block.get("block").getAsString())).getName());
            }
        }
        if (area.contains(mouseX, mouseY) && definition.has("tooltip") && !type.equals("guided:multiblock")) {
            customTooltip = tooltipLines(definition.get("tooltip"));
            if (item && !Content.string(definition, "tooltip_mode", "append").equals("replace") && minecraft.level != null) {
                ItemStack stack = ItemStack.CODEC.parse(minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), definition.get("stack")).getOrThrow();
                List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
                lines.addAll(customTooltip);
                customTooltip = lines;
            }
        }
        if (definition.has("text")) {
            int captionY = y + frameHeight + controls + nameHeight + 4;
            PageTypes.renderText(graphics, font, definition, x, captionY, width, height - (captionY - y), linkSink);
        }
    }

    private List<Component> tooltipLines(JsonElement value) {
        List<Component> lines = new ArrayList<>();
        if (value.isJsonArray()) for (JsonElement line : value.getAsJsonArray()) lines.addAll(tooltipLines(line)); else if (value.isJsonPrimitive()) lines.add(FormattingRegistry.parse(Component.translatable(value.getAsString()).getString()));
        return lines;
    }

    private void renderLabel(GuiGraphics graphics, String label, int x, int y, int maxWidth) {
        if (maxWidth <= 0) return;
        float scale = Math.min(1.0F, maxWidth / (float)Math.max(1, font.width(label)));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + (font.lineHeight * (1 - scale)) / 2, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, 0, 0, 0xff382719, false);
        graphics.pose().popPose();
    }

    private record Action(int x, int y, int w, int h, Runnable run) {
    }

    private record GalleryButton(int x, int y, String key, int next, boolean enabled) {
    }

    private record PreviewArea(int x, int y, int w, int h, String type, boolean canScale, boolean canDrag, PreviewPages.View view) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
}
