package dev.stardust.modules;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import dev.stardust.util.*;
import dev.stardust.Stardust;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.block.BlockState;
import net.minecraft.sound.SoundEvents;
import net.minecraft.block.FallingBlock;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.hit.BlockHitResult;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.entity.BlockEntity;
import meteordevelopment.meteorclient.settings.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import meteordevelopment.meteorclient.utils.Utils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.entity.BrushableBlockEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Archaeology extends Module {
    public Archaeology() { super(Stardust.CATEGORY, "Archaeology", "Tools to assist in your archaeological endeavors."); }

    public enum SuspiciousBlocks {
        Both, SuspiciousSand, SuspiciousGravel
    }

    private static final ReferenceSet<Item> ARCHAEOLOGY_LOOT_TABLE = ReferenceSet.of(
        Items.SHEAF_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.ANGLER_POTTERY_SHERD,
        Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BLADE_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD,
        Items.DANGER_POTTERY_SHERD, Items.BURN_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.FRIEND_POTTERY_SHERD,
        Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD, Items.MINER_POTTERY_SHERD,
        Items.MOURNER_POTTERY_SHERD, Items.PLENTY_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD, Items.SKULL_POTTERY_SHERD,
        Items.SNORT_POTTERY_SHERD, Items.SNIFFER_EGG, Items.MUSIC_DISC_RELIC, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.EMERALD,
        Items.COAL, Items.TNT, Items.WHEAT, Items.BRICK, Items.STICK, Items.SUSPICIOUS_STEW, Items.DIAMOND, Items.GUNPOWDER, Items.WOODEN_HOE, Items.IRON_AXE,
        Items.GOLD_NUGGET, Items.BLUE_DYE, Items.WHITE_DYE, Items.YELLOW_DYE, Items.DEAD_BUSH, Items.FLOWER_POT, Items.LEAD, Items.WHEAT_SEEDS, Items.STRING,
        Items.SPRUCE_HANGING_SIGN, Items.CLAY, Items.LIGHT_BLUE_DYE, Items.ORANGE_DYE, Items.BROWN_CANDLE, Items.GREEN_CANDLE, Items.RED_CANDLE, Items.PURPLE_CANDLE,
        Items.BEETROOT_SEEDS, Items.BLUE_STAINED_GLASS_PANE, Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE,
        Items.OAK_HANGING_SIGN, Items.PINK_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE
    );

    private final SettingGroup sgDig = settings.createGroup("Dig Settings");
    private final SettingGroup sgSurvey = settings.createGroup("Survey Settings");

    private final Setting<List<Item>> targetItems = sgDig.add(
        new ItemListSetting.Builder()
            .name("target-items")
            .description("Which items to follow through with brushing. All others will be ignored or broken.")
            .defaultValue(List.of(
                Items.SHEAF_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.ANGLER_POTTERY_SHERD,
                Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BLADE_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD,
                Items.DANGER_POTTERY_SHERD, Items.BURN_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.FRIEND_POTTERY_SHERD,
                Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD, Items.MINER_POTTERY_SHERD,
                Items.MOURNER_POTTERY_SHERD, Items.PLENTY_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD, Items.SKULL_POTTERY_SHERD,
                Items.SNORT_POTTERY_SHERD, Items.SHEAF_POTTERY_SHERD, Items.SNIFFER_EGG, Items.MUSIC_DISC_RELIC, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
                Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE
            ))
            .filter(this::isLootItem)
            .build()
    );
    private final Setting<Boolean> breakBad = sgDig.add(
        new BoolSetting.Builder()
            .name("break-bad")
            .description("Break suspicious blocks which don't contain target items.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> chat = sgDig.add(
        new BoolSetting.Builder()
            .name("break-notify")
            .description("Notifies you about the contents of bad suspicious blocks when breaking them.")
            .defaultValue(true)
            .visible(breakBad::get)
            .build()
    );
    private final Setting<Boolean> preventBreak = sgDig.add(
        new BoolSetting.Builder()
            .name("prevent-accidental-breakage")
            .description("Attempts to prevent you from accidentally breaking suspicious sand or gravel blocks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatNotify = sgSurvey.add(
        new BoolSetting.Builder()
            .name("chat-notify")
            .description("Notifies you in chat when a cluster of suspicious blocks is found.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> chatCoords = sgSurvey.add(
        new BoolSetting.Builder()
            .name("chat-coordinates")
            .description("Whether to add coordinates to the chat notification messages.")
            .visible(chatNotify::get)
            .defaultValue(false)
            .build()
    );
    private final Setting<SuspiciousBlocks> chatFor = sgSurvey.add(
        new EnumSetting.Builder<SuspiciousBlocks>()
            .name("chats-for")
            .description("Whether to send chat notifications for suspicious sand, gravel, or both.")
            .defaultValue(SuspiciousBlocks.Both)
            .visible(chatNotify::get)
            .build()
    );

    private final Setting<Boolean> waypoints = sgSurvey.add(
        new BoolSetting.Builder()
            .name("add-waypoints")
            .description("Adds waypoints to your Xaeros map on clusters of suspicious blocks.")
            .defaultValue(false)
            .visible(() -> StardustUtil.XAERO_AVAILABLE)
            .build()
    );
    private final Setting<Boolean> tempWaypoints = sgSurvey.add(
        new BoolSetting.Builder()
            .name("temporary-waypoints")
            .description("Temporary waypoints are removed when you disconnect from the server or close the game.")
            .defaultValue(true)
            .visible(() -> StardustUtil.XAERO_AVAILABLE && waypoints.get())
            .build()
    );
    private final Setting<SuspiciousBlocks> waypointsFor = sgSurvey.add(
        new EnumSetting.Builder<SuspiciousBlocks>()
            .name("waypoints-for")
            .description("Whether to add waypoints for suspicious sand, gravel, or both.")
            .defaultValue(SuspiciousBlocks.Both)
            .visible(() -> StardustUtil.XAERO_AVAILABLE && waypoints.get())
            .build()
    );

    private final Setting<Boolean> soundPing = sgSurvey.add(
        new BoolSetting.Builder()
            .name("sound-notification")
            .description("Play an audible notification when discovering an archaeological dig site.")
            .defaultValue(false)
            .build()
    );
    private final Setting<SuspiciousBlocks> soundFor = sgSurvey.add(
        new EnumSetting.Builder<SuspiciousBlocks>()
            .name("ping-for")
            .description("Whether to play sound notifications for suspicious sand, gravel, or both.")
            .defaultValue(SuspiciousBlocks.Both)
            .visible(soundPing::get)
            .build()
    );
    private final Setting<Double> pingVolume = sgSurvey.add(
        new DoubleSetting.Builder()
            .name("notification-volume")
            .description("Volume for the sound notification.")
            .range(0.0, 400.0).sliderRange(0.0, 100.0).defaultValue(50.0)
            .visible(soundPing::get)
            .build()
    );

    private final Setting<Boolean> render = sgSurvey.add(
        new BoolSetting.Builder()
            .name("render-suspicious-blocks")
            .description("Whether to render suspicious blocks.")
            .defaultValue(true)
            .build()
    );
    private final Setting<ESPBlockData> sandESP = sgSurvey.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("suspicious-sand-ESP")
            .description("Render settings for suspicious sand blocks.")
            .visible(render::get)
            .defaultValue(new ESPBlockData(
                ShapeMode.Both,
                new SettingColor(169, 169, 13, 255),
                new SettingColor(169, 169, 13, 7),
                true,
                new SettingColor(169, 169, 13, 137)
            ))
            .build()
    );
    private final Setting<ESPBlockData> gravelESP = sgSurvey.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("suspicious-gravel-ESP")
            .description("Render settings for suspicious gravel blocks.")
            .visible(render::get)
            .defaultValue(new ESPBlockData(
                ShapeMode.Both,
                new SettingColor(69, 69, 69, 255),
                new SettingColor(69, 69, 69, 25),
                true,
                new SettingColor(69, 69, 69, 137)
            ))
            .build()
    );

    private int timer = 0;
    private long lastPing = 0L;
    private int ticksBrushing = 0;
    private long lastNotified = 0L;
    private @Nullable BlockPos lastFoundPos = null;
    private final List<Long> toIgnore = new LongArrayList();
    private final Set<BlockPos> goodBlocks = new HashSet<>();
    private final BlockPos.Mutable testPos = new BlockPos.Mutable();
    private final BlockPos.Mutable testPos2 = new BlockPos.Mutable();
    private final Set<BlockPos> safeBlocksToBreak = new HashSet<>();
    private final Set<BlockPos> safeBlocksToBrush = new HashSet<>();
    private final Set<BlockPos> preventingBreakageBlocks = new HashSet<>();
    private final List<BlockPos> suspiciousSandBlocks = new ObjectArrayList<>();
    private final List<BlockPos> suspiciousGravelBlocks = new ObjectArrayList<>();

    private boolean isOutOfRange(BlockPos pos1, BlockPos pos2, int range) {
        if (pos1 == null || pos2 == null) return true;
        testPos2.set(pos2.getX(), pos1.getY(), pos2.getZ());
        return !pos1.isWithinDistance(testPos2, range);
    }

    private boolean isSafeToBrush(BlockPos pos) {
        if (mc.world == null) return false;
        if (safeBlocksToBrush.contains(pos)) return true;

        if (mc.world.getBlockState(pos.down()).isAir() || mc.world.getBlockState(pos.down()).isReplaceable()) {
            preventingBreakageBlocks.add(pos.down());
            MsgUtil.updateModuleMsg("It is not yet safe to brush this suspicious block, because it is §cfloating..! §8[§aTry placing a solid block underneath it§8]", this.name, "directBrushPrevent".hashCode());
            return false;
        }
        BlockPos.Mutable pos2 = new BlockPos.Mutable();
        for (Direction dir : Direction.values()) {
            pos2.set(pos.offset(dir));
            if (!toIgnore.contains(pos2.asLong())
                && (mc.world.getBlockState(pos2).isOf(Blocks.SUSPICIOUS_SAND)
                || mc.world.getBlockState(pos2).isOf(Blocks.SUSPICIOUS_GRAVEL)))
            {
                if (mc.world.getBlockState(pos2.down()).isAir() || mc.world.getBlockState(pos2.down()).isReplaceable()) {
                    preventingBreakageBlocks.add(pos2);
                    MsgUtil.updateModuleMsg("It is not yet safe to brush this suspicious block, as doing so will update an adjacent floating one§e..!", this.name, "preventBreakageBrush".hashCode());
                    return false;
                }
            }
        }

        safeBlocksToBrush.add(pos);
        return true;
    }

    private boolean isSafeToBreak(BlockPos pos) {
        if (mc.world == null) return false;
        if (safeBlocksToBreak.contains(pos)) return true;
        if (!toIgnore.contains(pos.asLong())
            && (mc.world.getBlockState(pos).isOf(Blocks.SUSPICIOUS_SAND)
            || mc.world.getBlockState(pos).isOf(Blocks.SUSPICIOUS_GRAVEL))) return false;

        BlockPos.Mutable checkPos = new BlockPos.Mutable();

        // Indirect block update check
        for (Direction dir : Direction.values()) {
            checkPos.set(pos.offset(dir));
            if (!toIgnore.contains(checkPos.asLong()) && mc.world.getBlockState(checkPos).isOf(Blocks.SUSPICIOUS_SAND) || mc.world.getBlockState(checkPos).isOf(Blocks.SUSPICIOUS_GRAVEL)) {
                if (mc.world.getBlockState(checkPos.down()).isAir() || mc.world.getBlockState(checkPos.down()).isReplaceable()) {
                    preventingBreakageBlocks.add(new BlockPos(checkPos));
                    MsgUtil.updateModuleMsg("§aPreventing accidental breakage from indirect block update for floating suspicious block§c..!", this.name, "indirectBreakPrevent".hashCode());
                    return false;
                }
            }
        }

        checkPos.set(pos.up());
        while (checkPos.getY() < mc.world.getHeight()) {
            BlockState checkState = mc.world.getBlockState(checkPos);
            if (!toIgnore.contains(checkPos.asLong())
                && (checkState.isOf(Blocks.SUSPICIOUS_GRAVEL)
                || checkState.isOf(Blocks.SUSPICIOUS_SAND)))
            {
                preventingBreakageBlocks.add(new BlockPos(checkPos));
                return false;
            }
            else if (!(checkState.getBlock() instanceof FallingBlock)) {
                break;
            } else {
                // Indirect block update check
                for (Direction dir : Direction.values()) {
                    // Skip up & down since we're already scanning the column
                    if (dir.equals(Direction.UP) || dir.equals(Direction.DOWN)) continue;
                    BlockPos.Mutable indirectPos = new BlockPos.Mutable();
                    indirectPos.set(checkPos.offset(dir));
                    if (!toIgnore.contains(indirectPos.asLong())
                        && (mc.world.getBlockState(indirectPos).isOf(Blocks.SUSPICIOUS_GRAVEL)
                        || mc.world.getBlockState(indirectPos).isOf(Blocks.SUSPICIOUS_SAND)))
                    {
                        if (mc.world.getBlockState(indirectPos.down()).isAir() || mc.world.getBlockState(indirectPos.down()).isReplaceable()) {
                            preventingBreakageBlocks.add(new BlockPos(indirectPos));
                            MsgUtil.updateModuleMsg("§ePreventing accidental breakage from indirect block update for floating suspicious block§c..!", this.name, "indirectBreakPrevent".hashCode());
                            return false;
                        }
                    }
                }
                checkPos.set(checkPos.up());
            }
        }

        safeBlocksToBreak.add(pos);
        return true;
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        lastPing = 0L;
        toIgnore.clear();
        ticksBrushing = 0;
        goodBlocks.clear();
        lastFoundPos = null;
        safeBlocksToBreak.clear();
        suspiciousSandBlocks.clear();
        suspiciousGravelBlocks.clear();
        preventingBreakageBlocks.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onStartBlockBreak(StartBreakingBlockEvent event) {
        if (!Utils.canUpdate() || !preventBreak.get()) return;
        if (!isSafeToBreak(event.blockPos)) {
            event.cancel();
            MsgUtil.updateModuleMsg("Preventing accidental breakage§c..!", this.name, "blockBreakPrevent".hashCode());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        ++timer;
        if (timer >= 5) {
            timer = 0;
            synchronized (suspiciousSandBlocks) {
                suspiciousSandBlocks
                    .removeIf(pos -> isOutOfRange(pos, mc.player.getBlockPos(), 256) || !(mc.world.getBlockEntity(pos) instanceof BrushableBlockEntity));
            }
            synchronized (suspiciousGravelBlocks) {
                suspiciousGravelBlocks
                    .removeIf(pos -> isOutOfRange(pos, mc.player.getBlockPos(), 256) || !(mc.world.getBlockEntity(pos) instanceof BrushableBlockEntity));
            }
            synchronized (preventingBreakageBlocks) {
                preventingBreakageBlocks.removeIf(pos -> !(mc.world.getBlockEntity(pos) instanceof BrushableBlockEntity));
            }
            safeBlocksToBreak
                .removeIf(pos -> !(mc.world.getBlockEntity(pos) instanceof BrushableBlockEntity) || isOutOfRange(pos, mc.player.getBlockPos(), 256));
            safeBlocksToBrush
                .removeIf(pos -> !(mc.world.getBlockEntity(pos) instanceof BrushableBlockEntity) || isOutOfRange(pos, mc.player.getBlockPos(), 256));

            for (BlockEntity be : Utils.blockEntities()) {
                if (suspiciousSandBlocks.contains(be.getPos()) || suspiciousGravelBlocks.contains(be.getPos()))
                    continue;
                if (be instanceof BrushableBlockEntity && !toIgnore.contains(be.getPos().asLong())) {
                    if (mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_GRAVEL)) {
                        suspiciousGravelBlocks.add(be.getPos());
                    } else {
                        suspiciousSandBlocks.add(be.getPos());
                    }

                    // Ignore Y value for distance checks
                    testPos.set(be.getPos().getX(), lastFoundPos == null ? be.getPos().getY() : lastFoundPos.getY(), be.getPos().getZ());
                    if (lastFoundPos == null || isOutOfRange(lastFoundPos, testPos, 69)) {
                        lastFoundPos = be.getPos();
                        if (StardustUtil.XAERO_AVAILABLE && waypoints.get()) switch (waypointsFor.get()) {
                            case SuspiciousSand -> {
                                if (mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_SAND)) {
                                    MapUtil.addWaypoint(
                                        be.getPos(), "Archaeology Dig Site", "ᛩ",
                                        MapUtil.Purpose.Normal, MapUtil.WpColor.Random, tempWaypoints.get()
                                    );
                                }
                            }
                            case SuspiciousGravel -> {
                                if (mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_GRAVEL)) {
                                    MapUtil.addWaypoint(
                                        be.getPos(), "Archaeology Dig Site", "ᛩ",
                                        MapUtil.Purpose.Normal, MapUtil.WpColor.Random, tempWaypoints.get()
                                    );
                                }
                            }
                            default -> MapUtil.addWaypoint(
                                be.getPos(), "Archaeology Dig Site", "ᛩ",
                                MapUtil.Purpose.Normal, MapUtil.WpColor.Random, tempWaypoints.get()
                            );
                        }

                        if (soundPing.get()) switch (soundFor.get()) {
                            case SuspiciousSand -> {
                                if (mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_SAND)) {
                                    long now = System.currentTimeMillis();
                                    if (now - lastPing >= 1337) {
                                        lastPing = now;
                                        mc.player.playSound(
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.ITEM_BRUSH_BRUSHING_SAND : SoundEvents.ITEM_BRUSH_BRUSHING_SAND_COMPLETE,
                                            pingVolume.get().floatValue(),
                                            ThreadLocalRandom.current().nextFloat(0.42f, 1.337f)
                                        );
                                    }
                                }
                            }
                            case SuspiciousGravel -> {
                                if (mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_GRAVEL)) {
                                    long now = System.currentTimeMillis();
                                    if (now - lastPing >= 1337) {
                                        lastPing = now;
                                        mc.player.playSound(
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL : SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL_COMPLETE,
                                            pingVolume.get().floatValue(),
                                            ThreadLocalRandom.current().nextFloat(0.42f, 1.337f)
                                        );
                                    }
                                }
                            }
                            default -> {
                                long now = System.currentTimeMillis();
                                if (now - lastPing >= 1337) {
                                    lastPing = now;
                                    mc.player.playSound(
                                        mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_SAND) ?
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.ITEM_BRUSH_BRUSHING_SAND : SoundEvents.ITEM_BRUSH_BRUSHING_SAND_COMPLETE :
                                            ThreadLocalRandom.current().nextInt(2) == 0 ? SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL : SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL_COMPLETE,
                                        pingVolume.get().floatValue(),
                                        ThreadLocalRandom.current().nextFloat(0.42f, 1.337f)
                                    );
                                }
                            }
                        }

                        if (chatNotify.get()) {
                            switch (chatFor.get()) {
                                case SuspiciousSand -> {
                                    if (mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_GRAVEL)) continue;
                                }
                                case SuspiciousGravel -> {
                                    if (mc.world.getBlockState(be.getPos()).isOf(Blocks.SUSPICIOUS_SAND)) continue;
                                }
                                default -> {
                                }
                            }
                            StringBuilder sb = new StringBuilder();

                            sb.append("Located an Archaeological Dig Site");
                            if (chatCoords.get()) {
                                sb.append(" at §8[§5").append(be.getPos().getX()).append("§8, §5")
                                    .append(be.getPos().getY()).append("§8, §5").append(be.getPos().getZ()).append("§8]");
                            }
                            sb.append(StardustUtil.rCC()).append("..!");
                            MsgUtil.sendModuleMsg(sb.toString(), this.name);
                        }
                    }
                }
            }
        }

        HitResult target = mc.crosshairTarget;
        if (!(target instanceof BlockHitResult blockHit)) return;

        BlockPos hitPos = blockHit.getBlockPos();
        if (mc.world.getBlockEntity(hitPos) instanceof BrushableBlockEntity) {
            if (toIgnore.contains(hitPos.asLong())) {
                mc.options.useKey.setPressed(false);

                if (breakBad.get()) {
                    FindItemResult result = InvUtils.findInHotbar(stack -> stack.getItem() instanceof ShovelItem);
                    if (result.found() && result.slot() != mc.player.getInventory().selectedSlot && !(mc.player.getOffHandStack().getItem() instanceof ShovelItem)) {
                        InvUtils.swap(result.slot(), false);
                    }
                    if (isSafeToBreak(hitPos)) BlockUtils.breakBlock(hitPos, true);
                    else {
                        MsgUtil.updateModuleMsg("Preventing accidental breakage§c..!", this.name, "blockBreakPrevent".hashCode());
                    }
                }
                return;
            }

            FindItemResult result = InvUtils.findInHotbar(Items.BRUSH);
            if (result.found()) {
                boolean offHand = mc.player.getOffHandStack().isOf(Items.BRUSH);
                if (mc.player.getInventory().selectedSlot == result.slot() || offHand) {
                    if (isSafeToBrush(hitPos)) {
                        if (!mc.player.isUsingItem() && mc.interactionManager != null) {
                            mc.interactionManager.interactItem(mc.player, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                        }
                        mc.options.useKey.setPressed(true);
                    }
                } else {
                    InvUtils.swap(result.slot(),false);
                    if (isSafeToBrush(hitPos)) {
                        if (!mc.player.isUsingItem() && mc.interactionManager != null) {
                            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        }
                        mc.options.useKey.setPressed(true);
                    }
                }
            } else {
                long now = System.currentTimeMillis();
                if (now - lastNotified >= 7777) {
                    lastNotified = now;
                    MsgUtil.sendModuleMsg("§4No brush found in hotbar§7..!", this.name);
                }
            }
        } else if (mc.player.getMainHandStack().isOf(Items.BRUSH) || mc.player.getOffHandStack().isOf(Items.BRUSH)) {
            mc.options.useKey.setPressed(false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        HitResult target = mc.crosshairTarget;
        if (!(target instanceof BlockHitResult blockHit)) return;

        BlockPos hitPos = blockHit.getBlockPos();
        if (toIgnore.contains(hitPos.asLong())) return;
        if (mc.world.getBlockEntity(hitPos) instanceof BrushableBlockEntity brushableBlock) {
            if (StardustUtil.XAERO_AVAILABLE && waypoints.get() && (suspiciousGravelBlocks.contains(hitPos) || suspiciousSandBlocks.contains(hitPos))) {
                MapUtil.removeWaypoints(
                    "Archaeology",
                    pos -> pos.isWithinDistance(hitPos, 64),
                    Optional.of(hitPos.getY())
                );
            }

            ItemStack stack = brushableBlock.getItem();
            if (stack == ItemStack.EMPTY || stack.getItem() == Items.AIR) {
                preventingBreakageBlocks.add(hitPos);
                if (mc.player.isUsingItem()) ++ticksBrushing;

                if (ticksBrushing > 20) {
                    ticksBrushing = 0;
                    LogUtil.warn("Retrying brush packet after response timeout...", this.name);
                    mc.player.stopUsingItem();
                    mc.options.useKey.setPressed(false);
                }
                return;
            } else {
                ticksBrushing = 0;
            }

            if (!targetItems.get().contains(stack.getItem())) {
                toIgnore.add(hitPos.asLong());
                if (chat.get() && breakBad.get()) {
                    MsgUtil.sendModuleMsg("Breaking suspicious block containing §c" + stack.getName().getString() + "§8.", this.name);
                }
                preventingBreakageBlocks.add(hitPos);
            } else {
                goodBlocks.add(hitPos);
                preventingBreakageBlocks.add(hitPos);
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        if (mc.player == null || mc.world == null) return;

        for (BlockPos pos : preventingBreakageBlocks) {
            if (toIgnore.contains(pos.asLong())) {
                RenderUtil.renderBlock(
                    event, pos,
                    new SettingColor(169, 13, 13, 255, true),
                    new SettingColor(169, 13, 13, 13, true),
                    ShapeMode.Both
                );
            } else if (goodBlocks.contains(pos)) {
                RenderUtil.renderBlock(
                    event, pos,
                    new SettingColor(13, 169, 69, 255, true),
                    new SettingColor(13, 169, 69, 13, true),
                    ShapeMode.Both
                );
            } else {
                RenderUtil.renderBlock(
                    event, pos,
                    new SettingColor(137, 169, 4, 255, true),
                    new SettingColor(137, 169, 4, 13, true),
                    ShapeMode.Both
                );
            }
        }

        ESPBlockData sand = sandESP.get();
        ESPBlockData gravel = gravelESP.get();
        for (BlockPos pos : suspiciousSandBlocks) {
            if (preventingBreakageBlocks.contains(pos)) continue;
            if (RenderUtil.shouldRenderBox(sand)) {
                int distance = pos.getManhattanDistance(mc.player.getBlockPos());
                if (distance <= 128) {
                    Color sideColor = new Color(sand.sideColor.r, sand.sideColor.g, sand.sideColor.b, MathHelper.clamp((int) Math.floor(sand.sideColor.a * ((128 - distance) * 0.333)), sand.sideColor.a, Math.max(sand.sideColor.a, 69)));
                    RenderUtil.renderBlock(event, pos, sand.lineColor, sideColor, sand.shapeMode);
                } else {
                    RenderUtil.renderBlock(event, pos, sand.lineColor, sand.sideColor, sand.shapeMode);
                }
            }
            if (RenderUtil.shouldRenderTracer(sand)) {
                RenderUtil.renderTracerTo(event, pos, sand.tracerColor);
            }
        }
        for (BlockPos pos : suspiciousGravelBlocks) {
            if (preventingBreakageBlocks.contains(pos)) continue;
            if (RenderUtil.shouldRenderBox(gravel)) {
                int distance = pos.getManhattanDistance(mc.player.getBlockPos());
                if (distance <= 128) {
                    Color sideColor = new Color(gravel.sideColor.r, gravel.sideColor.g, gravel.sideColor.b, MathHelper.clamp((int) Math.floor(gravel.sideColor.a * ((128 - distance) * 0.333)), gravel.sideColor.a, Math.max(gravel.sideColor.a, 69)));
                    RenderUtil.renderBlock(event, pos, gravel.lineColor, sideColor, gravel.shapeMode);
                } else {
                    RenderUtil.renderBlock(event, pos, gravel.lineColor, gravel.sideColor, gravel.shapeMode);
                }
            }
            if (RenderUtil.shouldRenderTracer(gravel)) {
                RenderUtil.renderTracerTo(event, pos, gravel.tracerColor);
            }
        }
    }

    public boolean isLootItem(Item item) {
        return ARCHAEOLOGY_LOOT_TABLE.contains(item);
    }
}
