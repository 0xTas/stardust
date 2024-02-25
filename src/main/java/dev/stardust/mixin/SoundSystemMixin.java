package dev.stardust.mixin;

import java.util.Map;
import net.minecraft.text.Text;
import javax.annotation.Nullable;
import net.minecraft.client.sound.*;
import org.spongepowered.asm.mixin.*;
import dev.stardust.modules.MusicTweaks;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(SoundSystem.class)
public class SoundSystemMixin {
    @Shadow
    @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Unique
    @Mutable
    private int totalTicksPlaying;
    @Unique
    private boolean dirtyPitch = false;
    @Unique
    private boolean dirtyVolume = false;


    // See MusicTweaks.java
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void mixinTick(CallbackInfo ci) {
        MusicTweaks tweaks = Modules.get().get(MusicTweaks.class);
        if (tweaks == null) return;

        boolean playing = false;
        @Nullable String songID = null;
        for (SoundInstance instance : sources.keySet()) {
            Sound sound = instance.getSound();
            if (sound == null) continue;

            String location = sound.getLocation().toString();
            if (!location.startsWith("minecraft:sounds/music/") && !sound.toString().contains("minecraft:records/")) continue;
            Channel.SourceManager sourceManager = this.sources.get(instance);
            songID = location.substring(location.lastIndexOf('/') + 1);

            if (sourceManager == null) continue;
            Source source = ((SourceManagerAccessor) sourceManager).getSource();
            if (source == null) continue;

            playing = true;
            tweaks.setCurrentSong(sound.toString());
            if (tweaks.isActive() && !tweaks.randomPitch()) {
                this.dirtyPitch = true;
                source.setPitch(1.0f + tweaks.getPitchAdjustment());
            } else if (tweaks.isActive() && tweaks.randomPitch() && tweaks.trippyPitch()) {
                this.dirtyPitch = true;
                source.setPitch(tweaks.getNextPitchStep(instance.getPitch())); // !!
            } else if (!tweaks.isActive() && this.dirtyPitch) {
                source.setPitch(1f);
                this.dirtyPitch = false;
            }
            if (tweaks.isActive()) {
                this.dirtyVolume = true;
                source.setVolume(MathHelper.clamp(tweaks.getClient().options.getSoundVolume(instance.getCategory()) + tweaks.getVolumeAdjustment(), 0.0f, 4.0f));
            } else if (this.dirtyVolume) {
                this.dirtyVolume = false;
                source.setVolume(tweaks.getClient().options.getSoundVolume(instance.getCategory()));
            }
        }
        if (playing) {
            ++this.totalTicksPlaying;
        } else {
            this.totalTicksPlaying = 0;
        }

        if (tweaks.isActive() && this.totalTicksPlaying % 30 == 0 && tweaks.shouldDisplayNowPlaying() && songID != null) {
            if (this.totalTicksPlaying <= 90 || !tweaks.shouldFadeOut()) {
                String songName = tweaks.getSongName(songID);

                // See NarratorManagerMixin.java lol
                switch (tweaks.getDisplayMode()) {
                    case Chat -> tweaks.sendNowPlayingMessage(songName);
                    case Record -> tweaks.getClient().inGameHud.setRecordPlayingOverlay(Text.of(songName));
                }
            }
        }
    }
}
