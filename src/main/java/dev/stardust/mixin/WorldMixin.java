package dev.stardust.mixin;

import net.minecraft.world.World;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.WorldAccess;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sound.SoundCategory;
import dev.stardust.modules.StashBrander;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess, AutoCloseable {
    // See StashBrander.java
    @Inject(method = "playSoundAtBlockCenter", at = @At("HEAD"), cancellable = true)
    private void mixinPlaySoundAtBlockCenter(BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
        StashBrander brander = Modules.get().get(StashBrander.class);
        if (!brander.isActive() || !brander.shouldMute()) return;
        if (sound == SoundEvents.BLOCK_ANVIL_USE || sound == SoundEvents.BLOCK_ANVIL_BREAK) ci.cancel();
    }
}
