package dev.stardust.mixin.meteor;

import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Hand;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.Vec3d;
import dev.stardust.modules.RocketMan;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.MeteorClient;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.world.AirPlace;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Adds an offhand bypass setting to Meteor's AirPlace, & allows holding ctrl+scroll-wheel to alter placement range.
 **/
@Mixin(value = AirPlace.class, remap = false)
public abstract class AirPlaceMixin extends Module {
    @Shadow
    @Final
    private SettingGroup sgGeneral;

    @Shadow
    @Final
    private Setting<Boolean> customRange;

    @Shadow
    @Final
    private Setting<Double> range;

    @Shadow
    private HitResult hitResult;

    public AirPlaceMixin(Category category, String name, String description) {
        super(category, name, description);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Unique
    private int timer = 0;
    @Unique
    private boolean justUsed = false;
    @Unique
    private @Nullable Setting<Boolean> bypass = null;

    @Unique
    @Override
    public void onDeactivate() {
        timer = 0;
        justUsed = false;
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/world/AirPlace;sgRange:Lmeteordevelopment/meteorclient/settings/SettingGroup;"))
    private void addSettings(CallbackInfo ci) {
        if (bypass == null) bypass = sgGeneral.add(
            new BoolSetting.Builder()
                .name("offhand-bypass")
                .description("Use a bypass for AirPlace on GrimAC.")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void hijackOnTick(CallbackInfo ci) {
        if (bypass == null || !bypass.get()) return;
        if (mc.getNetworkHandler() == null || mc.interactionManager == null) return;
        if (mc.player == null || mc.getCameraEntity() == null || mc.world == null) return;

        if (justUsed) {
            ++timer;
            if (timer >= 5) {
                timer = 0;
                justUsed = false;
            }
        } else {
            if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;

            double r = customRange.get() ? range.get() : mc.player.getBlockInteractionRange();
            hitResult = mc.getCameraEntity().raycast(r, 0, false);

            if (!(hitResult instanceof BlockHitResult blockHit) || !mc.world.getBlockState(blockHit.getBlockPos()).isAir()) return;

            ci.cancel();
            if (mc.options.useKey.isPressed() && !justUsed) {
                justUsed = true;
                BlockPos pos = blockHit.getBlockPos();
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false), 0));

                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            }
        }
    }

    @Unique
    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        if (bypass == null || !bypass.get() || hitResult == null) return;
        if (event.result.getBlockPos().isWithinDistance(((BlockHitResult) hitResult).getBlockPos(), 1) && justUsed) {
            event.cancel();
        }
    }

    @Unique
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onMouseScroll(MouseScrollEvent event) {
        if (mc.player == null) return;
        Modules mods = Modules.get();
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (mc.currentScreen != null || mods == null || mods.get(Freecam.class).isActive()) return;
        if (mods.get(RocketMan.class).isActive() && mods.get(RocketMan.class).boostSpeed.get()) return;
        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            event.cancel();
            range.set(MathHelper.clamp(range.get() + event.value, 1, 6));
        }
    }
}
