package dev.stardust.modules;
import net.minecraft.entity.player.PlayerInventory;
import dev.stardust.mixin.accessor.PlayerInventoryAccessor;

import dev.stardust.Stardust;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import dev.stardust.util.MsgUtil;
import net.minecraft.item.ItemStack;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.component.DataComponentTypes;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Paper seems to cuck the hell out of wind charges so probably don't even bother with this. Fun in vanilla, though.
 **/
public class Updraft extends Module {
    public Updraft() { super(Stardust.CATEGORY, "Updraft", "Automatically enhances your jumps with wind charges.");}

    private final Setting<Boolean> swapSetting = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("swap")
            .description("Automatically swaps to wind charges if none are being held.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> swapBackSetting = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("swap-back")
            .description("Automatically swaps back after using wind charges.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> hotBarSetting = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("hotbar-only")
            .description("Only swaps to wind charges in your hotbar.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> cooldownSetting = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("cooldown-ticks")
            .description("Cooldown between wind charge uses.")
            .defaultValue(10)
            .range(0, 1000)
            .sliderRange(10, 500)
            .build()
    );

    private final Setting<Double> pitchSpoofSetting = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("pitch-spoof")
            .description("The angle to look at when throwing a wind charge.")
            .min(-90).max(90)
            .sliderRange(-90, 90)
            .defaultValue(90)
            .build()
    );

    private final Setting<Integer> tickDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("tick-delay")
            .description("Delay between inventory actions.")
            .defaultValue(0)
            .range(0, 1000)
            .sliderRange(0, 100)
            .build()
    );

    private int timer = 0;
    private int notify = 0;
    private int returnSlot = -1;
    private int rotPriority = 69420;
    private boolean offhand = false;
    private State currentState = State.Idle;

    private void useWindCharge() {
        if (mc.interactionManager == null || mc.player == null || mc.player.isGliding()) {
            currentState = State.Idle;
            return;
        }
        if (offhand || mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE) {
            Rotations.rotate(
                mc.player.getYaw(), pitchSpoofSetting.get(), rotPriority, false,
                () -> mc.interactionManager.interactItem(mc.player, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND)
            );
            ++rotPriority;
            if (swapSetting.get() && swapBackSetting.get()) {
                timer = tickDelay.get();
                currentState = State.SwappingFrom;
            } else {
                currentState = State.Idle;
                timer = cooldownSetting.get();
            }
            return;
        } else if (chatFeedback && swapSetting.get() && notify <= 0) {
            notify = 100;
            MsgUtil.updateModuleMsg("No wind charges remaining§c..!", this.name, "windChargeAmmo".hashCode());
        }
        currentState = State.Idle;
    }

    private void swapToWindCharge() {
        ItemStack current = mc.player.getMainHandStack();
        ItemStack offhandStack = mc.player.getOffHandStack();
        if (current.getItem() == Items.WIND_CHARGE || offhandStack.getItem() == Items.WIND_CHARGE) {
            if (offhandStack.getItem() == Items.WIND_CHARGE) offhand = true;
            currentState = State.Using;
            useWindCharge();
            return;
        }

        for (int n = 0; n < (hotBarSetting.get() ? 9 : PlayerInventory.MAIN_SIZE); n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if (stack.getItem() == Items.WIND_CHARGE) {
                if (n < 9) {
                    InvUtils.swap(n, true);
                } else if (!hotBarSetting.get()) {
                    returnSlot = n;
                    InvUtils.move().from(n).to(((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot());
                }
                break;
            }
        }
        currentState = State.Using;
        if (tickDelay.get() == 0) {
            useWindCharge();
        } else {
            timer = tickDelay.get();
        }
    }

    private void swapFromWindCharge() {
        if (returnSlot == -1) InvUtils.swapBack();
        else InvUtils.move().from(((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot()).to(returnSlot);

        returnSlot = -1;
        offhand = false;
        currentState = State.Idle;
        timer = cooldownSetting.get();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        notify = 0;
        returnSlot = -1;
        offhand = false;
        rotPriority = 69420;
        currentState = State.Idle;
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (mc.options.jumpKey.matchesKey(event.key, 0)) {
            if (currentState == State.Idle) {
                if (swapSetting.get()) currentState = State.SwappingTo;
                else currentState = State.Using;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        --timer;
        --notify;
        if (mc.player.isGliding()) return;
        ItemStack current = mc.player.getMainHandStack();
        if ((current.contains(DataComponentTypes.FOOD) || Utils.isThrowable(current.getItem())) && mc.player.getItemUseTime() > 0) return;
        else if (current.isEmpty()) {
            ItemStack offhand = mc.player.getOffHandStack();
            if ((offhand.contains(DataComponentTypes.FOOD) || Utils.isThrowable(offhand.getItem())) && mc.player.getItemUseTime() > 0) return;
        }
        if (timer <= 0) {
            switch (currentState) {
                case Idle -> {} // defer to onKey
                case Using -> useWindCharge();
                case SwappingTo -> swapToWindCharge();
                case SwappingFrom -> swapFromWindCharge();
            }
        }
    }

    private enum State {
        Idle, SwappingTo, Using, SwappingFrom
    }
}
