package dev.stardust.mixin;

import dev.stardust.modules.MusicTweaks;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(MusicTracker.class)
public class MusicTrackerMixin {
    // See MusicTweaks.java
    @Inject(method = "tick", at = @At("TAIL"))
    private void mixinTick(CallbackInfo ci) {
        MusicTweaks tweaks = Modules.get().get(MusicTweaks.class);

        if (tweaks == null || !tweaks.isActive()) return;
        boolean currentlyPlaying = ((MusicTrackerAccessor) this).getCurrent() != null;

        if (!currentlyPlaying) { tweaks.nullifyCurrentType(); }
        if (tweaks.overrideDelay() && currentlyPlaying) {
            ((MusicTrackerAccessor) this).setTimeUntilNextSong(tweaks.getTimeUntilNextSong());
        }
    }
}
