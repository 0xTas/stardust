package dev.stardust.modules;

import java.io.File;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.ArrayList;
import java.nio.file.Files;
import dev.stardust.Stardust;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.LogUtil;
import net.minecraft.util.DyeColor;
import com.mojang.authlib.GameProfile;
import dev.stardust.util.StardustUtil;
import net.minecraft.block.entity.SignText;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import dev.stardust.mixin.accessor.GameProfileAccessor;
import meteordevelopment.meteorclient.systems.modules.Module;
import dev.stardust.mixin.accessor.PlayerListS2CPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import dev.stardust.mixin.accessor.EntityTrackerUpdateS2CPacketAccessor;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AntiToS extends Module {
    public AntiToS() { super(Stardust.CATEGORY, "AntiToS", "Censor player-generated text sources according to a content blacklist."); }

    private final String BLACKLIST_FILE = "meteor-client/anti-tos.txt";

    private final SettingGroup sgSources = settings.createGroup("Source Settings");
    private final SettingGroup sgBlacklist = settings.createGroup("Content Settings");

    public enum SignMode {
        Censor, Replace, NoRender
    }

    public enum ChatMode {
        Censor, Remove
    }

    public final Setting<ChatMode> chatMode = sgSources.add(
        new EnumSetting.Builder<ChatMode>()
            .name("chat-mode")
            .description("Censor or completely replace text in chat that matches the filter.")
            .defaultValue(ChatMode.Censor)
            .build()
    );
    public final Setting<SignMode> signMode = sgSources.add(
        new EnumSetting.Builder<SignMode>()
            .name("sign-mode")
            .description("Censor or completely replace SignText that matches the filter.")
            .defaultValue(SignMode.Censor)
            .build()
    );
    private final Setting<String> familyFriendlyLine1 = sgSources.add(
        new StringSetting.Builder()
            .name("replacement-line-1")
            .defaultValue("Original text")
            .visible(() -> signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<String> familyFriendlyLine2 = sgSources.add(
        new StringSetting.Builder()
            .name("replacement-line-2")
            .defaultValue("was replaced by")
            .visible(() -> signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<String> familyFriendlyLine3 = sgSources.add(
        new StringSetting.Builder()
            .name("replacement-line-3")
            .defaultValue("Stardust AntiToS")
            .visible(() -> signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<String> familyFriendlyLine4 = sgSources.add(
        new StringSetting.Builder()
            .name("replacement-line-4")
            .defaultValue("plz no ban ☺")
            .visible(() -> signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<DyeColor> familyFriendlyColor = sgSources.add(
        new EnumSetting.Builder<DyeColor>()
            .name("replacement-color")
            .description("Render replacement SignText with the selected dye color.")
            .defaultValue(DyeColor.RED)
            .visible(() -> signMode.get() == SignMode.Replace)
            .build()
    );
    private final Setting<Boolean> familyFriendlyGlowing = sgSources.add(
        new BoolSetting.Builder()
            .name("replacement-glowing")
            .description("Render replacement SignText with glowing text.")
            .defaultValue(true)
            .visible(() -> signMode.get() == SignMode.Replace)
            .build()
    );

    private final Setting<Boolean> openBlacklistFile = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("open-blacklist-file")
            .description("Open the anti-tos.txt file.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    if (StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) StardustUtil.openFile(BLACKLIST_FILE);
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
                MsgUtil.sendMsg(BLACKLIST_FILE + " §4 was empty§8..!");
                MsgUtil.sendMsg("Please write one blacklisted item for each line of the file.");
                MsgUtil.sendMsg("Spaces and other punctuation will be treated literally.");
                MsgUtil.sendMsg("You must toggle this setting or the AntiToS module after updating the file's contents.");
            }
        }catch (Exception err) {
            LogUtil.error("Failed to read from " + blackListFile.getAbsolutePath() +"! - Why:\n"+err, this.name);
        }
    }

    // See ChatHudMixin.java
    // && ItemStackMixin.java
    // && InGameHudMixin.java
    // && BookScreenMixin.java
    // && TextRendererMixin.java
    // && EntityRendererMixin.java
    // && AbstractSignBlockEntityRendererMixin.java
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
            MsgUtil.sendModuleMsg("§4§lFailed to create anti-tos.txt in your meteor-client folder§8..!", this.name);
            MsgUtil.sendModuleMsg("§4§lThis issue is fatal§8. §4§lPlease check latest.log for more info§8§l.", this.name);
        }
    }

    @Override
    public void onDeactivate() { blacklisted.clear(); }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!Utils.canUpdate()) return;
        if ((event.packet instanceof EntityTrackerUpdateS2CPacket packet)) {
            boolean modified = false;
            List<DataTracker.SerializedEntry<?>> entries = new ArrayList<>();
            for (DataTracker.SerializedEntry<?> entry : packet.trackedValues()) {
                // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity_Metadata
                if (entry.id() == 2) { // Optional text component used for the entity's custom name
                    @SuppressWarnings("unchecked")
                    DataTracker.Entry<Optional<Text>> e = new DataTracker.Entry<>(
                        (TrackedData<Optional<Text>>) entry.handler().create(entry.id()),
                        (Optional<Text>) entry.value()
                    );

                    if (e.get().isPresent()) {
                        Text data = e.get().get();
                        if (containsBlacklistedText(data.getString())) {
                            e.set(
                                Optional.of(
                                    Text.literal(censorText(data.getString())).setStyle(data.getStyle())
                                )
                            );

                            modified = true;
                            entries.add(e.toSerialized());
                        } else entries.add(entry);
                    } else entries.add(entry);
                } else {
                    entries.add(entry);
                }
            }

            if (modified) ((EntityTrackerUpdateS2CPacketAccessor)(Object) packet).setTrackedValues(entries);
        }
        else if ((event.packet instanceof PlayerListS2CPacket packet)) {
            for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                if (entry.profile() == null) continue;

                GameProfile profile = entry.profile();
                if (containsBlacklistedText(profile.getName())) {
                    ((GameProfileAccessor) profile).setName(censorText(profile.getName()));
                    ((PlayerListS2CPacketAccessor)(Object) entry).setProfile(profile);
                }
            }
        }
    }
}
