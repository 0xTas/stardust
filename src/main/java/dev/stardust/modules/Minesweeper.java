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
import org.jetbrains.annotations.Nullable;
import net.fabricmc.loader.api.FabricLoader;
import dev.stardust.gui.widgets.WMinesweeper;
import meteordevelopment.meteorclient.settings.*;
import dev.stardust.gui.screens.MinesweeperScreen;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: MinesweeperScreen.java && WMinesweeper.java
 **/
public class Minesweeper extends Module {
    public Minesweeper() {
        super(Stardust.CATEGORY, "Minesweeper", "Play Minesweeper from the comfort of your Meteor Client.");
        runInMainMenu = true;
        if (wonColor.get().rainbow) RainbowColors.add(wonColor.get());
        if (lostColor.get().rainbow) RainbowColors.add(lostColor.get());
        if (timerColor.get().rainbow) RainbowColors.add(timerColor.get());
        if (mineTextColor.get().rainbow) RainbowColors.add(mineTextColor.get());
        if (mineCellColor.get().rainbow) RainbowColors.add(mineCellColor.get());
        if (mineCountColor.get().rainbow) RainbowColors.add(mineCountColor.get());
        if (resetTextColor.get().rainbow) RainbowColors.add(resetTextColor.get());
        if (statusBarColor.get().rainbow) RainbowColors.add(statusBarColor.get());
        if (textShadowColor.get().rainbow) RainbowColors.add(textShadowColor.get());
        if (cellBorderColor.get().rainbow) RainbowColors.add(cellBorderColor.get());
        if (backgroundColor.get().rainbow) RainbowColors.add(backgroundColor.get());
        if (hiddenCellColor.get().rainbow) RainbowColors.add(hiddenCellColor.get());
        if (resetButtonColor.get().rainbow) RainbowColors.add(resetButtonColor.get());
        if (flaggedCellColor.get().rainbow) RainbowColors.add(flaggedCellColor.get());
        if (resetHoveredColor.get().rainbow) RainbowColors.add(resetHoveredColor.get());
        if (revealedCellColor.get().rainbow) RainbowColors.add(revealedCellColor.get());
        if (flaggedCellTextColor.get().rainbow) RainbowColors.add(flaggedCellTextColor.get());
    }

    private static final String GAME_FOLDER = "meteor-client/minigames/minesweeper";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final SettingGroup sgGeneral = settings.createGroup("General");
    public final SettingGroup sgSchemes = settings.createGroup("Color Scheme");

