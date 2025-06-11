package dev.stardust.mixin.meteor;

import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OnlinePlayers.class, remap = false)
public class OnlinePlayersMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private static void removeMeteorTelemetry(CallbackInfo ci) {
        if (StardustConfig.disableMeteorClientTelemetry.get()) ci.cancel();
    }

    @Inject(method = "leave", at = @At("HEAD"), cancellable = true)
    private static void removeMeteorTelemetryOnLeave(CallbackInfo ci) {
        if (StardustConfig.disableMeteorClientTelemetry.get()) ci.cancel();
    }
}
