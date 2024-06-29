package dev.stardust.mixin;

import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import dev.stardust.modules.RocketMan;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.entity.FlyingItemEntity;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.Constant;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(value = FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements FlyingItemEntity {

    @Shadow
    private @Nullable LivingEntity shooter;

    // See RocketMan.java
    @Inject(method = "tick", at = @At("HEAD"))
    private void createTrackedRocketEntity(CallbackInfo ci) {
        if (this.shooter == null) return;
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rm = modules.get(RocketMan.class);
        if (!rm.getClientInstance().player.isFallFlying()) return;
        if (!this.shooter.getUuid().equals(rm.getClientInstance().player.getUuid())) return;
        if (!rm.isActive() || rm.currentRocket == (Object)this) return;

        ClientPlayerEntity player = rm.getClientInstance().player;
        if (rm.currentRocket != null) {
            if (rm.currentRocket.getId() != ((FireworkRocketEntity)(Object)this).getId()) {
                rm.discardCurrentRocket("overwrite current");

                rm.hasActiveRocket = true;
                rm.currentRocket = (FireworkRocketEntity)(Object)this;
                rm.extensionStartPos = new BlockPos(player.getBlockX(), 0, player.getBlockZ());
            }
        } else {
            rm.hasActiveRocket = true;
            rm.currentRocket = (FireworkRocketEntity)(Object)this;
            rm.extensionStartPos = new BlockPos(player.getBlockX(), 0, player.getBlockZ());
            if (rm.debug.get()) player.sendMessage(Text.literal("§7Created tracked rocket entity!"));
        }
    }

    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 1.5))
    private double boostFireworkRocketSpeed(double multiplier) {
        Modules modules = Modules.get();
        if (modules == null) return multiplier;

        RocketMan rm = modules.get(RocketMan.class);
        if (!rm.isActive() || !rm.boostSpeed.get()) return multiplier;

        return rm.getRocketBoostAcceleration();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;"))
    private void spoofRotationVector(CallbackInfo ci, @Local(ordinal = 0) LocalRef<Vec3d> rotationVec) {
        Modules modules = Modules.get();
        if (modules == null) return;

        RocketMan rm = modules.get(RocketMan.class);
        if (!rm.isActive() || !rm.shouldLockYLevel()) return;
        if (!rm.getClientInstance().player.isFallFlying() || !rm.hasActiveRocket) return;

        float g = -rm.getClientInstance().player.getYaw() * ((float)Math.PI / 180);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);

        rotationVec.set(new Vec3d(i, -1, h));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FireworkRocketEntity;explodeAndRemove()V", shift = At.Shift.BEFORE), cancellable = true)
    private void extendFireworkDuration(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rm = modules.get(RocketMan.class);
        if (rm.currentRocket == null) return;
        if (!rm.isActive() || !rm.extendRockets.get()) return;
        if (rm.currentRocket.getId() != ((FireworkRocketEntity)(Object)this).getId()) return;
        if (rm.debug.get()) rm.getClientInstance().player.sendMessage(Text.literal("§7Cancelling natural rocket expiration!"));
        ci.cancel();
    }
}
