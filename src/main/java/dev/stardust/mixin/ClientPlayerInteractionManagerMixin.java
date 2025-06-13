package dev.stardust.mixin;

import dev.stardust.modules.RapidFire;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    // See RapidFire.java
    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    private void preventCrossbowUseReset(CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        RapidFire rf = mods.get(RapidFire.class);
        if (!rf.isActive() || !rf.charging) return;
        ci.cancel();
    }
}
