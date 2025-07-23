package dev.stardust.mixin.meteor;

import java.time.Instant;
import org.joml.Vector3d;
import java.time.Duration;
import org.lwjgl.glfw.GLFW;
import dev.stardust.util.MsgUtil;
import net.minecraft.block.AirBlock;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.settings.*;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.entity.ai.pathing.NavigationType;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Adds "Click-to-come" functionality to Meteor's built-in Freecam module.
 **/
@Mixin(value = Freecam.class, remap = false)
public abstract class FreecamMixin {
    @Shadow
    @Final
    private SettingGroup sgGeneral;

    @Shadow
    private Perspective perspective;
    @Shadow
    private double speedValue;
    @Shadow
    @Final
    public Vector3d prevPos;
    @Shadow
    @Final
    public Vector3d pos;

    @Shadow
    protected abstract boolean checkGuiMove();

    @Shadow
    private boolean up;
    @Shadow
    private boolean down;
    @Unique
    private int timer = 0;
    @Unique
    private int clicks = 0;
    @Unique
    @Nullable
    private Instant clickedAt = null;
    @Unique
    @Nullable
    private Setting<Boolean> clickToCome = null;
    @Unique
    @Nullable
    private Setting<Boolean> useBaritoneChat = null;
    @Unique
    @Nullable
    private Setting<String> baritoneChatPrefix = null;
    @Unique
    @Nullable
    private Setting<Boolean> doubleClickToCome = null;
    @Unique
    @Nullable
    private Setting<Boolean> satelliteCameraMode = null;
    @Unique
    @Nullable
    private Setting<Double> orbitHeight = null;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Freecam;rotate:Lmeteordevelopment/meteorclient/settings/Setting;"))
    private void addClickToComeSettings(CallbackInfo ci) {
        clickToCome = sgGeneral.add(
            new BoolSetting.Builder()
                .name("click-to-come")
                .description("Click on a block while in freecam to path there with Baritone.")
                .defaultValue(false)
                .build()
        );

        doubleClickToCome = sgGeneral.add(
            new BoolSetting.Builder()
                .name("double-click-only")
                .description("Require a double-click to Baritone path to your crosshair target.")
                .defaultValue(false)
                .visible(() -> clickToCome != null && clickToCome.get())
                .build()
        );

        useBaritoneChat = sgGeneral.add(
            new BoolSetting.Builder()
                .name("use-baritone-chat")
                .description("Use Baritone chat commands instead of the internal API. For compatibility with standalone Baritone versions.")
                .defaultValue(false)
                .visible(() -> clickToCome != null && clickToCome.get())
                .build()
        );

        baritoneChatPrefix = sgGeneral.add(
            new StringSetting.Builder()
                .name("baritone-command-prefix")
                .description("What prefix to use for Baritone chat commands.")
                .defaultValue("#")
                .visible(() -> clickToCome != null && clickToCome.get() && useBaritoneChat != null && useBaritoneChat.get())
                .build()
        );

        satelliteCameraMode = sgGeneral.add(
            new BoolSetting.Builder()
                .name("satellite-camera")
                .description("Lock Freecam to the player's position sans the y-value, which you'll set yourself.")
                .defaultValue(false)
                .build()
        );
        orbitHeight = sgGeneral.add(
            new DoubleSetting.Builder()
                .name("orbit-height")
                .description("Height for the satellite camera to orbit at.")
                .defaultValue(169.0).min(-69.0).sliderMax(420.0)
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void handleClickToCome(CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen == null && ((doubleClickToCome != null && !doubleClickToCome.get() && clicks >= 1) || clicks >= 2)) {
            clicks = 0;
            Direction side = null;
            BlockPos crosshairPos;
            if (mc.crosshairTarget instanceof EntityHitResult) {
                crosshairPos = ((EntityHitResult) mc.crosshairTarget).getEntity().getBlockPos();
            } else {
                BlockHitResult result = ((BlockHitResult) mc.crosshairTarget);
                if (mc.world.getBlockState(result.getBlockPos()).getBlock() instanceof AirBlock) {
                    Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
                    float pitch = mc.gameRenderer.getCamera().getPitch();
                    float yaw = mc.gameRenderer.getCamera().getYaw();

                    Vec3d direction = getRotationVector(pitch, yaw);
                    RaycastContext context = new RaycastContext(
                        cameraPos, cameraPos.add(direction.multiply(256)),
                        RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, mc.getCameraEntity()
                    );

                    BlockHitResult rayCast = mc.world.raycast(context);
                    if (rayCast != null && !(mc.world.getBlockState(rayCast.getBlockPos()).getBlock() instanceof AirBlock)) {
                        crosshairPos = rayCast.getBlockPos();
                        side = rayCast.getSide();
                    } else {
                        crosshairPos = result.getBlockPos();
                        side = result.getSide();
                    }
                } else {
                    crosshairPos = result.getBlockPos();
                    side = result.getSide();
                }
            }

            if (side != null) {
                // Try not to mine the block we clicked on
                if (side == Direction.DOWN) {
                    crosshairPos = crosshairPos.offset(side, 2);
                }else if (side == Direction.UP) {
                    crosshairPos = crosshairPos.offset(side);
                } else {
                    crosshairPos = crosshairPos.offset(side);
                    if (mc.world.getBlockState(crosshairPos.offset(Direction.DOWN)).canPathfindThrough(NavigationType.LAND)) {
                        crosshairPos = crosshairPos.offset(Direction.DOWN);
                    }
                }
            }

            if (useBaritoneChat != null && useBaritoneChat.get() && baritoneChatPrefix != null && !baritoneChatPrefix.get().isBlank()) {
                mc.getNetworkHandler().sendChatMessage(baritoneChatPrefix.get() + "goto "
                    + crosshairPos.getX() + " "
                    + crosshairPos.getY() + " "
                    + crosshairPos.getZ()
                );
            } else if (BaritoneUtils.IS_AVAILABLE) {
                PathManagers.get().stop();
                PathManagers.get().moveTo(crosshairPos);
                if (Modules.get().get(Freecam.class).chatFeedback) MsgUtil.sendModuleMsg("Baritone pathing to destination§a..!", "freecam");
            } else {
                MsgUtil.sendMsg("Baritone was not found to be installed. If this is a mistake, please enable the \"Use Baritone Chat\" setting and try again.");
            }
        }

        if (mc.currentScreen == null && clicks > 0) {
            ++timer;
            if (timer >= 10) {
                timer = 0;
                clicks = 0;
                clickedAt = null;
            }
        }

        if (satelliteCameraMode == null || !satelliteCameraMode.get()) return;
        if (mc.cameraEntity == null || mc.getCameraEntity() == null) return;
        ci.cancel();

        if (mc.cameraEntity.isInsideWall()) mc.getCameraEntity().noClip = true;
        if (!perspective.isFirstPerson()) mc.options.setPerspective(Perspective.FIRST_PERSON);

        double s = 0.5;
        double velY = 0;
        if (mc.options.sprintKey.isPressed()) s = 1;

        if (this.up) {
            velY += s * speedValue;
        }
        if (this.down) {
            velY -= s * speedValue;
        }

        Vec3d orbitPos = getOrbitPos(velY);

        prevPos.set(pos);
        pos.set(orbitPos.x, orbitPos.y, orbitPos.z);
    }

    // Allow RocketMan keyboard control to work & only cancel the shift/space preses for satellite cam
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void hijackOnKey(KeyEvent event, CallbackInfo ci) {
        if (Input.isKeyPressed(GLFW.GLFW_KEY_F3)) return;
        if (checkGuiMove()) return;
        if (satelliteCameraMode == null || !satelliteCameraMode.get()) return;

        ci.cancel();
        boolean cancel = true;
        if (mc.options.jumpKey.matchesKey(event.key, 0)) {
            up = event.action != KeyAction.Release;
            mc.options.jumpKey.setPressed(false);
        }
        else if (mc.options.sneakKey.matchesKey(event.key, 0)) {
            down = event.action != KeyAction.Release;
            mc.options.sneakKey.setPressed(false);
        } else {
            cancel = false;
        }

        if (cancel) event.cancel();
    }

    @Inject(method = "onMouseButton", at = @At("TAIL"))
    private void handleMouseClicks(MouseButtonEvent event, CallbackInfo ci) {
        if (mc.currentScreen != null) return;
        if (clickToCome == null || !clickToCome.get()) return;
        if (mc.options.attackKey.matchesMouse(event.button)) {
            Instant now = Instant.now();
            if (clickedAt == null || Duration.between(clickedAt, now).toMillis() > 100) {
                clicks++;
                clickedAt = now;
            }
        }
    }

    @Inject(method = "onDeactivate", at = @At("TAIL"))
    private void resetCounters(CallbackInfo ci) {
        timer = 0;
        clicks = 0;
        clickedAt = null;
    }

    @Unique
    private Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180);
        float g = -yaw * ((float)Math.PI / 180);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    @Unique
    private Vec3d getOrbitPos(double velY) {
        if (orbitHeight != null) {
            orbitHeight.set(orbitHeight.get() + velY);
        }
        if (mc.player == null || orbitHeight == null) return new Vec3d(0, orbitHeight == null ? velY : orbitHeight.get(), 0);
        return new Vec3d(mc.player.getX(), orbitHeight.get(), mc.player.getZ());
    }
}
