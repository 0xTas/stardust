package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.entity.projectile.FireworkRocketEntity;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {
    @Invoker("explodeAndRemove")
    void invokeExplodeAndRemove();
}
