package dev.stardust.mixin;

import dev.stardust.modules.AutoSmith;
import dev.stardust.modules.StashBrander;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess, AutoCloseable {

    // World#playSound(Entity, BlockPos, SoundEvent, SoundCategory, float, float)
    @Inject(
        method = "playSound(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void stardust$playSoundAtBlockCenter(Entity source, BlockPos pos, SoundEvent sound,
                                                 SoundCategory category, float volume, float pitch,
                                                 CallbackInfo ci) {
        if (stardust$shouldMute(sound)) ci.cancel();
    }

    // World#playSound(Entity, double, double, double, SoundEvent, SoundCategory, float, float)
    @Inject(
        method = "playSound(Lnet/minecraft/entity/Entity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void stardust$playSound(Entity source, double x, double y, double z, SoundEvent sound,
                                    SoundCategory category, float volume, float pitch,
                                    CallbackInfo ci) {
        if (stardust$shouldMute(sound)) ci.cancel();
    }

    // World#playSoundFromEntity(Entity, Entity, SoundEvent, SoundCategory, float, float)
    @Inject(
        method = "playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void stardust$playSoundFromEntity(Entity source, Entity entity, SoundEvent sound,
                                              SoundCategory category, float volume, float pitch,
                                              CallbackInfo ci) {
        if (stardust$shouldMute(sound)) ci.cancel();
    }

    private static boolean stardust$shouldMute(SoundEvent sound) {
        Modules modules = Modules.get();
        if (modules == null) return false;

        AutoSmith smith = modules.get(AutoSmith.class);
        StashBrander brander = modules.get(StashBrander.class);

        if (brander != null && brander.isActive() && brander.shouldMute()) {
            if (sound == SoundEvents.BLOCK_ANVIL_USE || sound == SoundEvents.BLOCK_ANVIL_BREAK) return true;
        }
        if (smith != null && smith.isActive() && smith.muteSmithy.get()) {
            if (sound == SoundEvents.BLOCK_SMITHING_TABLE_USE) return true;
        }
        return false;
    }
}
