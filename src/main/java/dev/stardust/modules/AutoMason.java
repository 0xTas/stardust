package dev.stardust.modules;
import net.minecraft.entity.player.PlayerInventory;

import java.util.List;
import java.util.ArrayDeque;
import dev.stardust.Stardust;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import dev.stardust.util.MsgUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.Registries;
import dev.stardust.util.StonecutterUtil;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.packet.Packet;
import meteordevelopment.orbit.EventHandler;
import java.util.concurrent.ThreadLocalRandom;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.recipe.StonecuttingRecipe;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.screen.slot.SlotActionType;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.util.context.ContextParameterMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AutoMason extends Module {
    public AutoMason() { super(Stardust.CATEGORY, "AutoMason", "Automates masonry interactions with the stonecutter."); }

    public enum Mode {
        Packet, Interact
    }

    private final Setting<Mode> moduleMode = settings.getDefaultGroup().add(
        new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Packet is faster but might also get you kicked in some scenarios.")
            .defaultValue(Mode.Packet)
            .build()
    );
    private final Setting<Integer> batchDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("packet-delay")
            .description("Increase this if the server is kicking you.")
            .min(0).max(1000)
            .sliderRange(0, 50)
            .defaultValue(1)
            .visible(() -> moduleMode.get().equals(Mode.Packet))
            .build()
    );
    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("tick-rate")
            .description("Increase this if the server is kicking you.")
            .min(0).max(1000)
            .sliderRange(0, 100)
            .defaultValue(4)
            .visible(() -> moduleMode.get().equals(Mode.Interact))
            .build()
    );

    private final Setting<List<Item>> itemList = settings.getDefaultGroup().add(
        new ItemListSetting.Builder()
            .name("target-items")
            .description("Which target items you wish to craft in the Stonecutter.")
            .filter(item -> StonecutterUtil.STONECUTTER_BLOCKS.values().stream().anyMatch(v -> v.contains(item)))
            .build()
    );
    private final Setting<Boolean> muteCutter = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("mute-stonecutter")
            .description("Mute the stonecutter sounds.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("close-stonecutter")
            .description("Automatically close the stonecutter screen when no more blocks can be crafted.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("disable-when-done")
            .description("Automatically disable the module when no more blocks can be crafted.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("sound-ping")
            .description("Play a sound cue when there are no more blocks to be crafted.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Double> pingVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("ping-volume")
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(0.5)
            .visible(pingOnDone::get)
            .build()
    );

    private int timer = 0;
    private boolean notified = false;
    private @Nullable ItemStack targetStack = null;
    private @Nullable ItemStack outputStack = null;
    private final IntArrayList projectedEmpty = new IntArrayList();
    private final IntArrayList processedSlots = new IntArrayList();
    private final ArrayDeque<Packet<?>> packetQueue = new ArrayDeque<>();

    @Override
    public void onDeactivate() {
        timer = 0;
        notified = false;
        targetStack = null;
        outputStack = null;
        packetQueue.clear();
        processedSlots.clear();
        projectedEmpty.clear();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof StonecutterScreen) {
            notified = false;
        }
    }

    @EventHandler
    private void onSoundPlay(PlaySoundEvent event) {
        if (!muteCutter.get()) return;
        if (event.sound.getId().equals(Registries.SOUND_EVENT.getId(SoundEvents.UI_STONECUTTER_TAKE_RESULT))) {
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.getNetworkHandler() == null) return;
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.currentScreenHandler instanceof StonecutterScreenHandler cutter)) return;

        if (!packetQueue.isEmpty()) {
            if (batchDelay.get() <= 0) {
                while (!packetQueue.isEmpty()) {
                    ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                        packetQueue.removeFirst(), null, true
                    );
                }
            } else {
                ++timer;
                if (timer >= batchDelay.get()) {
                    timer = 0;
                    ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                        packetQueue.removeFirst(), null, true
                    );
                }
            }

            if (packetQueue.isEmpty()) finished();
            return;
        }

        switch (moduleMode.get()) {
            case Packet -> {
                if (notified) return;
                if (itemList.get().isEmpty()) {
                    notified = true;
                    MsgUtil.sendModuleMsg("No target items selected§c..!", this.name);
                    finished();
                    return;
                }

                boolean exhausted = false;
                while (!exhausted) {
                    Packet<?> packet = generatePacket(cutter);

                    if (packet == null) {
                        exhausted = true;
                    } else packetQueue.addLast(packet);
                }
                if (packetQueue.isEmpty()) finished();
            }
            case Interact -> {
                if (timer >= tickRate.get()) {
                    timer = 0;
                } else {
                    ++timer;
                    return;
                }

                if (itemList.get().isEmpty()) {
                    if (!notified) {
                        MsgUtil.sendModuleMsg("No target items selected§c..!", this.name);
                        if (pingOnDone.get()) {
                            mc.player.playSound(
                                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                pingVolume.get().floatValue(),
                                ThreadLocalRandom.current().nextFloat(0.69f, 1.337f)
                            );
                        }
                    }
                    notified = true;

                    finished();
                    return;
                }

                ItemStack input = cutter.getSlot(StonecutterScreenHandler.INPUT_ID).getStack();
                ItemStack output = cutter.getSlot(StonecutterScreenHandler.OUTPUT_ID).getStack();

                if (!hasValidItems(cutter)) finished();
                else if (input.isEmpty() && output.isEmpty()) {
                    for (int n = 2; n < PlayerInventory.MAIN_SIZE + 2; n++) {
                        ItemStack stack = cutter.getSlot(n).getStack();

                        if (!isValidItem(stack)) continue;
                        InvUtils.shiftClick().slotId(n);
                    }
                } else if (output.isEmpty()) {
                    CuttingRecipeDisplay.Grouping<StonecuttingRecipe> available = mc.world
                        .getRecipeManager().getStonecutterRecipes().filter(input);
                    ContextParameterMap contextParameterMap = SlotDisplayContexts.createParameters(mc.world);

                    boolean found = false;
                    for (int n = 0; n < available.entries().size(); n++) {
                        CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> entry = available.entries().get(n);
                        ItemStack recipeStack = entry.recipe().optionDisplay().getFirst(contextParameterMap);

                        if (recipeStack.isEmpty()) continue;
                        if (itemList.get().contains(recipeStack.getItem())) {
                            found = true;
                            cutter.onButtonClick(mc.player, n);
                            ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                                new ButtonClickC2SPacket(cutter.syncId, n), null, true
                            );
                            break;
                        }
                    }

                    if (!found) {
                        if (!notified) {
                            notified = true;
                            MsgUtil.sendModuleMsg("Desired recipe not found§c..!", this.name);
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), ThreadLocalRandom.current().nextFloat(0.69f, 1.337f));
                        }
                        finished();
                    }
                } else {
                    InvUtils.shiftClick().slotId(StonecutterScreenHandler.OUTPUT_ID);
                }
            }
        }
    }

    private void finished() {
        if (mc.player == null) return;
        if (!notified) {
            if (chatFeedback) MsgUtil.sendModuleMsg("No more items to craft§a..!", this.name);
            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), ThreadLocalRandom.current().nextFloat(0.69f, 1.337f));
        }
        notified = true;
        processedSlots.clear();
        projectedEmpty.clear();
        if (closeOnDone.get()) mc.player.closeHandledScreen();
        if (disableOnDone.get()) toggle();
    }

    private @Nullable Packet<?> generatePacket(StonecutterScreenHandler handler) {
        if (mc.player == null || mc.world == null) return null;
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();

        if (targetStack != null && outputStack != null) {
            // take output
            changedSlots.put(0, ItemStack.EMPTY);
            changedSlots.put(1, ItemStack.EMPTY);
            int shiftClickTargetSlot = predictEmptySlot(handler);

            if (shiftClickTargetSlot == -1) {
                MsgUtil.sendModuleMsg("Failed to predict empty target slot §8[§7Is your inventory full§3..?§8]§c..!", this.name);
                return null;
            }
            changedSlots.put(shiftClickTargetSlot, new ItemStack(outputStack.getItem(), targetStack.getCount()));

            targetStack = null;
            outputStack = null;
            return new ClickSlotC2SPacket(
                handler.syncId, handler.getRevision(), 1, 0,
                SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
            );
        } else if (targetStack != null) {
            // pick recipe
            CuttingRecipeDisplay.Grouping<StonecuttingRecipe> available = mc.world
                .getRecipeManager().getStonecutterRecipes().filter(targetStack);
            ContextParameterMap contextParameterMap = SlotDisplayContexts.createParameters(mc.world);

            for (int n = 0; n < available.entries().size(); n++) {
                var entry = available.entries().get(n);
                ItemStack recipeStack = entry.recipe().optionDisplay().getFirst(contextParameterMap);

                if (recipeStack.isEmpty()) continue;
                if (itemList.get().contains(recipeStack.getItem())) {
                    outputStack = recipeStack;
                    return new ButtonClickC2SPacket(handler.syncId, n);
                }
            }
        } else {
            // fill input slot
            for (int n = 2; n < PlayerInventory.MAIN_SIZE + 2; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getStack();
                if (!isValidItem(stack)) continue;

                targetStack = stack;
                processedSlots.add(0);
                processedSlots.add(n);
                projectedEmpty.add(n);
                changedSlots.put(0, stack);
                changedSlots.put(n, ItemStack.EMPTY);

                return new ClickSlotC2SPacket(
                    handler.syncId, handler.getRevision(), n, 0,
                    SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
                );
            }
        }

        return null;
    }

    private int predictEmptySlot(StonecutterScreenHandler handler) {
        if (mc.player == null) return -1;
        for (int n = PlayerInventory.MAIN_SIZE + 1; n >= 2; n--) {
            if (processedSlots.contains(n) && !projectedEmpty.contains(n)) continue;
            if (projectedEmpty.contains(n)) {
                projectedEmpty.rem(n);
                return n;
            } else if (handler.getSlot(n).getStack().isEmpty()) {
                processedSlots.add(n);
                return n;
            }
        }
        return -1;
    }

    private boolean hasValidItems(StonecutterScreenHandler handler) {
        if (mc.player == null) return false;
        for (int n = 0; n < PlayerInventory.MAIN_SIZE + 2; n++) {
            if (n == 1) continue; // skip output slot
            if (isValidItem(handler.getSlot(n).getStack())) return true;
        }
        return false;
    }

    private boolean isValidItem(ItemStack stack) {
        if (itemList.get().isEmpty()) return false;
        if (stack.isEmpty() || stack.isOf(Items.AIR)) return false;
        if (!StonecutterUtil.STONECUTTER_BLOCKS.containsKey(stack.getItem())) return false;

        return StonecutterUtil
            .STONECUTTER_BLOCKS
            .get(stack.getItem())
            .stream().anyMatch(item -> itemList.get().contains(item));
    }
}
