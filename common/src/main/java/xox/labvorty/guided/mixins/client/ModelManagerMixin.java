package xox.labvorty.guided.mixins.client;

import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xox.labvorty.guided.data.registry.reloaders.BookRegistryReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public class ModelManagerMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private void guided$loadBooks(
            PreparableReloadListener.PreparationBarrier barrier,
            ResourceManager resourceManager,
            ProfilerFiller preparationProfiler,
            ProfilerFiller reloadProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor,
            CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        new BookRegistryReloadListener().loadImmediately(resourceManager);
    }
}
