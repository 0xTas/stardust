package dev.stardust.mixin.meteor;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.injection.Inject;
import dev.stardust.mixininterface.IItemEntityMixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.ItemPhysics;
import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Fixes a rare NPE with Item Physics, and adds item model tumbling to the random rotation feature.
 *     See also: ItemEntityMixin.java
 **/
@Mixin(value = ItemPhysics.class, remap = false)
public abstract class ItemPhysicsMixin {
    @Inject(method = "offsetInWater", at = @At("HEAD"), cancellable = true)
    private void fixCrashNPE(MatrixStack matrices, ItemEntity entity, CallbackInfo ci) {
        if (entity == null)
            ci.cancel();
    }

    @Inject(
        method = "onRenderItemEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V",
            ordinal = 1,
            shift = At.Shift.AFTER
        ),
        // remap must be true to properly target an invocation of a remapped class/method like MatrixStack#multiply (I think)
        remap = true
    )
    private void addItemTumble(RenderItemEntityEvent event, CallbackInfo ci) {
        ItemEntity entity = event.itemEntity;

        if (entity == null) return;
        if (!(entity instanceof IItemEntityMixin itemTumble)) return;

        float tickDelta = event.tickDelta;
        event.matrixStack.multiply(itemTumble.stardust$getRenderQuaternion(tickDelta));
    }
}
