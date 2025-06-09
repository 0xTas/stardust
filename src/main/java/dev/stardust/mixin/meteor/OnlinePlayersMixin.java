package dev.stardust.mixin.meteor;

import dev.stardust.Stardust;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OnlinePlayers.class, remap = false)
public class OnlinePlayersMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private static void removeMeteorTelemetry(CallbackInfo ci) {
        if (Stardust.disableMeteorClientTelemetry.get()) ci.cancel();
    }

    @Inject(method = "leave", at = @At("HEAD"), cancellable = true)
    private static void removeMeteorTelemetryOnLeave(CallbackInfo ci) {
        if (Stardust.disableMeteorClientTelemetry.get()) ci.cancel();
    }
}
