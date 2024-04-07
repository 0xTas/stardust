package dev.stardust.modules;

import java.util.*;
import java.io.File;
import java.util.List;
import java.time.Instant;
import java.time.Duration;
import java.nio.file.Files;
import java.time.LocalDate;
import dev.stardust.Stardust;
import net.minecraft.block.*;
import net.minecraft.text.Text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.stream.Collectors;
import net.minecraft.nbt.NbtCompound;
import dev.stardust.util.StardustUtil;
import dev.stardust.util.StardustUtil.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.RegistryKey;
import java.time.format.DateTimeFormatter;
import net.minecraft.util.shape.VoxelShape;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.entity.BlockEntity;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class ChatSigns extends Module {
    public ChatSigns() {
        super(Stardust.CATEGORY, "ChatSigns", "Read nearby signs in your chat.");
    }

    public enum ChatMode {
        ESP, Targeted, Both
    }
    public enum RepeatMode {
        Cooldown, On_Focus
    }

    private final String BLACKLIST_FILE = "meteor-client/chatsigns-blacklist.txt";

    private final SettingGroup modesGroup = settings.createGroup("Modes", true);
    private final SettingGroup formatGroup = settings.createGroup("Formatting", true);
    private final SettingGroup oldSignGroup = settings.createGroup("OldSigns Settings", true);
    private final SettingGroup blacklistGroup = settings.createGroup("SignText Blacklist", true);

    private final Setting<ChatMode> chatMode = modesGroup.add(
        new EnumSetting.Builder<ChatMode>()
            .name("Chat Mode")
            .description("ESP = nearby only, Targeted = looking at only")
            .defaultValue(ChatMode.Both)
            .build()
    );

    private final Setting<RepeatMode> repeatMode = modesGroup.add(
        new EnumSetting.Builder<RepeatMode>()
            .name("Repeat Mode")
            .description("How to handle repeating signs you're actively looking at.")
            .defaultValue(RepeatMode.Cooldown)
            .visible(() -> chatMode.get() != ChatMode.ESP)
            .build()
    );

    private final Setting<Integer> repeatSeconds = modesGroup.add(
        new IntSetting.Builder()
            .name("Repeat Cooldown")
            .description("Value in seconds to wait before repeating looked-at signs.")
            .visible(() -> repeatMode.get() == RepeatMode.Cooldown && repeatMode.isVisible())
            .range(1, 3600)
            .sliderRange(1, 120)
            .defaultValue(10)
            .build()
    );

    private final Setting<Boolean> showOldSigns  = oldSignGroup.add(
        new BoolSetting.Builder()
            .name("Show Possibly Old Signs*")
            .description("*will show signs placed before 1.8, AND after 1.19. Use your best judgment to determine what's legit.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyOldSigns = oldSignGroup.add(
        new BoolSetting.Builder()
            .name("Only Show New/Old Signs")
            .description("Only display text from signs that are either really old, or brand new.")
            .defaultValue(false)
            .visible(showOldSigns::get)
            .build()
    );

    private final Setting<Boolean> ignoreNether = oldSignGroup.add(
        new BoolSetting.Builder()
            .name("Ignore Nether")
            .description("Ignore potentially-old signs in the nether (near highways they're all certainly new.)")
            .visible(showOldSigns::get)
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreDuplicates = modesGroup.add(
        new BoolSetting.Builder()
            .name("Ignore Duplicates")
            .description("Ignore duplicate signs instead of displaying them with a counter.")
            .defaultValue(false)
            .build()
    );

    private final Setting<TextColor> signColor = formatGroup.add(
        new EnumSetting.Builder<TextColor>()
            .name("Sign Color")
            .description("The color of displayed sign text.")
            .defaultValue(TextColor.Light_Gray)
            .build()
    );

    private final Setting<TextFormat> textFormat = formatGroup.add(
        new EnumSetting.Builder<TextFormat>()
            .name("Text Formatting")
            .description("Apply formatting to displayed sign text.")
            .defaultValue(TextFormat.Italic)
            .build()
    );

    private final Setting<TextColor> oldSignColor = oldSignGroup.add(
        new EnumSetting.Builder<TextColor>()
            .name("Old Sign Color")
            .description("Text color for signs that might be old.")
            .defaultValue(TextColor.Yellow)
            .visible(showOldSigns::get)
            .build()
    );

    private final Setting<TextFormat> oldSignFormat = oldSignGroup.add(
        new EnumSetting.Builder<TextFormat>()
            .name("Old Text Format")
            .description("Apply formatting to text displayed from (maybe) old signs.")
            .defaultValue(TextFormat.Italic)
            .visible(showOldSigns::get)
            .build()
    );

    private final Setting<Boolean> chatFormat = formatGroup.add(new BoolSetting.Builder()
        .name("Fancy Display")
        .description("Displays each line of the sign on separate lines in chat.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showCoords = formatGroup.add(new BoolSetting.Builder()
        .name("Show Coordinates")
        .description("Display sign coordinates in chat.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderOldSigns = oldSignGroup.add(new BoolSetting.Builder()
        .name("OldSign ESP")
        .description("Render signs which could be old through walls.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ESPBlockData> espSettings = oldSignGroup.add(new GenericSetting.Builder<ESPBlockData>()
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

    private final Setting<Boolean> signBlacklist = blacklistGroup.add(
        new BoolSetting.Builder()
            .name("SignText Blacklist")
            .description("Ignore signs that contain specific text (line-separated list in chatsigns-blacklist.txt)")
            .defaultValue(false)
            .onChanged(it -> {
                if (it && this.isActive() && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) {
                    this.blacklisted.clear();
                    initBlacklistText();
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Please write one blacklisted item for each line of the file."));
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Spaces and other punctuation will be treated literally."));
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Toggle the module after updating the blacklist's contents."));
                    }
                }
            })
            .build()
    );

    private final Setting<Boolean> caseSensitive = blacklistGroup.add(
        new BoolSetting.Builder()
            .name("Case-sensitive Blacklist")
            .description("Force matches in the blacklist file to be case-sensitive.")
            .defaultValue(false)
            .visible(signBlacklist::get)
            .build()
    );

    private final Setting<Boolean> openBlacklistFile = blacklistGroup.add(
        new BoolSetting.Builder()
            .name("Open Blacklist File")
            .description("Open the chatsigns-blacklist.txt file.")
            .defaultValue(false)
            .visible(signBlacklist::get)
            .onChanged(it -> {
                if (it) {
                    if (StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) openBlacklistFile();
                    else resetBlacklistFileSetting();
                }
            })
            .build()
    );

    private int timer = 0;
    @Nullable private BlockPos lastFocusedSign = null;
    private final HashSet<BlockPos> posSet = new HashSet<>();
    private final HashSet<BlockPos> oldSet = new HashSet<>();
    private final ArrayList<String> blacklisted = new ArrayList<>();
    private final HashMap<BlockPos, Instant> cooldowns = new HashMap<>();
    private final HashMap<String, Integer> signMessages = new HashMap<>();
    private final HashMap<ChunkPos, Boolean> chunkCache = new HashMap<>();
    private final Pattern fullYearsPattern = Pattern.compile("202[0-9]");
    private final Pattern fullDatesPattern = Pattern.compile("\\b(\\d{1,2}[-/\\. _,'+]\\d{1,2}[-/\\. _,'+]\\d{2,4}|\\d{4}[-/\\. _,'+]\\d{1,2}[-/\\. _,'+]\\d{1,2})\\b");

    @Nullable
    private BlockPos getTargetedSign() {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return null;
        int viewDistance = mc.options.getViewDistance().getValue();

        double maxRangeBlocks = viewDistance * 16;
        HitResult trace = player.raycast(maxRangeBlocks, mc.getTickDelta(), false);
        if (trace != null) {
            BlockPos pos = ((BlockHitResult) trace).getBlockPos();
            if (mc.world.getBlockEntity(pos) instanceof SignBlockEntity) return pos;
        }

        return null;
    }

    private ArrayList<SignBlockEntity> getNearbySigns(WorldChunk chunk) {
        ArrayList<SignBlockEntity> signs = new ArrayList<>();
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();

        if (blockEntities == null) return signs;
        blockEntities.forEach((pos, entity) -> {
            if (entity instanceof SignBlockEntity sbe) signs.add(sbe);
        });

        return signs;
    }

    private boolean isSignEmpty(SignBlockEntity sbe) {
        return !sbe.getFrontText().hasText(mc.player) && !sbe.getBackText().hasText(mc.player);
    }

    @Nullable
    private LocalDate parseDate(String dateStr) {
        String[] formats = {
            "M/d/yy", "M/dd/yy", "MM/d/yy", "MM/dd/yy",
            "M/d/yyyy", "M/dd/yyyy", "MM/d/yyyy", "MM/dd/yyyy",
            "d/M/yy", "d/MM/yy", "dd/M/yy", "dd/MM/yy", "d/M/yyyy",
            "d/MM/yyyy", "dd/M/yyyy", "dd/MM/yyyy", "yyyy/M/d", "yyyy/MM/d",
            "yyyy/M/dd", "yyyy/MM/dd", "yyyy/d/M", "yyyy/dd/M", "yyyy/d/MM", "yyyy/dd/MM",
        };
        for (String format : formats) {
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (Exception ignored) {
                continue;
            }
            return date;
        }
        return null;
    }

    private String formatSignText(SignBlockEntity sign, WorldChunk chunk) {
        if (mc.world == null || isSignEmpty(sign)) return "";
        ArrayList<String> lines = new ArrayList<>();

        String color = signColor.get().label;
        String format = textFormat.get().label;
        for (Text line : sign.getFrontText().getMessages(false)) {
            line.visit(msg -> {
                if (chatFormat.get()) {
                    lines.add(msg);
                }else {
                    lines.add(msg.trim());
                }
                return Optional.empty();
            });
        }

        /* State of OldSigns on 2b2t in 1.20+ */
        // Signs that were placed in versions 1.8 - 1.12 are still recognizable,
        // but signs placed before 1.8 (old signs) are now indistinguishable
        // from new oak signs placed with standard text entries in pre-1.8 chunks after 1.19.

        // You must now consider context clues such as the sign's environment, material, and content,
        // in order to determine if it is truly likely to be old.

        // We can check for old (pre 1.8) chunks easily with granite, andesite, & diorite to rule out signs in newer chunks,
        // and we can make sure the sign material is oak for those signs in chunks devoid of alt-stones,
        // but new oak signs placed in old chunks can still be mistaken for old signs by metadata alone.

        // On 2b2t, this will single-out oak-material signs placed BOTH before January 2015, and AFTER August 2023 (in old pre 1.8 chunks).
        // While this will still be useful for identifying old bases for a while,
        // most old signs are at spawn, and the signal-to-noise ratio there will worsen every single day.
        boolean couldBeOld = false;
        RegistryKey<World> dimension = mc.world.getRegistryKey();
        if (dimension != World.NETHER || !ignoreNether.get()) {
            WoodType woodType = WoodType.BAMBOO;
            Block block = sign.getCachedState().getBlock();
            if (block instanceof SignBlock signBlock) woodType = signBlock.getWoodType();
            else if (block instanceof WallSignBlock wallSignBlock) woodType = wallSignBlock.getWoodType();

            if (woodType == WoodType.OAK) {
                NbtCompound metadata = sign.toInitialChunkDataNbt();
                if (!metadata.toString().contains("{\"extra\":[{\"") && !lines.isEmpty()) {
                    String testString = String.join(" ", lines);
                    Matcher fullYearsMatcher = fullYearsPattern.matcher(testString);

                    if (!fullYearsMatcher.find()) {
                        boolean invalidDate = false;
                        Matcher dateMatcher = fullDatesPattern.matcher(testString);
                        while (dateMatcher.find()) {
                            String dateStr = dateMatcher.group();
                            LocalDate date = parseDate(dateStr);
                            if (date != null && date.getYear() > 2015) invalidDate = true;
                        }
                        if (!invalidDate) couldBeOld = !inNewChunk(chunk, mc, dimension);
                    }
                }
            }
        }

        if (!couldBeOld && showOldSigns.get() && onlyOldSigns.get()) return "";
        if (couldBeOld && showOldSigns.get()) {
            color = oldSignColor.get().label;
            oldSet.add(sign.getPos());
        }
        String signText = chatFormat.get() ?
            String.join("\n"+ color + format, lines) : String.join(" ", lines);

        if (signText.trim().isEmpty()) return "";

        StringBuilder txt = new StringBuilder();
        if (showOldSigns.get() && couldBeOld) {
            txt.append("§8<§o").append(StardustUtil.rCC()).append("✨§r§8> ");

            txt.append("§8[§4Old§7..§a?§8] ");
            if (chatFormat.get()) txt.append("\n     ");

            txt.append(oldSignColor.get().label).append(oldSignFormat.get().label)
                .append(chatFormat.get() ? signText.replace("\n", "\n     ") : signText.trim());
        } else {
            txt.append("§8<§o").append(StardustUtil.rCC()).append(chatFormat.get() ? "✨§r§8>\n      " : "✨§r§8> ");
            txt.append(color).append(format)
                .append(chatFormat.get() ? signText.replace("\n", "\n     ") : signText.trim());
        }
        if (showCoords.get()) {
            BlockPos pos = sign.getPos();

            txt.append(chatFormat.get() ? "\n§8[" : " §8[")
                .append(color).append(pos.getX()).append("§8, ")
                .append(color).append(pos.getY()).append("§8, ").append(color).append(pos.getZ()).append("§r§8]");
        }

        return txt.toString();
    }

    private boolean inNewChunk(WorldChunk chunk, MinecraftClient mc, RegistryKey<World> dimension) {
        if (mc.world == null) return false;
        ChunkPos chunkPos = chunk.getPos();
        if (chunkCache.containsKey(chunkPos)) {
            return chunkCache.get(chunkPos);
        }

        if (dimension == World.NETHER) {
            BlockPos startPosDebris = chunkPos.getBlockPos(0, 0, 0);
            BlockPos endPosDebris = chunkPos.getBlockPos(15, 118, 15);

            int newBlocks = 0;
            for (BlockPos pos : BlockPos.iterate(startPosDebris, endPosDebris)) {
                if (newBlocks >= 13) {
                    chunkCache.put(chunkPos, true);
                    return true;
                }
                Block block = mc.world.getBlockState(pos).getBlock();
                if (block == Blocks.ANCIENT_DEBRIS || block == Blocks.BLACKSTONE || block == Blocks.BASALT
                    || block == Blocks.WARPED_NYLIUM || block == Blocks.CRIMSON_NYLIUM || block == Blocks.SOUL_SOIL) ++newBlocks;
            }
            chunkCache.put(chunkPos, (newBlocks >= 13));
            return newBlocks >= 13;
        } else if (dimension == World.OVERWORLD){
            BlockPos startPosAltStones = chunkPos.getBlockPos(0, 0, 0);
            BlockPos endPosAltStones = chunkPos.getBlockPos(15, 128, 15);

            int newBlocks = 0;
            for (BlockPos pos : BlockPos.iterate(startPosAltStones, endPosAltStones)) {
                if (newBlocks >=  33) {
                    chunkCache.put(chunkPos, true);
                    return true;
                }
                // Andesite, Diorite, and Granite were added in 1.8,
                // making it impossible to have old signs in chunks containing these, if naturally-generated.
                Block block = mc.world.getBlockState(pos).getBlock();
                if (block == Blocks.ANDESITE || block == Blocks.GRANITE || block == Blocks.DIORITE) ++newBlocks;
            }
            chunkCache.put(chunkPos, (newBlocks >= 33));
            return newBlocks >= 33;
        }// idk what to do about the end, so we'll detect signs by default. if you don't want it, turn it off in there.

        chunkCache.put(chunkPos, false);
        return false;
    }

    private void chatSigns(List<SignBlockEntity> signs, WorldChunk chunk, MinecraftClient mc) {
        if (mc.world == null || signs.isEmpty()) return;

        signs.forEach(sign -> {
            String textOnSign = Arrays.stream(sign.getFrontText().getMessages(false)).map(Text::getString).collect(Collectors.joining(" ")).trim();
            if (signMessages.containsKey(textOnSign) && ignoreDuplicates.get()) return;

            if (signBlacklist.get() && !blacklisted.isEmpty()) {
                if (caseSensitive.get()) {
                    if (blacklisted.stream().anyMatch(line -> textOnSign.contains(line.trim()))) return;
                } else if (blacklisted.stream().anyMatch(line -> textOnSign.toLowerCase().contains(line.trim().toLowerCase()))) return;
            }

            if (chatMode.get() == ChatMode.ESP && posSet.contains(sign.getPos())) return;
            if (chatMode.get() == ChatMode.Both) {
                if (posSet.contains(sign.getPos())) {
                    if (!sign.getPos().equals(lastFocusedSign)) return;
                }
            }

            String msg = formatSignText(sign, chunk);

            posSet.add(sign.getPos());
            if (msg.isBlank()) return;
            if (signMessages.containsKey(textOnSign) && !sign.getPos().equals(lastFocusedSign)) {
                int timesSeen = signMessages.get(textOnSign) + 1;
                signMessages.put(textOnSign, timesSeen);
                msg = msg + " " + "§8[§7§ox§4§o"+ timesSeen + "§r§8]";
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(Text.of(msg), textOnSign.hashCode());
            } else {
                signMessages.put(textOnSign, 1);
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(Text.of(msg), textOnSign.hashCode());
            }
        });
    }

    private void initBlacklistText() {
        File blackListFile = FabricLoader.getInstance().getGameDir().resolve(BLACKLIST_FILE).toFile();

        try(Stream<String> lineStream = Files.lines(blackListFile.toPath())) {
            blacklisted.addAll(lineStream.toList());
        }catch (Exception err) {
            Stardust.LOG.error("[Stardust] Failed to read from "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err);
        }
    }

    private void openBlacklistFile() {
        resetBlacklistFileSetting();
        StardustUtil.openFile(mc, BLACKLIST_FILE);
    }

    private void resetBlacklistFileSetting() {
        openBlacklistFile.set(false);
    }


    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;
        if (signBlacklist.get() && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) initBlacklistText();

        BlockPos pos = mc.player.getBlockPos();
        if (chatMode.get() == ChatMode.ESP || chatMode.get() == ChatMode.Both) {
            int viewDistance = mc.options.getViewDistance().getValue();

            int startChunkX = (pos.getX() - (viewDistance * 16)) >> 4;
            int endChunkX = (pos.getX() + (viewDistance * 16)) >> 4;
            int startChunkZ = (pos.getZ() - (viewDistance * 16)) >> 4;
            int endChunkZ = (pos.getZ() + (viewDistance * 16)) >> 4;

            for (int x = startChunkX; x < endChunkX; x++) {
                for (int z = startChunkZ; z < endChunkZ; z++) {
                    if (mc.world.isChunkLoaded(x, z)) {
                        WorldChunk chunk = mc.world.getChunk(x, z);
                        List<SignBlockEntity> signs = getNearbySigns(chunk);

                        chatSigns(signs, chunk, mc);
                    }
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        posSet.clear();
        oldSet.clear();
        cooldowns.clear();
        chunkCache.clear();
        blacklisted.clear();
        signMessages.clear();
    }

    @EventHandler
    private void onReceiveChunkData(ChunkDataEvent event) {
        if (mc.world == null || mc.player == null) return;

        if (chatMode.get() != ChatMode.Targeted) {
            List<SignBlockEntity> signs = getNearbySigns(event.chunk);

            chatSigns(signs, event.chunk, mc);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (chatMode.get() == ChatMode.ESP) return;
        if (mc.world == null || mc.player == null) return;
        if (timer >= 65535) timer = 0;
        else if (timer % 6000 == 0) signMessages.clear();

        ++timer;
        if (timer % 5 == 0) {
            timer = 0;
            BlockPos targetedSign = getTargetedSign();
            if (targetedSign == null) {
                lastFocusedSign = null;
                return;
            }

            if (targetedSign.equals(lastFocusedSign) && repeatMode.get() == RepeatMode.On_Focus) return;
            else if (!targetedSign.equals(lastFocusedSign)) lastFocusedSign = targetedSign;

            WorldChunk chunk = mc.world.getChunk(targetedSign.getX() >> 4, targetedSign.getZ() >> 4);
            if (repeatMode.get() == RepeatMode.Cooldown) {
                if (cooldowns.containsKey(targetedSign)) {
                    Instant now = Instant.now();
                    Instant stamp = cooldowns.get(targetedSign);

                    if (Duration.between(stamp, now).toSeconds() < repeatSeconds.get()) return;
                    if (mc.world.getBlockEntity(targetedSign) instanceof SignBlockEntity sign) chatSigns(List.of(sign), chunk, mc);
                } else if (mc.world.getBlockEntity(targetedSign) instanceof SignBlockEntity sign) {
                    chatSigns(List.of(sign), chunk, mc);
                }
            }else if (mc.world.getBlockEntity(targetedSign) instanceof SignBlockEntity sign) chatSigns(List.of(sign), chunk, mc);

            cooldowns.put(targetedSign, Instant.now());
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null || !renderOldSigns.get()) return;
        List<BlockPos> inRange = oldSet
            .stream()
            .filter(pos -> pos.isWithinDistance(mc.player.getBlockPos(), mc.options.getViewDistance().getValue() * 16+32))
            .toList();

        ESPBlockData esp = espSettings.get();
        for (BlockPos pos : inRange) {
            BlockState state = mc.world.getBlockState(pos);
            if (!(state.getBlock() instanceof SignBlock) && !(state.getBlock() instanceof WallSignBlock)) continue;
            VoxelShape shape = state.getOutlineShape(mc.world, pos);

            double x1 = pos.getX() + shape.getMin(Direction.Axis.X);
            double y1 = pos.getY() + shape.getMin(Direction.Axis.Y);
            double z1 = pos.getZ() + shape.getMin(Direction.Axis.Z);
            double x2 = pos.getX() + shape.getMax(Direction.Axis.X);
            double y2 = pos.getY() + shape.getMax(Direction.Axis.Y);
            double z2 = pos.getZ() + shape.getMax(Direction.Axis.Z);

            event.renderer.box(
                x1, y1, z1, x2, y2, z2,
                esp.sideColor, esp.lineColor, esp.shapeMode, 0
            );

            if (esp.tracer) {
                try {
                    double offsetX;
                    double offsetY;
                    double offsetZ;
                    if (state.getBlock() instanceof SignBlock) {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    } else if (state.getBlock() instanceof WallSignBlock) {
                        Direction facing = state.get(WallSignBlock.FACING);
                        switch (facing) {
                            case NORTH -> {
                                offsetX = pos.getX() + .5;
                                offsetY = pos.getY() + .5;
                                offsetZ = pos.getZ() + .937;
                            }
                            case EAST -> {
                                offsetX = pos.getX() + .1337;
                                offsetY = pos.getY() + .5;
                                offsetZ = pos.getZ() + .5;
                            }
                            case SOUTH -> {
                                offsetX = pos.getX() + .5;
                                offsetY = pos.getY() + .5;
                                offsetZ = pos.getZ() + .1337;
                            }
                            case WEST -> {
                                offsetX = pos.getX() + .937;
                                offsetY = pos.getY() + .5;
                                offsetZ = pos.getZ() + .5;
                            }
                            default -> {
                                offsetX = pos.getX() + .5;
                                offsetY = pos.getY() + .5;
                                offsetZ = pos.getZ() + .5;
                            }
                        }
                    } else {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    }

                    event.renderer.line(
                        RenderUtils.center.x,
                        RenderUtils.center.y,
                        RenderUtils.center.z,
                        offsetX,
                        offsetY,
                        offsetZ,
                        esp.tracerColor
                    );
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }
}
