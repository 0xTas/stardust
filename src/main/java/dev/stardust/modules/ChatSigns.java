package dev.stardust.modules;

import java.awt.*;
import java.util.*;
import java.io.File;
import java.util.List;
import java.time.Instant;
import java.time.Duration;
import java.nio.file.Path;
import java.nio.file.Files;
import dev.stardust.Stardust;
import net.minecraft.block.*;
import net.minecraft.text.Text;
import java.util.stream.Stream;
import net.minecraft.text.Style;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.stream.Collectors;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.ClickEvent;
import dev.stardust.util.StardustUtil;
import net.minecraft.text.MutableText;
import dev.stardust.util.StardustUtil.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.registry.RegistryKey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.entity.BlockEntity;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.block.entity.HangingSignBlockEntity;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;


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

    private final SettingGroup modesGroup = settings.createGroup("Modes", true);
    private final SettingGroup formatGroup = settings.createGroup("Formatting", true);
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

    private final Setting<Boolean> showOldSigns  = modesGroup.add(
        new BoolSetting.Builder()
            .name("Show Possibly Old Signs*")
            .description("*will show signs placed before 1.8, AND after 1.12. Use your best judgment to determine what's legit.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyOldSigns = modesGroup.add(
        new BoolSetting.Builder()
            .name("Only Show New/Old Signs")
            .description("Only display text from signs that are either really old, or brand new.")
            .defaultValue(false)
            .visible(showOldSigns::get)
            .build()
    );

    private final Setting<Boolean> ignoreNether = modesGroup.add(
        new BoolSetting.Builder()
            .name("Ignore Nether")
            .description("Ignore potentially-old signs in the nether since near highways they're almost all certainly new.")
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

    private final Setting<TextColor> oldSignColor = formatGroup.add(
        new EnumSetting.Builder<TextColor>()
            .name("Old Sign Color")
            .description("Text color for signs that might be old.")
            .defaultValue(TextColor.Yellow)
            .visible(showOldSigns::get)
            .build()
    );

    private final Setting<TextFormat> oldSignFormat = formatGroup.add(
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

    private final Setting<Boolean> signBlacklist = blacklistGroup.add(
        new BoolSetting.Builder()
            .name("SignText Blacklist")
            .description("Ignore signs that contain specific text (line-separated list in chatsigns-blacklist.txt)")
            .defaultValue(false)
            .onChanged(it -> {
                if (it && this.isActive() && checkOrCreateBlacklistFile()) {
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
            .visible(this.signBlacklist::get)
            .build()
    );

    private final Setting<Boolean> openBlacklistFile = blacklistGroup.add(
        new BoolSetting.Builder()
            .name("Open Blacklist File")
            .description("Open the chatsigns-blacklist.txt file.")
            .defaultValue(false)
            .visible(this.signBlacklist::get)
            .onChanged(it -> {
                if (it) {
                    if (checkOrCreateBlacklistFile()) openBlacklistFile();
                    else resetBlacklistFileSetting();
                }
            })
            .build()
    );

    private int totalTicksEnabled = 0;
    @Nullable private BlockPos lastFocusedSign = null;
    private final HashSet<BlockPos> posSet = new HashSet<>();
    private final ArrayList<String> blacklisted = new ArrayList<>();
    private final HashMap<BlockPos, Instant> cooldowns = new HashMap<>();
    private final HashMap<String, Integer> signMessages = new HashMap<>();

    @Nullable
    private BlockPos getTargetedSign() {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return null;
        int viewDistance = mc.options.getViewDistance().getValue();

        double maxRangeBlocks = viewDistance * 16;
        HitResult trace = player.raycast(maxRangeBlocks, mc.getTickDelta(), true);
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
        blockEntities.forEach((pos, entity) -> { // I'll deal with hanging signs when 2b gets 1.20
            if (entity instanceof SignBlockEntity && !(entity instanceof HangingSignBlockEntity)) {
                signs.add((SignBlockEntity) entity);
            }
        });

        return signs;
    }

    private String formatSignText(SignBlockEntity sign, WorldChunk chunk) {
        if (mc.world == null) return "";
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

        /* State of OldSigns on 2b2t in 1.19+ */
        // Signs that were placed in versions 1.8 through 1.12 are still recognizable,
        // however, signs placed before 1.8 (old signs) are now indistinguishable from new oak signs placed
        // with standard text entries in 1.19 (unless I'm just dumb and missing something).

        // You must now consider context clues such as the sign's environment, material, and content,
        // to determine if it is truly likely be old.

        // We can check for new (1.19+) chunks easily with copper to rule out signs in newer chunks,
        // and we can make sure the sign material is oak for those signs in chunks devoid of copper,
        // but new oak signs placed in old chunks can still be indistinguishable from old signs by metadata alone.

        // On 2b2t, this will single-out oak-material signs placed BOTH before January 2015, and AFTER August 2023 (in old chunks).
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
                    if (lines.stream().noneMatch(line -> line.contains("2023") || line.contains("/23") || line.contains("-23"))) {
                        couldBeOld = !likelyNewChunk(chunk, mc, dimension);
                    }
                }
            }
        }

        if (!couldBeOld && showOldSigns.get() && onlyOldSigns.get()) return "";
        if (couldBeOld && showOldSigns.get()) {
            color = oldSignColor.get().label;
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

    private boolean likelyNewChunk(WorldChunk chunk, MinecraftClient mc, RegistryKey<World> dimension) {
        if (mc.world == null) return false;
        ChunkPos chunkPos = chunk.getPos();
        if (dimension == World.NETHER) {
            BlockPos startPosDebris = chunkPos.getBlockPos(0, 0, 0);
            BlockPos endPosDebris = chunkPos.getBlockPos(15, 118, 15);

            int newBlocks = 0;
            for (BlockPos pos : BlockPos.iterate(startPosDebris, endPosDebris)) {
                if (newBlocks >= 13) return true;
                Block block = mc.world.getBlockState(pos).getBlock();
                if (block == Blocks.ANCIENT_DEBRIS || block == Blocks.BLACKSTONE || block == Blocks.BASALT
                    || block == Blocks.WARPED_NYLIUM || block == Blocks.CRIMSON_NYLIUM || block == Blocks.SOUL_SOIL) ++newBlocks;
            }
        } else if (dimension == World.OVERWORLD){
            BlockPos startPosCopper = chunkPos.getBlockPos(0, 0, 0);
            BlockPos endPosCopper = chunkPos.getBlockPos(15, 63, 15);

            int newBlocks = 0;
            for (BlockPos pos : BlockPos.iterate(startPosCopper, endPosCopper)) {
                if (newBlocks >=  13) return true;
                Block block = mc.world.getBlockState(pos).getBlock();
                if (block == Blocks.COPPER_ORE) ++newBlocks; // Copper generates in all overworld biomes except dripstone caves.
                else if (block == Blocks.DRIPSTONE_BLOCK || block == Blocks.POINTED_DRIPSTONE) ++newBlocks;
            }
        }// idk what to do about the end, so we'll detect signs by default. if you don't want it, turn it off in there.

        return false;
    }

    private void chatSigns(List<SignBlockEntity> signs, WorldChunk chunk, MinecraftClient mc) {
        if (mc.world == null || signs.isEmpty()) return;

        signs.forEach(sign -> {
            String textOnSign = Arrays.stream(sign.getFrontText().getMessages(false)).map(Text::getString).collect(Collectors.joining(" ")).trim();
            if (this.signMessages.containsKey(textOnSign) && ignoreDuplicates.get()) return;

            if (signBlacklist.get() && !this.blacklisted.isEmpty()) {
                if (this.caseSensitive.get()) {
                    if (this.blacklisted.stream().anyMatch(line -> textOnSign.contains(line.trim()))) return;
                } else if (this.blacklisted.stream().anyMatch(line -> textOnSign.toLowerCase().contains(line.trim().toLowerCase()))) return;
            }

            if (chatMode.get() == ChatMode.ESP && posSet.contains(sign.getPos())) return;
            if (chatMode.get() == ChatMode.Both) {
                if (posSet.contains(sign.getPos())) {
                    if (!sign.getPos().equals(this.lastFocusedSign)) return;
                }
            }

            String msg = formatSignText(sign, chunk);

            posSet.add(sign.getPos());
            if (msg.isBlank()) return;
            if (this.signMessages.containsKey(textOnSign) && !sign.getPos().equals(this.lastFocusedSign)) {
                int timesSeen = this.signMessages.get(textOnSign) + 1;
                this.signMessages.put(textOnSign, timesSeen);
                msg = msg + " " + "§8[§7§ox§4§o"+ timesSeen + "§r§8]";
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(Text.of(msg), textOnSign.hashCode());
            } else {
                this.signMessages.put(textOnSign, 1);
                ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(Text.of(msg), textOnSign.hashCode());
            }
        });
    }

    private boolean checkOrCreateBlacklistFile() {
        Path meteorFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client");
        File blackListFile = meteorFolder.resolve("chatsigns-blacklist.txt").toFile();

        if (!blackListFile.exists()) {
            try {
                if (blackListFile.createNewFile()) {
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Created chatsigns-blacklist.txt in meteor-client folder.")
                        );
                        Text msg = Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Click §2§lhere §r§7to open the file.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, blackListFile.getAbsolutePath()));

                        MutableText txt = msg.copyContentOnly().setStyle(style);
                        mc.player.sendMessage(txt);
                    }
                    return true;
                }
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Error creating "+ blackListFile.getAbsolutePath() + "! - Why:\n"+err);
            }
        } else return true;

        return false;
    }

    private void initBlacklistText() {
        Path meteorFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client");
        File blackListFile = meteorFolder.resolve("chatsigns-blacklist.txt").toFile();

        try(Stream<String> lineStream = Files.lines(blackListFile.toPath())) {
            this.blacklisted.addAll(lineStream.toList());
        }catch (Exception err) {
            Stardust.LOG.error("[Stardust] Failed to read from "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err);
        }
    }

    private void openBlacklistFile() {
        Path meteorFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client");
        File blackListFile = meteorFolder.resolve("chatsigns-blacklist.txt").toFile();

        if (Desktop.isDesktopSupported()) {
            EventQueue.invokeLater(() -> {
                try {
                    Desktop.getDesktop().open(blackListFile);
                }catch (Exception err) {
                    Stardust.LOG.error("[Stardust] Failed to open "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err);
                }
            });
        } else {
            try {
                Runtime runtime = Runtime.getRuntime();
                if (System.getenv("OS") == null) return;
                if (System.getenv("OS").contains("Windows")) {
                    runtime.exec("rundll32 url.dll, FileProtocolHandler " + blackListFile.getAbsolutePath());
                }else {
                    runtime.exec("xdg-open " + blackListFile.getAbsolutePath());
                }
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Failed to open "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err);
                if (mc.player != null) mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"✨§8> §4§oFailed to open chatsigns-blacklist.txt§7."));
            }
        }

        this.openBlacklistFile.set(false);
    }

    private void resetBlacklistFileSetting() {
        this.openBlacklistFile.set(false);
    }


    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;
        if (this.signBlacklist.get() && checkOrCreateBlacklistFile()) initBlacklistText();

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
        this.posSet.clear();
        this.cooldowns.clear();
        this.blacklisted.clear();
        this.signMessages.clear();
        this.totalTicksEnabled = 0;
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
        if (this.totalTicksEnabled >= 65535) this.totalTicksEnabled = 0;
        else if (this.totalTicksEnabled % 6000 == 0) this.signMessages.clear();

        ++this.totalTicksEnabled;
        if (this.totalTicksEnabled % 5 == 0) {
            BlockPos targetedSign = getTargetedSign();
            if (targetedSign == null) {
                this.lastFocusedSign = null;
                return;
            }

            if (targetedSign.equals(this.lastFocusedSign) && repeatMode.get() == RepeatMode.On_Focus) return;
            else if (!targetedSign.equals(this.lastFocusedSign)) this.lastFocusedSign = targetedSign;

            WorldChunk chunk = mc.world.getChunk(targetedSign.getX(), targetedSign.getZ());
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
}
