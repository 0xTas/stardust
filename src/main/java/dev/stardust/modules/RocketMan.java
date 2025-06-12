package dev.stardust.modules;

import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.tag.FluidTags;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import meteordevelopment.orbit.EventPriority;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.component.DataComponentTypes;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import dev.stardust.mixin.accessor.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class RocketMan extends Module {
    public RocketMan() {
        super(Stardust.CATEGORY, "RocketMan", "Enhanced elytra flight using firework rockets.");
    }

    public enum KeyModifiers { Alt, Ctrl, Shift, None }
    public enum HoverMode { Off, Hold, Toggle, Creative }
    public enum RocketMode {OnKey, Static, Dynamic, Speed }
    public enum TakeoffMode { None, Full, Partial, DeployElytra, UseRocket, Jump}

    private final SettingGroup sgRockets = settings.createGroup("Rocket Usage");
    private final SettingGroup sgBoosts = settings.createGroup("Rocket Boosts");
    private final SettingGroup sgHover = settings.createGroup("Hover Modes");
    private final SettingGroup sgControl = settings.createGroup("Control Modes");
    private final SettingGroup sgScroll = settings.createGroup("Scroll Wheel Speed");
    private final SettingGroup sgSound = settings.createGroup("Sounds & Notifications");

    public final Setting<RocketMode> usageMode = sgRockets.add(
        new EnumSetting.Builder<RocketMode>()
            .name("usage-mode")
            .description("Which mode to operate in.")
            .defaultValue(RocketMode.OnKey)
            .build()
    );

    public final Setting<Keybind> usageKey = sgRockets.add(
        new KeybindSetting.Builder()
            .name("rocket-key")
            .description("The key you want to press to use a rocket.")
            .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_W))
            .visible(() -> usageMode.get().equals(RocketMode.OnKey))
            .build()
    );

    private final Setting<Integer> usageCooldown = sgRockets.add(
        new IntSetting.Builder()
            .name("rocket-usage-cooldown")
            .description("How often (in ticks) to allow using firework rockets.")
            .range(1, 10000).sliderRange(2, 100).defaultValue(40)
            .visible(() -> usageMode.get().equals(RocketMode.OnKey) || usageMode.get().equals(RocketMode.Speed))
            .build()
    );

    private final Setting<Double> usageSpeed = sgRockets.add(
        new DoubleSetting.Builder()
            .name("minimum-speed-threshold-(b/s)")
            .description("Will use a rocket when your speed falls below this threshold.")
            .range(1, 1000).sliderRange(2, 100).defaultValue(24)
            .visible(() -> usageMode.get().equals(RocketMode.Speed))
            .build()
    );

    private final Setting<Integer> usageTickRate = sgRockets.add(
        new IntSetting.Builder()
            .name("rocket-usage-rate")
            .description("How often (in ticks) to use firework rockets.")
            .range(1, 10000).sliderRange(2, 420).defaultValue(100)
            .visible(() -> usageMode.get().equals(RocketMode.Static))
            .build()
    );

    public final Setting<Boolean> yLevelLock = sgRockets.add(
        new BoolSetting.Builder()
            .name("y-level-lock")
            .description("Lock Your Y level while flying (requires Dynamic mode.)")
            .defaultValue(false)
            .visible(() -> usageMode.get().equals(RocketMode.Dynamic))
            .build()
    );

    private final Setting<Boolean> combatAssist = sgRockets.add(
        new BoolSetting.Builder()
            .name("combat-assist")
            .description("Automatically launch a rocket after firing arrows, throwing tridents, or eating food.")
            .defaultValue(false)
            .onChanged(it -> ticksBusy = 0)
            .build()
    );

    public final Setting<Boolean> boostSpeed = sgBoosts.add(
        new BoolSetting.Builder()
            .name("speed-boost")
            .description("Boost the speed of your firework rockets.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Double> speedSetting = sgBoosts.add(
        new DoubleSetting.Builder()
            .name("speed")
            .description("How much to boost your rocket speed by (maximum.)")
            .range(0, 100)
            .sliderRange(0, 20)
            .defaultValue(3.3237)
            .visible(boostSpeed::get)
            .build()
    );

    public final Setting<Double> accelerationSetting = sgBoosts.add(
        new DoubleSetting.Builder()
            .name("acceleration")
            .description("Acceleration Speed: lower values make grim less angry, but also soft-cap your max speed.")
            .range(0, 10)
            .sliderRange(0, 2)
            .defaultValue(0.3777)
            .visible(boostSpeed::get)
            .build()
    );

    private final Setting<Double> rocketSpeedThreshold = sgBoosts.add(
        new DoubleSetting.Builder()
            .name("acceleration-backoff-threshold")
            .description("Backs down on acceleration when you've slowed down enough since using your last rocket (to prevent rubberbanding.)")
            .min(0).max(2).defaultValue(.69)
            .visible(boostSpeed::get)
            .build()
    );

    public final Setting<Boolean> extendRockets = sgBoosts.add(
        new BoolSetting.Builder()
            .name("boost-rocket-duration")
            .description("Extend the duration of your rocket's boost effect.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> extendedDuration = sgBoosts.add(
        new DoubleSetting.Builder()
            .name("max-duration")
            .description("Maximum amount in seconds to extend your firework's boost duration by.")
            .range(0.01, 420).sliderRange(0.1, 69.0).defaultValue(0.90)
            .visible(extendRockets::get)
            .build()
    );

    private final Setting<Integer> extensionRange = sgBoosts.add(
        new IntSetting.Builder()
            .name("extension-range")
            .description("Max range from usage point before refreshing your rocket, to prevent lagbacks.")
            .range(1, 1024).sliderRange(2, 512).defaultValue(200)
            .visible(extendRockets::get)
            .build()
    );

    private final Setting<Boolean> keyboardControl = sgControl.add(
        new BoolSetting.Builder()
            .name("keyboard-control")
            .description("Allows you to adjust your heading with WASD/Shift/Spacebar keys.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> freeLookOnly = sgControl.add(
        new BoolSetting.Builder()
            .name("free-look-only")
            .description("Only allow rotation control when Free Look module is active.")
            .defaultValue(true)
            .visible(keyboardControl::get)
            .build()
    );

    private final Setting<Boolean> invertPitch = sgControl.add(
        new BoolSetting.Builder()
            .name("invert-pitch")
            .description("Invert pitch control for W & S keys.")
            .defaultValue(false)
            .visible(() -> keyboardControl.get() && !usageMode.get().equals(RocketMode.OnKey))
            .build()
    );

    private final Setting<Integer> pitchSpeed = sgControl.add(
        new IntSetting.Builder()
            .name("pitch-speed")
            .visible(keyboardControl::get)
            .range(0, 1000).sliderRange(0, 50).defaultValue(10)
            .build()
    );

    private final Setting<Integer> yawSpeed = sgControl.add(
        new IntSetting.Builder()
            .name("yaw-speed")
            .visible(keyboardControl::get)
            .range(0, 1000).sliderRange(0, 50).defaultValue(20)
            .build()
    );

    public final Setting<HoverMode> hoverMode = sgHover.add(
        new EnumSetting.Builder<HoverMode>()
            .name("hover-mode")
            .description("Allows you to hover by pressing the backwards movement key (or by default in creative mode.)")
            .defaultValue(HoverMode.Toggle)
            .build()
    );

    public final Setting<Keybind> hoverKey = sgHover.add(
        new KeybindSetting.Builder()
            .name("hover-keybind")
            .description("The key you want to press or hold to initiate hover mode.")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<KeyModifiers> modifierKey = sgHover.add(
        new EnumSetting.Builder<KeyModifiers>()
            .name("modifier-key")
            .description("Require a modifier key to be held down alongside your hover keybind.")
            .defaultValue(KeyModifiers.None)
            .build()
    );

    public final Setting<Boolean> forceRocketUsage = sgHover.add(
        new BoolSetting.Builder()
            .name("force-rocket-usage")
            .description("Force rocket usage when hovering.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> verticalSpeed = sgHover.add(
        new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("Change your Y level with shift or space (pitch control moves to arrow keys.)")
            .min(0.0).max(10.0).defaultValue(0.69)
            .build()
    );

    public final Setting<Double> horizontalSpeed = sgHover.add(
        new DoubleSetting.Builder()
            .name("horizontal-speed")
            .description("For use with creative hover mode (yaw control moves to arrow keys.)")
            .min(0.0).max(10.0).defaultValue(0.69)
            .build()
    );

    private final Setting<Double> maxSpeedScrollSensitivity = sgScroll.add(
        new DoubleSetting.Builder()
            .name("scroll-sensitivity-(Max-Speed)")
            .description("Change your max speed by holding ctrl and scrolling the mouse wheel.")
            .min(0).max(2).defaultValue(.25)
            .build()
    );

    private final Setting<Double> accelerationScrollSensitivity = sgScroll.add(
        new DoubleSetting.Builder()
            .name("scroll-sensitivity-(Acceleration)")
            .description("Change your acceleration speed by holding alt and scrolling the mouse wheel.")
            .min(0).max(2).defaultValue(.025)
            .build()
    );

    private final Setting<Double> verticalScrollSensitivity = sgScroll.add(
        new DoubleSetting.Builder()
            .name("scroll-sensitivity-(Vertical-Hover)")
            .description("Change your vertical hover speed by holding ctrl and scrolling the mouse wheel.")
            .min(0).max(2).defaultValue(.025)
            .build()
    );

    private final Setting<Double> horizontalScrollSensitivity = sgScroll.add(
        new DoubleSetting.Builder()
            .name("scroll-sensitivity-(Horizontal-Hover)")
            .description("Change your horizontal hover speed by holding alt and scrolling the mouse wheel.")
            .min(0).max(2).defaultValue(.025)
            .build()
    );

    private final Setting<Boolean> muteRockets = sgSound.add(
        new BoolSetting.Builder()
            .name("mute-rockets")
            .description("Mute the firework rocket sounds.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> muteElytra = sgSound.add(
        new BoolSetting.Builder()
            .name("mute-elytra")
            .description("Mute the elytra wind sounds.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> notifyOnLow = sgSound.add(
        new BoolSetting.Builder()
            .name("warn-low-rockets")
            .description("Warn you audibly and/or in chat when you are low on rockets.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> notifyVolume = sgSound.add(
        new IntSetting.Builder()
            .name("low-rockets-volume")
            .sliderRange(0, 100)
            .defaultValue(37)
            .visible(notifyOnLow::get)
            .build()
    );

    private final Setting<Integer> notifyAmount = sgSound.add(
        new IntSetting.Builder()
            .name("low-rockets-threshold")
            .range(1, 384)
            .sliderRange(1, 128)
            .defaultValue(64)
            .visible(notifyOnLow::get)
            .build()
    );

    private final Setting<Boolean> warnOnLow = sgSound.add(
        new BoolSetting.Builder()
            .name("warn-low-durability")
            .description("Warn you audibly and/or in chat when your elytra durability is low.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> warnVolume = sgSound.add(
        new IntSetting.Builder()
            .name("low-durability-volume")
            .sliderRange(0, 100)
            .defaultValue(50)
            .visible(warnOnLow::get)
            .build()
    );

    private final Setting<Integer> durabilityThreshold = sgSound.add(
        new IntSetting.Builder()
            .name("low-durability-threshold-%")
            .sliderRange(1, 99)
            .defaultValue(5)
            .visible(warnOnLow::get)
            .build()
    );

    public final Setting<Boolean> durationFeedback = sgSound.add(
        new BoolSetting.Builder()
            .name("duration-feedback")
            .description("Display a message in chat indicating the duration of your currently-boosted rocket.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> scrollSpeedFeedback = sgSound.add(
        new BoolSetting.Builder()
            .name("scroll-speed-feedback")
            .description("Display a message in chat indicating speed value changes triggered by the scroll wheel.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiLagBackFeedback = sgSound.add(
        new BoolSetting.Builder()
            .name("antiLagBack-feedback")
            .description("Display a message in chat indicating when AntiLagBack is preventing you from being stuck in a rubberbanding loop.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> hoverModeFeedback = sgSound.add(
        new BoolSetting.Builder()
            .name("hover-mode-feedback")
            .description("Display a message in chat indicating when hover mode is toggled.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnLand = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("disable-on-land")
            .description("Disable the module upon landing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> syncInventory = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("sync-inventory")
            .description("Automatically open and close the inv when joining the game, to prevent inventory desync when afk reconnecting.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoEquip = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("auto-equip")
            .description("Automatically equip an elytra when enabling the module.")
            .defaultValue(false)
            .build()
    );

    private final Setting<TakeoffMode> takeoff = settings.getDefaultGroup().add(
        new EnumSetting.Builder<TakeoffMode>()
            .name("takeoff-assist-mode")
            .defaultValue(TakeoffMode.Full)
            .build()
    );

    private final Setting<Integer> assistCooldown = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("takeoff-assist-cooldown")
            .description("How long to wait before attempting another takeoff assist. Set to 0 to disable.")
            .sliderRange(0, 100)
            .min(-1).defaultValue(40)
            .visible(() -> !takeoff.get().equals(TakeoffMode.None))
            .build()
    );

    private final Setting<Boolean> autoReplace = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("elytra-replace")
            .description("Automatically replace your elytra with a fresh one when it reaches a durability threshold.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> replaceThreshold = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("durability-%-threshold")
            .sliderRange(1, 99)
            .defaultValue(5)
            .visible(autoReplace::get)
            .build()
    );

    private final Setting<Boolean> escapeLava = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("attempt-escape-lava")
            .description("Automatically pitch upwards and fire a rocket to fly out of lava pools in the nether.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> lagBackDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("lagBack-delay")
            .description("How many ticks to chill out for when AntiLagBack is triggered.")
            .range(1, 1200).sliderRange(10, 69).defaultValue(37)
            .build()
    );

    public final Setting<Boolean> debug = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("debug")
            .description("Print various debug messages to your chat (useful for configuring & debugging the module.)")
            .defaultValue(false)
            .visible(() -> false) // toggle via settings command only
            .build()
    );

    private int timer = 0;
    private int ticksBusy = 0;
    private int hoverTimer = 0;
    private int ticksFlying = 0;
    private int setbackTimer = 0;
    private int ticksSinceUsed = 0;
    private int setbackCounter = 0;
    private int rocketStockTicks = 0;
    private int durabilityCheckTicks = 0;
    private double rocketBoostSpeed = 1.5;
    private int tridentThrowGracePeriod = 0;
    private float lastPlayerPitch = -420.69f;
    private boolean inLava = false;
    private boolean synced = false;
    private boolean boosted = false;
    private boolean justUsed = false;
    private boolean assisted = false;
    private boolean needReset = false;
    private boolean takingOff = false;
    public boolean isHovering = false;
    public boolean wasHovering = false;
    private boolean firstRocket = false;
    public boolean durationBoosted = false;
    private String rcc = StardustUtil.rCC();
    private long assistTimer = assistCooldown.get();
    public @Nullable Long extensionStartTime = null;
    public @Nullable BlockPos extensionStartPos = null;
    public @Nullable FireworkRocketEntity currentRocket = null;
    private final ArrayList<CommonPongC2SPacket> pongQueue = new ArrayList<>();

    private void useFireworkRocket(String caller) {
        if (mc.player == null) return;
        if (mc.interactionManager == null) return;
        if (debug.get() && chatFeedback) mc.player.sendMessage(Text.literal("§7Caller: "+StardustUtil.rCC()+caller), false);

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
            timer = 0;
            justUsed = true;
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
                timer = 0;
                justUsed = true;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                //noinspection ConstantConditions
                if (movedSlot != -1) {
                    InvUtils.move().from(mc.player.getInventory().selectedSlot).to(movedSlot);
                }
            }
        }
    }

    public void discardCurrentRocket(String source) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!source.trim().isEmpty() && debug.get() && chatFeedback) {
            mc.player.sendMessage(
                Text.literal("§7Discarding current rocket! Why: "
                    +StardustUtil.rCC()+source+" §7| Packets: "
                    +StardustUtil.rCC()+pongQueue.size()
                ), false
            );
        }

        if (firstRocket) firstRocket = false;
        if (currentRocket != null && durationBoosted) {
            currentRocket.discard();
        }
        durationBoosted = false;
        extensionStartPos = null;
        extensionStartTime = null;

        if (extendRockets.get() && !pongQueue.isEmpty()) {
            for (CommonPongC2SPacket pong : pongQueue) {
                mc.getNetworkHandler().sendPacket(pong);
            }
            pongQueue.clear();
        }
    }

    public boolean hasActiveRocket() {
        if (mc.world == null) return false;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof FireworkRocketEntity r && r.getOwner() != null && r.getOwner().equals(mc.player)) {
                return true;
            }
        }
        return false;
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
                    if (durabilityCheckTicks < 100) return;
                    if (percentDurability <= durabilityThreshold.get()) {
                        float vol = warnVolume.get() / 100f;
                        mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK, vol, 1f);
                        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                            Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Elytra durability: §4"+percentDurability+"§7%"),
                            "Elytra durability warning".hashCode()
                        );
                        durabilityCheckTicks = 0;
                    }
                }
            }
        } else if (warnOnLow.get()) {
            if (durabilityCheckTicks < 100) return;
            if (percentDurability <= durabilityThreshold.get()) {
                float vol = warnVolume.get() / 100f;
                mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK, vol, 1f);
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Elytra durability: §4"+percentDurability+"§7%"),
                    "Elytra durability warning".hashCode()
                );
                durabilityCheckTicks = 0;
            }
        }
    }

    private void handleFireworkRocketChecks() {
        if (mc.player == null) return;
        if (!notifyOnLow.get() || rocketStockTicks < 100) return;

        int totalRockets = 0;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if (stack.getItem() == Items.FIREWORK_ROCKET) {
                totalRockets += stack.getCount();
            }
        }

        if (totalRockets < notifyAmount.get()) {
            float vol = notifyVolume.get() / 100f;
            mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, vol, 1f);
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Rockets remaining: §c"+totalRockets+"§7."),
                "Rockets remaining warning".hashCode()
            );
            rocketStockTicks = 0;
        }
    }

    private void assistTakeoff() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (escapeLava.get() && PlayerUtils.getDimension().equals(Dimension.Nether) && mc.player.isSubmergedIn(FluidTags.LAVA)) {
            inLava = true;
            if (lastPlayerPitch == -420.69f) {
                lastPlayerPitch = mc.player.getPitch();
                mc.player.setPitch(-75);
            }
            mc.options.jumpKey.setPressed(true);
            if (mc.player.isGliding() && !justUsed) {
                assisted = true;
                justUsed = true;
                takingOff = true;
                useFireworkRocket("full lava escape");
            } else if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                mc.player.startGliding();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        } else switch (takeoff.get()) {
            case None -> assisted = true;
            case Full -> {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                } else if (!mc.player.isGliding() && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    mc.player.startGliding();
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                } else if (!justUsed) {
                    assisted = true;
                    justUsed = true;
                    takingOff = true;
                    useFireworkRocket("full takeoff assist");
                }
            }
            case Jump -> {
                if (mc.player.isGliding()) assisted = true;
                else if (mc.player.isOnGround()) {
                    mc.player.jump();
                }
            }
            case Partial -> {
                if (mc.player.isGliding() && !justUsed) {
                    assisted = true;
                    justUsed = true;
                    takingOff = true;
                    useFireworkRocket("partial takeoff assist");
                } else if (!mc.player.isOnGround() && !mc.player.isGliding() && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    mc.player.startGliding();
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                }
            }
            case UseRocket -> {
                if (mc.player.isGliding() && !justUsed) {
                    justUsed = true;
                    takingOff = true;
                    useFireworkRocket("rocket takeoff assist");
                }
            }
            case DeployElytra -> {
                if (!mc.player.isOnGround() && !mc.player.isGliding() && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    assisted = true;
                    takingOff = true;
                    mc.player.startGliding();
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                }
            }
        }
    }

    // See PlayerEntityMixin.java
    public boolean isHoverKeyPressed() {
        return switch (modifierKey.get()) {
            case None -> hoverKey.get().isPressed();
            case Alt -> Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_ALT) && hoverKey.get().isPressed();
            case Shift -> Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) && hoverKey.get().isPressed();
            case Ctrl -> Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) && hoverKey.get().isPressed();
        };
    }

    // See EntityMixin.java && LivingEntityMixin.java && PlayerEntityMixin.java
    public boolean shouldLockYLevel() {
        return usageMode.get().equals(RocketMode.Dynamic) && yLevelLock.get();
    }

    // See PlayerEntityMixin.java
    public void setIsHovering(boolean hovering) {
        isHovering = hovering;
        if (hoverModeFeedback.get() && chatFeedback) {
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                Text.of("§8<"+ StardustUtil.rCC()+"§o✨§r§8> "+"§7Hover Mode "+ (hovering ? "§2§oEnabled§7§o." : "§4§oDisabled§7.")),
                "hover mode feedback".hashCode()
            );
        }
    }

    // See MinecraftClientMixin.java
    // We want to update movement on every frame instead of every tick for that buttery smooth experience
    public boolean shouldTickRotation() {
        if (mc.player == null) return false;
        if (freeLookOnly.get() && !Modules.get().get(FreeLook.class).isActive()) return false;
        return (keyboardControl.get() || isHovering) && mc.player.isGliding();
    }

    public boolean shouldInvertPitch() {
        return invertPitch.isVisible() && invertPitch.get();
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
        return muteElytra.get() && mc.player.isGliding();
    }

    // See FireworkRocketEntityMixin.java
    public double getRocketBoostAcceleration() {
        if (isHovering) return 0.0;
        double maxSpeed = speedSetting.get();
        double currentBps = Math.round(Utils.getPlayerSpeed().length() * 100) * .01;
        double increment = shouldLockYLevel() ? accelerationSetting.get() * 2 : accelerationSetting.get();

        if (currentBps <= 0.0 || needReset) return Math.min(1.5, maxSpeed);
        if (currentBps >= 33.63) {
            boosted = true;
            double expectedBps = Math.round(((33.63 * rocketBoostSpeed) / 1.5) * 100) * .01;
            if (currentBps <= expectedBps * rocketSpeedThreshold.get()) {
                rocketBoostSpeed = ((currentBps * 1.5) / 33.63) + increment;
            } else {
                rocketBoostSpeed = Math.min(rocketBoostSpeed + increment, maxSpeed);
            }
        } else if (boosted) {
            boosted = false;
            rocketBoostSpeed = Math.min(1.5 + increment, maxSpeed);
        } else {
            if (firstRocket) { return Math.min(1.5, maxSpeed); }
            else rocketBoostSpeed = Math.min(rocketBoostSpeed + increment, maxSpeed);
        }
        return rocketBoostSpeed;
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;
        if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getPlayerList().size() <= 1) return; // Ignore queue
        boolean isWearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;

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
        } else if (!takingOff && !assisted) assistTakeoff();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        synced = false;
        hoverTimer = 0;
        boosted = false;
        setbackTimer = 0;
        assisted = false;
        takingOff = false;
        needReset = false;
        isHovering = false;
        firstRocket = true;
        setbackCounter = 0;
        wasHovering = false;
        rocketBoostSpeed = 1.5;
        assistTimer = assistCooldown.get();
        discardCurrentRocket("on deactivate");
        rcc = StardustUtil.rCC();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.getNetworkHandler() == null) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (syncInventory.get() && !synced && mc.getNetworkHandler().getPlayerList().size() > 1) {
            if (timer == 0 && debug.get()) mc.player.sendMessage(Text.literal("§7Priming inventory to prevent desync..."), false);
            ++timer;
            if (timer >= 37) {
                timer = 0;
                synced = true;
                mc.player.closeHandledScreen();
                if (debug.get()) mc.player.sendMessage(Text.literal("§7Inventory synced with server."), false);
            }
            return;
        }

        if (hasActiveRocket() && currentRocket == null) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof FireworkRocketEntity r && r.getOwner() != null && r.getOwner().equals(mc.player)) {
                    currentRocket = r;
                    break;
                }
            }
        }

        try {
            if (extendRockets.get() && hasActiveRocket() && extensionStartTime != null && extensionStartPos != null) {
                BlockPos playerPos = new BlockPos(mc.player.getBlockX(), 0, mc.player.getBlockZ());
                long elapsed = System.currentTimeMillis() - extensionStartTime;

                String duration = String.valueOf(Math.round((elapsed / 1000f) * 100) * .01);
                String formatted = duration.substring(0, Math.min(5, duration.length()));
                if (formatted.length() <= 4) formatted = formatted+"0";

                if (debug.get() || durationFeedback.get() && chatFeedback) ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.literal("§7Duration Boost: §e§o"+formatted+" §7§oseconds."),
                    "rocketDurationUpdate".hashCode()
                );

                if (pongQueue.size() >= 1900) {
                    discardCurrentRocket("max packet queue size reached");
                } else if (elapsed >= extendedDuration.get() * 1000.0) {
                    discardCurrentRocket("max duration reached");
                } else if (!playerPos.isWithinDistance(extensionStartPos, extensionRange.get())) {
                    extensionStartPos = null;
                    discardCurrentRocket("max range from origin reached");
                }
            }
        } catch (Exception err) {
            Stardust.LOG.error("[RocketMan] extensionStartPos should not have been null, but it was! Why:\n{}", err.toString());
        }


        ++setbackTimer;
        if (needReset) {
            if (antiLagBackFeedback.get() || debug.get() && chatFeedback) ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                Text.literal("§8§o["+rcc+"§oAntiLagBack...§8§o]"),
                "LagBackReset".hashCode()
            );

            if (isHovering) {
                isHovering = false;
                wasHovering = true;
                return;
            }else if (wasHovering && setbackTimer > 4) {
                hoverTimer = 0;
                setbackTimer = 0;
                isHovering = true;
                needReset = false;
                wasHovering = false;
            } else if (setbackTimer >= lagBackDelay.get()) {
                setbackTimer = 0;
                needReset = false;
            } else return;
        } else if (setbackTimer >= 100) {
            setbackTimer = 0;
            setbackCounter = 0;
        }

        ItemStack activeItem = mc.player.getActiveItem();
        if ((activeItem.contains(DataComponentTypes.FOOD) || Utils.isThrowable(activeItem.getItem())) && mc.player.getItemUseTime() > 0) {
            if (!isHovering || hasActiveRocket()) {
                ++ticksBusy;
                return;
            }
        }else if (combatAssist.get() && ticksBusy >= 10 && mc.player.isGliding() && activeItem.getItem() == Items.TRIDENT) {
            ++tridentThrowGracePeriod;
            if (tridentThrowGracePeriod >= 20) {
                ticksBusy = 0;
                useFireworkRocket("combat assist trident throw");
                tridentThrowGracePeriod = 0;
                return;
            }
        } else if (combatAssist.get() && ticksBusy >= 10 && mc.player.isGliding() && activeItem.getItem() != Items.TRIDENT) {
            useFireworkRocket("combat assist miscellaneous");
            ticksBusy = 0;
            return;
        }

        if (assisted && mc.player.isOnGround() && (assistCooldown.get() > 0 || assistCooldown.get() == -1)) {
            --assistTimer;
            if (assistTimer <= 0) {
                assisted = false;
                assistTimer = assistCooldown.get();
            }
        }

        if (mc.player.isGliding() && hasActiveRocket()) takingOff = true;
        boolean needsEscape = escapeLava.get() && PlayerUtils.getDimension().equals(Dimension.Nether) && mc.player.isSubmergedIn(FluidTags.LAVA);

        if (!needsEscape && inLava) {
            inLava = false;
            mc.options.jumpKey.setPressed(false);
            if (lastPlayerPitch != -420.69f) {
                mc.player.setPitch(lastPlayerPitch);
                lastPlayerPitch = -420.69f;
            }
        }

        if ((!takingOff && !assisted) || needsEscape) assistTakeoff();
        else if (mc.player.isOnGround() || !mc.player.isGliding()) {
            discardCurrentRocket("");
            ticksBusy = 0;
            hoverTimer = 0;
            ticksFlying = 0;
            justUsed = false;
            takingOff = false;
            firstRocket = true;
            rocketBoostSpeed = 1.5;
            if (!hoverMode.get().equals(HoverMode.Toggle)) isHovering = false;
            if (mc.player.isOnGround() && disableOnLand.get()) {
                toggle();
                sendToggledMsg();
            }
            return;
        }

        if (!mc.player.isGliding()) return;

        handleDurabilityChecks();
        handleFireworkRocketChecks();
        if (isHovering) {
            ++timer;
            ++hoverTimer;
            ++ticksFlying;
            firstRocket = true;
            ++rocketStockTicks;
            ++durabilityCheckTicks;
            if (hoverTimer == 2 && (!hasActiveRocket() || durationBoosted)) {
                useFireworkRocket("hover initiate");
            } else if (!hasActiveRocket() && forceRocketUsage.get()) useFireworkRocket("hover maintain");
            return;
        } else hoverTimer = 0;

        ++timer;
        ++ticksFlying;
        ++rocketStockTicks;
        ++durabilityCheckTicks;
        if (ticksFlying > 10) firstRocket = false;
        switch (usageMode.get()) {
            case Speed -> {
                double blocksPerSecond = Utils.getPlayerSpeed().length();
                if (blocksPerSecond <= usageSpeed.get() && !justUsed && !hasActiveRocket()) {
                    useFireworkRocket("speed threshold usage");
                }
            }
            case Static -> {
                if (timer >= usageTickRate.get()) {
                    timer = 0;
                    useFireworkRocket("static usage");
                }
            }
            case Dynamic -> {
                if (!hasActiveRocket() && !justUsed) useFireworkRocket("dynamic usage");
            }
            case OnKey -> {
                if (usageKey.get().isPressed() && !justUsed) {
                    useFireworkRocket("forward key usage");
                }
            }
        }
        if (justUsed) {
            ++ticksSinceUsed;
            if (ticksSinceUsed >= usageCooldown.get() || (usageMode.get().equals(RocketMode.Dynamic) && ticksSinceUsed >= 10)) {
                justUsed = false;
                ticksSinceUsed = 0;
            }
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || !mc.player.isGliding()) return;
        if (extendRockets.get() && durationBoosted && event.packet instanceof CommonPongC2SPacket packet) {
            event.cancel();
            pongQueue.add(packet);
        }

        if (!shouldLockYLevel()) return;
        if (!(event.packet instanceof PlayerMoveC2SPacket packet)) return;

        if (mc.player.input.playerInput.jump() && verticalSpeed.get() > 0) {
            if (isHovering) ((PlayerMoveC2SPacketAccessor) packet).setPitch(-90);
            else ((PlayerMoveC2SPacketAccessor) packet).setPitch(-45);
        } else if (mc.player.input.playerInput.sneak() && verticalSpeed.get() > 0) {
            if (isHovering) ((PlayerMoveC2SPacketAccessor) packet).setPitch(90);
            else ((PlayerMoveC2SPacketAccessor) packet).setPitch(45);
        } else ((PlayerMoveC2SPacketAccessor) packet).setPitch(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.getNetworkHandler() == null) return;
        if (mc.player == null || !mc.player.isGliding()) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            ++setbackCounter;
            if (setbackCounter > 5) {
                needReset = true;
                setbackTimer = 0;
                setbackCounter = 0;
                rcc = StardustUtil.rCC();
            }
            rocketBoostSpeed = 1.5;
            if (durationBoosted) {
                discardCurrentRocket("lagback reset");
            }
        }else if (extendRockets.get() && currentRocket != null && event.packet instanceof EntitiesDestroyS2CPacket packet) {
            boolean cancelled = false;
            IntList entityIds = new IntArrayList();
            for (int id : packet.getEntityIds()) {
                if (id == currentRocket.getId()) {
                    event.cancel();
                    cancelled = true;
                    durationBoosted = true;
                    extensionStartTime = System.currentTimeMillis();
                } else {
                    entityIds.add(id);
                }
            }
            if (cancelled && !entityIds.isEmpty()) {
                mc.getNetworkHandler().onEntitiesDestroy(new EntitiesDestroyS2CPacket(entityIds));
            }
        }

        if (!(event.packet instanceof PlaySoundS2CPacket packet)) return;
        if (packet.getSound().value() == SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH) {
            if (muteRockets.get()) event.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onScrollWheel(MouseScrollEvent event) {
        Modules mods = Modules.get();
        if (mc.currentScreen != null || !boostSpeed.get()) return;
        if (mods == null || mods.get(Freecam.class).isActive()) return;

        if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            if (isHovering) {
                if (verticalScrollSensitivity.get() <= 0) return;
                event.cancel();
                double speed = verticalSpeed.get();
                double newSpeed = speed + (event.value * verticalScrollSensitivity.get());
                verticalSpeed.set(newSpeed);
                if (scrollSpeedFeedback.get() && chatFeedback) ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.literal("§8<§5§o✨§r§8> §7Vertical Speed: §3"+String.valueOf(newSpeed).substring(0, Math.min(5, String.valueOf(newSpeed).length()))),
                    "verticalSpeedScroll".hashCode()
                );
            } else {
                if (maxSpeedScrollSensitivity.get() <= 0) return;
                event.cancel();
                double speed = speedSetting.get();
                double newSpeed = speed + (event.value * maxSpeedScrollSensitivity.get());
                speedSetting.set(newSpeed);
                if (scrollSpeedFeedback.get() && chatFeedback) ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.literal("§8<§5§o✨§r§8> §7Max Speed: §3"+String.valueOf(newSpeed).substring(0, Math.min(5, String.valueOf(newSpeed).length()))),
                    "maxSpeedScroll".hashCode()
                );
            }
        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT_ALT)) {
            if (isHovering) {
                if (horizontalScrollSensitivity.get() <= 0) return;
                event.cancel();
                double speed = horizontalSpeed.get();
                double newSpeed = speed + (event.value * horizontalScrollSensitivity.get());
                horizontalSpeed.set(newSpeed);
                if (scrollSpeedFeedback.get() && chatFeedback) ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.literal("§8<§3§o✨§r§8> §7Horizontal Speed: §5"+String.valueOf(newSpeed).substring(0, Math.min(5, String.valueOf(newSpeed).length()))),
                    "horizontalSpeedScroll".hashCode()
                );
            } else {
                if (accelerationScrollSensitivity.get() <= 0) return;
                event.cancel();
                double speed = accelerationSetting.get();
                double newSpeed = speed + (event.value * accelerationScrollSensitivity.get());
                accelerationSetting.set(newSpeed);
                if (scrollSpeedFeedback.get() && chatFeedback) ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.literal("§8<§3§o✨§r§8> §7Acceleration: §5"+String.valueOf(newSpeed).substring(0, Math.min(5, String.valueOf(newSpeed).length()))),
                    "accelerationScroll".hashCode()
                );
            }
        }
    }
}
