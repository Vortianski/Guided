package xox.labvorty.guided.neoforge.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.data.model.BookModel;
import xox.labvorty.guided.data.registry.BookRegistry;
import xox.labvorty.guided.init.GuidedKeyMappings;

@EventBusSubscriber(value = Dist.CLIENT)
public class GuidedClient {
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GuidedKeyMappings.TOGGLE_READ);
        event.register(GuidedKeyMappings.RELOAD);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        GuidedMod.LOGGER.info(
                "Registering models for {} Guided books!",
                BookRegistry.getRegistry().size()
        );

        BookRegistry.getRegistry().values().forEach(bookDefinition -> {
            event.register(
                    ModelResourceLocation.standalone(bookDefinition.itemModel())
            );
        });
    }

    @SubscribeEvent
    public static void replaceModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().computeIfPresent(
                ModelResourceLocation.inventory(ResourceLocation.parse("guided:guide_book")),
                (key, original) ->
                        new BookModel(original, model ->
                                Minecraft.getInstance().getModelManager().getModel(
                                        ModelResourceLocation.standalone(model)
                                )
                        )
        );
    }
}
