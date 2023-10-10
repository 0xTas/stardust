package dev.stardust.mixin;

import net.minecraft.item.Item;
import dev.stardust.modules.Honker;
import net.minecraft.item.GoatHornItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(GoatHornItem.class)
public class GoatHornItemMixin extends Item {
    // See Honker.java
    public GoatHornItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private static void mixinPlaySound(CallbackInfo ci) {
        Honker honker = Modules.get().get(Honker.class);
        if (honker.shouldMuteHorns()) ci.cancel();
    }
}
