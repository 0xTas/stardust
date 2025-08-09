package dev.stardust.modules;
import net.minecraft.entity.player.PlayerInventory;

import java.util.ArrayList;
import java.util.ArrayDeque;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import dev.stardust.util.MsgUtil;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import java.util.concurrent.TimeUnit;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import dev.stardust.config.StardustConfig;
import net.minecraft.entity.EquipmentSlot;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import dev.stardust.mixin.accessor.DisconnectS2CPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class RoadTrip extends Module {
    public RoadTrip() {
        super(Stardust.CATEGORY, "RoadTrip", "Tools for AFK-travelling over long distances.");
    }

    @SuppressWarnings("unused")
    public enum ToggleModes {
        Module, Settings, None
    }

    private final SettingGroup sgETA = settings.createGroup("ETA Settings (Spoiler)", false);
    private final SettingGroup sgAutoLog = settings.createGroup("AutoLog Settings");
    private final SettingGroup sgNotify = settings.createGroup("Notification Settings");

    private final Setting<Boolean> etaSetting = sgETA.add(
        new BoolSetting.Builder()
            .name("display-ETA")
            .description("Display an estimated time of arrival for your target coords.")
            .defaultValue(false)
            .build()
    );
    public final Setting<Integer> targetX = sgETA.add(
        new IntSetting.Builder()
            .name("target-X")
            .description("Not dimension-translated.")
            .range(-30000000, 30000000).noSlider().defaultValue(0)
            .onChanged(it -> this.bpsValues.clear())
            .build()
    );
    public final Setting<Integer> targetZ = sgETA.add(
        new IntSetting.Builder()
            .name("target-Z")
            .description("Not dimension-translated.")
            .range(-30000000, 30000000).noSlider().defaultValue(0)
            .onChanged(it -> this.bpsValues.clear())
            .build()
    );
    private final Setting<Boolean> speedUpdates = sgETA.add(
        new BoolSetting.Builder()
            .name("average-speed-updates")
            .description("Display your average speed over a period of time.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> averageETA = sgETA.add(
        new BoolSetting.Builder()
            .name("ETA-average")
            .description("Display ETA based on your average speed over X minutes.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> averageSpeedMinutes = sgETA.add(
        new IntSetting.Builder()
            .name("average-speed-over-X-minutes")
            .description("Display your average speed over a period of time.")
            .range(0, 3200).sliderRange(0, 60).defaultValue(10)
            .onChanged(it -> this.bufferSize = it * 1200)
            .build()
    );

    private final Setting<Boolean> forceKick = sgAutoLog.add(
        new BoolSetting.Builder()
            .name("illegal-disconnect")
            .description("Forces the server to kick you immediately by sending an illegal packet.")
            .defaultValue(true)
            .build()
    );
    private final Setting<ToggleModes> autoLogToggle = sgAutoLog.add(
        new EnumSetting.Builder<ToggleModes>()
            .name("disable-on-disconnect")
            .description("Disables either the module or the AutoLog settings on disconnect, to prevent kicks on rejoin.")
            .defaultValue(ToggleModes.Module)
            .build()
    );

    private final Setting<Boolean> antiTrapAutoLog = sgAutoLog.add(
        new BoolSetting.Builder()
            .name("anti-trap-autoLog")
            .description("Illegally disconnects you when any TNT minecarts are rendered, to avoid highway traps.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> etaAutoLog = sgAutoLog.add(
        new BoolSetting.Builder()
            .name("ETA-autoLog")
            .description("Logs you out on arrival at your destination.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> etaAutoLogThreshold = sgAutoLog.add(
        new IntSetting.Builder()
            .name("ETA-autoLog-threshold")
            .description("Logs you out if you come within <threshold> blocks of your ETA.")
            .range(0, 25000).noSlider().defaultValue(150)
            .build()
    );

    private final Setting<Boolean> timeoutAutoLog = sgAutoLog.add(
        new BoolSetting.Builder()
            .name("timer-autoLog")
            .description("Logs you out x minutes after enabling the module, or the setting.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) this.setLogOutTimer(this.timeoutAutoLogTimer.get());
                else this.disableLogOutTimer();
            })
            .build()
    );
    private final Setting<Integer> timeoutAutoLogTimer = sgAutoLog.add(
        new IntSetting.Builder()
            .name("timer-autoLog-seconds")
            .description("Logs you out x seconds after enabling the module, or the Timer AutoLog setting.")
            .min(0).noSlider().defaultValue(3600)
            .onChanged(it -> {
                if (timeoutAutoLog.get()) this.setLogOutTimer(it);
            })
            .build()
    );

    private final Setting<Boolean> lowElytraAutoLog = sgAutoLog.add(
        new BoolSetting.Builder()
            .name("elytraStock-autoLog")
            .description("Logs you out if your stock of elytras is running low.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> elytraStock = sgAutoLog.add(
        new IntSetting.Builder()
            .name("elytraStock-autoLog-threshold")
            .description("Logs you out if your stock of elytras is running low.")
            .range(0, 40).sliderRange(0, 30).defaultValue(1)
            .build()
    );

    private final Setting<Boolean> lowRocketsAutoLog = sgAutoLog.add(
        new BoolSetting.Builder()
            .name("rocketStock-autoLog")
            .description("Logs you out if your stock of firework rockets is running low.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> rocketStock = sgAutoLog.add(
        new IntSetting.Builder()
            .name("rocketStock-autoLog-threshold")
            .description("Logs you out if your stock of firework rockets is running low.")
            .range(0, 1024).sliderRange(0, 128).defaultValue(32)
            .build()
    );

    private final Setting<Boolean> yLevelAutoLog = sgAutoLog.add(
        new BoolSetting.Builder()
            .name("y-level-autoLog")
            .description("Logs you out if your Y level gets too low.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> yLevelThreshold = sgAutoLog.add(
        new IntSetting.Builder()
            .name("y-level-threshold")
            .description("Logs you out if your Y level is lower than the threshold.")
            .range(-69, 30000000).noSlider().defaultValue(-69)
            .build()
    );

    private final Setting<Boolean> elytraNotify = sgNotify.add(
        new BoolSetting.Builder()
            .name("elytra-notify")
            .description("Notify you with sound pings when your last elytra is below 5% durability.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> lagNotify = sgNotify.add(
        new BoolSetting.Builder()
            .name("lag-notify")
            .description("Play a sound notification if you've stopped making progress (excess rubberbanding, hit a roadblock, etc.)")
            .defaultValue(false)
            .build()
    );
    private final Setting<Double> pingVolume = sgNotify.add(
        new DoubleSetting.Builder()
            .name("ping-volume")
            .range(0, 5).sliderMin(0).sliderMax(5).defaultValue(.5)
            .build()
    );

    private int timer = 0;
    private int ticksNotMoved = 0;
    private int ticksSinceWarned = 0;
    private long logOutTimer = -69L;
    private long timerTimestamp = 0L;
    private @Nullable BlockPos lastPos = null;
    private @Nullable Text disconnectReason = null;
    private int bufferSize = averageSpeedMinutes.get() * 1200;
    private final ArrayDeque<Double> bpsValues = new ArrayDeque<>();

    private void setLogOutTimer(int timer) {
        logOutTimer = timer;
        timerTimestamp = System.currentTimeMillis();
        if (timeoutAutoLog.get() && isActive()) {
            MsgUtil.updateModuleMsg("Set Timer AutoLog to disconnect you §a§o" + timeoutAutoLogTimer.get() + " §7seconds from now.", this.name, "timerAutoLog".hashCode());
        }
    }
    private void disableLogOutTimer() {
        logOutTimer = -69L;
    }

    @Override
    public void onActivate() {
        disconnectReason = null;
        timerTimestamp = System.currentTimeMillis();
        if (timeoutAutoLog.get()) {
            MsgUtil.updateModuleMsg("Set Timer AutoLog to disconnect you §a§o" + timeoutAutoLogTimer.get()  + " §7seconds from now.", this.name, "timerAutoLog".hashCode());
        }
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        lastPos = null;
        bpsValues.clear();
        ticksNotMoved = 0;
        logOutTimer = -69L;
        timerTimestamp = 0L;
        ticksSinceWarned = 0;
        bufferSize = averageSpeedMinutes.get() * 1200;
    }

    private void handleDurabilityChecks() {
        if (mc.player == null) return;
        if (ticksSinceWarned < 100) return;

        boolean reset = false;
        if (elytraNotify.get()) {
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;

            ItemStack equippedElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST);

            int maxDurability = equippedElytra.getMaxDamage();
            int currentDurability = maxDurability - equippedElytra.getDamage();
            double percentDurability = Math.floor((currentDurability / (double) maxDurability) * 100);

            if (percentDurability <= 5) {
                mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK.value(), pingVolume.get().floatValue(), 1f);
                MsgUtil.updateModuleMsg("Elytra durability: §c" + percentDurability + "§7%", this.name, "roadTripElytraWarn".hashCode());
                reset = true;
            }
        }

        if (lagNotify.get() && ticksNotMoved >= 80) {
            reset = true;
            ticksNotMoved = 0;
            mc.player.playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, pingVolume.get().floatValue(), 1f);
        }

        if (reset) ticksSinceWarned = 0;
    }

    private boolean hasEnoughElytras() {
        if (mc.player == null) return false;
        ArrayList<Integer> goodSlotsLeft = new ArrayList<>();
        for (int n = 0; n < PlayerInventory.MAIN_SIZE; n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);

            if (stack.getItem() == Items.ELYTRA) {
                int max = stack.getMaxDamage();
                int curr = max - stack.getDamage();
                double percent = Math.floor((curr / (double) max) * 100);

                if (percent > 5) {
                    goodSlotsLeft.add(n);
                }
            }
        }
        return goodSlotsLeft.size() > elytraStock.get();
    }

    private boolean hasEnoughRockets() {
        if (mc.player == null) return false;
        int totalRocketsLeft = 0;
        for (int n = 0; n < PlayerInventory.MAIN_SIZE; n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if (stack.getItem() == Items.FIREWORK_ROCKET) {
                totalRocketsLeft += stack.getCount();
            }
        }
        return totalRocketsLeft > rocketStock.get();
    }

    private void doForceKick(Text disconnectReason) {
        this.disconnectReason = disconnectReason;
        StardustUtil.illegalDisconnect(true, StardustConfig.illegalDisconnectMethodSetting.get());
    }

    private void disconnect(Text reason) {
        if (mc.getNetworkHandler() == null) return;
        StardustUtil.disableAutoReconnect();
        mc.getNetworkHandler().onDisconnect(new DisconnectS2CPacket(reason));
        switch (autoLogToggle.get()) {
            case Module -> toggle();
            case Settings -> disableAutoLogSettings();
        }
    }

    private void disableAutoLogSettings() {
        etaAutoLog.set(false);
        yLevelAutoLog.set(false);
        timeoutAutoLog.set(false);
        antiTrapAutoLog.set(false);
        lowElytraAutoLog.set(false);
        lowRocketsAutoLog.set(false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (StardustUtil.isIn2b2tQueue()) return;
        if (mc.getNetworkHandler() == null) return;
        if (mc.player == null || mc.world == null) return;

        ++timer;
        ++ticksSinceWarned;
        handleDurabilityChecks();
        if (timeoutAutoLog.get() && logOutTimer != -69L) {
            long now = System.currentTimeMillis();
            if (now - timerTimestamp >= timeoutAutoLogTimer.get() * 1000) {
                timerTimestamp = now;
                Text reason = Text.literal("§8[§5RoadTrip§8] §7Disconnected you because your §3" + timeoutAutoLogTimer.get() + "§7-second timer has elapsed§a..!");
                if (forceKick.get()) {
                    doForceKick(reason);
                } else {
                    disconnect(reason);
                }
            }
        }

        if (yLevelAutoLog.get() && mc.player.getY() < yLevelThreshold.get()) {
            Text reason = Text.literal("§8[§4RoadTrip§8] §fDisconnected you because your Y level descended below §c"+ yLevelThreshold.get()+"§8!");
            if (forceKick.get()) {
                doForceKick(reason);
            } else {
                disconnect(reason);
            }
        }

        if (lowElytraAutoLog.get() && !hasEnoughElytras()) {
            Text reason = Text.literal("§8[§2RoadTrip§8] §fDisconnected you because you are running low on healthy elytras§c!");
            if (forceKick.get()) {
                doForceKick(reason);
            } else {
                disconnect(reason);
            }
        }

        if (lowRocketsAutoLog.get() && !hasEnoughRockets()) {
            Text reason = Text.literal("§8[§2RoadTrip§8] §fDisconnected you because you are running low on firework rockets§c!");
            if (forceKick.get()) {
                doForceKick(reason);
            } else {
                disconnect(reason);
            }
        }

        BlockPos newPos = mc.player.getBlockPos();

        if (lastPos == null) lastPos = newPos;
        else if (lastPos.getManhattanDistance(newPos) <= 1) {
            ++ticksNotMoved;
            lastPos = newPos;
        }

        BlockPos destination = new BlockPos(targetX.get(), mc.player.getBlockY(), targetZ.get());

        int totalBlocksLeft = newPos.getManhattanDistance(destination);
        double blocksPerSecond = Utils.getPlayerSpeed().horizontalLength();

        if (etaAutoLog.get() && totalBlocksLeft <= etaAutoLogThreshold.get()) {
            if (mc.getNetworkHandler().getPlayerList().size() > 1) {
                Text reason = Text.literal("§8[§2RoadTrip§8] §fDisconnected you because you have reached your destination§2! §5:§3]");
                if (forceKick.get()) {
                    doForceKick(reason);
                } else {
                    disconnect(reason);
                }
            }
        }

        if (etaSetting.get()) {
            if (bpsValues.size() > bufferSize) {
                int diff = bpsValues.size() - bufferSize;
                for (int n = 0; n < diff; n++) {
                    if (!bpsValues.isEmpty()) bpsValues.removeFirst();
                }
            } else if (bpsValues.size() == bufferSize) {
                if (!bpsValues.isEmpty()) bpsValues.removeFirst();
                bpsValues.add(blocksPerSecond);
            } else bpsValues.add(blocksPerSecond);
            if (averageSpeedMinutes.get() > 0) {
                double average = 0;
                for (double d : bpsValues) {
                    average += d;
                }

                if (averageETA.get()) {
                    blocksPerSecond = average / bpsValues.size();
                }
                if (speedUpdates.get() && timer >= 20) MsgUtil.updateMsg(
                    "Average speed over " + averageSpeedMinutes.get() + " minute(s): "
                        + (Math.floor(average / bpsValues.size()) * 3600) / 1000 + "Km/h", "averageBPSUpdate".hashCode()
                );
            }

            if (timer >= 20) {
                timer = 0;
                if (blocksPerSecond == 0) {
                    double verticalBPS = Utils.getPlayerSpeed().y;
                    if (verticalBPS == 0) return;
                    MsgUtil.updateMsg("Vertical Speed: §6§o" + Math.floor(verticalBPS) + "§7§ob/s. §5§o" + (Math.floor(verticalBPS) * 3600) / 1000 + "§7§oKm/h", "verticalSpeedUpdate".hashCode());
                    return;
                }
                double totalSeconds = totalBlocksLeft / blocksPerSecond;

                long days = TimeUnit.SECONDS.toDays((long) totalSeconds);
                totalSeconds -= TimeUnit.DAYS.toSeconds(days);

                long hours = TimeUnit.SECONDS.toHours((long) totalSeconds);
                totalSeconds -= TimeUnit.HOURS.toSeconds(hours);

                long minutes = TimeUnit.SECONDS.toMinutes((long) totalSeconds);
                totalSeconds -= TimeUnit.MINUTES.toSeconds(minutes);

                long seconds = TimeUnit.SECONDS.toSeconds((long) totalSeconds);
                StringBuilder sb = new StringBuilder().append(StardustUtil.rCC()).append("§8<§a§o✨§r§8> §7§oETA: §2§o");

                if (days == 0 && hours == 0 && minutes == 0 && seconds <= 3) {
                    if (totalBlocksLeft <= 69) {
                        sb.append("§2§oYou have arrived at your destination§8§o. §5:§3]");
                    } else {
                        sb.append("Imminent§8§o...");
                    }
                } else {
                    if (days != 0) sb.append(days).append(" §7§oDays, §2§o");
                    if (hours != 0) sb.append(hours).append(" §7§oHours, §2§o");
                    if (minutes != 0) sb.append(minutes).append(" §7§oMinutes, §2§o");
                    if (seconds != 0) sb.append(seconds).append(" §7§oSeconds.");
                }

                MsgUtil.updateMsg(sb.toString(), "RoadTripETAUpdate".hashCode());
            }
            lastPos = newPos;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEntityAddHighPriority(EntityAddedEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!antiTrapAutoLog.get() || event.entity.getType() != EntityType.TNT_MINECART) return;
        doForceKick(Text.literal("§8[§aRoadTrip§8] §c§oDisconnected you to avoid a trap§8§o! §8§o(§c§oTNT minecarts §7§owere rendered§c§o!§8§o)"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof DisconnectS2CPacket packet) || disconnectReason == null) return;
        ((DisconnectS2CPacketAccessor)(Object) packet).setReason(disconnectReason);
        switch (autoLogToggle.get()) {
            case Module -> toggle();
            case Settings -> disableAutoLogSettings();
        }
    }
}
