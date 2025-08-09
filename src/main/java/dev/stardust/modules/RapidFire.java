package dev.stardust.modules;
import dev.stardust.mixin.accessor.PlayerInventoryAccessor;

import java.util.List;
import org.lwjgl.glfw.GLFW;
import dev.stardust.Stardust;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import dev.stardust.util.MsgUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.tag.ItemTags;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.collection.ArrayListDeque;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class RapidFire extends Module {
    public RapidFire() { super(Stardust.CATEGORY, "RapidFire", "Rapidly fires every crossbow you've got.");}

    private final Setting<Boolean> autoLoad = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("auto-reload")
            .description("Automatically recharge crossbows while holding them.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoCycleReload = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("reload-entire-hotbar")
            .description("Automatically recharge all crossbows in hotbar after rapid-firing.")
            .defaultValue(true)
            .visible(autoLoad::get)
            .build()
    );

    private final Setting<Boolean> fromInventory = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("from-inventory")
            .description("Move crossbows from your inventory to your hotbar when firing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoFire = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("auto-fire")
            .description("Automatically fire your held crossbow as soon as it is loaded.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Item>> swapBlacklist = settings.getDefaultGroup().add(
        new ItemListSetting.Builder()
            .name("swap-blacklist")
            .description("Don't replace hotbar slots with crossbows if they contain these items.")
            .defaultValue(List.of(Items.NETHERITE_PICKAXE, Items.NETHERITE_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD, Items.GOLDEN_CARROT, Items.ENCHANTED_GOLDEN_APPLE))
            .visible(fromInventory::get)
            .build()
    );

    public final Setting<Keybind> fireKey = settings.getDefaultGroup().add(
        new KeybindSetting.Builder()
            .name("fire-key")
            .description("The key you want to press to fire every crossbow in your hotbar/inventory.")
            .defaultValue(Keybind.fromKeys(GLFW.GLFW_KEY_V, GLFW.GLFW_MOD_CONTROL))
            .build()
    );

    private final Setting<Integer> tickDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("tick-delay")
            .range(0, 100).sliderRange(0, 20)
            .defaultValue(10)
            .build()
    );

    private int timer = 0;
    private boolean firing = false;
    private final ArrayListDeque<Integer> jobList = new ArrayListDeque<>();

    // See ClientPlayerInteractionManagerMixin.java
    public boolean charging = false;

    @Override
    public void onDeactivate() {
        timer = 0;
        firing = false;
        jobList.clear();
        charging = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;
        boolean hasAmmo = InvUtils.find(stack -> stack.isIn(ItemTags.ARROWS)).found();
        if (fireKey.get().isPressed() && !firing) {
            firing = true;
            if (fromInventory.get()) {
                int[] hotbarSlots = new int[9];
                for (int n = 0; n < 9; n++) {
                    ItemStack stack = mc.player.getInventory().getStack(n);
                    if (stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
                        hotbarSlots[n] = 1;
                    }
                }

                int i = 0;
                for (int slot : hotbarSlots) {
                    ItemStack current = mc.player.getInventory().getStack(i);
                    if (slot == 1 || swapBlacklist.get().contains(current.getItem())) {
                        i++;
                        continue;
                    }
                    for (int n = 9; n < mc.player.getInventory().size(); n++) {
                        ItemStack stack = mc.player.getInventory().getStack(n);
                        if (stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
                            InvUtils.move().from(n).to(i);
                            break;
                        }
                    }
                    i++;
                }
            }

            for (int n = 0; n < 9; n++) {
                ItemStack stack = mc.player.getInventory().getStack(n);
                if (stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
                    jobList.addLast(n);
                }
            }
        }else if (firing && !jobList.isEmpty()) {
            ++timer;
            if (timer >= tickDelay.get()) {
                timer = 0;
                InvUtils.swap(jobList.removeFirst(), false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        } else {
            firing = false;
            ItemStack current = mc.player.getMainHandStack();
            if (current.getItem() != Items.CROSSBOW) {
                charging = false;
                return;
            }
            if (charging) {
                int useTime = mc.player.getItemUseTime();
                float pullTime = MathHelper.floor(EnchantmentHelper.getCrossbowChargeTime(current, mc.player, 1.25F) * 20);
                if (useTime / pullTime > 1) {
                    charging = false;
                    mc.interactionManager.stopUsingItem(mc.player);
                } else if (!hasAmmo && mc.player.getOffHandStack().getItem() != Items.FIREWORK_ROCKET) {
                    charging = false;
                    if (chatFeedback) MsgUtil.updateModuleMsg("Out of ammo§4..!", this.name, "rapidFireAmmo".hashCode());
                }
            } else if (autoLoad.get() && !CrossbowItem.isCharged(current)) {
                if (hasAmmo || mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
                    charging = true;
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                } else if (chatFeedback) MsgUtil.updateModuleMsg("Out of ammo§4..!", this.name, "rapidFireAmmo".hashCode());
            }  else if (autoFire.get() && CrossbowItem.isCharged(current)) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            } else if (autoCycleReload.get() && CrossbowItem.isCharged(current)) {
                if (hasAmmo || mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
                    int slot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
                    if (slot >= 0) {
                        for (int n = slot; n >= 0; n--) {
                            ItemStack stack = mc.player.getInventory().getStack(n);
                            if (stack.getItem() == Items.CROSSBOW && !CrossbowItem.isCharged(stack)) {
                                InvUtils.swap(n, false);
                                break;
                            }
                        }
                    }
                } else if (chatFeedback) MsgUtil.updateModuleMsg("Out of ammo§4..!", this.name, "rapidFireAmmo".hashCode());
            }
        }
    }
}
