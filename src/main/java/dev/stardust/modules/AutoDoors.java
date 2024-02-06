package dev.stardust.modules;

import dev.stardust.Stardust;
import net.minecraft.block.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import meteordevelopment.meteorclient.settings.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AutoDoors extends Module {
    public AutoDoors()  {
        super(Stardust.CATEGORY, "AutoDoors", "Automatically interact with doors.");
    }

    public enum DoorModes {
        Classic, Spammer
    }
    public enum MuteModes {
        Never, Always, Spammer
    }

    private final Setting<DoorModes> modeSetting = settings.getDefaultGroup().add(
        new EnumSetting.Builder<DoorModes>()
            .name("Mode")
            .description("Which mode to operate in.")
            .defaultValue(DoorModes.Classic)
            .build()
    );

    private final Setting<MuteModes> muteSetting = settings.getDefaultGroup().add(
        new EnumSetting.Builder<MuteModes>()
            .name("Mute Doors")
            .description("Whether to mute door sounds when the module is active.")
            .defaultValue(MuteModes.Never)
            .build()
    );

    private final Setting<Integer> spamRange = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Spam Range")
            .description("Range of blocks to look for doors in.")
            .range(1, 5)
            .sliderRange(1, 5)
            .defaultValue(5)
            .visible(() -> modeSetting.get() == DoorModes.Spammer)
            .build()
    );

    private final Setting<Integer> spamRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Spam Delay")
            .description("Delay (in ticks) between each interaction.")
            .range(2, 20)
            .sliderRange(2, 20)
            .defaultValue(2)
            .visible(() -> modeSetting.get() == DoorModes.Spammer)
            .build()
    );

    private final Setting<Integer> interactDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Lever Delay")
            .description("Increase this if iron doors controlled by levers are acting scuffed.")
            .range(0, 100)
            .sliderRange(2, 60)
            .defaultValue(5)
            .build()
    );

    private final Setting<Boolean> autoOpen = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Auto Open")
            .description("Automatically open doors as you move towards them.")
            .defaultValue(true)
            .visible(() -> modeSetting.get() == DoorModes.Classic)
            .build()
    );

    private final Setting<Boolean> silentSwing = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Silent Swing")
            .description("No client-side hand swing.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ninjaSwing = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Ninja Swing")
            .description("No server-side hand swing.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> useIronDoors = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Iron Doors")
            .description("Interact with iron doors using nearby buttons or levers.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> useTrapdoors = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Trapdoors")
            .description("Interact with trapdoors (only works when not on ladders.)")
            .defaultValue(false)
            .build()
    );

    private int tickCounter = 0;
    private int ticksSinceInteracted = 0;
    private Vec3d lastBlock = new Vec3d(0.0, 0.0, 0.0);


    // See DoorBlockMixin.java
    public boolean shouldMute() {
        return this.isActive() && (muteSetting.get() == MuteModes.Always
            || (modeSetting.get() == DoorModes.Spammer && muteSetting.get() == MuteModes.Spammer));
    }

    private void interactDoor(BlockPos pos, Direction direction) {
        if (mc.player == null) return;
        Vec3d pPos = mc.player.getPos();

        Direction side;
        switch (direction) {
            case EAST -> {
                if (pPos.x < pos.getX()) side = Direction.WEST;
                else side = Direction.EAST;
            }
            case WEST -> {
                if (pPos.x > pos.getX()) side = Direction.EAST;
                else side = Direction.WEST;
            }
            case NORTH -> {
                if (pPos.z > pos.getZ()) side = Direction.SOUTH;
                else side = Direction.NORTH;
            }
            case SOUTH -> {
                if (pPos.z < pos.getZ()) side = Direction.NORTH;
                else side = Direction.SOUTH;
            }
            case UP -> {
                if (pPos.y < pos.getY()) side = Direction.DOWN;
                else side = Direction.UP;
            }
            case DOWN -> {
                if (pPos.y > pos.getY()) side = Direction.DOWN;
                else side = Direction.UP;
            }
            default -> side = Direction.DOWN;
        }

        if (mc.interactionManager == null) return;
        mc.interactionManager.interactBlock(
            mc.player,
            Hand.MAIN_HAND,
            new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), side, pos, true)
        );

        if (silentSwing.get() && ninjaSwing.get()) return;
        if (!silentSwing.get() && ninjaSwing.get()) {
            ((LivingEntity) mc.player).swingHand(Hand.MAIN_HAND);
        }else if (silentSwing.get() && !ninjaSwing.get()) {
            if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }else mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean scanForSwitches(BlockPos pos, Block block, Boolean open, Direction moving, Direction side, int n) {
        if (mc.world == null) return true;
        if (block instanceof ButtonBlock || block instanceof LeverBlock) {
            BlockState state = mc.world.getBlockState(pos);
            try {
                if (open && block instanceof ButtonBlock && state.get(ButtonBlock.POWERED)) return false;
                else if (open && block instanceof LeverBlock && state.get(LeverBlock.POWERED)) return false;
                else if(!open && block instanceof LeverBlock && !state.get(LeverBlock.POWERED)) return false;
            } catch (IllegalArgumentException ignored) {} // skill issue insurance

            if (!open && block instanceof ButtonBlock) return true;
            this.interactDoor(pos.offset(side, n), moving);
            return true;
        } else {
            BlockState upState = mc.world.getBlockState(pos.offset(moving.getOpposite()).offset(side, n).up());
            BlockState downState = mc.world.getBlockState(pos.offset(moving.getOpposite()).offset(side, n).down());
            Block upBlock = upState.getBlock();
            Block downBlock = downState.getBlock();

            if (upBlock instanceof ButtonBlock || upBlock instanceof LeverBlock) {
                try {
                    if (open && upBlock instanceof ButtonBlock && upState.get(ButtonBlock.POWERED)) return false;
                    else if (open && upBlock instanceof LeverBlock && upState.get(LeverBlock.POWERED)) return false;
                    else if(!open && upBlock instanceof LeverBlock && !upState.get(LeverBlock.POWERED)) return false;
                }catch (IllegalArgumentException ignored) {}

                if (!open && upBlock instanceof ButtonBlock) return true;
                this.interactDoor(pos.offset(moving.getOpposite()).offset(side, n).up(), moving);
                return true;
            } else if (downBlock instanceof ButtonBlock || downBlock instanceof LeverBlock) {
                try {
                    if (open && downBlock instanceof ButtonBlock && downState.get(ButtonBlock.POWERED)) return false;
                    else if (open && downBlock instanceof LeverBlock && downState.get(LeverBlock.POWERED)) return false;
                    else if(!open && downBlock instanceof LeverBlock && !downState.get(LeverBlock.POWERED)) return false;
                } catch (IllegalArgumentException ignored) {}

                if (!open && downBlock instanceof ButtonBlock) return true;
                this.interactDoor(pos.offset(moving).offset(side, n).down(), moving);
                return true;
            }
        }

        return false;
    }

    private void tryInteractIronDoor(BlockPos pos, BlockState state, Direction direction, boolean open) {
        if (mc.world == null || mc.interactionManager == null) return;
        if (!(state.getBlock() instanceof DoorBlock ironDoor)) return;
        if (open == ironDoor.isOpen(state)) return;

        this.ticksSinceInteracted = 0;
        for (int n = 0; n < 4; n++) {
            for (Direction side : Direction.values()) {
                Block offset = mc.world.getBlockState(pos.offset(direction.getOpposite()).offset(side, n)).getBlock();
                Block offset2 = mc.world.getBlockState(pos.offset(side, n)).getBlock();
                Block offset3 = mc.world.getBlockState(pos.offset(direction).offset(side, n)).getBlock();

                if (this.scanForSwitches(pos, offset, open, direction, side, n)) return;
                else if (this.scanForSwitches(pos, offset2, open, direction, side, n)) return;
                else if (this.scanForSwitches(pos, offset3, open, direction, side, n)) return;
            }
        }
    }

    private LongArrayList getSurroundingDoors() {
        LongArrayList doors = new LongArrayList();
        if (mc.player == null || mc.world == null) return doors;

        int range = spamRange.get();
        BlockPos bPos = mc.player.getBlockPos();
        BlockPos.Mutable doorPos = new BlockPos.Mutable();
        for (int x = bPos.getX() - range; x < bPos.getX() + range; x++) {
            for (int y = bPos.getY() - range; y < bPos.getY() + range; y++) {
                for (int z = bPos.getZ() - range; z < bPos.getZ() + range; z++) {
                    doorPos.set(x, y, z);
                    if (mc.world.getBlockState(doorPos).getBlock() instanceof DoorBlock) {
                        doors.add(doorPos.asLong());
                    }
                }
            }
        }

        return doors;
    }


    @Override
    public void onDeactivate() {
        this.tickCounter = 0;
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (modeSetting.get() == DoorModes.Spammer || mc.player == null || mc.world == null) return;
        if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() instanceof PressurePlateBlock) return;

        ++this.ticksSinceInteracted;
        Vec3d pPos = mc.player.getPos();
        if (pPos.x <= this.lastBlock.x + .1337 && pPos.x >= this.lastBlock.x - .1337
            && pPos.z <= this.lastBlock.z + .1337 && pPos.z >= this.lastBlock.z - .1337) return;

        this.lastBlock = pPos;
        double velocityX = event.movement.x;
        double velocityY = event.movement.y;
        double velocityZ = event.movement.z;

        double directionRadians = Math.atan2(-velocityZ, velocityX);
        double directionDegrees = Math.toDegrees(directionRadians);
        double normalizedDegrees = (directionDegrees + 360) % 360;

        Direction movementDirection;
        if (normalizedDegrees >= 45 && normalizedDegrees < 135) movementDirection = Direction.NORTH;
        else if (normalizedDegrees >= 135 && normalizedDegrees < 225) movementDirection = Direction.WEST;
        else if (normalizedDegrees >= 225 && normalizedDegrees < 315) movementDirection = Direction.SOUTH;
        else if (normalizedDegrees >= 315 || normalizedDegrees < 45) movementDirection = Direction.EAST;
        else if (velocityY > 0) movementDirection = Direction.UP;
        else movementDirection = Direction.DOWN;

        BlockPos frontPos;
        BlockPos behindPos;
        BlockPos pbPos = mc.player.getBlockPos();
        switch (movementDirection) {
            case NORTH -> {
                frontPos = pbPos.north();
                behindPos = pbPos.south();
            }
            case SOUTH -> {
                frontPos = pbPos.south();
                behindPos = pbPos.north();
            }
            case EAST -> {
                frontPos = pbPos.east();
                behindPos = pbPos.west();
            }
            case WEST -> {
                frontPos = pbPos.west();
                behindPos = pbPos.east();
            }
            case UP -> {
                frontPos = pbPos.up();
                behindPos = pbPos.down();
            }
            default -> {
                frontPos = pbPos.down();
                behindPos = pbPos.up();
            }
        }

        String yString = String.valueOf(pPos.y);
        String sunk = yString.substring(yString.indexOf(".")+1, yString.indexOf(".")+2);
        if (mc.player.isOnGround() && Integer.parseInt(sunk) >= 5) {
            frontPos = frontPos.up();
            behindPos = behindPos.up();
        }
        BlockState frontState = mc.world.getBlockState(frontPos);
        BlockState behindState = mc.world.getBlockState(behindPos);
        Block doorInFront = frontState.getBlock();
        Block doorBehind = behindState.getBlock();

        if (useTrapdoors.get() && doorInFront instanceof TrapdoorBlock && autoOpen.get()) {
            try {
                if (!frontState.get(TrapdoorBlock.OPEN)) {
                    this.interactDoor(frontPos, movementDirection);
                    return;
                }
            } catch (IllegalArgumentException ignored) {} // skill issue insurance
        } else if (useTrapdoors.get() && mc.world.getBlockState(frontPos.down()).getBlock() instanceof TrapdoorBlock && autoOpen.get()) {
            try {
                if (!mc.world.getBlockState(frontPos.down()).get(TrapdoorBlock.OPEN)) {
                    this.interactDoor(frontPos.down(), Direction.DOWN);
                    return;
                }
            } catch (IllegalArgumentException ignored) {}
        }
        if (doorInFront instanceof DoorBlock frontDoor && autoOpen.get()) {
            if (useIronDoors.get() && frontDoor == Blocks.IRON_DOOR) {
                if (this.ticksSinceInteracted >= interactDelay.get()) {
                    this.tryInteractIronDoor(frontPos, frontState, movementDirection, true);
                }
                return;
            } else if (frontDoor == Blocks.IRON_DOOR) return;
            if (!frontDoor.isOpen(frontState)) this.interactDoor(frontPos, movementDirection);
            switch (movementDirection) {
                case NORTH, SOUTH -> {
                    BlockState eastState = mc.world.getBlockState(frontPos.east());
                    BlockState westState = mc.world.getBlockState(frontPos.west());
                    if (eastState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (!nextDoor.isOpen(eastState)) this.interactDoor(frontPos.east(), movementDirection);
                    } else if (westState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (!nextDoor.isOpen(westState)) this.interactDoor(frontPos.west(), movementDirection);
                    }
                }
                case EAST, WEST -> {
                    BlockState northState = mc.world.getBlockState(frontPos.north());
                    BlockState southState = mc.world.getBlockState(frontPos.south());
                    if (northState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (!nextDoor.isOpen(northState)) this.interactDoor(frontPos.north(), movementDirection);
                    } else if (southState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (!nextDoor.isOpen(southState)) this.interactDoor(frontPos.south(), movementDirection);
                    }
                }
                default -> {}
            }
        }
        if (useTrapdoors.get() && doorBehind instanceof TrapdoorBlock) {
            try {
                if (behindState.get(TrapdoorBlock.OPEN)) {
                    this.interactDoor(behindPos, movementDirection);
                    return;
                }
            }catch (IllegalArgumentException ignored) {}
        } else if (useTrapdoors.get() && mc.world.getBlockState(behindPos.down()).getBlock() instanceof TrapdoorBlock) {
            try {
                if (mc.world.getBlockState(behindPos.down()).get(TrapdoorBlock.OPEN)) {
                    this.interactDoor(behindPos.down(), Direction.DOWN);
                    return;
                }
            } catch (IllegalArgumentException ignored) {}
        }
        if (doorBehind instanceof DoorBlock behindDoor) {
            if (useIronDoors.get() && behindDoor == Blocks.IRON_DOOR) {
                if (this.ticksSinceInteracted >= interactDelay.get()) {
                    this.tryInteractIronDoor(behindPos, behindState, movementDirection, false);
                }
                return;
            } else if (behindDoor == Blocks.IRON_DOOR) return;
            if (behindDoor.isOpen(behindState)) this.interactDoor(behindPos, movementDirection);
            switch (movementDirection) {
                case NORTH, SOUTH -> {
                    BlockState eastState = mc.world.getBlockState(behindPos.east());
                    BlockState westState = mc.world.getBlockState(behindPos.west());
                    if (eastState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (nextDoor.isOpen(eastState)) this.interactDoor(behindPos.east(), movementDirection);
                    } else if (westState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (nextDoor.isOpen(westState)) this.interactDoor(behindPos.west(), movementDirection);
                    }
                }
                case EAST, WEST -> {
                    BlockState northState = mc.world.getBlockState(behindPos.north());
                    BlockState southState = mc.world.getBlockState(behindPos.south());
                    if (northState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (nextDoor.isOpen(northState)) this.interactDoor(behindPos.north(), movementDirection);
                    } else if (southState.getBlock() instanceof DoorBlock nextDoor) {
                        if (nextDoor == Blocks.IRON_DOOR) return;
                        if (nextDoor.isOpen(southState)) this.interactDoor(behindPos.south(), movementDirection);
                    }
                }
                default -> {}
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (modeSetting.get() == DoorModes.Classic) return;

        ++this.tickCounter;
        if (this.tickCounter >= spamRate.get()) {
            this.tickCounter = 0;
            if (mc.player == null || mc.world == null) return;

            Vec3d pPos = mc.player.getPos();
            LongArrayList doors = this.getSurroundingDoors();
            for (long door : doors) {
                BlockPos doorPos = BlockPos.fromLong(door);
                if (mc.world.getBlockState(doorPos).getBlock() == Blocks.IRON_DOOR) continue;

                Direction side;
                if (pPos.x > doorPos.getX()) side = Direction.EAST;
                else if (pPos.x < doorPos.getX()) side = Direction.WEST;
                else if (pPos.z > doorPos.getZ()) side = Direction.SOUTH;
                else if (pPos.z < doorPos.getZ()) side = Direction.NORTH;
                else if (pPos.y > doorPos.getY()) side = Direction.UP;
                else side = Direction.DOWN;

                this.interactDoor(doorPos, side);
            }
        }
    }
}
