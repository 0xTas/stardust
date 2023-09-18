package dev.stardust.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import dev.stardust.modules.AutoDoors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block {
    public DoorBlockMixin(Settings settings) {
        super(settings);
    }

    // See AutoDoors.java
    @Inject(method = "playOpenCloseSound", at = @At("HEAD"), cancellable = true)
    private void mixinPlayOpenCloseSound(CallbackInfo ci) {
        AutoDoors autoDoors = Modules.get().get(AutoDoors.class);

        if (autoDoors.shouldMute()) ci.cancel();
    }
}
