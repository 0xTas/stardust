package dev.stardust.modules;

import java.util.List;
import dev.stardust.Stardust;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import meteordevelopment.orbit.EventHandler;
import dev.stardust.mixin.AnvilScreenAccessor;
import net.minecraft.screen.AnvilScreenHandler;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.component.DataComponentTypes;
import dev.stardust.mixin.AnvilScreenHandlerAccessor;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class StashBrander extends Module {
    public StashBrander() { super(Stardust.CATEGORY, "StashBrander","Allows you to automatically rename items in bulk when using anvils."); }

    private final Setting<List<Item>> itemList = settings.getDefaultGroup().add(
        new ItemListSetting.Builder()
            .name("Items")
            .description("Items to automatically rename (or exclude from being renamed, if blacklist mode is enabled.)")
            .build()
    );

    private final Setting<String> itemName = settings.getDefaultGroup().add(
        new StringSetting.Builder()
            .name("Custom Name")
            .description("The name you want to give to qualifying items.")
            .defaultValue("")
            .onChanged(name -> {
                if (name.length() > AnvilScreenHandler.MAX_NAME_LENGTH) {
                    if (mc.player != null) mc.player.sendMessage(
                        Text.of("§8<"+ StardustUtil.rCC()+"✨§8> §4§oCustom name exceeds max accepted length§8§o!")
                    );
                }
            })
            .build()
    );

    private final Setting<Boolean> blacklistMode = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Blacklist Mode")
            .description("Rename all items except the ones selected in the Items list.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> renameNamed = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Rename Prenamed")
            .description("Rename items which already have a different custom name.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> muteAnvils = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Mute Anvils")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Sound Ping")
            .description("Play a sound cue when no more items can be renamed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> pingVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("Ping Volume")
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(1.0)
            .build()
    );

    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Close Anvil")
            .description("Automatically close the anvil screen when no more items can be renamed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Disable on Done")
            .description("Automatically disable the module when no more items can be renamed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Tick Rate")
            .min(0).max(1000)
            .sliderRange(0, 100)
            .defaultValue(0)
            .build()
    );

    private int timer = 0;
    private boolean notified = false;
    private static final int ANVIL_OFFSET = 3;

    // See WorldMixin.java
    public boolean shouldMute() { return muteAnvils.get(); }

    private boolean hasValidItems() {
        if (mc.player == null) return false;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if ((blacklistMode.get() && !itemList.get().contains(stack.getItem()))
                || (!blacklistMode.get() && itemList.get().contains(stack.getItem())))
            {
                if (itemName.get().isBlank() && stack.contains(DataComponentTypes.CUSTOM_NAME)) return true;
                else if (!stack.getName().getString().equals(itemName.get())) return true;
            }
        }
        return false;
    }

    private void noXP() {
        if (!notified) {
            mc.player.sendMessage(
                Text.of("§8<" + StardustUtil.rCC() + "✨§8> §4§oNot enough experience§8§o...")
            );
            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
        }
        notified = true;
        if (closeOnDone.get()) mc.player.closeHandledScreen();
        if (disableOnDone.get()) this.toggle();
    }

    private void finished() {
        if (!notified) {
            mc.player.sendMessage(
                Text.of("§8<" + StardustUtil.rCC() + "✨§8> §4§oNo more items to rename§8§o.")
            );
            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
        }
        notified = true;
        if (closeOnDone.get()) mc.player.closeHandledScreen();
        if (disableOnDone.get()) this.toggle();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        notified = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (mc.currentScreen == null) {
            notified = false;
            return;
        }
        if (!(mc.currentScreen instanceof AnvilScreen anvilScreen)) return;
        if (!(mc.player.currentScreenHandler instanceof AnvilScreenHandler anvil)) return;

        if (timer < tickRate.get()) {
            timer++;
            return;
        } else {
            timer = 0;
        }

        ItemStack input1 = anvil.getSlot(AnvilScreenHandler.INPUT_1_ID).getStack();
        ItemStack input2 = anvil.getSlot(AnvilScreenHandler.INPUT_2_ID).getStack();
        ItemStack output = anvil.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();

        if (!hasValidItems()) finished();
        else if (input1.isEmpty() && input2.isEmpty()) {
            for (int n = ANVIL_OFFSET; n < mc.player.getInventory().main.size() + ANVIL_OFFSET; n++) {
                ItemStack stack = anvil.getSlot(n).getStack();
                if (stack.contains(DataComponentTypes.CUSTOM_NAME) && !renameNamed.get()) continue;
                else if (stack.getName().getString().equals(itemName.get())) continue;
                else if (itemName.get().isBlank() && !stack.contains(DataComponentTypes.CUSTOM_NAME)) continue;

                if ((blacklistMode.get() && !itemList.get().contains(stack.getItem()))
                    || (!blacklistMode.get() && itemList.get().contains(stack.getItem())))
                {
                    InvUtils.shiftClick().slotId(n);
                    ((AnvilScreenAccessor) anvilScreen).getNameField().setText(itemName.get());
                    ItemStack check = anvil.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
                    if (itemList.get().contains(check.getItem())) {
                        if (check.getName().getString().equals(itemName.get()) || (itemName.get().isBlank() && stack.contains(DataComponentTypes.CUSTOM_NAME))) {
                            int cost = ((AnvilScreenHandlerAccessor) anvil).getLevelCost().get();
                            if (mc.player.experienceLevel >= cost) {
                                InvUtils.shiftClick().slotId(AnvilScreenHandler.OUTPUT_ID);
                            } else noXP();

                            return;
                        }
                    }
                }
            }
            finished();
        } else if (!output.isEmpty() && itemList.get().contains(output.getItem())) {
            if (output.getName().getString().equals(itemName.get()) || (itemName.get().isBlank() && input1.contains(DataComponentTypes.CUSTOM_NAME))) {
                int cost = ((AnvilScreenHandlerAccessor) anvil).getLevelCost().get();
                if (mc.player.experienceLevel >= cost) {
                    InvUtils.shiftClick().slotId(AnvilScreenHandler.OUTPUT_ID);
                } else noXP();
            }
        } else if (!input2.isEmpty()) {
            InvUtils.shiftClick().slotId(AnvilScreenHandler.INPUT_2_ID);
        } else if (output.isEmpty()) {
            InvUtils.shiftClick().slotId(AnvilScreenHandler.INPUT_1_ID);
        }
    }
}
