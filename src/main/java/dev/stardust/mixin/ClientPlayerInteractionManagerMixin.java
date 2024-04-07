package dev.stardust.mixin;

import dev.stardust.modules.RocketMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayerInteractionManager;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    // See RocketMan.java
    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    private void preventTridentUseResetOnScreenChange(CallbackInfo ci) {
        RocketMan rm = Modules.get().get(RocketMan.class);
        if (!rm.isActive() || !rm.tridentBoost.get() || !rm.chargingTrident) return;
        ci.cancel();
    }
}
