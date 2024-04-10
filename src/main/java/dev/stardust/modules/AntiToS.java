package dev.stardust.modules;

import java.io.File;
import java.util.HashSet;
import java.nio.file.Files;
import dev.stardust.Stardust;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import dev.stardust.util.StardustUtil;
import net.minecraft.block.entity.SignText;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AntiToS extends Module {
    public AntiToS() { super(Stardust.CATEGORY, "AntiToS", "Censor player-generated text sources according to a content blacklist."); }

    private final String BLACKLIST_FILE = "meteor-client/anti-tos.txt";

    private final SettingGroup sgSources = settings.createGroup("Source Settings");
    private final SettingGroup sgBlacklist = settings.createGroup("Content Settings");

    public enum SignMode {
        Censor, Replace
    }

    public final Setting<Boolean> chatSetting = sgSources.add(
        new BoolSetting.Builder()
            .name("Chat Messages")
            .description("Censor text in chat if it matches the content blacklist.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> booksSetting = sgSources.add(
        new BoolSetting.Builder()
            .name("Book Text")
            .description("Censor text in books if it matches the content blacklist.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> signsSetting = sgSources.add(
        new BoolSetting.Builder()
            .name("Sign Text")
            .description("Filter sign text according to the content blacklist.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SignMode> signMode = sgSources.add(
        new EnumSetting.Builder<SignMode>()
            .name("Sign Mode")
            .description("Censor or completely replace SignText that matches the filter.")
            .defaultValue(SignMode.Censor)
            .visible(signsSetting::get)
            .build()
    );
    private final Setting<String> familyFriendlyLine1 = sgSources.add(
        new StringSetting.Builder()
            .name("Replacement Line 1")
            .defaultValue("Original text")
            .visible(() -> signsSetting.get() && signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<String> familyFriendlyLine2 = sgSources.add(
        new StringSetting.Builder()
            .name("Replacement Line 2")
            .defaultValue("was replaced by")
            .visible(() -> signsSetting.get() && signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<String> familyFriendlyLine3 = sgSources.add(
        new StringSetting.Builder()
            .name("Replacement Line 3")
            .defaultValue("Stardust AntiToS")
            .visible(() -> signsSetting.get() && signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<String> familyFriendlyLine4 = sgSources.add(
        new StringSetting.Builder()
            .name("Replacement Line 4")
            .defaultValue("plz no ban ☺")
            .visible(() -> signsSetting.get() && signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<DyeColor> familyFriendlyColor = sgSources.add(
        new EnumSetting.Builder<DyeColor>()
            .name("Replacement Color")
            .description("Render replacement SignText with the selected dye color.")
            .defaultValue(DyeColor.RED)
            .visible(() -> signsSetting.get() && signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<Boolean> familyFriendlyGlowing = sgSources.add(
        new BoolSetting.Builder()
            .name("Replacement Glowing")
            .description("Render replacement SignText with glowing text.")
            .defaultValue(true)
            .visible(() -> signsSetting.get() && signMode.get() == SignMode.Replace)
            .build()
    );

    private final Setting<Boolean> openBlacklistFile = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("Open Blacklist File")
            .description("Open the anti-tos.txt file.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    if (StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) StardustUtil.openFile(mc, BLACKLIST_FILE);
                    resetBlacklistFileSetting();
                }
            })
            .build()
    );

    private final HashSet<String> blacklisted = new HashSet<>();

    private void resetBlacklistFileSetting() { openBlacklistFile.set(false); }

    private void initBlacklistText() {
        File blackListFile = FabricLoader.getInstance().getGameDir().resolve(BLACKLIST_FILE).toFile();

        try(Stream<String> lineStream = Files.lines(blackListFile.toPath())) {
            blacklisted.addAll(lineStream.toList());
            if (blacklisted.isEmpty()) {
                mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7"+BLACKLIST_FILE+" §4was empty§7!"));
                mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Please write one blacklisted item for each line of the file."));
                mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Spaces and other punctuation will be treated literally."));
                mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7You must toggle this setting or the module after updating the blacklist's contents."));
            }
        }catch (Exception err) {
            Stardust.LOG.error("[Stardust] Failed to read from "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err);
        }
    }

    // See ChatHudMixin.java
    // && BookScreenMixin.java
    // && SignBlockEntityRendererMixin.java
    public boolean containsBlacklistedText(String text) {
        return blacklisted.stream().anyMatch(line -> text.trim().toLowerCase().contains(line.trim().toLowerCase()));
    }

    public String censorText(String text) {
        for (String filter : blacklisted) {
            text = text.replaceAll("(?i)"+ Pattern.quote(filter), "*".repeat(filter.length()));
        }
        return text;
    }

    public SignText familyFriendlySignText(SignText original) {
        if (signMode.get() == SignMode.Censor) {
            Text[] lines = {
                Text.of(censorText(original.getMessage(0, false).getString())),
                Text.of(censorText(original.getMessage(1, false).getString())),
                Text.of(censorText(original.getMessage(2, false).getString())),
                Text.of(censorText(original.getMessage(3, false).getString()))
            };
            return new SignText(lines, lines, original.getColor(), original.isGlowing());
        } else {
            Text[] lines = {
                Text.of(familyFriendlyLine1.get()),
                Text.of(familyFriendlyLine2.get()),
                Text.of(familyFriendlyLine3.get()),
                Text.of(familyFriendlyLine4.get())
            };
            return new SignText(lines, lines, familyFriendlyColor.get(), familyFriendlyGlowing.get());
        }
    }

    @Override
    public void onActivate() {
        if (StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) initBlacklistText();
        else {
            toggle();
            mc.player.sendMessage(Text.of(
                "§8[§4Stardust§8] §4§lFailed to create anti-tos.txt in your meteor-client folder§8!"
            ));
            mc.player.sendMessage(Text.of(
                "§8[§4Stardust§8] §4§lThis issue is fatal§8§l. §4§lPlease check latest.log for more info§8§l."
            ));
        }
    }

    @Override
    public void onDeactivate() { blacklisted.clear(); }
}
