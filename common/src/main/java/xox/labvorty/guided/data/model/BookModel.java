package xox.labvorty.guided.data.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import xox.labvorty.guided.data.book.BookDefinition;
import xox.labvorty.guided.data.registry.BookRegistry;
import xox.labvorty.guided.init.GuidedDataComponents;

import java.util.List;
import java.util.function.Function;

public class BookModel implements BakedModel {
    private final BakedModel original;
    private final ItemOverrides overrides;

    public BookModel(BakedModel original, Function<ResourceLocation, BakedModel> modelGetter) {
        this.original = original;
        this.overrides = new ItemOverrides(new DummyModelBaker(), null, List.of()) {
            @Override
            public BakedModel resolve(
                    BakedModel model,
                    ItemStack stack,
                    ClientLevel level,
                    LivingEntity entity,
                    int seed
            ) {
                ResourceLocation bookId = stack.get(GuidedDataComponents.BOOK_ID.get());
                if (bookId == null) return original;

                BookDefinition book = BookRegistry.get(bookId);
                if (book == null) return original;

                BakedModel customModel = modelGetter.apply(book.itemModel());

                if (customModel == null || customModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
                    return original;
                }

                return customModel;
            }
        };
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
        return original.getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return original.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return original.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return original.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return original.getTransforms();
    }

    private static class DummyModelBaker implements ModelBaker {
        @Override
        public UnbakedModel getModel(ResourceLocation location) {
            return null;
        }

        @Override
        public BakedModel bake(ResourceLocation location, ModelState state) {
            return null;
        }

        public UnbakedModel getTopLevelModel(ModelResourceLocation location) {
            return null;
        }

        public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
            return null;
        }

        public BakedModel bake(ResourceLocation location, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
            return null;
        }

        public BakedModel bakeUncached(UnbakedModel model, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
            return null;
        }
    }
}