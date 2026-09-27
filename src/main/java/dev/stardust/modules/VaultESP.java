package dev.stardust.modules;

import java.util.*;
import org.lwjgl.glfw.GLFW;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.MapUtil;
import dev.stardust.util.StardustUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.VaultBlock;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import meteordevelopment.meteorclient.gui.GuiTheme;
import net.minecraft.block.entity.VaultBlockEntity;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class VaultESP extends Module {
    public VaultESP() { super(Stardust.CATEGORY, "VaultESP", "ESP for ominous vaults."); }

    private final SettingGroup sgNotifications = settings.createGroup("Notifications");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgESP = settings.createGroup("ESP");

    private final Setting<Boolean> chatSetting = sgNotifications.add(
        new BoolSetting.Builder()
            .name("chat")
            .description("Notify with a chat message.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> coordsSetting = sgNotifications.add(
        new BoolSetting.Builder()
            .name("coords")
            .description("Display vault coordinates in chat notifications.")
            .defaultValue(false)
            .visible(chatSetting::get)
            .build()
    );
    private final Setting<Boolean> waypoints = sgNotifications.add(
        new BoolSetting.Builder()
            .name("add-waypoints")
            .description("Adds waypoints to your Xaeros map for ominous vaults.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> tempWaypoints = sgNotifications.add(
        new BoolSetting.Builder()
            .name("temporary-waypoints")
            .description("Temporary waypoints are removed when you disconnect from the server, or close the game.")
            .defaultValue(false)
            .visible(waypoints::get)
            .build()
    );
    private final Setting<Boolean> soundSetting = sgNotifications.add(
        new BoolSetting.Builder()
            .name("sound")
            .description("Notify with sound.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> volumeSetting = sgNotifications.add(
        new DoubleSetting.Builder()
            .name("volume")
            .min(0.0)
            .max(10.0)
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(1.0)
            .build()
    );

    private final Setting<Boolean> auto = sgAutomation.add(
        new BoolSetting.Builder()
            .name("path-to-vault")
            .description("Uses Baritone to path to the closest ominous vault to you.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Keybind> autoKey = sgAutomation.add(
        new KeybindSetting.Builder()
            .name("path-keybind")
            .description("Which key to press to make Baritone path to the next closest vault.")
            .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_DOWN))
            .build()
    );

    private final Setting<Boolean> espSetting = sgESP.add(
        new BoolSetting.Builder()
            .name("ESP")
            .description("Highlight ominous vaults through walls with ESP.")
            .defaultValue(true)
            .build()
    );
    private final Setting<ESPBlockData> espColorSettings = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("ESP-settings")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(147, 190, 233),
                    new SettingColor(147, 190, 233, 25),
                    true,
                    new SettingColor(147, 190, 233, 125)
                )
            )
            .build()
    );

    private @Nullable BlockPos goal = null;
    private final Set<Long> looted = new LongOpenHashSet();
    private final List<BlockPos> notified = new ArrayList<>();

    private boolean isOminousVault(BlockEntity be) {
        if (be == null) return false;
        if (mc.world == null) return false;
        if (!(be instanceof VaultBlockEntity)) return false;
        BlockState vaultState = mc.world.getBlockState(be.getPos());

        return vaultState.getBlock() instanceof VaultBlock
            && vaultState.contains(VaultBlock.OMINOUS) && vaultState.get(VaultBlock.OMINOUS);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();
        WButton clearLooted = list.add(theme.button("Clear Looted")).widget();

        clearLooted.action = looted::clear;

        return list;
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;

        for (BlockEntity be : Utils.blockEntities()) {
            if (notified.contains(be.getPos())) continue;
            if (looted.contains(be.getPos().asLong())) continue;

            if (isOminousVault(be)) {
                notified.add(be.getPos());

                if (waypoints.get()) {
                    MapUtil.addWaypoint(
                        be.getPos(), "VaultESP - Ominous Vault", "⭐",
                        MapUtil.Purpose.Normal, MapUtil.WpColor.Dark_Green, tempWaypoints.get()
                    );
                }
                if (soundSetting.get()) {
                    mc.player.playSound(SoundEvents.BLOCK_VAULT_OPEN_SHUTTER, volumeSetting.get().floatValue(), 1f);
                }
                if (chatSetting.get()) {
                    Text notification;
                    if (coordsSetting.get()) {
                        notification = Text.of(
                            "§8<" + StardustUtil.rCC() + "✨§8> §a§oFound an ominous vault at §8[§7§o"
                                + be.getPos().getX() + "§8, §7§o" + be.getPos().getY() + "§8, §7§o" + be.getPos().getZ() + "§8]"
                        );
                    } else {
                        notification = Text.of(
                            "§8<" + StardustUtil.rCC() + "✨§8> §a§oFound an ominous vault§7§o!"
                        );
                    }
                    mc.player.sendMessage(notification, false);
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        goal = null;
        notified.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        if (mc.player == null || mc.world == null) return;
        Map<BlockPos, BlockEntity> blockEntities = event.chunk().getBlockEntities();

        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            if (notified.contains(entry.getKey())) continue;

            if (isOminousVault(entry.getValue())) {
                notified.add(entry.getKey());

                if (waypoints.get()) {
                    MapUtil.addWaypoint(
                        entry.getKey(), "VaultESP - Ominous Vault", "⭐",
                        MapUtil.Purpose.Normal, MapUtil.WpColor.Dark_Green, tempWaypoints.get()
                    );
                }
                if (soundSetting.get()) {
                    mc.player.playSound(SoundEvents.BLOCK_VAULT_OPEN_SHUTTER, volumeSetting.get().floatValue(), 1f);
                }
                if (chatSetting.get()) {
                    Text notification;
                    if (coordsSetting.get()) {

                        notification = Text.of(
                            "§8<" + StardustUtil.rCC() + "✨§8> §a§oFound an ominous vault at §8[§7§o"
                                + entry.getKey().getX() + "§8, §7§o" + entry.getKey().getY() + "§8, §7§o" + entry.getKey().getZ() + "§8]"
                        );
                    } else {
                        notification = Text.of(
                            "§8<" + StardustUtil.rCC() + "✨§8> §a§oFound an ominous vault§7§o!"
                        );
                    }
                    mc.player.sendMessage(notification, false);
                }
            }
        }

        if (looted.size() >= 1337) looted.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!espSetting.get()) return;
        if (mc.player == null || mc.world == null) return;

        List<BlockPos> inRange = notified
            .stream()
            .filter(pos -> pos.isWithinDistance(mc.player.getBlockPos(), mc.options.getViewDistance().getValue() * 16+32))
            .toList();

        ESPBlockData espSettings = espColorSettings.get();
        for (BlockPos pos : inRange) {
            if (looted.contains(pos.asLong())) continue;
            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX()+1, pos.getY()+1, pos.getZ()+1,
                espSettings.sideColor, espSettings.lineColor, espSettings.shapeMode, 0
            );

            if (espSettings.tracer) {
                event.renderer.line(
                    RenderUtils.center.x,
                    RenderUtils.center.y,
                    RenderUtils.center.z,
                    pos.getX() + .5,
                    pos.getY() + .5,
                    pos.getZ() + .5,
                    espSettings.tracerColor
                );
            }
        }
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (notified.contains(event.result.getBlockPos())) {
            BlockEntity be = mc.world.getBlockEntity(event.result.getBlockPos());
            if (event.result.getType() == HitResult.Type.BLOCK&& isOminousVault(be)
                && mc.player.getMainHandStack().isOf(Items.OMINOUS_TRIAL_KEY)
            ) {
                goal = null;
                looted.add(be.getPos().asLong());
                BlockPos wpPos = event.result.getBlockPos();
                MapUtil.removeWaypoints(
                    "VaultESP",
                    pos -> pos.getX() == wpPos.getX() && pos.getY() == wpPos.getY() && pos.getZ() == wpPos.getZ(),
                    Optional.empty()
                );
            }
        }
    }

    @EventHandler
    private void onKeyPress(KeyEvent event) {
        if (goal != null) return;
        if (mc.player == null || mc.world == null) return;
        if (!auto.get() || event.key != autoKey.get().getValue()) return;

        List<BlockPos> inRange = notified
            .stream()
            .filter(pos -> !looted.contains(pos.asLong()))
            .filter(pos -> pos.isWithinDistance(mc.player.getBlockPos(), mc.options.getViewDistance().getValue() * 16+32))
            .toList();

        if (inRange.isEmpty()) return;

        BlockPos closest = null;
        double distance = Double.MAX_VALUE;

        for (BlockPos pos : inRange) {
            if (closest == null) closest = pos;
            else {
                double d = mc.player.getBlockPos().getSquaredDistance(pos);
                if (d < distance) {
                    distance = d;
                    closest = pos;
                }
            }
        }

        if (closest != null && BaritoneUtils.IS_AVAILABLE) {
            BlockPos bestSpot = null;
            for (Direction dir : Direction.values()) {
                BlockPos offset = closest.offset(dir);
                if (bestSpot == null) {
                    if (mc.world.getBlockState(offset).isAir()) {
                        bestSpot = closest.offset(dir.getOpposite());
                        break;
                    } else {
                        bestSpot = offset;
                    }
                } else if (mc.world.getBlockState(offset).isAir()) {
                    bestSpot = closest.offset(dir.getOpposite());
                    break;
                }
            }

            if (bestSpot != null) goal = bestSpot;
            else goal = closest;

            PathManagers.get().moveTo(goal);
            if (chatFeedback) MsgUtil.sendModuleMsg("Pathing to nearest ominous vault§5..!", this.name);
        }
    }
}
