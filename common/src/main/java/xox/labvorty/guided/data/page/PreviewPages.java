package xox.labvorty.guided.data.page;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.TreeSet;

public final class PreviewPages {
    private final View active;

    public PreviewPages(View view) {
        this.active = view;
    }

    public static List<Integer> slices(JsonObject definition) {
        TreeSet<Integer> layers = new TreeSet<>();
        if (definition.has("slices") && definition.get("slices").isJsonArray()) {
            for (JsonElement layer : definition.getAsJsonArray("slices")) {
                layers.add(layer.getAsInt());
            }
        } else if (flag(definition, "slices", false) && definition.has("blocks")) {
            for (JsonElement block : definition.getAsJsonArray("blocks")) {
                layers.add((int) number(block.getAsJsonObject(), "y", 0));
            }
        }
        return List.copyOf(layers);
    }

    private static boolean visibleBlock(View view, JsonObject definition, JsonObject block) {
        if (view == null || view.slice == 0) {
            return true;
        }
        List<Integer> layers = slices(definition);
        return view.slice > layers.size() || number(block, "y", 0) == layers.get(view.slice - 1);
    }

    public float zoom() {
        return active == null ? 1 : active.zoom;
    }
    // Invert the exact model transform and intersect a screen-space ray with each unit cube.

    public static JsonObject hoveredBlock(View view, JsonObject definition, int mouseX, int mouseY) {
        if (view.blockTransform == null) {
            return null;
        }
        Matrix4f inverse = new Matrix4f(view.blockTransform).invert();
        Vector3f origin = inverse.transformPosition(new Vector3f(mouseX, mouseY, 10000));
        Vector3f direction = inverse.transformDirection(new Vector3f(0, 0, -1));
        float nearest = Float.POSITIVE_INFINITY;
        JsonObject hit = null;
        for (JsonElement value : definition.getAsJsonArray("blocks")) {
            JsonObject block = value.getAsJsonObject();
            if (!visibleBlock(view, definition, block)) {
                continue;
            }
            float near = 0, far = Float.POSITIVE_INFINITY;
            float[] mins = {
                number(block, "x", 0) - 0.5f, number(block, "y", 0) - 0.5f, number(block, "z", 0) - 0.5f
            };
            for (int axis = 0; axis < 3; axis++) {
                float o = origin.get(axis), d = direction.get(axis), min = mins[axis], max = min + 1;
                if (Math.abs(d) < 1e-8f) {
                    if (o < min || o > max) {
                        far = -1;
                        break;
                    }
                    continue;
                }
                float a = (min - o) / d;
                float b = (max - o) / d;
                near = Math.max(near, Math.min(a, b));
                far = Math.min(far, Math.max(a, b));
                if (near > far) {
                    break;
                }
            }
            if (near <= far && near < nearest) {
                nearest = near;
                hit = block;
            }
        }
        return hit;
    }

