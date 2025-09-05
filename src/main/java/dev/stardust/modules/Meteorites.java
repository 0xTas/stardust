package dev.stardust.modules;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Files;
import com.google.gson.Gson;
import dev.stardust.Stardust;
import dev.stardust.util.LogUtil;
import dev.stardust.util.MsgUtil;
import com.google.gson.GsonBuilder;
import dev.stardust.util.StardustUtil;
import java.nio.file.StandardOpenOption;
import dev.stardust.config.StardustConfig;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.meteorclient.settings.*;
import dev.stardust.gui.screens.MeteoritesScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import dev.stardust.gui.widgets.meteorites.SaveData;
import meteordevelopment.meteorclient.gui.GuiThemes;
import dev.stardust.gui.widgets.meteorites.FieldSize;
import dev.stardust.gui.widgets.meteorites.HighScore;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: MeteoritesScreen.java && dev/stardust/gui/widgets/meteorites/*
 **/
public class Meteorites extends Module {
    public Meteorites() {
        super(Stardust.CATEGORY, "Meteorites", "Play Meteorites (an Asteroids-style arcade shooter).");
        runInMainMenu = true;
        if (shipColor.get().rainbow) RainbowColors.add(shipColor.get());
        if (flameColor.get().rainbow) RainbowColors.add(flameColor.get());
        if (bulletColor.get().rainbow) RainbowColors.add(bulletColor.get());
    }

    private static final String GAME_FOLDER = "meteor-client/minigames/meteorites";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String[] GAMEPLAY_TIPS = new String[] {
        "Press P to pause the game.",
        "Press M to mute the game audio.",
        "Every ten waves, a boss appears.",
        "Hold space or left-click to shoot.",
        "Each field size tracks its own high score data.",
        "You can disable these tips in the module settings.",
        "Every six waves you are granted an extra life, up to 5.",
        "Some powerups are only available when mouse-aim is enabled.",
        "Clicking the middle-mouse button toggles mouse-aim on or off.",
        "Exiting the game will save automatically, if saving is enabled.",
        "Some powerups display useful info near the top of the game window.",
        "The rare Stardust powerup comes in multiple different gun variants.",
        "With mouse-aim disabled, rotate with A, D, Right Arrow, Left Arrow.",
        "Perform a hyperjump by pressing H or right-click. Costs 1000 points.",
        "The starfield can be disabled in the settings if you find it distracting.",
        "Your right-click ability can be used to get you out of sticky situations.",
        "The Gravity Well can be repositioned as often as you like while deployed.",
        "Some powerups replace your right-click ability, but can be used for free.",
        "Every third wave you gain a random powerup if you don't already have one.",
        "You may want to sacrifice an extra life for a chance at a better powerup.",
        "Waves get harder as the game goes on, but rare powerups become more common.",
        "The powerup you earn the most points with is tracked along with high scores."
    };

