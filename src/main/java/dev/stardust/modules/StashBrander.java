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

    private boolean notified = false;
    private static final int ANVIL_OFFSET = 3;

    // See WorldMixin.java
    public boolean shouldMute() { return muteAnvils.get(); }

    @Override
    public void onDeactivate() {
        this.notified = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (mc.currentScreen == null) {
            this.notified = false;
            return;
        }
        if (!(mc.currentScreen instanceof AnvilScreen anvilScreen)) return;
        if (!(mc.player.currentScreenHandler instanceof AnvilScreenHandler anvil)) return;

        ItemStack input1 = anvil.getSlot(AnvilScreenHandler.INPUT_1_ID).getStack();
        ItemStack input2 = anvil.getSlot(AnvilScreenHandler.INPUT_2_ID).getStack();
        ItemStack output = anvil.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();

        if (input1.isEmpty() && input2.isEmpty()) {
            for (int n = ANVIL_OFFSET; n < mc.player.getInventory().main.size() + ANVIL_OFFSET; n++) {
                ItemStack stack = anvil.getSlot(n).getStack();
                if (stack.hasCustomName() && !renameNamed.get()) continue;
                else if (stack.getName().getString().equals(itemName.get())) continue;

                if ((blacklistMode.get() && !itemList.get().contains(stack.getItem()))
                    || (!blacklistMode.get() && itemList.get().contains(stack.getItem())))
                {
                    InvUtils.shiftClick().slotId(n);
                    ((AnvilScreenAccessor) anvilScreen).getNameField().setText(itemName.get());

                    ItemStack check = anvil.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
                    if (itemList.get().contains(check.getItem()) && check.getName().getString().equals(itemName.get())) {
                        int cost = ((AnvilScreenHandlerAccessor) anvil).getLevelCost().get();
                        if (mc.player.experienceLevel >= cost) {
                            InvUtils.shiftClick().slotId(AnvilScreenHandler.OUTPUT_ID);
                        } else {
                            if (!this.notified) mc.player.sendMessage(
                                Text.of("§8<"+ StardustUtil.rCC()+"✨§8> §4§oNot enough experience§8§o...")
                            );
                            this.notified = true;
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (disableOnDone.get()) this.toggle();
                        }
                        if (n == 38) {
                            if (!this.notified) mc.player.sendMessage(
                                Text.of("§8<"+ StardustUtil.rCC()+"✨§8> §4§oNo more items to rename§8§o.")
                            );
                            this.notified = true;
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (disableOnDone.get()) this.toggle();
                        }
                        return;
                    }
                }
                if (n == 38) {
                    if (!this.notified) mc.player.sendMessage(
                        Text.of("§8<"+ StardustUtil.rCC()+"✨§8> §4§oNo more items to rename§8§o.")
                    );
                    this.notified = true;
                    if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
                    if (closeOnDone.get()) mc.player.closeHandledScreen();
                    if (disableOnDone.get()) this.toggle();
                }
            }
        } else if (!output.isEmpty() && itemList.get().contains(output.getItem()) && output.getName().getString().equals(itemName.get())) {
            int cost = ((AnvilScreenHandlerAccessor) anvil).getLevelCost().get();
            if (mc.player.experienceLevel >= cost) {
                InvUtils.shiftClick().slotId(AnvilScreenHandler.OUTPUT_ID);
            } else {
                if (!this.notified) mc.player.sendMessage(
                    Text.of("§8<"+ StardustUtil.rCC()+"✨§8> §4§oNot enough experience§8§o...")
                );
                this.notified = true;
                if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
                if (closeOnDone.get()) mc.player.closeHandledScreen();
                if (disableOnDone.get()) this.toggle();
            }
        } else if (!input2.isEmpty()) {
            InvUtils.shiftClick().slotId(AnvilScreenHandler.INPUT_2_ID);
        } else if (output.isEmpty()) {
            InvUtils.shiftClick().slotId(AnvilScreenHandler.INPUT_1_ID);
        }
    }
}
