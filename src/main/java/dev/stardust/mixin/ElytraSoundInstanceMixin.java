package dev.stardust.mixin;

import dev.stardust.modules.RocketMan;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.sound.ElytraSoundInstance;
import net.minecraft.client.sound.MovingSoundInstance;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ElytraSoundInstance.class)
public abstract class ElytraSoundInstanceMixin extends MovingSoundInstance {
    protected ElytraSoundInstanceMixin(SoundEvent soundEvent, SoundCategory soundCategory, Random random) {
        super(soundEvent, soundCategory, random);
    }

    // See RocketMan.java
    @Inject(method = "tick", at = @At("HEAD"))
    private void mixinTick(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rocketMan = modules.get(RocketMan.class);
        if (rocketMan.isActive() && rocketMan.shouldMuteElytra()) this.setDone();
    }
}