    public final Setting<Boolean> debug = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("debug")
            .description("cheater cheater pumpkin eater")
            .defaultValue(false)
            .visible(() -> false)
            .onChanged(it -> {
                if (!this.loaded) {
                    if (it && !this.enteredCheatCode) {
                        this.disableDebug();
                    } else if (!it && this.enteredCheatCode) {
                        this.enteredCheatCode = false;
                    }
                }
            })
            .build()
    );
    public final Setting<Boolean> randomizePowerups = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("randomize-powerups")
            .defaultValue(false)
            .visible(debug::get)
            .build()
    );
    public final Setting<Boolean> renderStarfield = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("render-starfield")
            .description("Renders a background starfield with parallax visual effects.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> gameTips = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("gameplay-tips")
            .description("Displays random gameplay tips on the pause screen.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> mouseAim = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("allow-mouse-aim")
            .description("Makes the ship point towards the mouse cursor when inside the play area.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> mouseAimOutside = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("when-outside-area")
            .description("Follow the mouse even when the cursor is outside of the play area.")
            .defaultValue(true)
            .visible(mouseAim::get)
            .build()
    );
    public final Setting<Boolean> renderMap = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("force-render-minimap")
            .description("Continues rendering the Xaeros minimap while the Meteorites screen is open.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> saveGames = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("save-games")
            .description("Saves the state of your game when closing the Meteorites screen.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> sounds = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("game-sounds")
            .description("Plays game sounds.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Double> soundVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("sounds-volume")
            .min(0.1).max(4.0)
            .defaultValue(0.5)
            .visible(sounds::get)
            .build()
    );
    public final Setting<FieldSize> canvasSize = settings.getDefaultGroup().add(
        new EnumSetting.Builder<FieldSize>()
            .name("field-size")
            .description("The size preset to use for the game board area.")
            .defaultValue(FieldSize.Small)
            .build()
    );
    public final Setting<SettingColor> shipColor = settings.getDefaultGroup().add(
        new ColorSetting.Builder()
            .name("ship-color")
            .description("What color to use for the player's ship.")
            .defaultValue(new SettingColor(48, 61, 84))
            .build()
    );
    public final Setting<SettingColor> flameColor = settings.getDefaultGroup().add(
        new ColorSetting.Builder()
            .name("flame-color")
            .description("What color to use for the ship's thrust flame.")
            .defaultValue(new SettingColor(255, 160, 32))
            .build()
    );
    public final Setting<SettingColor> bulletColor = settings.getDefaultGroup().add(
        new ColorSetting.Builder()
            .name("laser-color")
            .description("What color to use for the ship's laser bullets.")
            .defaultValue(new SettingColor(224, 0, 118))
            .build()
    );
    public final Setting<SettingColor> gravityColor = settings.getDefaultGroup().add(
        new ColorSetting.Builder()
            .name("gravity-color")
            .description("What color to use for the gravity well indicator.")
            .defaultValue(new SettingColor(255, 0, 255, 69))
            .build()
    );

    private boolean loaded;
    public boolean enteredCheatCode;
    public @Nullable SaveData saveData = null;
    public @Nullable HighScore highScore = null;

    public void saveGame(SaveData data) {
        saveData = data;
        Path saveFolder = FabricLoader.getInstance().getGameDir().resolve(GAME_FOLDER);

        //noinspection ResultOfMethodCallIgnored
        saveFolder.toFile().mkdirs();
        Path save = saveFolder.resolve("save.json");
        if (!Files.exists(save)) {
            if (!StardustUtil.checkOrCreateFile(mc, GAME_FOLDER + "/save.json")) {
                MsgUtil.sendModuleMsg("Failed to create save file§c..!", this.name);
            }
        }
        try (Writer writer = Files.newBufferedWriter(save, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(data, writer);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }
    }

    public void loadGame() {
        loaded = true;
        Path saveFolder = FabricLoader.getInstance().getGameDir().resolve(GAME_FOLDER);

        //noinspection ResultOfMethodCallIgnored
        saveFolder.toFile().mkdirs();
        Path save = saveFolder.resolve("save.json");
        if (!Files.exists(save)) {
            if (!StardustUtil.checkOrCreateFile(mc, GAME_FOLDER + "/save.json")) {
                MsgUtil.sendModuleMsg("Failed to create save file§c..!", this.name);
            }
        }

        SaveData data = null;
        try (Reader reader = Files.newBufferedReader(save)) {
            data = GSON.fromJson(reader, SaveData.class);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }

        if (data != null) {
            saveData = data;
            enteredCheatCode = data.cheating();
            debug.set(enteredCheatCode);
        }
    }

    public void clearSave() {
        saveGame(null);
    }

    public void saveHighScore(HighScore score) {
        highScore = score;
        Path scoreFolder = FabricLoader.getInstance().getGameDir().resolve(GAME_FOLDER);

        //noinspection ResultOfMethodCallIgnored
        scoreFolder.toFile().mkdirs();
        Path scores = scoreFolder.resolve("highscores.json");
        if (!Files.exists(scores)) {
            if (!StardustUtil.checkOrCreateFile(mc, GAME_FOLDER + "/highscores.json")) {
                MsgUtil.sendModuleMsg("Failed to create high score file§c..!", this.name);
            }
        }
        try (Writer writer = Files.newBufferedWriter(scores, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(score, writer);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }
    }

    public void loadHighScore() {
        Path scoreFolder = FabricLoader.getInstance().getGameDir().resolve(GAME_FOLDER);

        //noinspection ResultOfMethodCallIgnored
        scoreFolder.toFile().mkdirs();
        Path scores = scoreFolder.resolve("highscores.json");
        if (!Files.exists(scores)) {
            if (!StardustUtil.checkOrCreateFile(mc, GAME_FOLDER + "/highscores.json")) {
                MsgUtil.sendModuleMsg("Failed to create save file§c..!", this.name);
            }
        }

        HighScore score = null;
        try (Reader reader = Files.newBufferedReader(scores)) {
            score = GSON.fromJson(reader, HighScore.class);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }

        if (score != null) highScore = score;
    }

    public void clearHighScore() { saveHighScore(null); }

    private void disableDebug() {
        debug.set(false);
    }

    @Override
    public void onActivate() {
        loadGame();
        loadHighScore();
        try {
            mc.setScreen(new MeteoritesScreen(this, GuiThemes.get(), "Meteorites"));
        } catch (Exception err) {
            LogUtil.error("Failed to open Meteorites screen: " + err, this.name);
        }
    }

    @Override
    public void onDeactivate() {
        loaded = false;
        if (mc.currentScreen instanceof MeteoritesScreen) {
            try {
                mc.setScreen(null);
            } catch (Exception err) {
                LogUtil.error("Failed to close Meteorites screen: " + err, this.name);
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();

        WButton clearSave = list.add(theme.button("Clear Save")).widget();
        WButton clearScore = list.add(theme.button("Clear High Scores")).widget();

        clearSave.action = this::clearSave;
        clearScore.action = this::clearHighScore;

        return list;
    }
}

