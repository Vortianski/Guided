package xox.labvorty.guided.data.book;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record BookDefinition(
        String name,
        ResourceLocation style,
        @Nullable ResourceLocation creativeTab,
        boolean navigation,
        ResourceLocation itemModel
) {}