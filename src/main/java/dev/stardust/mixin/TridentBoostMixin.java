package dev.stardust.mixin;

import dev.stardust.modules.RocketMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.TridentBoost;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(value = TridentBoost.class, remap = false)
public class TridentBoostMixin {

    // See RocketMan.java
    @Inject(method = "getMultiplier", at = @At("RETURN"), cancellable = true)
    private void configureMultiplier(CallbackInfoReturnable<Double> cir) {
        RocketMan rm = Modules.get().get(RocketMan.class);
        if (!rm.isActive() || !rm.tridentBoost.get()) return;
        cir.setReturnValue(1.069);
    }

    @Inject(method = "allowOutOfWater", at = @At("RETURN"), cancellable = true)
    private void doAllowOutOfWater(CallbackInfoReturnable<Boolean> cir) {
        RocketMan rm = Modules.get().get(RocketMan.class);
        if (!rm.isActive() || !rm.tridentBoost.get()) return;
        cir.setReturnValue(true);
    }
}
