package dev.stardust.modules;

import java.util.*;
import dev.stardust.Stardust;
import net.minecraft.block.*;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.MapUtil;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.ChunkSectionPos;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.block.entity.ChestBlockEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class TreasureESP extends Module {
    public TreasureESP() {
        super(Stardust.CATEGORY, "TreasureESP", "Notifies you when a buried treasure chest is nearby.");
    }

    private final SettingGroup sgNotifications = settings.createGroup("Notifications");
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
            .description("Display chest coordinates in chat notifications.")
            .defaultValue(false)
            .visible(chatSetting::get)
            .build()
    );

    private final Setting<Boolean> waypoints = sgNotifications.add(
        new BoolSetting.Builder()
            .name("add-waypoints")
            .description("Adds waypoints to your Xaeros map for treasure chests.")
            .defaultValue(false)
            .visible(() -> StardustUtil.XAERO_AVAILABLE)
            .build()
    );
    private final Setting<Boolean> tempWaypoints = sgNotifications.add(
        new BoolSetting.Builder()
            .name("temporary-waypoints")
            .description("Temporary waypoints are removed when you disconnect from the server, or close the game.")
            .defaultValue(true)
            .visible(() -> StardustUtil.XAERO_AVAILABLE && waypoints.get())
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

    private final Setting<Boolean> espSetting = sgESP.add(
        new BoolSetting.Builder()
            .name("ESP")
            .description("Highlight treasure chests through walls with ESP.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ESPBlockData> espColorSettings = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("ESP-settings")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(147, 233, 190),
                    new SettingColor(147, 233, 190, 25),
                    true,
                    new SettingColor(147, 233, 190, 125)
                )
            )
            .build()
    );

    private final Set<BlockPos> looted = new HashSet<>();
    private final List<BlockPos> notified = new ArrayList<>();

    private boolean isBuriedNaturally(BlockPos pos) {
        if (mc.world == null) return false;
        Block block = mc.world.getBlockState(pos.up()).getBlock();

        return block == Blocks.SAND || block == Blocks.DIRT || block == Blocks.GRAVEL
            || block == Blocks.STONE || block == Blocks.DIORITE || block == Blocks.GRANITE
            || block == Blocks.ANDESITE || block == Blocks.SANDSTONE || block == Blocks.COAL_ORE;
    }

    @Override
    public void onDeactivate() {
        notified.clear();
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;

        BlockPos pos = mc.player.getBlockPos();
        int viewDistance = mc.options.getViewDistance().getValue();

        int startChunkX = (pos.getX() - (viewDistance*16)) >> 4;
        int endChunkX = (pos.getX() + (viewDistance * 16)) >> 4;
        int startChunkZ = (pos.getZ() - (viewDistance * 16)) >> 4;
        int endChunkZ = (pos.getZ() + (viewDistance * 16)) >> 4;

        for (int x = startChunkX; x < endChunkX; x++) {
            for (int z = startChunkZ; z < endChunkZ; z++) {
                if (mc.world.isChunkLoaded(x,z)) {
                    WorldChunk chunk = mc.world.getChunk(x, z);
                    Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();

                    for (BlockPos blockPos : blockEntities.keySet()) {
                        if (blockEntities.get(blockPos) instanceof ChestBlockEntity) {
                            int localX = ChunkSectionPos.getLocalCoord(blockPos.getX());
                            int localZ = ChunkSectionPos.getLocalCoord(blockPos.getZ());

                            // Buried treasure chests always generate at local chunk coordinates of x=9,z=9
                            if (localX == 9 && localZ == 9 && isBuriedNaturally(blockPos)) {
                                if (StardustUtil.XAERO_AVAILABLE && waypoints.get()) {
                                    MapUtil.addWaypoint(
                                        blockPos, "TreasureESP - Buried Treasure", "❌",
                                        MapUtil.Purpose.Normal, MapUtil.WpColor.Dark_Red, tempWaypoints.get()
                                    );
                                }
                                if (soundSetting.get()) {
                                    mc.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, volumeSetting.get().floatValue(), 1f);
                                }
                                if (chatSetting.get()) {
                                    String notification;
                                    if (coordsSetting.get()) {
                                        notification = "§3§oFound buried treasure at §8[§7§o"
                                            + blockPos.getX() + "§8, §7§o" + blockPos.getY() + "§8, §7§o" + blockPos.getZ() + "§8]";
                                    } else {
                                        notification = "§3§oFound buried treasure§7§o!";
                                    }
                                    MsgUtil.sendModuleMsg(notification, this.name);
                                }
                                notified.add(blockPos);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        if (mc.world == null || mc.player == null) return;
        Map<BlockPos, BlockEntity> blockEntities = event.chunk().getBlockEntities();

        for (BlockPos pos : blockEntities.keySet()) {
            if (notified.contains(pos)) continue;
            if (blockEntities.get(pos) instanceof ChestBlockEntity) {
                int localX = ChunkSectionPos.getLocalCoord(pos.getX());
                int localZ = ChunkSectionPos.getLocalCoord(pos.getZ());

                // Buried treasure chests always generate at local chunk coordinates of x=9,z=9
                if (localX == 9 && localZ == 9 && isBuriedNaturally(pos)) {
                    if (StardustUtil.XAERO_AVAILABLE && waypoints.get()) {
                        MapUtil.addWaypoint(
                            pos, "TreasureESP - Buried Treasure", "❌",
                            MapUtil.Purpose.Normal, MapUtil.WpColor.Dark_Red, tempWaypoints.get()
                        );
                    }
                    if (soundSetting.get()) {
                        mc.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, volumeSetting.get().floatValue(), 1f);
                    }
                    if (chatSetting.get()) {
                        String notification;
                        if (coordsSetting.get()) {
                            notification = "§3§oFound buried treasure at §8[§7§o"
                                + pos.getX() + "§8, §7§o" + pos.getY() + "§8, §7§o" + pos.getZ() + "§8]";
                        } else {
                            notification = "§3§oFound buried treasure§7§o!";
                        }
                        MsgUtil.sendModuleMsg(notification, this.name);
                    }
                    notified.add(pos);
                }
            }
        }
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
            if (looted.contains(pos)) continue;
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
            if (event.result.getType() == HitResult.Type.BLOCK && mc.world.getBlockState(event.result.getBlockPos()).getBlock() instanceof ChestBlock) {
                looted.add(event.result.getBlockPos());
                if (StardustUtil.XAERO_AVAILABLE && waypoints.get()) {
                    BlockPos wpPos = event.result.getBlockPos();
                    MapUtil.removeWaypoints(
                        "TreasureESP",
                        pos -> pos.getX() == wpPos.getX() && pos.getY() == wpPos.getY() && pos.getZ() == wpPos.getZ(),
                        Optional.empty()
                    );
                }
            }
        }
    }
}
