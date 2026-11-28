package xox.labvorty.guided.fabric.init;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import xox.labvorty.guided.data.model.BookModel;
import xox.labvorty.guided.data.registry.BookRegistry;
import xox.labvorty.guided.init.GuidedKeyMappings;

public class GuidedClient {
    public static void register() {
        registerModels();
        replaceModels();
        registerKeys();
    }

    private static void registerModels() {
        ModelLoadingPlugin.register(context -> {
            BookRegistry.getRegistry().values().forEach(bookDefinition -> {
                context.addModels(bookDefinition.itemModel());
            });
        });
    }

    private static void replaceModels() {
        ModelLoadingPlugin.register(context -> {
            context.modifyModelAfterBake().register((original, modelContext) -> {
                if (original != null && ModelResourceLocation.inventory(
                        ResourceLocation.parse("guided:guide_book")
                ).equals(modelContext.topLevelId())) {

                    return new BookModel(original, model ->
                            ((FabricBakedModelManager) Minecraft.getInstance()
                                    .getModelManager()).getModel(model)
                    );
                }

                return original;
            });
        });
    }

    private static void registerKeys() {
        KeyBindingHelper.registerKeyBinding(GuidedKeyMappings.TOGGLE_READ);
        KeyBindingHelper.registerKeyBinding(GuidedKeyMappings.RELOAD);
    }
}
