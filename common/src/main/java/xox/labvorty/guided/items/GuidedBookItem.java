package xox.labvorty.guided.items;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xox.labvorty.guided.data.client.BookScreen;
import xox.labvorty.guided.init.GuidedDataComponents;

public class GuidedBookItem extends Item {
    public GuidedBookItem() {
        super(
                new Properties()
                        .component(
                                GuidedDataComponents.BOOK_ID.get(),
                                ResourceLocation.parse("guided:default")
                        )
                        .stacksTo(1)
        );
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        if (level.isClientSide) {
            ResourceLocation resourceLocation = player.getItemInHand(interactionHand).get(GuidedDataComponents.BOOK_ID.get());
            if (resourceLocation!=null) {
                Minecraft.getInstance().setScreen(new BookScreen(resourceLocation));
            }
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(interactionHand), level.isClientSide);
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        ResourceLocation bookId = stack.get(GuidedDataComponents.BOOK_ID.get());

        if (bookId == null) {
            return super.getName(stack);
        }

        return Component.translatable("guided." + bookId.getNamespace() + "." + bookId.getPath());
    }
}