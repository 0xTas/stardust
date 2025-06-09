package dev.stardust.modules;

import dev.stardust.Stardust;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import dev.stardust.util.MsgUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import net.minecraft.registry.tag.ItemTags;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class RocketJump extends Module {
    public RocketJump() {super(Stardust.CATEGORY, "RocketJump", "Rocket-boosted jumps (requires an elytra)."); }

    private final Setting<Boolean> preferChestplate = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("prefer-chestplate")
            .description("If enabled, will always try to re-equip a chestplate no matter what you were wearing when the module was enabled.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> swapBackTicks = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("swap-delay-ticks")
            .min(0).sliderRange(0, 20)
            .defaultValue(3)
            .build()
    );

    private int timer = -1;
    private int swapSlot = -1;
    private boolean jumped = false;
    private boolean jumping = false;
    private @Nullable Item chestplate = null;

    private void starFlying() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.player.startGliding();
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    private void useRocket() {
        if (mc.player == null) return;
        int rocketSlot = getRocketSlot();
        if (rocketSlot == -1) {
            MsgUtil.sendModuleMsg("No rockets found§c..!", this.name);
            toggle();
            sendToggledMsg();
            return;
        }
        if (rocketSlot != mc.player.getInventory().selectedSlot) {
            if (rocketSlot < 9) InvUtils.swap(rocketSlot, true);
            else {
                InvUtils.move().from(rocketSlot).to(mc.player.getInventory().selectedSlot);
            }
        }
        if (mc.interactionManager != null) mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (rocketSlot < 9) InvUtils.swapBack();
        else InvUtils.move().from(mc.player.getInventory().selectedSlot).to(rocketSlot);
    }

    private int getRocketSlot() {
        FindItemResult rockets = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!rockets.found()) {
            rockets = InvUtils.find(Items.FIREWORK_ROCKET);
            if (!rockets.found()) {
                return -1;
            }
        }

        return rockets.slot();
    }

    private boolean hasActiveRocket() {
        if (mc.world == null) return false;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof FireworkRocketEntity r && r.getOwner() != null && r.getOwner() != null && r.getOwner().equals(mc.player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) {
            toggle();
            return;
        }
        if (getRocketSlot() == -1) {
            MsgUtil.sendModuleMsg("No rockets found§c..!", this.name);
            toggle();
            sendToggledMsg();
            return;
        }

        jumping = true;
    }

    @Override
    public void onDeactivate() {
        timer = -1;
        swapSlot = -1;
        jumped = false;
        jumping = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || !jumping) return;
        boolean wearingSomething = !mc.player.getEquippedStack(EquipmentSlot.CHEST).isEmpty();

        if (!jumped && wearingSomething) {
            boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)
                && mc.player.getEquippedStack(EquipmentSlot.CHEST).getDamage() < Items.ELYTRA.getDefaultStack().getMaxDamage();

            if (wearingElytra) {
                if (mc.player.isGliding()) {
                    jumped = true;
                    swapSlot = -69;
                    if (!hasActiveRocket()) useRocket();
                    if (timer == -1) {
                        timer = swapBackTicks.get();
                    }
                } else if (mc.player.isOnGround()) {
                    mc.player.jump();
                    return;
                } else {
                    starFlying();
                    return;
                }
            } else {
                chestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();
                FindItemResult elytra = InvUtils.find(stack -> stack.isOf(Items.ELYTRA) && stack.getDamage() < stack.getMaxDamage());
                if (!elytra.found()) {
                    MsgUtil.sendModuleMsg("No good elytra found§c..!", this.name);
                    toggle();
                    sendToggledMsg();
                    return;
                }

                swapSlot = elytra.slot();
                InvUtils.move().from(elytra.slot()).toArmor(2);
                return;
            }
        } else if (!jumped) {
            FindItemResult elytra = InvUtils.find(stack -> stack.isOf(Items.ELYTRA) && stack.getDamage() < stack.getMaxDamage());
            if (!elytra.found()) {
                MsgUtil.sendModuleMsg("No good elytra found§c..!", this.name);
                toggle();
                sendToggledMsg();
                return;
            }

            if (preferChestplate.get()) {
                boolean found = false;
                if (chestplate != null) for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                    ItemStack stack = mc.player.getInventory().getStack(n);
                    if (stack.isOf(chestplate)) {
                        swapSlot = n;
                        found = true;
                        break;
                    }
                }
                if (!found) for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                    ItemStack stack = mc.player.getInventory().getStack(n);
                    if (stack.isIn(ItemTags.CHEST_ARMOR)) {
                        swapSlot = n;
                        found = true;
                        break;
                    }
                }
                if (!found) swapSlot = elytra.slot();
            } else {
                swapSlot = elytra.slot();
            }
            InvUtils.move().from(elytra.slot()).toArmor(2);
            return;
        }

        if (swapSlot != -1) {
            --timer;
            if (timer <= 0) {
                if (swapSlot == -69) {
                    if (preferChestplate.get()) {
                        if (chestplate != null) for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                            ItemStack stack = mc.player.getInventory().getStack(n);
                            if (stack.isOf(chestplate)) {
                                InvUtils.move().fromArmor(2).to(n);
                                toggle();
                                sendToggledMsg();
                                return;
                            }
                        }
                        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                            ItemStack stack = mc.player.getInventory().getStack(n);
                            if (stack.isIn(ItemTags.CHEST_ARMOR)) {
                                InvUtils.move().fromArmor(2).to(n);
                                toggle();
                                sendToggledMsg();
                                return;
                            }
                        }
                    }

                    swapSlot = -420;
                    InvUtils.click().slotArmor(2);
                } else if (swapSlot == -420) {
                    InvUtils.click().slotArmor(2);
                    toggle();
                    sendToggledMsg();
                } else {
                    InvUtils.move().fromArmor(2).to(swapSlot);
                    toggle();
                    sendToggledMsg();
                }
            }
        }
    }
}
