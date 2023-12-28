package dev.stardust.mixin;

import net.minecraft.sound.SoundEvent;
import dev.stardust.modules.MusicTweaks;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.util.math.random.Random;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(PositionedSoundInstance.class)
public class PositionedSoundInstanceMixin extends AbstractSoundInstance {
    protected PositionedSoundInstanceMixin(SoundEvent sound, SoundCategory category, Random random) {
        super(sound, category, random);
    }

    // See MusicTweaks.java
    @Inject(method = "music", at = @At("HEAD"), cancellable = true)
    private static void mixinMusic(SoundEvent sound, CallbackInfoReturnable<PositionedSoundInstance> cir) {
        MusicTweaks tweaks = Modules.get().get(MusicTweaks.class);
        if (tweaks == null || !tweaks.isActive()) return;

        float adjustedPitch;
        if (tweaks.randomPitch()) {
            adjustedPitch = 1.0f + tweaks.getRandomPitch();
        } else {
            adjustedPitch = 1.0f + tweaks.getPitchAdjustment();
        }

        if (adjustedPitch == 1.0f) return;

        cir.setReturnValue(
            new PositionedSoundInstance(
                sound.getId(),
                SoundCategory.MUSIC,
                1f, adjustedPitch,
                SoundInstance.createRandom(),
                false, 0,
                AttenuationType.NONE,
                0f, 0f, 0f, true
            )
        );
    }
}
