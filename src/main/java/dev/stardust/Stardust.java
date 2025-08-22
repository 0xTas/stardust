package dev.stardust;

import org.slf4j.Logger;
import dev.stardust.modules.*;
import dev.stardust.commands.*;
import dev.stardust.gui.themes.*;
import dev.stardust.util.MsgUtil;
import dev.stardust.hud.ConwayHud;
import com.mojang.logging.LogUtils;
import dev.stardust.util.StardustUtil;
import dev.stardust.config.StardustConfig;
import dev.stardust.managers.PacketManager;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import net.fabricmc.loader.api.metadata.CustomValue;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Stardust extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final HudGroup HUD_GROUP = new HudGroup("Stardust");
    public static final Category CATEGORY = new Category("Stardust", StardustUtil.chooseMenuIcon());

    private PacketManager packetManager;

    @Override
    public void onInitialize() {
        Commands.add(new Life());
        Commands.add(new Loadout());
        Commands.add(new Panorama());
        Commands.add(new Stats2b2t());
        Commands.add(new Playtime2b2t());
        Commands.add(new LastSeen2b2t());
        Commands.add(new FirstSeen2b2t());

        Modules.get().add(new Honker());
        Modules.get().add(new WaxAura());
        Modules.get().add(new AntiToS());
        Modules.get().add(new Updraft());
        Modules.get().add(new Grinder());
        Modules.get().add(new RoadTrip());
        Modules.get().add(new Loadouts());
        Modules.get().add(new AdBlocker());
        Modules.get().add(new AutoDoors());
        Modules.get().add(new AutoMason());
        Modules.get().add(new AutoSmith());
        Modules.get().add(new BookTools());
        Modules.get().add(new ChatSigns());
        Modules.get().add(new RapidFire());
        Modules.get().add(new RocketMan());
        Modules.get().add(new RocketJump());
        Modules.get().add(new BannerData());
        Modules.get().add(new PagePirate());
        Modules.get().add(new Archaeology());
        Modules.get().add(new MusicTweaks());
        Modules.get().add(new TreasureESP());
        Modules.get().add(new LoreLocator());
        Modules.get().add(new AxolotlTools());
        Modules.get().add(new StashBrander());
        Modules.get().add(new SignatureSign());
        Modules.get().add(new SignHistorian());
        Modules.get().add(new AutoDyeShulkers());
        Modules.get().add(new AutoDrawDistance());

        Hud.get().register(ConwayHud.INFO);

        GuiThemes.add(DarkTheme.INSTANCE);
        GuiThemes.add(SnowyTheme.INSTANCE);
        GuiThemes.add(LambdaTheme.INSTANCE);
        GuiThemes.add(StardustTheme.INSTANCE);
        GuiThemes.add(MidnightTheme.INSTANCE);
        GuiThemes.add(MonochromeTheme.INSTANCE);

        packetManager = new PacketManager();

        StardustConfig.initialize();
        MsgUtil.initModulePrefixes();
        LOG.info("<✨> Stardust initialized.");

        if (!StardustUtil.XAERO_AVAILABLE) {
            LOG.warn("[Stardust] Skipping Xaero Map integration as one or both of xaero world map & xaero minimap are missing..!");
        }
    }


    @Override
    public String getPackage() {
        return "dev.stardust";
    }
    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Stardust.CATEGORY);
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
