package dev.stardust;

import org.slf4j.Logger;
import dev.stardust.modules.*;
import dev.stardust.commands.*;
import dev.stardust.util.MsgUtil;
import com.mojang.logging.LogUtils;
import dev.stardust.util.StardustUtil;
import net.fabricmc.loader.api.FabricLoader;
import dev.stardust.managers.PacketSpamManager;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.loader.api.metadata.CustomValue;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.settings.SettingGroup;
import dev.stardust.util.StardustUtil.IllegalDisconnectMethod;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Stardust extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Stardust", StardustUtil.chooseMenuIcon());
    public static Setting<Boolean> greenSplashTextSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> rotateSplashTextSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> directConnectButtonSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> illegalDisconnectButtonSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> disableMeteorClientTelemetry = new BoolSetting.Builder().build();
    public static Setting<Boolean> antiInventoryPacketKick = new BoolSetting.Builder().build();
    public static Setting<IllegalDisconnectMethod> illegalDisconnectMethodSetting = new EnumSetting.Builder<IllegalDisconnectMethod>().defaultValue(IllegalDisconnectMethod.Slot).build();

    private PacketSpamManager packetSpamManager;

    @Override
    public void onInitialize() {
        Commands.add(new Panorama());
        Commands.add(new Stats2b2t());
        Commands.add(new Playtime2b2t());
        Commands.add(new LastSeen2b2t());
        Commands.add(new FirstSeen2b2t());

        Modules.get().add(new Honker());
        Modules.get().add(new WaxAura());
        Modules.get().add(new AntiToS());
        Modules.get().add(new Updraft());
        Modules.get().add(new AutoDoors());
        Modules.get().add(new AutoSmith());
        Modules.get().add(new BookTools());
        Modules.get().add(new ChatSigns());
        Modules.get().add(new RocketMan());
        Modules.get().add(new RocketJump());
        Modules.get().add(new BannerData());
        Modules.get().add(new PagePirate());
        Modules.get().add(new Archaeology());
        Modules.get().add(new MusicTweaks());
        Modules.get().add(new TreasureESP());
        Modules.get().add(new LoreLocator());
        // Modules.get().add(new AxolotlTools());
        Modules.get().add(new StashBrander());
        Modules.get().add(new SignatureSign());
        Modules.get().add(new SignHistorian());
        Modules.get().add(new AutoDyeShulkers());
        Modules.get().add(new AutoDrawDistance());
        SettingGroup sgStardust = Config.get().settings.createGroup("Stardust");

        // See SplashTextRendererMixin.java
        greenSplashTextSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("green-splash-text")
                .description(">Makes the title splash texts green.")
                .defaultValue(false)
                .build()
        );
        // See TitleScreenMixin.java
        rotateSplashTextSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("rotate-splash-text")
                .description("Picks a new random splash text every 20 seconds.")
                .defaultValue(false)
                .build()
        );
        directConnectButtonSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("direct-connect-button")
                .description("Adds a button to the main menu that directly connects you to 2b2t.org")
                .defaultValue(false)
                .build()
        );
        // See GameMenuScreenMixin.java
        illegalDisconnectButtonSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("illegal-disconnect-button")
                .description("Adds a button to the main menu that forces the server to kick you when pressed.")
                .defaultValue(false)
                .build()
        );
        // See StardustUtil.java
        illegalDisconnectMethodSetting = sgStardust.add(
            new EnumSetting.Builder<IllegalDisconnectMethod>()
                .name("illegal-disconnect-method")
                .description("The method to use to cause the server to kick you.")
                .defaultValue(IllegalDisconnectMethod.Chat)
                .build()
        );
        // See OnlinePlayersMixin.java
        disableMeteorClientTelemetry = sgStardust.add(
            new BoolSetting.Builder()
                .name("disable-meteor-telemetry")
                .description("Disables sending periodic telemetry pings to meteorclient.com for their online player count api.")
                .defaultValue(false)
                .build()
        );
        // See PacketSpamManager.java
        antiInventoryPacketKick = sgStardust.add(
            new BoolSetting.Builder()
                .name("anti-packet-spam-kick")
                .description("Attempts to prevent you from getting kicked for sending too many invalid inventory packets.")
                .defaultValue(false)
                .build()
        );

        packetSpamManager = new PacketSpamManager();

        MsgUtil.initModulePrefixes();
        LOG.info("<✨> Stardust initialized.");
    }


    @Override
    public String getPackage() {
        return "dev.stardust";
    }
    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
    @Override
    public String getWebsite() { return "https://github.com/0xTas/stardust"; }
    @Override
    public GithubRepo getRepo() { return new GithubRepo("0xTas", "Stardust", "main", null); }
    @Override
    public String getCommit() {
        CustomValue commit = FabricLoader.getInstance()
            .getModContainer("stardust")
            .orElseThrow()
            .getMetadata()
            .getCustomValue(MeteorClient.MOD_ID + ":commit");

        return commit == null ? null : commit.getAsString();
    }
}
