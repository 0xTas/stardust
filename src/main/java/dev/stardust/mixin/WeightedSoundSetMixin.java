package dev.stardust.mixin;

import java.util.List;
import dev.stardust.modules.MusicTweaks;
import net.minecraft.client.sound.Sound;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.netty.util.internal.ThreadLocalRandom;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.sound.SoundContainer;
import net.minecraft.client.sound.WeightedSoundSet;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(WeightedSoundSet.class)
public abstract class WeightedSoundSetMixin implements SoundContainer<Sound> {
    @Shadow
    @Final
    private List<SoundContainer<Sound>> sounds;

    // See MusicTweaks.java
    @Inject(method = "getSound(Lnet/minecraft/util/math/random/Random;)Lnet/minecraft/client/sound/Sound;", at = @At("HEAD"), cancellable = true)
    private void mixinGetSound(net.minecraft.util.math.random.Random random, CallbackInfoReturnable<Sound> cir) {
        Modules modules = Modules.get();
        if (modules == null) return;
        MusicTweaks tweaks = modules.get(MusicTweaks.class);
        if (tweaks == null || !tweaks.isActive()) return;

        boolean overwrite = false;
        for (SoundContainer<Sound> sound : this.sounds) {
            String id = sound.getSound(random).toString();

            if (id.contains("minecraft:music/")) {
                overwrite = true;
                break;
            }
        }

        if (!overwrite) return;
        List<String> soundIDs = tweaks.getSoundSet();
        if (soundIDs.isEmpty()) return;

        float adjustedPitch;
        if (tweaks.randomPitch()) {
            adjustedPitch = 1.0f + tweaks.getRandomPitch();
        } else {
            adjustedPitch = 1.0f + tweaks.getPitchAdjustment();
        }
        float adjustedVolume = tweaks.getClient().options.getSoundVolume(SoundCategory.MUSIC) + tweaks.getVolumeAdjustment();

        cir.setReturnValue(
            new Sound(
                soundIDs.get(ThreadLocalRandom.current().nextInt(soundIDs.size())),
                ConstantFloatProvider.create(adjustedVolume),
                ConstantFloatProvider.create(adjustedPitch),
                this.getWeight(), Sound.RegistrationType.SOUND_EVENT,
                true, true, 16
            )
        );
    }
}
