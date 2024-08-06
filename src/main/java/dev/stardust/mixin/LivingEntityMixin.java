package dev.stardust.mixin;

import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import dev.stardust.modules.RocketMan;
import net.minecraft.entity.Attackable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity
    implements Attackable {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Unique
    @Nullable private RocketMan rm;

    // See RocketMan.java
    @Inject(method = "travel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D"))
    private void spoofPitchForSpeedCalcs(CallbackInfo ci, @Local(ordinal = 0) LocalFloatRef f, @Local(ordinal = 1)LocalRef<Vec3d> rotationVec) {
        if (this.rm == null) {
            Modules modules = Modules.get();
            if (modules == null) return;
            rm = modules.get(RocketMan.class);
        }

        if (!rm.isActive() || !rm.shouldLockYLevel()) return;
        if (!this.getUuid().equals(rm.getClientInstance().player.getUuid())) return;
        if (!rm.getClientInstance().player.isFallFlying() || !rm.hasActiveRocket) return;

        if (rm.getClientInstance().player.input.jumping && rm.verticalSpeed.get() > 0) {
            f.set(-45);
            rotationVec.set(this.getRotationVector(45, this.getYaw()));
        } else if (rm.getClientInstance().player.input.sneaking && rm.verticalSpeed.get() > 0) {
            f.set(45);
            rotationVec.set(this.getRotationVector(45, this.getYaw()));
        } else {
            f.set(0);
            rotationVec.set(this.getRotationVector(0, this.getYaw()));
        }
    }
}
