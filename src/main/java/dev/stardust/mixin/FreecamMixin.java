package dev.stardust.mixin;

import java.time.Instant;
import java.time.Duration;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.entity.ai.pathing.NavigationType;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.settings.StringSetting;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Adds "click-to-come" functionality to Meteor's built-in Freecam module (requires Baritone)
 **/
@Mixin(value = Freecam.class, remap = false)
public class FreecamMixin {
    @Shadow
    private boolean forward;
    @Shadow
    private boolean backward;
    @Shadow
    private boolean right;
    @Shadow
    private boolean left;
    @Shadow
    private boolean up;
    @Shadow
    private boolean down;
    @Shadow
    @Final
    private SettingGroup sgGeneral;

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
    private Setting<String> baritoneChatPrefix = null;
    @Unique
    @Nullable
    private Setting<Boolean> doubleClickToCome = null;

    // 1.20.1 Fix for Freecam scroll speed update, was fixed in newer version, in commit 2c5e8ed
    // Original commit: https://github.com/MeteorDevelopment/meteor-client/commit/2c5e8edc29e84d4f07e5534c8b0c967bb6696379
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void fixScrollSpeedUpdate(MouseScrollEvent event, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen != null) ci.cancel();
    }

    // 1.20.1 Fix for Freecam not properly recording previous movement values. Fixed in commit ea41864
    // Original commit: https://github.com/MeteorDevelopment/meteor-client/commit/ea418641b25d5d7ddec15e0a3ec659e5c8cc3862
    @Inject(method = "onActivate", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Freecam;unpress()V"))
    private void preservePreviousMovementValues(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();

        this.up = mc.options.jumpKey.isPressed();
        this.left = mc.options.leftKey.isPressed();
        this.down = mc.options.sneakKey.isPressed();
        this.right = mc.options.rightKey.isPressed();
        this.backward = mc.options.backKey.isPressed();
        this.forward = mc.options.forwardKey.isPressed();
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Freecam;staticView:Lmeteordevelopment/meteorclient/settings/Setting;", shift = At.Shift.AFTER))
    private void addClickToComeSettings(CallbackInfo ci) {
        clickToCome = sgGeneral.add(
            new BoolSetting.Builder()
                .name("click-to-come")
                .description("Click on a block while in freecam to path there with Baritone chat commands.")
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

        baritoneChatPrefix = sgGeneral.add(
            new StringSetting.Builder()
                .name("baritone-command-prefix")
                .description("What prefix to use for Baritone chat commands.")
                .defaultValue("#")
                .visible(() -> clickToCome != null && clickToCome.get())
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("HEAD"))
    private void handleClickToCome(CallbackInfo ci) {
        if ((doubleClickToCome != null && !doubleClickToCome.get() && clicks >= 1) || clicks >= 2) {
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

            if (baritoneChatPrefix != null && !baritoneChatPrefix.get().isBlank()) {
                if (side != null) {
                    // Try not to mine the block we clicked on
                    if (side == Direction.DOWN) {
                        crosshairPos = crosshairPos.offset(side, 2);
                    }else if (side == Direction.UP) {
                        crosshairPos = crosshairPos.offset(side);
                    } else {
                        crosshairPos = crosshairPos.offset(side);
                        if (mc.world.getBlockState(crosshairPos.offset(Direction.DOWN)).canPathfindThrough(mc.world, crosshairPos.offset(Direction.DOWN), NavigationType.LAND)) {
                            crosshairPos = crosshairPos.offset(Direction.DOWN);
                        }
                    }
                }
                mc.getNetworkHandler().sendChatMessage(baritoneChatPrefix.get() + "goto "
                    + crosshairPos.getX() + " "
                    + crosshairPos.getY() + " "
                    + crosshairPos.getZ()
                );
            }
        }

        if (clicks <= 0) return;

        ++timer;
        if (timer >= 10) {
            timer = 0;
            clicks = 0;
            clickedAt = null;
        }
    }

    @Inject(method = "onMouseButton", at = @At("TAIL"))
    private void handleMouseClicks(MouseButtonEvent event, CallbackInfo ci) {
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
}
