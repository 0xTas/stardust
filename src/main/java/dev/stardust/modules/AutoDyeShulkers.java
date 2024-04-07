package dev.stardust.modules;

import dev.stardust.Stardust;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import net.minecraft.item.DyeItem;
import net.minecraft.util.DyeColor;
import net.minecraft.item.ItemStack;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import meteordevelopment.orbit.EventHandler;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.screen.PlayerScreenHandler;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AutoDyeShulkers extends Module {
    public AutoDyeShulkers() { super(Stardust.CATEGORY, "AutoDyeShulkers", "Automatically dye shulker boxes in crafting grids."); }

    private final Setting<DyeColor> dyeColor = settings.getDefaultGroup().add(
        new EnumSetting.Builder<DyeColor>()
            .name("Color")
            .defaultValue(DyeColor.LIGHT_BLUE)
            .build()
    );

    private final Setting<Boolean> reDyeColored = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Recolor Colored")
            .description("Re-color shulker boxes which already have a different dye color applied.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Close Screen")
            .description("Automatically close the crafting table screen when no more shulkers can be dyed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Disable on Done")
            .description("Automatically disable the module when no more shulkers can be dyed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Sound Ping")
            .description("Play a sound cue when no more shulkers can be dyed.")
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

    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Tick Rate")
            .description("You may need to increase this if the server is lagging.")
            .range(1, 100)
            .sliderRange(2, 20)
            .defaultValue(2)
            .build()
    );

    private int timer = 0;
    private boolean notified = false;

    public static boolean isColoredShulker(Item box) {
        return (box == Items.BLACK_SHULKER_BOX
            || box == Items.GRAY_SHULKER_BOX || box == Items.LIGHT_GRAY_SHULKER_BOX
            || box == Items.WHITE_SHULKER_BOX || box == Items.RED_SHULKER_BOX
            || box == Items.ORANGE_SHULKER_BOX || box == Items.YELLOW_SHULKER_BOX
            || box == Items.LIME_SHULKER_BOX || box == Items.GREEN_SHULKER_BOX
            || box == Items.CYAN_SHULKER_BOX || box == Items.LIGHT_BLUE_SHULKER_BOX
            || box == Items.BLUE_SHULKER_BOX || box == Items.PURPLE_SHULKER_BOX
            || box == Items.MAGENTA_SHULKER_BOX || box == Items.PINK_SHULKER_BOX || box == Items.BROWN_SHULKER_BOX);
    }

    private boolean isValidShulker(Item box) {
        if (!reDyeColored.get()) return box == Items.SHULKER_BOX;
        else {
            return switch (dyeColor.get()) {
                case BLACK -> box != Items.BLACK_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case GRAY -> box != Items.GRAY_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case LIGHT_GRAY -> box != Items.LIGHT_GRAY_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case WHITE -> box != Items.WHITE_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case RED -> box != Items.RED_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case ORANGE -> box != Items.ORANGE_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case YELLOW -> box != Items.YELLOW_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case LIME -> box != Items.LIME_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case GREEN -> box != Items.GREEN_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case CYAN -> box != Items.CYAN_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case LIGHT_BLUE -> box != Items.LIGHT_BLUE_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case BLUE -> box != Items.BLUE_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case MAGENTA -> box != Items.MAGENTA_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case PURPLE -> box != Items.PURPLE_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case PINK -> box != Items.PINK_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
                case BROWN -> box != Items.BROWN_SHULKER_BOX && (isColoredShulker(box) || box == Items.SHULKER_BOX);
            };
        }
    }

    private int getUnoccupiedSlot(int occupied, int inputEnd) {
        int slot;
        do {
            slot = ThreadLocalRandom.current().nextInt(1, inputEnd);
        } while (slot == occupied);
        return  slot;
    }

    private <T extends AbstractRecipeScreenHandler<RecipeInputInventory>> int getItemSlot(Item wanted, T cs, int invStart, int invEnd) {
        for (int n = invStart; n < invEnd; n++) {
            ItemStack stack = cs.getSlot(n).getStack();
            if (wanted == Items.SHULKER_BOX) {
                if (isValidShulker(stack.getItem())) return n;
            } else if (wanted == stack.getItem()) return n;
        }

        return -1;
    }

    private <T extends AbstractRecipeScreenHandler<RecipeInputInventory>> void dyeShulker(T cs, int inputEnd, int invStart, int invEnd) {
        ItemStack output = cs.getSlot(0).getStack();

        if (isColoredShulker(output.getItem())) {
            InvUtils.shiftClick().slotId(0);
        } else {
            boolean hasDye = false;
            boolean hasShulk = false;
            int occupiedSlotDye = -1;
            int occupiedSlotShulk = -1;
            for (int n = 1; n < inputEnd; n++) {
                ItemStack stack = cs.getSlot(n).getStack();
                if (stack.getItem() == DyeItem.byColor(dyeColor.get())) {
                    if (!hasDye) {
                        hasDye = true;
                        occupiedSlotDye = n;
                    } else InvUtils.shiftClick().slotId(n);
                } else if (isValidShulker(stack.getItem())) {
                    if (!hasShulk) {
                        hasShulk = true;
                        occupiedSlotShulk = n;
                    } else InvUtils.shiftClick().slotId(n);
                }
            }
            if (!hasDye) {
                int dyeSlot = getItemSlot(DyeItem.byColor(dyeColor.get()), cs, invStart, invEnd);
                if (dyeSlot != -1) {
                    if (occupiedSlotShulk != -1) {
                        InvUtils.move().fromId(dyeSlot).toId(getUnoccupiedSlot(occupiedSlotShulk, inputEnd));
                    } else InvUtils.move().fromId(dyeSlot).toId(1);
                    return;
                }
            }
            if (!hasShulk) {
                int shulkSlot = getItemSlot(Items.SHULKER_BOX, cs, invStart, invEnd);
                if (shulkSlot != -1) {
                    if (occupiedSlotDye != -1) {
                        InvUtils.move().fromId(shulkSlot).toId(getUnoccupiedSlot(occupiedSlotShulk, inputEnd));
                    } else InvUtils.move().fromId(shulkSlot).toId(1);
                    return;
                }
            }
            if (hasDye && hasShulk && output.isEmpty()) {
                timer = tickRate.get() - 1;
            } else if (!hasShulk || !hasDye) {
                if (!notified) {
                    notified = true;
                    if (disableOnDone.get()) toggle();
                    if (closeOnDone.get() && cs instanceof CraftingScreenHandler) mc.player.closeHandledScreen();
                    if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1f);
                    mc.player.sendMessage(
                        Text.of("§8<" + StardustUtil.rCC() + "✨§8> §2§oFinished coloring shulkers§8§o.")
                    );
                }
            }
        }
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
            onDeactivate();
            return;
        }

        if (mc.currentScreen instanceof CraftingScreen && mc.player.currentScreenHandler instanceof CraftingScreenHandler cs) {
            ++timer;
            if (timer >= tickRate.get()) {
                timer = 0;
                dyeShulker(cs, 10, 10, 46);
            }
        } else if (mc.currentScreen instanceof InventoryScreen && mc.player.currentScreenHandler instanceof PlayerScreenHandler ps) {
            ++timer;
            if (timer >= tickRate.get()) {
                timer = 0;
                dyeShulker(ps, 5, 9, 45);
            }
        }
    }
}
