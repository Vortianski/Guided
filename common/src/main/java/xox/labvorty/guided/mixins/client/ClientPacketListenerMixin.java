package xox.labvorty.guided.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xox.labvorty.guided.data.client.BookScreen;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleAwardStats", at = @At("TAIL"))
    private void guided$onStatisticsReceived(
            ClientboundAwardStatsPacket packet,
            CallbackInfo ci
    ) {
        if (Minecraft.getInstance().screen instanceof BookScreen screen) {
            screen.onStatisticsUpdated();
        }
    }
}