    public void render(String pageType, GuiGraphics graphics, Font font, JsonObject definition, int x, int y, int width, int height) {
        if (pageType.equals("guided:itemstack")) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            ItemStack stack = ItemStack.CODEC.parse(minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), definition.get("stack")).getOrThrow();
            float base = Math.min(width, height) * (flag(definition, "huge", false) ? 0.62f : 0.40f);
            preview(graphics, definition, x, y, width, height, base, rotates(definition), () -> {
                minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, graphics.pose(), graphics.bufferSource(), minecraft.level, 0);
            });
            return;
        }
        if (pageType.equals("guided:entity")) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(definition.get("entity").getAsString()));
            Entity entity = type.create(minecraft.level);
            if (entity == null) {
                return;
            }
            float base = Math.min(width, height) * 0.65f / Math.max(1f, Math.max(entity.getBbHeight(), entity.getBbWidth()));
            preview(graphics, definition, x, y, width, height, base, rotates(definition), () -> {
                graphics.pose().translate(0, -entity.getBbHeight() / 2, 0);
                minecraft.getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, 0, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT);
            });
            return;
        }
        if (pageType.equals("guided:multiblock")) {
            JsonArray blocks = definition.getAsJsonArray("blocks");
            if (blocks == null || blocks.isEmpty() || blocks.size() > 512) {
                return;
            }
            float extent = 1;
            for (JsonElement value : blocks) {
                JsonObject blockDefinition = value.getAsJsonObject();
                extent = Math.max(extent, 2 * Math.max(Math.abs(number(blockDefinition, "x", 0)), Math.max(Math.abs(number(blockDefinition, "y", 0)), Math.abs(number(blockDefinition, "z", 0)))) + 2);
            }
            preview(graphics, definition, x, y, width, height, Math.min(width, height) * 0.8f / extent, rotates(definition), () -> {
                for (JsonElement value : blocks) {
                    JsonObject blockDefinition = value.getAsJsonObject();
                    if (!visibleBlock(active, definition, blockDefinition)) {
                        continue;
                    }
                    Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockDefinition.get("block").getAsString()));
                    BlockState state = block.defaultBlockState();
                    graphics.pose().pushPose();
                    try {
                        graphics.pose().translate(number(blockDefinition, "x", 0) - 0.5, number(blockDefinition, "y", 0) - 0.5, number(blockDefinition, "z", 0) - 0.5);
                        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    } finally {
                        graphics.pose().popPose();
                    }
                }
            });
            return;
        }
        if (pageType.equals("guided:gallery")) {
            JsonArray images = definition.getAsJsonArray("images");
            if (images == null || images.isEmpty()) {
                return;
            }
            JsonObject image = galleryImage(definition, images.get(0));
            drawImage(graphics, image, image.get("image").getAsString(), x, y, width, height);
        }
        if (pageType.equals("guided:image")) {
            drawImage(graphics, definition, definition.get("image").getAsString(), x, y, width, height);
        }
    }

    public static JsonObject galleryImage(JsonObject defaults, JsonElement value) {
        JsonObject result = defaults.deepCopy();
        if (value.isJsonObject()) {
            value.getAsJsonObject().entrySet().forEach(e -> result.add(e.getKey(), e.getValue().deepCopy()));
        }
        else {
            result.addProperty("image", value.getAsString());
        }
        return result;
    }

    public static boolean rotates(JsonObject definition) {
        return flag(definition, "can_rotate", flag(definition, "can_spin", true));
    }

    public static double imageAspectRatio(JsonObject definition) {
        double ratio = 1;
        try {
            String[] parts = definition.has("aspect_ratio") ? definition.get("aspect_ratio").getAsString().split(":") : new String[] {
                "1", "1"
            };
            ratio = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        } catch (RuntimeException ignored) {
        }
        if (!Double.isFinite(ratio) || ratio <= 0) {
            ratio = 1;
        }
        return ratio;
    }

    public static int[] fitImage(JsonObject definition, int width, int height) {
        double ratio = imageAspectRatio(definition);
        int imageWidth = Math.max(1, Math.min(width, (int) (height * ratio)));
        int imageHeight = Math.max(1, Math.min(height, (int) (imageWidth / ratio)));
        return new int[] {
            imageWidth, imageHeight
        };
    }

    public void drawImage(GuiGraphics graphics, JsonObject definition, String image, int x, int y, int width, int height) {
        double ratio = imageAspectRatio(definition);
        int[] fitted = fitImage(definition, width, height);
        int imageWidth = fitted[0], imageHeight = fitted[1];
        int textureWidth = Math.max(1, (int) number(definition, "texture_width", imageWidth));
        int textureHeight = Math.max(1, (int) number(definition, "texture_height", imageHeight));
        float u = number(definition, "start_x", 0), textureV = number(definition, "start_y", 0);
        int sourceWidth = Math.max(1, (int) (number(definition, "end_x", textureWidth) - u));
        int sourceHeight = Math.max(1, (int) (number(definition, "end_y", textureHeight) - textureV));
        if (flag(definition, "crop_to_fit", false)) {
            if (sourceWidth / (double) sourceHeight > ratio) {
                int n = Math.max(1, (int) (sourceHeight * ratio));
                u += (sourceWidth - n) / 2f;
                sourceWidth = n;
            } else {
                int n = Math.max(1, (int) (sourceWidth / ratio));
                textureV += (sourceHeight - n) / 2f;
                sourceHeight = n;
            }
        }
        imageWidth = Math.max(1, (int) (imageWidth * zoom()));
        imageHeight = Math.max(1, (int) (imageHeight * zoom()));
        graphics.blit(ResourceLocation.parse(image), x + (width - imageWidth) / 2 + (active == null ? 0 : Math.round(active.panX)), y + (height - imageHeight) / 2 + (active == null ? 0 : Math.round(active.panY)), imageWidth, imageHeight, u, textureV, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    private void preview(GuiGraphics graphics, JsonObject definition, int x, int y, int width, int height, float base, boolean spin, Runnable draw) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float scale = base * Math.max(0.01f, Math.min(10f, number(definition, "size", 1)));
        float angle = active == null ? 0 : active.angle;
        scale *= zoom();
        JsonObject offset = definition.has("translation") ? definition.getAsJsonObject("translation") : new JsonObject();
        graphics.enableScissor(x, y, x + width, y + height);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x + width / 2f + number(offset, "x", 0) + (active == null ? 0 : active.panX), y + height / 2f + number(offset, "y", 0) + (active == null ? 0 : active.panY), 100 + number(offset, "z", 0));
            pose.scale(scale, -scale, scale);
            pose.mulPose(Axis.XP.rotationDegrees(active == null ? 0 : active.pitch));
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            if (active != null) {
                active.blockTransform = new Matrix4f(pose.last().pose());
            }
            draw.run();
            graphics.flush();
        } finally {
            pose.popPose();
            graphics.disableScissor();
        }
    }

    private static float number(JsonObject definition, String key, float fallback) {
        float value = definition.has(key) ? definition.get(key).getAsFloat() : fallback;
        return Float.isFinite(value) ? value : fallback;
    }

    private static boolean flag(JsonObject definition, String key, boolean fallback) {
        return definition.has(key) ? definition.get(key).getAsBoolean() : fallback;
    }

    public static final class View {
        public float angle;
        public float pitch;
        public float zoom = 1;
        public int slice = 0;
        public float panX;
        public float panY;
        private float minPitch = -180;
        private float maxPitch = 180;
        private float minZoom = 0.25f;
        private float maxZoom = 5;
        private float automaticAngle;
        private long last;
        private long interaction;
        public Matrix4f blockTransform;

        public void pan(double deltaX, double deltaY) {
            panX += (float) deltaX;
            panY += (float) deltaY;
            touch();
        }

        public void configure(JsonObject definition) {
            minPitch = number(definition, "min_pitch", -180);
            maxPitch = Math.max(minPitch, number(definition, "max_pitch", 180));
            minZoom = Math.max(0.01f, number(definition, "min_scale",.25f));
            maxZoom = Math.max(minZoom, number(definition, "max_scale", 5));
            pitch = clamp(pitch, minPitch, maxPitch);
            zoom = clamp(zoom, minZoom, maxZoom);
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        public void touch() {
            interaction = System.currentTimeMillis();
        }

        public void drag(double deltaX, double deltaY) {
            angle += (float) deltaX * 2;
            pitch = clamp(pitch + (float) deltaY * 2, minPitch, maxPitch);
            touch();
        }

        public void scroll(double amount) {
            zoom = clamp(zoom * (float) Math.pow(1.15, amount), minZoom, maxZoom);
            touch();
        }

        public void tick(boolean rotate, float speed) {
            long now = System.currentTimeMillis();
            float deltaTime = last == 0 ? 0 : Math.min(100, Math.max(0, now - last)) / 1000f;
            last = now;
            if (now - interaction < 3000) {
                return;
            }

            if (rotate) {
                automaticAngle = (automaticAngle + deltaTime * speed) % 360;
            }

            float blend = Math.min(1, deltaTime * 6);
            angle += Mth.wrapDegrees(automaticAngle - angle) * blend;
            pitch += (clamp(0, minPitch, maxPitch) - pitch) * blend;
            zoom += (clamp(1, minZoom, maxZoom) - zoom) * blend;
            panX *= 1 - blend;
            panY *= 1 - blend;
        }
    }
}
