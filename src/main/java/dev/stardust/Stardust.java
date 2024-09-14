package dev.stardust;

import org.slf4j.Logger;
import dev.stardust.modules.*;
import dev.stardust.commands.*;
import com.mojang.logging.LogUtils;
import dev.stardust.util.StardustUtil;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.loader.api.metadata.CustomValue;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.systems.config.Config;
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

    @Override
    public void onInitialize() {
        Commands.add(new Panorama());
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
        Modules.get().add(new BannerData());
        Modules.get().add(new MusicTweaks());
        Modules.get().add(new TreasureESP());
        Modules.get().add(new LoreLocator());
        Modules.get().add(new AxolotlTools());
        Modules.get().add(new StashBrander());
        Modules.get().add(new SignatureSign());
        Modules.get().add(new SignHistorian());
        Modules.get().add(new AutoDyeShulkers());
        Modules.get().add(new AutoDrawDistance());

        // See SplashTextRendererMixin.java
        greenSplashTextSetting = Config.get().settings.getGroup("Visual").add(
            new BoolSetting.Builder()
                .name("Green Splash Text")
                .description(">Makes the title splash texts green.")
                .defaultValue(false)
                .build()
        );
        // See TitleScreenMixin.java
        rotateSplashTextSetting = Config.get().settings.getGroup("Visual").add(
            new BoolSetting.Builder()
                .name("Rotate Splash Text")
                .description("Picks a new random splash text every 20 seconds.")
                .defaultValue(false)
                .build()
        );
        directConnectButtonSetting = Config.get().settings.getGroup("Visual").add(
            new BoolSetting.Builder()
                .name("Direct Connect Button")
                .description("Adds a button to the main menu that directly connects you to 2b2t.org")
                .defaultValue(false)
                .build()
        );

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
    public GithubRepo getRepo() { return new GithubRepo("0xTas", "Stardust", "1.21.1", null); }
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
