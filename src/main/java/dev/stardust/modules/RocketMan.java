package dev.stardust.modules;

import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.EquipmentSlot;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.enchantment.EnchantmentHelper;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.movement.TridentBoost;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class RocketMan extends Module {
    public RocketMan() {
        super(Stardust.CATEGORY, "RocketMan", "Makes flying with fireworks much easier (bring lots of rockets!)");
    }

    public static String[] rocketModes = {"W Key", "Spacebar", "Auto Use"};

    private final SettingGroup sgRockets = settings.createGroup("Rockets");
    private final SettingGroup sgControl = settings.createGroup("Control");
    private final SettingGroup sgSound = settings.createGroup("Sounds");

    // See TridentBoostMixin.java
    public final Setting<Boolean> tridentBoost = sgRockets.add(
        new BoolSetting.Builder()
            .name("Trident Boost")
            .description("Make use of the Trident Boost module for propulsion instead of rockets.")
            .defaultValue(false)
            .onChanged(it -> {
                TridentBoost boost = Modules.get().get(TridentBoost.class);
                if (it) {
                    if (!boost.isActive()) boost.toggle();
                }
            })
            .build()
    );

    private final Setting<String> usageMode = sgRockets.add(
        new ProvidedStringSetting.Builder()
            .name("Usage Mode")
            .description("Which mode to operate in.")
            .supplier(() -> rocketModes)
            .defaultValue("W Key")
            .build()
    );

    private final Setting<Boolean> keyboardControl = sgControl.add(
        new BoolSetting.Builder()
            .name("Keyboard Control")
            .description("Allows you to adjust your heading with WASD/Shift/Spacebar keys.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> freeLookOnly = sgControl.add(
        new BoolSetting.Builder()
            .name("Free Look Only")
            .description("Only allow keyboard control when Free Look module is active.")
            .defaultValue(false)
            .visible(keyboardControl::get)
            .build()
    );

    private final Setting<Boolean> invertPitch = sgControl.add(
        new BoolSetting.Builder()
            .name("Invert Pitch")
            .description("Invert pitch control for W & S keys.")
            .defaultValue(false)
            .visible(() -> keyboardControl.get() && !usageMode.get().equals("W Key"))
            .build()
    );

    private final Setting<Integer> yawSpeed = sgControl.add(
        new IntSetting.Builder()
            .name("Yaw Speed")
            .visible(keyboardControl::get)
            .sliderRange(0, 50)
            .range(0, 1000)
            .defaultValue(20)
            .build()
    );

    private final Setting<Integer> pitchSpeed = sgControl.add(
        new IntSetting.Builder()
            .name("Pitch Speed")
            .visible(keyboardControl::get)
            .sliderRange(0, 50)
            .range(0, 1000)
            .defaultValue(10)
            .build()
    );

    private final Setting<Boolean> autoEquip = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Auto-Equip")
            .description("Automatically equip an elytra when enabling the module.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> takeoff = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Takeoff Assist")
            .description("Assist takeoff by launching a rocket as soon as you deploy your elytra.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> usageCooldown = sgRockets.add(
        new IntSetting.Builder()
            .name("Rocket Usage Cooldown")
            .description("How often (in ticks) to allow using firework rockets (or tridents.)")
            .range(2, 1200)
            .sliderRange(2, 100)
            .defaultValue(10)
            .visible(() -> !usageMode.get().equals("Auto Use"))
            .build()
    );

    private final Setting<Integer> usageTickRate = sgRockets.add(
        new IntSetting.Builder()
            .name("Rocket Usage-Rate")
            .description("How often (in ticks) to use firework rockets (or tridents.)")
            .range(2, 1200)
            .sliderRange(2, 200)
            .defaultValue(50)
            .visible(() -> usageMode.get().equals("Auto Use"))
            .build()
    );

    private final Setting<Boolean> combatAssist = sgRockets.add(
        new BoolSetting.Builder()
            .name("Combat Assist")
            .description("Automatically launch a rocket after firing arrows, throwing tridents, or eating food.")
            .defaultValue(true)
            .onChanged(it -> ticksBusy = 0)
            .build()
    );

    private final Setting<Boolean> muteRockets = sgSound.add(
        new BoolSetting.Builder()
            .name("Mute Rockets")
            .description("Mute the firework rocket sounds.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> muteElytra = sgSound.add(
        new BoolSetting.Builder()
            .name("Mute Elytra")
            .description("Mute the elytra wind sounds.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> notifyOnLow = sgSound.add(
        new BoolSetting.Builder()
            .name("Warn Low Rockets")
            .description("Warn you audibly and/or in chat when you are low on rockets.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> notifyAudible = sgSound.add(
        new BoolSetting.Builder()
            .name("Low Rockets Sound")
            .defaultValue(false)
            .visible(notifyOnLow::get)
            .build()
    );

    private final Setting<Integer> notifyVolume = sgSound.add(
        new IntSetting.Builder()
            .name("Warn Volume")
            .sliderRange(0, 100)
            .defaultValue(100)
            .visible(() -> notifyOnLow.get() && notifyAudible.get())
            .build()
    );

    private final Setting<Integer> notifyAmount = sgSound.add(
        new IntSetting.Builder()
            .name("Low Rockets Threshold")
            .range(1, 384)
            .sliderRange(1, 128)
            .defaultValue(64)
            .visible(notifyOnLow::get)
            .build()
    );

    private final Setting<Boolean> warnOnLow = sgSound.add(
        new BoolSetting.Builder()
            .name("Warn Low Durability")
            .description("Warn you audibly and/or in chat when your elytra durability is low.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> warnAudible = sgSound.add(
        new BoolSetting.Builder()
            .name("Low Durability Sound")
            .defaultValue(false)
            .visible(warnOnLow::get)
            .build()
    );

    private final Setting<Integer> warnVolume = sgSound.add(
        new IntSetting.Builder()
            .name("Warn Volume")
            .sliderRange(0, 100)
            .defaultValue(100)
            .visible(() -> warnOnLow.get() && warnAudible.get())
            .build()
    );

    private final Setting<Integer> durabilityThreshold = sgSound.add(
        new IntSetting.Builder()
            .name("Durability % Threshold")
            .sliderRange(1, 99)
            .defaultValue(5)
            .visible(warnOnLow::get)
            .build()
    );

    private final Setting<Boolean> autoReplace = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Auto-Replace")
            .description("Automatically replace your elytra with a fresh one when it reaches a durability threshold.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> replaceThreshold = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Durability % Threshold")
            .sliderRange(1, 99)
            .defaultValue(5)
            .visible(autoReplace::get)
            .build()
    );

    private int timer = 0;
    private int ticksBusy = 0;
    private int ticksSinceUsed = 0;
    private int tridentHoldTicks = 0;
    private int ticksSinceWarned = 0;
    private int ticksSinceNotified = 0;
    private int tridentThrowGracePeriod = 0;
    private boolean justUsed = false;
    private boolean takingOff = false;
    public boolean chargingTrident = false;

    private boolean equippedTrident() {
        if (mc.player == null || mc.interactionManager == null) return false;

        ItemStack active = mc.player.getActiveItem();
        if (active.getItem() == Items.TRIDENT && EnchantmentHelper.get(active).containsKey(Enchantments.RIPTIDE)) return true;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if (stack.getItem() == Items.TRIDENT && EnchantmentHelper.get(stack).containsKey(Enchantments.RIPTIDE)) {
                if (n < 9) InvUtils.swap(n, false);
                else InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);
                return true;
            }
        }
        return false;
    }

    private void useTrident() {
        chargingTrident = true;
        mc.options.useKey.setPressed(true);
    }

    private void useFireworkRocket() {
        if (mc.player == null) return;
        if (mc.interactionManager == null) return;

        boolean foundRocket = false;
        for (int n = 0; n < 9; n++) {
            Item item = mc.player.getInventory().getStack(n).getItem();

            if (item == Items.FIREWORK_ROCKET) {
                InvUtils.swap(n, true);
                foundRocket = true;
                break;
            }
        }

        if (foundRocket) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
        }else {
            int movedSlot = -1;
            for (int n = 9; n < mc.player.getInventory().main.size(); n++) {
                Item item = mc.player.getInventory().getStack(n).getItem();

                if (item == Items.FIREWORK_ROCKET) {
                    InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);
                    movedSlot = n;
                    foundRocket = true;
                    break;
                }
            }

            if (foundRocket) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                //noinspection ConstantConditions
                if (movedSlot != -1) {
                    InvUtils.move().from(mc.player.getInventory().selectedSlot).to(movedSlot);
                }
            }
        }
    }

    private boolean replaceElytra() {
        if (mc.player == null) return false;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            ItemStack item = mc.player.getInventory().getStack(n);
            if (item.getItem() == Items.ELYTRA) {
                int max = item.getMaxDamage();
                int current = max - item.getDamage();
                double percent = Math.floor((current / (double) max) * 100);

                if (percent <= replaceThreshold.get()) continue;
                InvUtils.move().from(n).toArmor(2);
                return true;
            }
        }
        return false;
    }

    private void handleDurabilityChecks() {
        if (mc.player == null) return;
        if (!warnOnLow.get() && !autoReplace.get()) return;
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;

        ItemStack equippedElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        int maxDurability = equippedElytra.getMaxDamage();
        int currentDurability = maxDurability - equippedElytra.getDamage();
        double percentDurability = Math.floor((currentDurability / (double) maxDurability) * 100);

        if (autoReplace.get()) {
            if (percentDurability <= replaceThreshold.get()) {
                if (!replaceElytra() && warnOnLow.get()) {
                    if (ticksSinceWarned < 100) return;
                    if (percentDurability <= durabilityThreshold.get()) {
                        if (warnAudible.get()) {
                            float vol = warnVolume.get() / 100f;
                            mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK, vol, 1f);
                        }
                        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                            Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Elytra durability: §4"+percentDurability+"§7%"),
                            "Elytra durability warning".hashCode()
                        );
                        ticksSinceWarned = 0;
                    }
                }
            }
        } else if (warnOnLow.get()) {
            if (ticksSinceWarned < 100) return;
            if (percentDurability <= durabilityThreshold.get()) {
                if (warnAudible.get()) {
                    float vol = warnVolume.get() / 100f;
                    mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK, vol, 1f);
                }
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Elytra durability: §4"+percentDurability+"§7%"),
                    "Elytra durability warning".hashCode()
                );
                ticksSinceWarned = 0;
            }
        }
    }

    private void handleFireworkRocketChecks() {
        if (mc.player == null) return;
        if (ticksSinceNotified < 100) return;

        int totalRockets = 0;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if (stack.getItem() == Items.FIREWORK_ROCKET) {
                totalRockets += stack.getCount();
            }
        }

        if (totalRockets <= notifyAmount.get()) {
            if (notifyAudible.get()) {
                float vol = notifyVolume.get() / 100f;
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, vol, 1f);
            }
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Rockets remaining: §c"+totalRockets+"§7."),
                "Rockets remaining warning".hashCode()
            );
            ticksSinceNotified = 0;
        }
    }


    // See MinecraftClientMixin.java
    // We want to update movement on every frame instead of every tick for that buttery smooth experience
    public boolean shouldTickRotation() {
        if (mc.player == null) return false;
        if (freeLookOnly.get() && !Modules.get().get(FreeLook.class).isActive()) return false;
        return keyboardControl.get() && mc.player.isFallFlying();
    }

    public boolean shouldInvertPitch() {
        return invertPitch.isVisible() && invertPitch.get();
    }

    public String getUsageMode() {
        return usageMode.get();
    }

    public MinecraftClient getClientInstance() {
        return mc;
    }

    public int getPitchSpeed() {
        return pitchSpeed.get();
    }

    public int getYawSpeed() {
        return yawSpeed.get();
    }

    // See ClientPlayerEntityMixin.java && ElytraSoundInstanceMixin.java
    public boolean shouldMuteElytra() {
        if (mc.player == null) return false;
        return muteElytra.get() && mc.player.isFallFlying();
    }


    @Override
    public void onActivate() {
        timer = 0;
        tridentHoldTicks = 0;
        if (mc.player == null) return;
        boolean isWearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;

        if (tridentBoost.get()) {
            TridentBoost tb = Modules.get().get(TridentBoost.class);
            if (!tb.isActive()) tb.toggle();
        }

        if (!isWearingElytra) {
            if (autoEquip.get()) {
                boolean foundElytra = false;
                for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                    ItemStack stack = mc.player.getInventory().main.get(n);

                    if (stack.getItem() == Items.ELYTRA) {
                        if (autoReplace.get()) {
                            int max = stack.getMaxDamage();
                            int current = max - stack.getDamage();
                            double durability = Math.floor((current / (double) max) * 100);

                            if (durability <= replaceThreshold.get()) continue;
                        }
                        foundElytra = true;
                        InvUtils.move().from(n).toArmor(2);
                        break;
                    }
                }
                if (!foundElytra) {
                    ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                        Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> "+"§4No elytra in inventory!"),
                        "No elytra warning".hashCode()
                    );
                }
            } else {
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> "+"§4No elytra equipped!"),
                    "No elytra warning".hashCode()
                );
            }
        } else if (takeoff.get() && mc.player.isFallFlying()) {
            if (!tridentBoost.get() || !equippedTrident()) useFireworkRocket();
            else useTrident();
        }
    }

    @Override
    public void onDeactivate() {
        if (chargingTrident) {
            tridentHoldTicks = 0;
            chargingTrident = false;
            mc.options.useKey.setPressed(false);
            if (mc.currentScreen != null) mc.interactionManager.stopUsingItem(mc.player);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (chargingTrident) {
            if (!mc.player.isFallFlying()) {
                tridentHoldTicks = 0;
                chargingTrident = false;
                mc.options.useKey.setPressed(false);
                if (mc.currentScreen != null) mc.interactionManager.stopUsingItem(mc.player);
            } else {
                ++tridentHoldTicks;
                if (tridentHoldTicks > 10) {
                    tridentHoldTicks = 0;
                    chargingTrident = false;
                    mc.options.useKey.setPressed(false);
                    if (mc.currentScreen != null) mc.interactionManager.stopUsingItem(mc.player);
                } else {
                    ++timer;
                    ++ticksSinceWarned;
                    ++ticksSinceNotified;
                    mc.options.useKey.setPressed(true);
                    if (!mc.player.isUsingItem()) Utils.rightClick();
                    return;
                }
            }
        }

        Item activeItem = mc.player.getActiveItem().getItem();
        if (!tridentBoost.get()) {
            if (combatAssist.get() && (activeItem.isFood() || Utils.isThrowable(activeItem)) && mc.player.getItemUseTime() > 0) {
                ++ticksBusy;
                return;
            }else if (combatAssist.get() && ticksBusy >= 10 && mc.player.isFallFlying() && activeItem == Items.TRIDENT) {
                ++tridentThrowGracePeriod;
                if (tridentThrowGracePeriod >= 20) {
                    ticksBusy = 0;
                    useFireworkRocket();
                    tridentThrowGracePeriod = 0;
                }
            } else if (combatAssist.get() && ticksBusy >= 10 && mc.player.isFallFlying() && activeItem != Items.TRIDENT) {
                useFireworkRocket();
                ticksBusy = 0;
            }
        } else if (activeItem.isFood() && mc.player.getItemUseTime() > 0) {
            ++timer;
            ++ticksSinceWarned;
            ++ticksSinceNotified;
            return;
        }

        if (mc.player.isOnGround()) {
            takingOff = false;
        }else if (!takingOff && takeoff.get() && mc.player.isFallFlying()) {
            takingOff = true;
            if (!tridentBoost.get() || !equippedTrident()) useFireworkRocket();
            else useTrident();
        }else if (mc.player.isFallFlying()) {
            handleDurabilityChecks();
            handleFireworkRocketChecks();
        }

        ++timer;
        ++ticksSinceWarned;
        ++ticksSinceNotified;
        if (usageMode.get().equals("Auto Use") && mc.player.isFallFlying()) {
            if (timer >= usageTickRate.get()) {
                timer = 0;
                if (!tridentBoost.get() || !equippedTrident()) useFireworkRocket();
                else useTrident();
            }
        }else if (usageMode.get().equals("W Key") && mc.player.input.pressingForward && mc.player.isFallFlying() && !justUsed) {
            justUsed = true;
            if (!tridentBoost.get() || !equippedTrident()) useFireworkRocket();
            else useTrident();
        } else if (usageMode.get().equals("Spacebar") && mc.player.input.jumping && mc.player.isFallFlying() && !justUsed) {
            justUsed = true;
            if (!tridentBoost.get() || !equippedTrident()) useFireworkRocket();
            else useTrident();
        }else if (justUsed) {
            ++ticksSinceUsed;
            if (ticksSinceUsed >= usageCooldown.get()) {
                justUsed = false;
                ticksSinceUsed = 0;
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlaySoundS2CPacket packet)) return;

        if (packet.getSound().value() == SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH) {
            if (muteRockets.get()) event.cancel();
        }
    }

    @EventHandler
    private void onSentPacket(PacketEvent.Sent event) {
        if (mc.player == null) return;
        if (!tridentBoost.get() || !mc.player.isFallFlying()) return;
        if (!(event.packet instanceof TeleportConfirmC2SPacket)) return;

        if (usageMode.get().equals("Auto Use")) {
            timer = usageTickRate.get();
        } else timer = usageCooldown.get();

        tridentHoldTicks = 0;
        if (usageMode.get().equals("Auto Use")) {
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §c§oVelocity was reset by server§7§o.. §a§oResetting cooldown in response§7§o!"),
                "rocketmanTridentReset".hashCode()
            );
        }
        useTrident();
    }
}
