package dev.stardust.mixin;

import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import dev.stardust.modules.RocketMan;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique private long toggleTimestamp = System.currentTimeMillis();

    // See RocketMan.java
    @SuppressWarnings("UnnecessaryContinue")
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void allowRocketHover(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rm = modules.get(RocketMan.class);
        if (!rm.isActive()) {
            while (rm.getClientInstance().options.backKey.wasPressed()) { continue; }
            return;
        }

        Vec3d hoverVec = Vec3d.ZERO;
        ClientPlayerEntity player = rm.getClientInstance().player;
        if (rm.hoverMode.get().equals(RocketMan.HoverMode.Toggle) || rm.hoverMode.get().equals(RocketMan.HoverMode.Creative)) {
            if (player.input.pressingLeft && player.input.pressingForward) {
                Vec3d rotVec = Vec3d.fromPolar(0, player.getYaw() - 45);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingRight && player.input.pressingForward) {
                Vec3d rotVec = Vec3d.fromPolar(0, player.getYaw() + 45);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingLeft && player.input.pressingBack) {
                Vec3d rotVec = Vec3d.fromPolar(-180, player.getYaw() + 45);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingRight && player.input.pressingBack) {
                Vec3d rotVec = Vec3d.fromPolar(-180, player.getYaw() - 45);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingLeft) {
                Vec3d rotVec = Vec3d.fromPolar(0, player.getYaw() - 90);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingRight) {
                Vec3d rotVec = Vec3d.fromPolar(0, player.getYaw() + 90);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingForward) {
                Vec3d rotVec = Vec3d.fromPolar(0, player.getYaw());
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingBack) {
                Vec3d rotVec = Vec3d.fromPolar(-180, player.getYaw());
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            }

            if (rm.getClientInstance().player.input.jumping && rm.getClientInstance().player.input.sneaking) {
                rm.getClientInstance().player.setSneaking(true);
            } else if (rm.getClientInstance().player.input.jumping) {
                rm.getClientInstance().player.setSneaking(false);
                hoverVec = hoverVec.add(0, rm.verticalSpeed.get(), 0);
            } else if (rm.getClientInstance().player.input.sneaking) {
                rm.getClientInstance().player.setSneaking(false);
                hoverVec = hoverVec.add(0, -rm.verticalSpeed.get(), 0);
            }
        } else if (rm.hoverMode.get().equals(RocketMan.HoverMode.Hold)) {
            if (player.input.pressingLeft) {
                Vec3d rotVec = Vec3d.fromPolar(0, player.getYaw() - 90);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            } else if (player.input.pressingRight) {
                Vec3d rotVec = Vec3d.fromPolar(0, player.getYaw() + 90);
                hoverVec = rotVec.multiply(rm.horizontalSpeed.get());
            }
            if (rm.getClientInstance().player.input.jumping) {
                hoverVec = hoverVec.add(0, rm.verticalSpeed.get(), 0);
            } else if (rm.getClientInstance().player.input.sneaking) {
                hoverVec = hoverVec.add(0, -rm.verticalSpeed.get(), 0);
            }
        } else if (rm.shouldLockYLevel()) {
            if (rm.getClientInstance().player.input.jumping) {
                hoverVec = hoverVec.add(0, rm.verticalSpeed.get(), 0);
            } else if (rm.getClientInstance().player.input.sneaking) {
                hoverVec = hoverVec.add(0, -rm.verticalSpeed.get(), 0);
            }
        }

        if (!rm.hoverMode.get().equals(RocketMan.HoverMode.Off)) {
            switch (rm.hoverMode.get()) {
                case Hold -> {
                    if (rm.getClientInstance().player.isFallFlying()) {
                        if (rm.isHoverKeyPressed() && !rm.wasHovering) {
                            ci.cancel();
                            if (!rm.isHovering) rm.setIsHovering(true);
                            rm.getClientInstance().player.move(MovementType.SELF, hoverVec);
                        } else {
                            rm.setIsHovering(false);
                        }
                    }
                }
                case Toggle -> {
                    boolean processed = false;
                    boolean toggled = rm.isHovering;

                    long now = System.currentTimeMillis();
                    if (rm.isHoverKeyPressed() && now - toggleTimestamp >= 420) {
                        if (!toggled) {
                            toggled = true;
                            rm.setIsHovering(true);
                            if (rm.getClientInstance().player.isFallFlying()) {
                                ci.cancel();
                                rm.getClientInstance().player.move(MovementType.SELF, hoverVec);
                            }
                        } else {
                            toggled = false;
                            rm.setIsHovering(false);
                        }
                        processed = true;
                        toggleTimestamp = now;
                    }
                    if (!processed && toggled && rm.getClientInstance().player.isFallFlying()) {
                        ci.cancel();
                        rm.getClientInstance().player.move(MovementType.SELF, hoverVec);
                    }
                }
                case Creative -> {
                    if (rm.getClientInstance().player.isFallFlying()) {
                        ci.cancel();
                        if (!rm.isHovering) rm.setIsHovering(true);
                        if (player.input.pressingForward || player.input.pressingRight || player.input.pressingLeft
                            || player.input.pressingBack || player.input.jumping || player.input.sneaking) {
                            player.move(MovementType.SELF, hoverVec);
                        }

                        // consume key-presses for s, so we don't toggle hover mode off automatically if we switch back to toggle mode
                        while (rm.getClientInstance().options.backKey.wasPressed()) {
                            continue;
                        }
                    }
                }
            } // consume irrelevant presses, so they don't trigger hover mode unintentionally
        } else while (rm.getClientInstance().options.backKey.wasPressed()) { continue; }
    }
}
