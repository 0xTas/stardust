package dev.stardust.mixin;

import dev.stardust.modules.RocketMan;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    // See RocketMan.java
    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", at = @At("HEAD"), cancellable = true)
    private void mixinPlaySound(SoundEvent sound, SoundCategory category, float volume, float pitch, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rocketMan = modules.get(RocketMan.class);
        if (rocketMan.isActive() && sound == SoundEvents.ITEM_ELYTRA_FLYING) {
            if (rocketMan.shouldMuteElytra()) ci.cancel();
        }
    }
}
