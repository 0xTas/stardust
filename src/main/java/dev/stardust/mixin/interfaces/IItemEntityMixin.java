package dev.stardust.mixin.interfaces;

import org.joml.Quaternionf;

public interface IItemEntityMixin {
    Quaternionf stardust$getRenderQuaternion(float tickDelta);
}
