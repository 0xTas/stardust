package dev.stardust.modules;

import java.awt.*;
import java.util.*;
import java.io.File;
import java.util.List;
import java.nio.file.Path;
import java.time.LocalDate;
import java.nio.file.Files;
import net.minecraft.text.*;
import dev.stardust.Stardust;
import java.util.stream.Stream;
import net.minecraft.util.Hand;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.DyeItem;
import net.minecraft.util.DyeColor;
import java.util.stream.Collectors;
import net.minecraft.item.ItemStack;
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
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;


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
    private final SettingGroup sgLine1 = settings.createGroup("Line 1");
    private final SettingGroup sgLine2 = settings.createGroup("Line 2");
    private final SettingGroup sgLine3 = settings.createGroup("Line 3");
    private final SettingGroup sgLine4 = settings.createGroup("Line 4");

    private final Setting<Boolean> storyMode = sgMode.add(
        new BoolSetting.Builder()
            .name("Story Mode")
            .description("Fill signs continuously from .minecraft/meteor-client/storysign.text")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> secretSign = sgMode.add(
        new BoolSetting.Builder()
            .name("Secret Signs")
            .description("Pad each line with spaces to hide your message from being rendered. Will then only be viewable via metadata (ChatSigns etc.)")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> glowSigns = sgMode.add(
        new BoolSetting.Builder()
            .name("Glow Signs")
            .description("Apply glow squid ink onto signs if found in inventory.")
            .defaultValue(false)
            .build()
    );

    private final Setting<DyeColor> signColor = sgMode.add(
        new EnumSetting.Builder<DyeColor>()
            .name("Sign Color")
            .description("Apply selected dye color onto signs if found in inventory.")
            .defaultValue(DyeColor.BLACK)
            .build()
    );

    public final Setting<Boolean> signFreedom = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Bypass Length Limits")
            .description("Bypass client-sided length limits for sign text.")
            .defaultValue(true)
            .visible(() -> !secretSign.get())
            .build()
    );

    private final Setting<String> line1Mode = sgLine1.add(
        new ProvidedStringSetting.Builder()
            .name("Line 1 Mode")
            .description("Line 1 template mode")
            .defaultValue("Stardust")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line1Text = sgLine1.add(
        new StringSetting.Builder()
            .name("Line 1 Text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(1))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(1);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    lastLine1Text = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line1FileLine = sgLine1.add(
        new IntSetting.Builder()
            .name("Line 1 File Line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(1)
            .visible(() -> !storyMode.get() && line1Mode.get().equals("File"))
            .build()
    );

    private final Setting<String> line1TimestampType = sgLine1.add(
        new ProvidedStringSetting.Builder()
            .name("Line 1 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line1Mode.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line1TimestampDelim = sgLine1.add(
        new ProvidedStringSetting.Builder()
            .name("Line 1 Timestamp Delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line1Mode.get().equals("Timestamp") && line1TimestampType.get().contains("/"))
            .build()
    );

    private final Setting<String> line2Mode = sgLine2.add(
        new ProvidedStringSetting.Builder()
            .name("Line 2 Mode")
            .description("Line 2 template mode")
            .defaultValue("Stardust")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line2Text = sgLine2.add(
        new StringSetting.Builder()
            .name("Line 2 Text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(2))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(2);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    lastLine2Text = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line2FileLine = sgLine2.add(
        new IntSetting.Builder()
            .name("Line 2 File Line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(2)
            .visible(() -> !storyMode.get() && line2Mode.get().equals("File"))
            .build()
    );

    private final Setting<String> line2TimestampType = sgLine2.add(
        new ProvidedStringSetting.Builder()
            .name("Line 2 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line2Mode.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line2TimestampDelim = sgLine2.add(
        new ProvidedStringSetting.Builder()
            .name("Line 2 Timestamp Delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line2Mode.get().equals("Timestamp") && line2TimestampType.get().contains("/"))
            .build()
    );

    private final Setting<String> line3Mode = sgLine3.add(
        new ProvidedStringSetting.Builder()
            .name("Line 3 Mode")
            .defaultValue("Stardust")
            .description("Line 3 template mode")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line3Text = sgLine3.add(
        new StringSetting.Builder()
            .name("Line 3 Text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(3))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(3);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    lastLine3Text = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line3FileLine = sgLine3.add(
        new IntSetting.Builder()
            .name("Line 3 File Line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(3)
            .visible(() -> !storyMode.get() && line3Mode.get().equals("File"))
            .build()
    );

    private final Setting<String> line3TimestampType = sgLine3.add(
        new ProvidedStringSetting.Builder()
            .name("Line 3 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line3Mode.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line3TimestampDelim = sgLine3.add(
        new ProvidedStringSetting.Builder()
            .name("Line 3 Timestamp Delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line3Mode.get().equals("Timestamp") && line3TimestampType.get().contains("/"))
            .build()
    );

    private final Setting<String> line4Mode = sgLine4.add(
        new ProvidedStringSetting.Builder()
            .name("Line 4 Mode")
            .defaultValue("Stardust")
            .description("Line 4 template mode")
            .supplier(() -> lineModes)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<String> line4Text = sgLine4.add(
        new StringSetting.Builder()
            .name("Line 4 Text")
            .defaultValue("")
            .visible(() -> !storyMode.get() && textLineVisibility(4))
            .onChanged(txt -> {
                if (signFreedom.isVisible() && !signFreedom.get() && inputTooLong(txt)) {
                    restoreValidInput(4);
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    lastLine4Text = txt;
                }
            })
            .build()
    );

    private final Setting<Integer> line4FileLine = sgLine4.add(
        new IntSetting.Builder()
            .name("Line 4 File Line")
            .description("Which line of .minecraft/meteor-client/autosign.txt to use.")
            .range(1, 1000)
            .sliderRange(1, 420)
            .defaultValue(4)
            .visible(() -> !storyMode.get() && line4Mode.get().equals("File"))
            .build()
    );

    private final Setting<String> line4TimestampType = sgLine4.add(
        new ProvidedStringSetting.Builder()
            .name("Line 4 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && line4Mode.get().equals("Timestamp"))
            .build()
    );

    private final Setting<String> line4TimestampDelim = sgLine4.add(
        new ProvidedStringSetting.Builder()
            .name("Line 4 Timestamp Delimiter")
            .defaultValue("/")
            .supplier(() -> timestampDelimiters)
            .visible(() -> !storyMode.get() && line4Mode.get().equals("Timestamp") && line4TimestampType.get().contains("/"))
            .build()
    );

    private final Setting<Boolean> autoConfirm = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Auto Confirm")
            .description("Automatically confirm and close the sign edit screen.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> shortenedMonth = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Shortened Month")
            .description("Shorten the month to its abbreviation")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoDisable = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Auto Disable")
            .description("Automatically disable the module after placing a sign.")
            .defaultValue(false)
            .visible(() -> !storyMode.get())
            .build()
    );

    private final Setting<Boolean> redo = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Redo Last Sign")
            .description("Click this to redo your last-placed story sign. Useful if you misplaced it.")
            .defaultValue(false)
            .visible(storyMode::get)
            .build()
    );

    private final Setting<Boolean> openFolder = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Open Meteor Client Folder")
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
    private int lastIndexAmount = 0;
    private String lastLine1Text = line1Text.get();
    private String lastLine2Text = line2Text.get();
    private String lastLine3Text = line3Text.get();
    private String lastLine4Text = line4Text.get();
    private final ArrayList<String> lastLines = new ArrayList<>();
    private final ArrayList<String> storyText = new ArrayList<>();
    private final HashSet<SignBlockEntity> signsToColor = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToGlowInk = new HashSet<>();


    @Override
    public void onActivate() {
        lastLine1Text = line1Text.get();
        lastLine2Text = line2Text.get();
        lastLine3Text = line3Text.get();
        lastLine4Text = line4Text.get();
    }

    @Override
    public void onDeactivate() {
        storyText.clear();
        lastLines.clear();
        signsToColor.clear();

        timer = 0;
        storyIndex = 0;
        lastIndexAmount = 0;
    }


    // See AbstractSignEditScreenMixin.java
    public SignText getSignature(SignBlockEntity sign) {
        Text[] signature = new Text[4];

        List<String> lines = getSignText();
        for (int i = 0; i < lines.size(); i++) {
            signature[i] = Text.of(lines.get(i));
        }

        if (glowSigns.get()) {
            signsToGlowInk.add(sign);
        }
        if (signColor.get() != DyeColor.BLACK) {
            signsToColor.add(sign);
        }

        return new SignText(signature, signature, DyeColor.BLACK, false);
    }

    public boolean needsDisabling() {
        return autoDisable.get() && !storyMode.get();
    }

    public boolean getAutoConfirm() { return autoConfirm.get(); }

    private boolean textLineVisibility(int line) {
        String md = switch (line) {
            case 1 -> line1Mode.get();
            case 2 -> line2Mode.get();
            case 3 -> line3Mode.get();
            default -> line4Mode.get();
        };

        return md.equals("Custom") || md.equals("Base64") || md.equals("Hex")
            || md.equals("0xHex") || md.equals("ROT13");
    }

    private boolean inputTooLong(String input) {
        return mc.textRenderer.getWidth(input) > 90;
    }

    private void restoreValidInput(int line) {
        switch (line) {
            case 1 -> line1Text.set(lastLine1Text);
            case 2 -> line2Text.set(lastLine2Text);
            case 3 -> line3Text.set(lastLine3Text);
            default -> line4Text.set(lastLine4Text);
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
            switch (line1Mode.get()) {
                case "Custom" -> signText.add(line1Text.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line1FileLine.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(1));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust" -> signText.add("<✨>");
                case "Oasis" -> signText.add("<☯>");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line1Text.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line1Text.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line1Text.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line1Text.get()));
            }
            switch (line2Mode.get()) {
                case "Custom" -> signText.add(line2Text.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line2FileLine.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(2));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust", "Oasis" -> signText.add("<"+username+">");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line2Text.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line2Text.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line2Text.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line2Text.get()));
            }
            switch (line3Mode.get()) {
                case "Custom" -> signText.add(line3Text.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line3FileLine.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(3));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust", "Oasis" -> signText.add(System.currentTimeMillis() / 1000 + " UTC");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line3Text.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line3Text.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line3Text.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line3Text.get()));
            }
            switch (line4Mode.get()) {
                case "Custom" -> signText.add(line4Text.get());
                case "Empty" -> signText.add(" ");
                case "File" -> signText.add(getSignTextFromFile(line4FileLine.get()-1));
                case "Timestamp" -> signText.add(getTimestamp(4));
                case "Username" -> signText.add(username);
                case "Username was here" -> signText.add(username+" was here");
                case "Stardust" -> signText.add("<✨>");
                case "Oasis" -> signText.add("<☯>");
                case "Base64" -> signText.add(Base64.getEncoder().encodeToString(line4Text.get().getBytes()));
                case "Hex" -> signText.add(Hex.encodeHexString(line4Text.get().getBytes()));
                case "0xHex" -> signText.add("0x"+Hex.encodeHexString(line4Text.get().getBytes()));
                case "ROT13" -> signText.add(rot13(line4Text.get()));
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
                        Text.of("§8<§4✨§8> §7§oLine §4§o"+(i+1)+" §7§owon't render fully due to length..")
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
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Created autosign.txt in meteor-client folder."));

                        Text msg = Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Click §2§lhere §r§7to open the folder.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, meteorFolder.toFile().getAbsolutePath()));

                        MutableText txt = msg.copyContentOnly().setStyle(style);
                        mc.player.sendMessage(txt);
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
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Created storysign.txt in meteor-client folder."));

                        Text msg = Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Click §2§lhere §r§7to open the folder.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, meteorFolder.toFile().getAbsolutePath()));

                        MutableText txt = msg.copyContentOnly().setStyle(style);
                        mc.player.sendMessage(txt);
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
                    Text.of("§8<"+StardustUtil.rCC()+"✨§8> §7§oSign story complete.")
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
                return switch (line1TimestampType.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line1TimestampDelim.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line1TimestampDelim.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line1TimestampDelim.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line1TimestampDelim.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line1TimestampDelim.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line1TimestampDelim.get());
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
                return switch (line2TimestampType.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line2TimestampDelim.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line2TimestampDelim.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line2TimestampDelim.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line2TimestampDelim.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line2TimestampDelim.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line2TimestampDelim.get());
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
                return switch (line3TimestampType.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line3TimestampDelim.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line3TimestampDelim.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line3TimestampDelim.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line3TimestampDelim.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line3TimestampDelim.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line3TimestampDelim.get());
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
                return switch (line4TimestampType.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy).replace("/", line4TimestampDelim.get());
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy).replace("/", line4TimestampDelim.get());
                    case "DD/MM/YY" -> currentDate.format(ddmmyy).replace("/", line4TimestampDelim.get());
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy).replace("/", line4TimestampDelim.get());
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd).replace("/", line4TimestampDelim.get());
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm).replace("/", line4TimestampDelim.get());
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
        Path meteorFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client");
        File folder = meteorFolder.toFile();

        if (Desktop.isDesktopSupported()) {
            EventQueue.invokeLater(() -> {
                try {
                    Desktop.getDesktop().open(folder);
                }catch (Exception err) {
                    Stardust.LOG.error("[Stardust] Failed to open "+ folder.getAbsolutePath() +"! - Why:\n"+err);
                }
            });
        } else {
            try {
                Runtime runtime = Runtime.getRuntime();
                if (System.getenv("OS") == null) return;
                if (System.getenv("OS").contains("Windows")) {
                    runtime.exec("rundll32 url.dll, FileProtocolHandler " + folder.getAbsolutePath());
                } else {
                    runtime.exec("xdg-open " + folder.getAbsolutePath());
                }
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Failed to open "+ folder.getAbsolutePath() +"! - Why:\n"+err);
                if (mc.player != null) mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"✨§8> §4§oFailed to open meteor-client folder§7."));
            }

        }

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
                    if (n < 9) InvUtils.swap(n, true);
                    else InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);

                    dyeSlot = n;
                    timer = 3;
                    return;
                }
            }
        } else {
            Rotations.rotate(
                Rotations.getYaw(pos),
                Rotations.getPitch(pos), 69420,
                () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit)
            );
            timer = -1;
        }

        if (dye == Items.GLOW_INK_SAC) {
            this.signsToGlowInk.remove(sbe);
        } else {
            this.signsToColor.remove(sbe);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.currentScreen != null) return;

        if (timer == -1) {
            if (dyeSlot != -1) {
                if (dyeSlot < 9) InvUtils.swapBack();
                else InvUtils.move().from(mc.player.getInventory().selectedSlot).to(dyeSlot);
                dyeSlot = -1;
                timer = 7;
            }
        }

        if (signsToColor.isEmpty() && signsToGlowInk.isEmpty()) return;

        ++timer;
        if (timer >= 10) {
            timer = 0;
            if (!signsToColor.isEmpty()) {
                List<SignBlockEntity> signs = signsToColor
                    .stream()
                    .toList();

                if (signs.isEmpty()) return;
                SignBlockEntity sbe = signs.get(0);
                Text[] msgs = sbe.getFrontText().getMessages(false);
                if (Arrays.stream(msgs).allMatch(msg -> msg.getString().trim().isEmpty())) return;

                interactSign(sbe, DyeItem.byColor(signColor.get()));
            } else {
                List<SignBlockEntity> signs = signsToGlowInk
                    .stream()
                    .toList();

                if (signs.isEmpty()) return;
                SignBlockEntity sbe = signs.get(0);
                Text[] msgs = sbe.getFrontText().getMessages(false);
                if (Arrays.stream(msgs).allMatch(msg -> msg.getString().trim().isEmpty())) return;

                interactSign(sbe, Items.GLOW_INK_SAC);
            }
        }
    }
}
