package dev.stardust.modules;

import java.util.*;
import java.io.File;
import java.nio.file.Path;
import java.io.IOException;
import java.time.LocalDate;
import java.nio.file.Files;
import dev.stardust.Stardust;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import dev.stardust.util.StardustUtil;
import java.time.format.DateTimeFormatter;
import net.minecraft.block.entity.SignText;
import org.apache.commons.codec.binary.Hex;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class SignatureSign extends Module {
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
                if (this.inputTooLong(txt)) {
                    this.restoreValidInput(1);
                    if (this.mc.player != null) {
                        this.mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    this.lastLine1Text = txt;
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
            .visible(() -> !storyMode.get() && Objects.equals(line1Mode.get(), "File"))
            .build()
    );

    private final Setting<String> line1TimestampType = sgLine1.add(
        new ProvidedStringSetting.Builder()
            .name("Line 1 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && Objects.equals(line1Mode.get(), "Timestamp"))
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
                if (this.inputTooLong(txt)) {
                    this.restoreValidInput(2);
                    if (this.mc.player != null) {
                        this.mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    this.lastLine2Text = txt;
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
            .visible(() -> !storyMode.get() && Objects.equals(line2Mode.get(), "File"))
            .build()
    );

    private final Setting<String> line2TimestampType = sgLine2.add(
        new ProvidedStringSetting.Builder()
            .name("Line 2 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && Objects.equals(line2Mode.get(), "Timestamp"))
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
                if (this.inputTooLong(txt)) {
                    this.restoreValidInput(3);
                    if (this.mc.player != null) {
                        this.mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    this.lastLine3Text = txt;
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
            .visible(() -> !storyMode.get() && Objects.equals(line3Mode.get(), "File"))
            .build()
    );

    private final Setting<String> line3TimestampType = sgLine3.add(
        new ProvidedStringSetting.Builder()
            .name("Line 3 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && Objects.equals(line3Mode.get(), "Timestamp"))
            .build()
    );

    private final Setting<String> line4Mode = sgLine4.add(
        new ProvidedStringSetting.Builder()
            .name("Line 4 Mode")
            .defaultValue("Stardust")
            .description("Line 1 template mode")
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
                if (this.inputTooLong(txt)) {
                    this.restoreValidInput(4);
                    if (this.mc.player != null) {
                        this.mc.player.sendMessage(
                            Text.of("§8<§4✨§8> §4Input too long§7..")
                        );
                    }
                } else {
                    this.lastLine4Text = txt;
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
            .visible(() -> !storyMode.get() && Objects.equals(line4Mode.get(), "File"))
            .build()
    );

    private final Setting<String> line4TimestampType = sgLine4.add(
        new ProvidedStringSetting.Builder()
            .name("Line 4 Timestamp Type")
            .defaultValue("Month Day Year")
            .supplier(() -> timestampTypes)
            .visible(() -> !storyMode.get() && Objects.equals(line4Mode.get(), "Timestamp"))
            .build()
    );

    private final Setting<Boolean> autoConfirm = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Auto Confirm")
            .description("Automatically confirm and close the sign edit screen.")
            .defaultValue(true)
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
            .description("Click this if your last story sign failed to update with the server. (Not when sign is ghost block)")
            .defaultValue(false)
            .visible(storyMode::get)
            .build()
    );

    public static final String[] lineModes = {"Custom", "Empty", "File", "Username",
        "Username was here", "Timestamp", "Stardust", "Oasis", "Base64", "Hex", "0xHex", "ROT13"};

    public static final String[] timestampTypes = {"MM/DD/YY", "MM/DD/YYYY", "DD/MM/YY", "DD/MM/YYYY",
        "YYYY/MM/DD", "YYYY/DD/MM", "Day Month Year", "Month Day Year", "Month Year", "Year", "Day Month", "Month Day",
        "Unix Epoch"};


    public SignatureSign() { super(Stardust.CATEGORY, "SignatureSign", "Autofill signs with custom text."); }

    private int storyIndex = 0;
    private int lastIndexAmount = 0;
    private String lastLine1Text = line1Text.get();
    private String lastLine2Text = line2Text.get();
    private String lastLine3Text = line3Text.get();
    private String lastLine4Text = line4Text.get();
    private final ArrayList<String> lastLines = new ArrayList<>();
    private final ArrayList<String> storyText = new ArrayList<>();

    @Override
    public void onActivate() {
        this.lastLine1Text = line1Text.get();
        this.lastLine2Text = line2Text.get();
        this.lastLine3Text = line3Text.get();
        this.lastLine4Text = line4Text.get();
    }

    @Override
    public void onDeactivate() {
        storyText.clear();
        lastLines.clear();

        storyIndex = 0;
        lastIndexAmount = 0;
    }


    // See AbstractSignEditScreenMixin.java
    public SignText getSignature() {
        Text[] signature = new Text[4];

        List<String> lines = getSignText();
        for (int i = 0; i < lines.size(); i++) {
            signature[i] = Text.of(lines.get(i));
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

        return Objects.equals(md, "Custom") || Objects.equals(md, "Base64") || Objects.equals(md, "Hex")
            || Objects.equals(md, "0xHex") || Objects.equals(md, "ROT13");
    }

    private boolean inputTooLong(String input) {
        return this.mc.textRenderer.getWidth(input) > 90;
    }

    private void restoreValidInput(int line) {
        switch (line) {
            case 1 -> line1Text.set(this.lastLine1Text);
            case 2 -> line2Text.set(this.lastLine2Text);
            case 3 -> line3Text.set(this.lastLine3Text);
            default -> line4Text.set(this.lastLine4Text);
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

        for (int i = 0; i < signText.size(); i++) {
            if (this.inputTooLong(signText.get(i))) {
                if (mc.player != null) {
                    mc.player.sendMessage(
                        Text.of("§8<§4✨§8> §7§oLine §4§o"+(i+1)+" §7was §4§otruncated §7due to length§7..")
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
            } catch (IOException err) {
                Stardust.LOG.error("[Stardust] "+err);
            }
        } else {
            switch (line) {
                case 1: return "File not found";
                case 2: return "Please create a";
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
                storyText.addAll(Arrays.stream(String.join(" ", lineStream.toList().stream().map(String::trim).toList()).split(" ")).toList());
            } catch (IOException err) {
                Stardust.LOG.error("[Stardust] "+err);
            }
        }else {
            File backupFile = meteorFolder.resolve("autosign.txt").toFile();

            if (backupFile.exists()) {
                if (mc.player != null) {
                    mc.player.sendMessage(
                        Text.of("§8<"+StardustUtil.rCC()+"✨§8> §7A storysign.txt file was not found in your meteor-client folder.")
                    );
                    mc.player.sendMessage(
                        Text.of("§8<"+StardustUtil.rCC()+"✨§8> §7Falling back to autosign.txt instead.")
                    );
                }
                try(Stream<String> lineStream = Files.lines(backupFile.toPath())) {
                    storyText.addAll(Arrays.stream(String.join(" ", lineStream.toList()).split(" ")).toList());
                } catch (IOException err) {
                    Stardust.LOG.error("[Stardust] "+err);
                }
            } else {
                if (mc.player != null) {
                    mc.player.sendMessage(
                        Text.of("§8<"+StardustUtil.rCC()+"✨§8> §7A storysign.txt file was not found in your meteor-client folder.")
                    );
                    mc.player.sendMessage(
                        Text.of("§8<"+StardustUtil.rCC()+"✨§8> §7Please create either it, or an autosign.txt file.")
                    );
                }
                storyText.add("File not found.");
                storyText.add("Please create a");
                storyText.add("storysign.txt in");
                storyText.add("/meteor-client");
            }
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
                if (textRenderer.getWidth(line.toString()) >= 85) break;

                if (textRenderer.getWidth(storyText.get(i).trim()) > 85) {
                    if (!line.isEmpty()) break;
                    line.append(textRenderer.trimToWidth(storyText.get(i).trim(), 83));

                    ++storyIndex;
                    ++lastIndexAmount;
                    break;
                }

                if (textRenderer.getWidth(line + storyText.get(i).trim()) > 85) break;

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
                    Text.of("§8<"+StardustUtil.rCC()+"✨§8> §7Sign story complete.")
                );
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

        String currentMonth = (currentDate.getMonth().name().charAt(0)
            + currentDate.getMonth().name().substring(1).toLowerCase());

        switch (line) {
            case 1 -> {
                return switch (line1TimestampType.get()) {
                    case "MM/DD/YY" -> currentDate.format(mmddyy);
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy);
                    case "DD/MM/YY" -> currentDate.format(ddmmyy);
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy);
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd);
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm);
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
                    case "MM/DD/YY" -> currentDate.format(mmddyy);
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy);
                    case "DD/MM/YY" -> currentDate.format(ddmmyy);
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy);
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd);
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm);
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
                    case "MM/DD/YY" -> currentDate.format(mmddyy);
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy);
                    case "DD/MM/YY" -> currentDate.format(ddmmyy);
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy);
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd);
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm);
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
                    case "MM/DD/YY" -> currentDate.format(mmddyy);
                    case "MM/DD/YYYY" -> currentDate.format(mmddyyyy);
                    case "DD/MM/YY" -> currentDate.format(ddmmyy);
                    case "DD/MM/YYYY" -> currentDate.format(ddmmyyyy);
                    case "YYYY/MM/DD" -> currentDate.format(yyyymmdd);
                    case "YYYY/DD/MM" -> currentDate.format(yyyyddmm);
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
}