    public final Setting<WMinesweeper.Difficulty> difficulty = sgGeneral.add(
        new EnumSetting.Builder<WMinesweeper.Difficulty>()
            .name("difficulty")
            .description("What difficulty to use for the Minesweeper game.")
            .defaultValue(WMinesweeper.Difficulty.Easy)
            .build()
    );
    public final Setting<Integer> rows = sgGeneral.add(
        new IntSetting.Builder()
            .name("rows")
            .description("How many rows to have on the board.")
            .min(4).noSlider()
            .defaultValue(37)
            .visible(() -> difficulty.get().equals(WMinesweeper.Difficulty.Custom))
            .build()
    );
    public final Setting<Integer> columns = sgGeneral.add(
        new IntSetting.Builder()
            .name("columns")
            .description("How many columns to have on the board.")
            .min(4).noSlider()
            .defaultValue(37)
            .visible(() -> difficulty.get().equals(WMinesweeper.Difficulty.Custom))
            .build()
    );
    public final Setting<Integer> mines = sgGeneral.add(
        new IntSetting.Builder()
            .name("mines")
            .description("How many mines to fill the board with.")
            .min(1).noSlider()
            .defaultValue(169)
            .visible(() -> difficulty.get().equals(WMinesweeper.Difficulty.Custom))
            .build()
    );
    public final Setting<Integer> cellSize = sgGeneral.add(
        new IntSetting.Builder()
            .name("cell-size")
            .description("Size in pixels to use for each of the grid cells.")
            .min(4).sliderRange(10, 50)
            .defaultValue(32)
            .build()
    );
    public final Setting<Boolean> shouldSave = sgGeneral.add(
        new BoolSetting.Builder()
            .name("save-games")
            .description("Saves your game state when closing the Minesweeper screen. Does not persist across game restarts.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> renderMap = sgGeneral.add(
        new BoolSetting.Builder()
            .name("force-render-minimap")
            .description("Continues rendering the Xaeros minimap while the Minesweeper screen is open.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> gameSounds = sgGeneral.add(
        new BoolSetting.Builder()
            .name("game-sounds")
            .description("Plays a sound when you win or lose the game.")
            .defaultValue(true)
            .build()
    );
    public final Setting<Double> soundVolume = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("sounds-volume")
            .min(0.1).max(4.0)
            .defaultValue(1.0)
            .visible(gameSounds::get)
            .build()
    );

    public final Setting<WMinesweeper.ColorSchemes> colorScheme = sgSchemes.add(
        new EnumSetting.Builder<WMinesweeper.ColorSchemes>()
            .name("color-scheme")
            .defaultValue(WMinesweeper.ColorSchemes.Themed)
            .build()
    );
    public final Setting<SettingColor> wonColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("won-text-color")
            .defaultValue(new SettingColor(0, 255, 0))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> lostColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("lost-text-color")
            .defaultValue(new SettingColor(255, 0, 0))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> cellBorderColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("cell-border-color")
            .defaultValue(new SettingColor(0, 0, 0))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> mineTextColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("mine-text-color")
            .defaultValue(new SettingColor(255, 0, 0))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> mineCellColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("mine-cell-color")
            .defaultValue(new SettingColor(13, 13, 13))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> timerColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("timer-text-color")
            .defaultValue(new SettingColor(255, 255, 0))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> resetTextColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("reset-text-color")
            .defaultValue(new SettingColor(20, 20, 20))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> statusBarColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("status-bar-color")
            .defaultValue(new SettingColor(42, 42, 42))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> backgroundColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("background-color")
            .defaultValue(new SettingColor(51, 51, 51))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> mineCountColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("mines-count-color")
            .defaultValue(new SettingColor(255, 255, 255))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> hiddenCellColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("hidden-cell-color")
            .defaultValue(new SettingColor(176, 176, 176))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> flaggedCellColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("flagged-cell-color")
            .defaultValue(new SettingColor(255, 204, 102))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> flaggedCellTextColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("flagged-text-color")
            .defaultValue(new SettingColor(169, 0, 0))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> resetButtonColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("reset-button-color")
            .defaultValue(new SettingColor(200, 200, 200))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> resetHoveredColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("reset-button-hovered-color")
            .defaultValue(new SettingColor(230, 230, 230))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> revealedCellColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("revealed-cell-color")
            .defaultValue(new SettingColor(248, 248, 248))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );
    public final Setting<SettingColor> textShadowColor = sgSchemes.add(
        new ColorSetting.Builder()
            .name("text-shadow-color")
            .defaultValue(new SettingColor(0, 0, 0))
            .visible(() -> colorScheme.get().equals(WMinesweeper.ColorSchemes.Custom))
            .build()
    );

    public @Nullable WMinesweeper.SaveState saveData = null;

    public void saveGame(WMinesweeper.SaveState data) {
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

    public void clearSave() {
        saveGame(null);
    }

    @Override
    public void onActivate() {
        Path saveFolder = FabricLoader.getInstance().getGameDir().resolve(GAME_FOLDER);

        //noinspection ResultOfMethodCallIgnored
        saveFolder.toFile().mkdirs();
        Path save = saveFolder.resolve("save.json");
        if (!Files.exists(save)) {
            if (!StardustUtil.checkOrCreateFile(mc, GAME_FOLDER + "/save.json")) {
                MsgUtil.sendModuleMsg("Failed to create save file§c..!", this.name);
            }
        }

        WMinesweeper.SaveState data = null;
        try (Reader reader = Files.newBufferedReader(save)) {
            data = GSON.fromJson(reader, WMinesweeper.SaveState.class);
        } catch (Exception err) {
            LogUtil.error(err.toString(), this.name);
        }

        if (data != null) saveData = data;

        try {
            mc.setScreen(new MinesweeperScreen(this, GuiThemes.get(), "Minesweeper"));
        } catch (Exception err) {
            LogUtil.error("Failed to open Minesweeper screen: " + err, this.name);
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.currentScreen instanceof MinesweeperScreen) {
            try {
                mc.setScreen(null);
            } catch (Exception err) {
                LogUtil.error("Failed to close Minesweeper screen: " + err, this.name);
                toggle();
            }
        }
    }
}
