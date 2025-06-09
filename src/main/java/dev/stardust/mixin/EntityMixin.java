package dev.stardust.mixin;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.Vec3d;
import dev.stardust.modules.RocketMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.server.command.CommandOutput;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;

@Mixin(Entity.class)
public abstract class EntityMixin
    implements Nameable, EntityLike, CommandOutput {

    @Shadow
    public abstract UUID getUuid();

    // See RocketMan.java
    @ModifyVariable(method = "setVelocity(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), argsOnly = true)
    private Vec3d spoofYMovement(Vec3d velocity) {
        Modules modules = Modules.get();
        if (modules == null) return velocity;
        RocketMan rm = modules.get(RocketMan.class);
        if (!rm.isActive() || !rm.shouldLockYLevel()) return velocity;
        if (!this.getUuid().equals(rm.getClientInstance().player.getUuid())) return velocity;
        if (!rm.getClientInstance().player.isGliding() || !rm.hasActiveRocket()) return velocity;

        Vec3d spoofVec;
        if (rm.getClientInstance().player.input.playerInput.jump()) {
            spoofVec = new Vec3d(velocity.x, rm.verticalSpeed.get(), velocity.z);
        } else if (rm.getClientInstance().player.input.playerInput.sneak()) {
            spoofVec = new Vec3d(velocity.x, -rm.verticalSpeed.get(), velocity.z);
        } else spoofVec = new Vec3d(velocity.x, 0, velocity.z);

        return spoofVec;
    }
}
