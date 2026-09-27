package dev.stardust.mixininterface;

import org.joml.Quaternionf;

public interface IItemEntityMixin {
    Quaternionf stardust$getRenderQuaternion(float tickDelta);
}
