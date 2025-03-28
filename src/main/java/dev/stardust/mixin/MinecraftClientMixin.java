package dev.stardust.mixin;

import org.lwjgl.glfw.GLFW;
import dev.stardust.modules.RocketMan;
import net.minecraft.sound.MusicSound;
import dev.stardust.modules.MusicTweaks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private long lastFrameTime = System.nanoTime();

    // See RocketMan.java
    @Inject(method = "render", at = @At("HEAD"))
    private void mixinRender(CallbackInfo ci) {
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastFrameTime) / 10000000f;

        Modules modules = Modules.get();
        if (modules == null ) return;
        RocketMan rocketMan = modules.get(RocketMan.class);
        if (!rocketMan.isActive() || !rocketMan.shouldTickRotation()) return;

        MinecraftClient mc = rocketMan.getClientInstance();

        if (mc.player == null) return;
        if (!rocketMan.hoverMode.get().equals(RocketMan.HoverMode.Off)) {
            if (mc.player.input.sneaking && !rocketMan.shouldLockYLevel() && !rocketMan.isHovering) {
                mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
            } else if (mc.player.input.jumping && !rocketMan.shouldLockYLevel() && !rocketMan.isHovering) {
                mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
            } else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
            } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
            }
        } else {
            boolean inverted = rocketMan.shouldInvertPitch();
            RocketMan.RocketMode mode = rocketMan.usageMode.get();

            switch (mode) {
                case OnKey -> {
                    if (mc.player.input.sneaking) {
                        mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
                    } else if (mc.player.input.jumping) {
                        mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                    } else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                        mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                    } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                        mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
                    }
                }
                case Static, Dynamic -> {
                    if (inverted) {
                        if ((mc.player.input.pressingForward || mc.player.input.sneaking) && !rocketMan.shouldLockYLevel()) {
                            mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        } else if ((mc.player.input.pressingBack || mc.player.input.jumping) && !rocketMan.shouldLockYLevel()) {
                            mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                            mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                            mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        }
                    } else {
                        if ((mc.player.input.pressingBack || mc.player.input.sneaking) && !rocketMan.shouldLockYLevel()) {
                            mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        } else if ((mc.player.input.pressingForward || mc.player.input.jumping) && !rocketMan.shouldLockYLevel()) {
                            mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        }else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                            mc.player.changeLookDirection(0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                            mc.player.changeLookDirection(0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        }
                    }
                }
            }
        }

        if (mc.player.input.pressingRight && !rocketMan.isHovering) {
            mc.player.changeLookDirection(rocketMan.getYawSpeed() * deltaTime, 0.0f);
        } else if (mc.player.input.pressingLeft && !rocketMan.isHovering) {
            mc.player.changeLookDirection(-rocketMan.getYawSpeed() * deltaTime, 0.0f);
        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) {
            mc.player.changeLookDirection(rocketMan.getYawSpeed() * deltaTime, 0.0f);
        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) {
            mc.player.changeLookDirection(-rocketMan.getYawSpeed() * deltaTime, 0.0f);
        }

        lastFrameTime = currentTime;
    }

    // See MusicTweaks.java
    @Inject(method = "getMusicType", at = @At("HEAD"), cancellable = true)
    public void mixinGetMusicType(CallbackInfoReturnable<MusicSound> cir) {
        Modules modules = Modules.get();
        if (modules == null ) return;
        MusicTweaks tweaks = modules.get(MusicTweaks.class);

        if (tweaks == null || !tweaks.isActive()) return;
        MusicSound type = tweaks.getType();

        if (type != null) {
            cir.setReturnValue(type);
        }
    }
}
