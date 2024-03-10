package dev.stardust.modules;

import java.util.*;
import dev.stardust.Stardust;
import net.minecraft.block.*;
import net.minecraft.text.Text;
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
        super(Stardust.CATEGORY, "TreasureESP", "Notifies you when a treasure chest is nearby.");
    }

    private final SettingGroup sgNotifications = settings.createGroup("Notifications");
    private final SettingGroup sgESP = settings.createGroup("ESP");

    private final Setting<Boolean> chatSetting = sgNotifications.add(
        new BoolSetting.Builder()
            .name("Chat")
            .description("Notify with a chat message.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> soundSetting = sgNotifications.add(
        new BoolSetting.Builder()
            .name("Sound")
            .description("Notify with sound.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> volumeSetting = sgNotifications.add(
        new DoubleSetting.Builder()
            .name("Volume")
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
            .name("ESP Settings")
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
        this.notified.clear();
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
                            if (localX == 9 && localZ == 9 && this.isBuriedNaturally(blockPos)) {
                                if (soundSetting.get()) {
                                    mc.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, volumeSetting.get().floatValue(), 1f);
                                }
                                if (chatSetting.get()) mc.player.sendMessage(
                                    Text.of(
                                        "§8<"+ StardustUtil.rCC()+"✨§8> §3§oFound buried treasure at §8[§7§o"
                                            +blockPos.getX()+"§8, §7§o"+blockPos.getY()+"§8, §7§o"+blockPos.getZ()+"§8]"
                                    )
                                );
                                this.notified.add(blockPos);
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
        Map<BlockPos, BlockEntity> blockEntities = event.chunk.getBlockEntities();

        for (BlockPos pos : blockEntities.keySet()) {
            if (this.notified.contains(pos)) continue;
            if (blockEntities.get(pos) instanceof ChestBlockEntity) {
                int localX = ChunkSectionPos.getLocalCoord(pos.getX());
                int localZ = ChunkSectionPos.getLocalCoord(pos.getZ());

                // Buried treasure chests always generate at local chunk coordinates of x=9,z=9
                if (localX == 9 && localZ == 9 && this.isBuriedNaturally(pos)) {
                    if (soundSetting.get()) {
                        mc.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, volumeSetting.get().floatValue(), 1f);
                    }
                    if (chatSetting.get()) mc.player.sendMessage(
                        Text.of("§8<"+ StardustUtil.rCC()+"✨§8> §3§oFound buried treasure at §8[§7§o"+pos.getX()+"§8, §7§o"+pos.getY()+"§8, §7§o"+pos.getZ()+"§8]")
                    );
                    this.notified.add(pos);
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!espSetting.get()) return;
        if (mc.player == null || mc.world == null) return;
        List<BlockPos> inRange = this.notified
            .stream()
            .filter(pos -> pos.isWithinDistance(mc.player.getBlockPos(), mc.options.getViewDistance().getValue() * 16+32))
            .toList();

        ESPBlockData espSettings = espColorSettings.get();
        for (BlockPos pos : inRange) {
            if (this.looted.contains(pos)) continue;
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
        if (this.notified.contains(event.result.getBlockPos())) {
            if (event.result.getType() == HitResult.Type.BLOCK && mc.world.getBlockState(event.result.getBlockPos()).getBlock() instanceof ChestBlock) {
                this.looted.add(event.result.getBlockPos());
            }
        }
    }
}
