package dev.stardust.mixin.accessor;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameProfile.class)
public interface GameProfileAccessor {
    @Mutable
    @Accessor(value = "name", remap = false)
    void setName(String name);
}
