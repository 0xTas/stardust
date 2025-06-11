package dev.stardust.modules;

import java.util.*;
import java.io.File;
import java.util.List;
import java.nio.file.Path;
import java.time.LocalDate;
import java.nio.file.Files;
import net.minecraft.item.*;
import net.minecraft.text.*;
import dev.stardust.Stardust;
import java.util.stream.Stream;
import net.minecraft.util.Hand;
import net.minecraft.util.DyeColor;
import java.util.stream.Collectors;
import net.minecraft.util.math.Vec3d;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import java.time.format.DateTimeFormatter;
import net.minecraft.block.entity.SignText;
import org.apache.commons.codec.binary.Hex;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.font.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.block.entity.SignBlockEntity;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import dev.stardust.mixin.accessor.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class SignatureSign extends Module {
    public SignatureSign() { super(Stardust.CATEGORY, "SignatureSign", "Autofill signs with custom text."); }

    public static final String[] lineModes = {"Custom", "Empty", "File", "Username",
        "Username was here", "Timestamp", "Stardust", "Oasis", "Base64", "Hex", "0xHex", "ROT13"};
    public static final String[] timestampTypes = {"MM/DD/YY", "MM/DD/YYYY", "DD/MM/YY", "DD/MM/YYYY",
        "YYYY/MM/DD", "YYYY/DD/MM", "Day Month Year", "Month Day Year", "Month Year", "Year", "Day Month", "Month Day",
        "Unix Epoch"};
    public static final String[] timestampDelimiters = {"/", "//", "\\", "\\\\", "|", "||", "-", "_", "~", ".", ",", "x", "•", "✨"};

    private final SettingGroup sgMode = settings.createGroup("Module Mode");
    private final SettingGroup sgSignsOpts = settings.createGroup("Sign Options");
    private final SettingGroup sgLine1Front = settings.createGroup("Front Line 1");
    private final SettingGroup sgLine2Front = settings.createGroup("Front Line 2");
    private final SettingGroup sgLine3Front = settings.createGroup("Front Line 3");
    private final SettingGroup sgLine4Front = settings.createGroup("Front Line 4");

    private final Setting<Boolean> storyMode = sgMode.add(
        new BoolSetting.Builder()
            .name("story-mode")
            .description("Fill signs continuously from .minecraft/meteor-client/storysign.text")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> secretSign = sgMode.add(
        new BoolSetting.Builder()
            .name("secret-signs")
            .description("Pad each line with spaces to hide your message from being rendered. Will then only be viewable via metadata (ChatSigns etc.)")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> protectSigns = sgSignsOpts.add(
        new BoolSetting.Builder()
            .name("wax-signs")
            .description("Apply honeycomb wax onto signs if found in inventory.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> glowSigns = sgSignsOpts.add(
        new BoolSetting.Builder()
            .name("glow-signs")
            .description("Apply glow squid ink onto signs if found in inventory.")
            .defaultValue(false)
            .build()
    );

    private final Setting<DyeColor> signColor = sgSignsOpts.add(
        new EnumSetting.Builder<DyeColor>()
            .name("sign-color")
            .description("Apply selected dye color onto signs if found in inventory.")
            .defaultValue(DyeColor.BLACK)
            .build()
    );

    public final Setting<Boolean> signFreedom = sgSignsOpts.add(
        new BoolSetting.Builder()
            .name("bypass-length-limits")
            .description("Bypass client-sided length limits for sign text.")
            .defaultValue(true)
            .visible(() -> !secretSign.get())
            .build()
    );

    private final Setting<String> line1ModeFront = sgLine1Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-1-mode")
            .description("Line 1 template mode")
            .defaultValue("Stardust")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line1TextFront = sgLine1Front.add(
        new StringSetting.Builder()
            .name("line-1-text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(1))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(1);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7.."), false
                        );
                    }
                } else {
                    lastLine1TextFront = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line1FileLineFront = sgLine1Front.add(
        new IntSetting.Builder()
            .name("line-1-file-line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(1)
            .visible(() -> !storyMode.get() && line1ModeFront.get().equals("File"))
            .build()
    );

    private final Setting<String> line1TimestampTypeFront = sgLine1Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-1-timestamp-type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line1ModeFront.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line1TimestampDelimFront = sgLine1Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-1-timestamp-delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line1ModeFront.get().equals("Timestamp") && line1TimestampTypeFront.get().contains("/"))
            .build()
    );

    private final Setting<String> line2ModeFront = sgLine2Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-2-mode")
            .description("Line 2 template mode")
            .defaultValue("Stardust")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line2TextFront = sgLine2Front.add(
        new StringSetting.Builder()
            .name("line-2-text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(2))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(2);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7.."), false
                        );
                    }
                } else {
                    lastLine2TextFront = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line2FileLineFront = sgLine2Front.add(
        new IntSetting.Builder()
            .name("line-2-file-line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(2)
            .visible(() -> !storyMode.get() && line2ModeFront.get().equals("File"))
            .build()
    );

    private final Setting<String> line2TimestampTypeFront = sgLine2Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-2-timestamp-type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line2ModeFront.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line2TimestampDelimFront = sgLine2Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-2-timestamp-delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line2ModeFront.get().equals("Timestamp") && line2TimestampTypeFront.get().contains("/"))
            .build()
    );

    private final Setting<String> line3ModeFront = sgLine3Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-3-mode")
            .defaultValue("Stardust")
            .description("Line 3 template mode")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line3TextFront = sgLine3Front.add(
        new StringSetting.Builder()
            .name("line-3-text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(3))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(3);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7.."), false
                        );
                    }
                } else {
                    lastLine3TextFront = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line3FileLineFront = sgLine3Front.add(
        new IntSetting.Builder()
            .name("line-3-file-line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(3)
            .visible(() -> !storyMode.get() && line3ModeFront.get().equals("File"))
            .build()
    );

    private final Setting<String> line3TimestampTypeFront = sgLine3Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-3-timestamp-type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line3ModeFront.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line3TimestampDelimFront = sgLine3Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-3-timestamp-delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line3ModeFront.get().equals("Timestamp") && line3TimestampTypeFront.get().contains("/"))
            .build()
    );

    private final Setting<String> line4ModeFront = sgLine4Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-4-mode")
            .defaultValue("Stardust")
            .description("Line 4 template mode")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line4TextFront = sgLine4Front.add(
        new StringSetting.Builder()
            .name("line-4-text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(4))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(4);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7.."), false
                        );
                    }
                } else {
                    lastLine4TextFront = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line4FileLineFront = sgLine4Front.add(
        new IntSetting.Builder()
            .name("line-4-file-line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(4)
            .visible(() -> !storyMode.get() && line4ModeFront.get().equals("File"))
            .build()
    );

    private final Setting<String> line4TimestampTypeFront = sgLine4Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-4-timestamp-type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line4ModeFront.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line4TimestampDelimFront = sgLine4Front.add(
        new ProvidedStringSetting.Builder()
            .name("line-4-timestamp-delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line4ModeFront.get().equals("Timestamp") && line4TimestampTypeFront.get().contains("/"))
            .build()
    );

    private final Setting<Boolean> shortenedMonth = sgSignsOpts.add(
        new BoolSetting.Builder()
            .name("shortened-month")
            .description("Shorten the month to its abbreviation")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> packetDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("packet-delay")
            .description("How many ticks to delay before sending the UpdateSign packet. Lower values have a higher chance of being rejected by the AC.")
            .range(0, 500).sliderRange(0, 50).defaultValue(20)
            .build()
    );

    private final Setting<Boolean> autoConfirm = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("auto-confirm")
            .description("Automatically confirm and close the sign edit screen.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoDisable = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("auto-disable")
            .description("Automatically disable the module after placing a sign.")
            .defaultValue(false)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<Boolean> redo = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("redo-last-sign")
            .description("Click this to redo your last-placed story sign. Useful if you misplaced it.")
            .defaultValue(false)
            .visible(storyMode::get)
            .build()
    );

    private final Setting<Boolean> openFolder = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("open-meteor-client-folder")
            .description("Opens the meteor-client folder where autosign.txt & storysign.txt are kept.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    openMeteorFolder();
                }
            })
            .build()
    );

    private int timer = 0;
    private int dyeSlot = -1;
    private int storyIndex = 0;
    private int packetTimer = 0;
    private int lastIndexAmount = 0;
    private int rotationPriority = 69420;
    private boolean didDisableWaxAura = false;
    private boolean needDelayedDeactivate = false;
    private String lastLine1TextFront = line1TextFront.get();
    private String lastLine2TextFront = line2TextFront.get();
    private String lastLine3TextFront = line3TextFront.get();
    private String lastLine4TextFront = line4TextFront.get();
    private final ArrayList<String> lastLines = new ArrayList<>();
    private final ArrayList<String> storyText = new ArrayList<>();
    private final HashSet<SignBlockEntity> signsToWax = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToColor = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToGlowInk = new HashSet<>();
    private final ArrayDeque<UpdateSignC2SPacket> packetQueue = new ArrayDeque<>();


    @Override
    public void onActivate() {
        lastLine1TextFront = line1TextFront.get();
        lastLine2TextFront = line2TextFront.get();
        lastLine3TextFront = line3TextFront.get();
        lastLine4TextFront = line4TextFront.get();
    }

    @Override
    public void onDeactivate() {
        storyText.clear();
        lastLines.clear();
        signsToWax.clear();
        signsToColor.clear();
        signsToGlowInk.clear();

        timer = 0;
        storyIndex = 0;
        packetTimer = 0;
        lastIndexAmount = 0;
        rotationPriority = 69420;
        didDisableWaxAura = false;
        needDelayedDeactivate = false;
    }


    // See AbstractSignEditScreenMixin.java
    public SignText getSignature(SignBlockEntity sign) {
        Text[] signature = new Text[4];
        List<String> lines = getSignText();
        for (int i = 0; i < lines.size(); i++) {
            signature[i] = Text.of(lines.get(i));
        }

        if (protectSigns.get() && !sign.isWaxed()) {
            signsToWax.add(sign);
        }
        if (signColor.get() != sign.getFrontText().getColor()) signsToColor.add(sign);
        if (glowSigns.get() && !sign.getFrontText().isGlowing()) signsToGlowInk.add(sign);

        return new SignText(signature, signature, DyeColor.BLACK, false);
    }

    public void disable() {
        if (signsToWax.isEmpty() && signsToColor.isEmpty() && signsToGlowInk.isEmpty()) {
            toggle();
        } else needDelayedDeactivate = true;
    }

    public boolean needsDisabling() {
        return autoDisable.get() && !storyMode.get();
    }

    public boolean getAutoConfirm() { return autoConfirm.get(); }

    private boolean textLineVisibility(int line) {
        String md;
        md = switch (line) {
            case 1 -> line1ModeFront.get();
            case 2 -> line2ModeFront.get();
            case 3 -> line3ModeFront.get();
            default -> line4ModeFront.get();
        };


        return md.equals("Custom") || md.equals("Base64") || md.equals("Hex")
            || md.equals("0xHex") || md.equals("ROT13");
    }

    private boolean inputTooLong(String input) {
        return mc.textRenderer.getWidth(input) > 90;
    }

    private void restoreValidInput(int line) {
        switch (line) {
            case 1 -> line1TextFront.set(lastLine1TextFront);
            case 2 -> line2TextFront.set(lastLine2TextFront);
            case 3 -> line3TextFront.set(lastLine3TextFront);
            default -> line4TextFront.set(lastLine4TextFront);
        }
    }

    private List<String> getSignText() {
        List<String> signText = new ArrayList<>();
        if (mc.player == null) return signText;

        String username = mc.player.getName().getString();
        if (storyMode.get()) {
            if (storyText.isEmpty()) {
                if (redo.get()) {
                    signText.addAll(lastLines);
                    redo.set(false);
                } else initStoryTextFromFile();
                if (storyText.isEmpty()) return signText;
                else signText.addAll(getNextLinesOfStory());
            }else {
                signText.addAll(getNextLinesOfStory());
            }
        } else {
            switch (line1ModeFront.get()) {
                case "Custom" -> signText.add(line1TextFront.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line1FileLineFront.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(1));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust" -> signText.add("<✨>");
                case "Oasis" -> signText.add("<☯>");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line1TextFront.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line1TextFront.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line1TextFront.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line1TextFront.get()));
            }
            switch (line2ModeFront.get()) {
                case "Custom" -> signText.add(line2TextFront.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line2FileLineFront.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(2));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust", "Oasis" -> signText.add("<"+username+">");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line2TextFront.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line2TextFront.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line2TextFront.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line2TextFront.get()));
            }
            switch (line3ModeFront.get()) {
                case "Custom" -> signText.add(line3TextFront.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line3FileLineFront.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(3));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust", "Oasis" -> signText.add(System.currentTimeMillis() / 1000 + " UTC");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line3TextFront.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line3TextFront.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line3TextFront.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line3TextFront.get()));
            }
            switch (line4ModeFront.get()) {
                case "Custom" -> signText.add(line4TextFront.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line4FileLineFront.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(4));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust" -> signText.add("<✨>");
                case "Oasis" -> signText.add("<☯>");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line4TextFront.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line4TextFront.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line4TextFront.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line4TextFront.get()));
            }
        }

        if (secretSign.get()) {
            signText = signText.stream().map(line -> {
                StringBuilder secretSpaces = new StringBuilder();
                secretSpaces.append("              ");
                while (!inputTooLong(secretSpaces.toString())) {
                    secretSpaces.append(" ");
                }
                secretSpaces.append(line);
                return secretSpaces.toString();
            }).collect(Collectors.toList());
            return signText;
        }

        if (signFreedom.get()) return signText;
        for (int i = 0; i < signText.size(); i++) {
            if (inputTooLong(signText.get(i))) {
                if (mc.player != null) {
                    mc.player.sendMessage(
                        Text.of("§8<§4✨§8> §7§oLine §4§o"+(i+1)+" §7§owon't render fully due to length.."), false
                    );
                }
            }
        }
        return signText;
    }

    private String getSignTextFromFile(int line) {
        Path meteorFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client");

        File file = meteorFolder.resolve("autosign.txt").toFile();

        if (file.exists()) {
            try(Stream<String> lineStream = Files.lines(file.toPath())) {
                List<String> lines = lineStream.toList();

                return line <= lines.size() ? lines.get(line) : lines.get(lines.size()-1);
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Failed to read from "+ file.getAbsolutePath() +"! - Why:\n"+err);
            }
        } else {
            try {
                if (file.createNewFile()) {
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Created autosign.txt in meteor-client folder."), false);

                        Text msg = Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Click §2§lhere §r§7to open the folder.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, meteorFolder.toFile().getAbsolutePath()));

                        MutableText txt = msg.copyContentOnly().setStyle(style);
                        mc.player.sendMessage(txt, false);
                    }
                }
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Failed to create " + file.getAbsolutePath() + "! Why:\n" + err);
            }

            switch (line) {
                case 1: return "File was empty";
                case 2: return "Please use the";
                case 3: return "autosign.txt";
                case 4: return "in /meteor-client";
            }
        }
        return "File not found";
    }

    private void initStoryTextFromFile() {
        Path meteorFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client");
        File file = meteorFolder.resolve("storysign.txt").toFile();

        if (file.exists()) {
            try(Stream<String> lineStream = Files.lines(file.toPath())) {
                storyText.addAll(Arrays.stream(lineStream.collect(Collectors.joining(" ")).split(" ")).toList());
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Failed to read from "+ file.getAbsolutePath() +"! - Why:\n"+err);
            }
        }else {
            try {
                if (file.createNewFile()) {
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Created storysign.txt in meteor-client folder."), false);

                        Text msg = Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Click §2§lhere §r§7to open the folder.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, meteorFolder.toFile().getAbsolutePath()));

                        MutableText txt = msg.copyContentOnly().setStyle(style);
                        mc.player.sendMessage(txt, false);
                    }
                }
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Failed to create " + file.getAbsolutePath() + "! Why:\n" + err);
                storyText.add("File not found.");
                storyText.add("Please create a");
                storyText.add("storysign.txt in");
                storyText.add("/meteor-client");
                return;
            }

            storyText.add("File was empty.");
            storyText.add("Please use the");
            storyText.add("storysign.txt in");
            storyText.add("/meteor-client");
        }
    }

    private List<String> getNextLinesOfStory() {
        List<String> storyLines = new ArrayList<>();

        TextRenderer textRenderer = mc.textRenderer;
        if (redo.get()) {
            storyIndex -= lastIndexAmount;
            redo.set(false);
        }

        lastIndexAmount = 0;
        for (int n = 0; n < 4; n++) {
            StringBuilder line = new StringBuilder();

            for (int i = storyIndex; i < storyText.size(); i++) {
                if (storyText.get(i).trim().isEmpty()) {
                    ++storyIndex;
                    ++lastIndexAmount;
                    continue;
                }
                if (textRenderer.getWidth(line.toString()) >= 87) break;

                if (textRenderer.getWidth(storyText.get(i).trim()) > 87) {
                    if (!line.isEmpty()) break;
                    line.append(textRenderer.trimToWidth(storyText.get(i).trim(), 85));

                    ++storyIndex;
                    ++lastIndexAmount;
                    break;
                }

                if (textRenderer.getWidth(line + storyText.get(i).trim()) > 87) break;

                if (line.isEmpty()) line.append(storyText.get(i).trim());
                else line.append(" ").append(storyText.get(i).trim());

                ++storyIndex;
                ++lastIndexAmount;
            }
            storyLines.add(line.toString());
        }

        if (storyIndex >= storyText.size() - 1) {
            storyText.clear();
            storyIndex = 0;
            lastIndexAmount = 0;
            lastLines.addAll(storyLines);
            if (mc.player != null) {
                mc.player.sendMessage(
                    Text.of("§8<"+StardustUtil.rCC()+"✨§8> §7§oSign story complete."), false
                );
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.77f, 0.77f);
            }
        }

        return storyLines;
    }

    private String getTimestamp(int line) {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter mmddyy = DateTimeFormatter.ofPattern("MM/dd/yy");
        DateTimeFormatter mmddyyyy = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter ddmmyy = DateTimeFormatter.ofPattern("dd/MM/yy");
        DateTimeFormatter ddmmyyyy = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        DateTimeFormatter yyyyddmm = DateTimeFormatter.ofPattern("yyyy/dd/MM");

        String currentMonth = "" + currentDate.getMonth().name().charAt(0);
        if (shortenedMonth.get()) {
            currentMonth += currentDate.getMonth().name().toLowerCase().substring(1,3); // :3
        }
        else{
            currentMonth += currentDate.getMonth().name().substring(1).toLowerCase();
        }

        switch (line) {
            case 1 -> {
                return switch (line1TimestampTypeFront.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line1TimestampDelimFront.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line1TimestampDelimFront.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line1TimestampDelimFront.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line1TimestampDelimFront.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line1TimestampDelimFront.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line1TimestampDelimFront.get());
                    case "Day Month Year" ->
                        dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentMonth + " " + currentDate.getYear();
                    case "Month Day Year" ->
                        currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentDate.getYear();
                    case "Month Year" -> currentMonth + " " + currentDate.getYear();
                    case "Year" -> String.valueOf(currentDate.getYear());
                    case "Day Month" -> dayOfMonthSuffix(currentDate.getDayOfMonth()) + " of " + currentMonth;
                    case "Month Day" -> currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth());
                    default -> System.currentTimeMillis() / 1000 + " UTC";
                };
            }
            case 2 -> {
                return switch (line2TimestampTypeFront.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line2TimestampDelimFront.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line2TimestampDelimFront.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line2TimestampDelimFront.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line2TimestampDelimFront.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line2TimestampDelimFront.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line2TimestampDelimFront.get());
                    case "Day Month Year" ->
                        dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentMonth + " " + currentDate.getYear();
                    case "Month Day Year" ->
                        currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentDate.getYear();
                    case "Month Year" -> currentMonth + " " + currentDate.getYear();
                    case "Year" -> String.valueOf(currentDate.getYear());
                    case "Day Month" -> dayOfMonthSuffix(currentDate.getDayOfMonth()) + " of " + currentMonth;
                    case "Month Day" -> currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth());
                    default -> System.currentTimeMillis() / 1000 + " UTC";
                };
            }
            case 3 -> {
                return switch (line3TimestampTypeFront.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line3TimestampDelimFront.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line3TimestampDelimFront.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line3TimestampDelimFront.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line3TimestampDelimFront.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line3TimestampDelimFront.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line3TimestampDelimFront.get());
                    case "Day Month Year" ->
                        dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentMonth + " " + currentDate.getYear();
                    case "Month Day Year" ->
                        currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentDate.getYear();
                    case "Month Year" -> currentMonth + " " + currentDate.getYear();
                    case "Year" -> String.valueOf(currentDate.getYear());
                    case "Day Month" -> dayOfMonthSuffix(currentDate.getDayOfMonth()) + " of " + currentMonth;
                    case "Month Day" -> currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth());
                    default -> System.currentTimeMillis() / 1000 + " UTC";
                };
            }
            case 4 -> {
                return switch (line4TimestampTypeFront.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line4TimestampDelimFront.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line4TimestampDelimFront.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line4TimestampDelimFront.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line4TimestampDelimFront.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line4TimestampDelimFront.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line4TimestampDelimFront.get());
                    case "Day Month Year" ->
                        dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentMonth + " " + currentDate.getYear();
                    case "Month Day Year" ->
                        currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth()) + " " + currentDate.getYear();
                    case "Month Year" -> currentMonth + " " + currentDate.getYear();
                    case "Year" -> String.valueOf(currentDate.getYear());
                    case "Day Month" -> dayOfMonthSuffix(currentDate.getDayOfMonth()) + " of " + currentMonth;
                    case "Month Day" -> currentMonth + " " + dayOfMonthSuffix(currentDate.getDayOfMonth());
                    default -> System.currentTimeMillis() / 1000 + " UTC";
                };
            }
            default -> {
                return System.currentTimeMillis() / 1000 + " UTC";
            }
        }
    }

    private void openMeteorFolder() {
        StardustUtil.openFile(mc, "meteor-client");
        openFolder.set(false);
    }

    private String dayOfMonthSuffix(int dom) {
        String day = String.valueOf(dom);

        if (!day.endsWith("11") && day.endsWith("1")) {
            return day+"st";
        } else if (!day.endsWith("12") && day.endsWith("2")) {
            return day+"nd";
        }else if (!day.endsWith("13") && day.endsWith("3")) {
            return day+"rd";
        } else {
            return day+"th";
        }
    }

    private String rot13(String input) {
        StringBuilder rot = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'a' && c <= 'z') {
                c += 13;
                if (c > 'z') c -= 26;
            } else if (c >= 'A' && c <= 'Z') {
                c += 13;
                if (c > 'Z') c -= 26;
            }
            rot.append(c);
        }
        return rot.toString();
    }

    private void interactSign(SignBlockEntity sbe, Item dye) {
        if (mc.player == null || mc.interactionManager == null) return;

        BlockPos pos = sbe.getPos();
        Vec3d hitVec = Vec3d.ofCenter(pos);
        BlockHitResult hit = new BlockHitResult(hitVec, mc.player.getHorizontalFacing().getOpposite(), pos, false);

        ItemStack current = mc.player.getInventory().getMainHandStack();
        if (current.getItem() != dye) {
            for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                ItemStack stack = mc.player.getInventory().getStack(n);
                if (stack.getItem() == dye) {
                    if (current.getItem() instanceof SignItem && current.getCount() > 1) dyeSlot = n;
                    if (n < 9) InvUtils.swap(n, true);
                    else InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);

                    timer = 3;
                    return;
                }
            }
        } else {
            Rotations.rotate(
                Rotations.getYaw(pos),
                Rotations.getPitch(pos), rotationPriority,
                () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit)
            );
            ++rotationPriority;
        }

        if (dye == Items.GLOW_INK_SAC) {
            signsToGlowInk.remove(sbe);
            if (!signsToWax.contains(sbe) && !signsToColor.contains(sbe)) timer = -1;
        } else if (dye == Items.HONEYCOMB){
            signsToWax.remove(sbe);
            if (!signsToColor.contains(sbe) && !signsToGlowInk.contains(sbe)) timer = -1;
        } else {
            signsToColor.remove(sbe);
            if (!signsToGlowInk.contains(sbe) && !signsToWax.contains(sbe)) timer = -1;
        }
    }

    @EventHandler
    private void onScreenOpened(OpenScreenEvent event) {
        if (!(event.screen instanceof AbstractSignEditScreen editScreen)) return;
        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) editScreen).getBlockEntity();

        Modules mods = Modules.get();
        if (mods == null) return;
        SignHistorian sh = mods.get(SignHistorian.class);
        if (sh.isActive() && sh.getRestoration(sign) != null) return;

        if (autoConfirm.get()) {
            event.cancel();
            SignText signature = getSignature(sign);
            List<String> msgs = Arrays.stream(signature.getMessages(false)).map(Text::getString).toList();
            String[] messages = new String[msgs.size()];
            messages = msgs.toArray(messages);

            if (packetQueue.isEmpty()) packetTimer = 0;
            packetQueue.addLast(new UpdateSignC2SPacket(
                sign.getPos(), true, messages[0], messages[1], messages[2], messages[3]
            ));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.getNetworkHandler() == null) return;

        if (!packetQueue.isEmpty()) {
            ++packetTimer;
            if (packetTimer >= packetDelay.get()) {
                packetTimer = 0;
                ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                    packetQueue.removeFirst(), null, true
                );
            }
        }

        if (timer == -1) {
            if (dyeSlot != -1) {
                if (dyeSlot < 9) InvUtils.swapBack();
                else InvUtils.move().from(mc.player.getInventory().selectedSlot).to(dyeSlot);
                dyeSlot = -1;
                timer = 3;
            }
        }

        WaxAura waxAura = Modules.get().get(WaxAura.class);
        if (!signsToColor.isEmpty() || !signsToGlowInk.isEmpty() || !signsToWax.isEmpty()) {
            if (waxAura.isActive()) {
                waxAura.toggle();
                didDisableWaxAura = true;
            }
        }

        ++timer;
        if (timer >= 5) {
            timer = 0;

            signsToWax.removeIf(sbe -> !sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 6));
            signsToColor.removeIf(sbe -> !sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 6));
            signsToGlowInk.removeIf(sbe -> !sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 6));
            if (!signsToColor.isEmpty()) {
                List<SignBlockEntity> signs = signsToColor
                    .stream()
                    .filter(sbe -> sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 5))
                    .filter(sbe -> Arrays.stream(sbe.getFrontText().getMessages(false)).anyMatch(msg -> !msg.getString().isEmpty())
                        || Arrays.stream(sbe.getBackText().getMessages(false)).anyMatch(msg -> !msg.getString().isEmpty()))
                    .toList();

                if (!signs.isEmpty()) {
                    SignBlockEntity sbe = signs.get(0);
                    interactSign(sbe, DyeItem.byColor(signColor.get()));
                    return;
                }
            }
            if (!signsToGlowInk.isEmpty()) {
                List<SignBlockEntity> signs = signsToGlowInk
                    .stream()
                    .filter(sbe -> sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 5))
                    .filter(sbe -> Arrays.stream(sbe.getFrontText().getMessages(false)).anyMatch(msg -> !msg.getString().isEmpty())
                        || Arrays.stream(sbe.getBackText().getMessages(false)).anyMatch(msg -> !msg.getString().isEmpty()))
                    .toList();

                if (!signs.isEmpty()) {
                    SignBlockEntity sbe = signs.get(0);
                    interactSign(sbe, Items.GLOW_INK_SAC);
                    return;
                }
            }
            if (!signsToWax.isEmpty()) {
                List<SignBlockEntity> signs = signsToWax
                    .stream()
                    .filter(sbe -> sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 5))
                    .filter(sbe -> Arrays.stream(sbe.getFrontText().getMessages(false)).anyMatch(msg -> !msg.getString().isEmpty())
                        || Arrays.stream(sbe.getBackText().getMessages(false)).anyMatch(msg -> !msg.getString().isEmpty()))
                    .toList();

                if (!signs.isEmpty()) {
                    SignBlockEntity sbe = signs.get(0);
                    interactSign(sbe, Items.HONEYCOMB);
                }
            } else {
                if (didDisableWaxAura && !waxAura.isActive()) {
                    waxAura.toggle();
                    didDisableWaxAura = false;
                }
                if (needDelayedDeactivate) toggle();
            }
        }
    }
}
