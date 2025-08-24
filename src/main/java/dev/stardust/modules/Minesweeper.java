package dev.stardust.modules;

import dev.stardust.Stardust;
import org.jetbrains.annotations.Nullable;
import dev.stardust.gui.widgets.WMinesweeper;
import meteordevelopment.meteorclient.settings.*;
import dev.stardust.gui.screens.MinesweeperScreen;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Minesweeper extends Module {
    public Minesweeper() {
        super(Stardust.CATEGORY, "Minesweeper", "Minesweeper inside of Minecraft.");
        runInMainMenu = true;
        // todo: maybe allow rainbow colors in custom ColorScheme
    }

    public final SettingGroup sgGeneral = settings.createGroup("General");
    public final SettingGroup sgSchemes = settings.createGroup("Color Scheme");

    // See WMinesweeper.java
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
    public final Setting<Boolean> gameSounds = sgGeneral.add(
        new BoolSetting.Builder()
            .name("game-over-sounds")
            .description("Plays a sound when you win or lose the game.")
            .defaultValue(true)
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

    private @Nullable SaveState saveData = null;
    public void saveGame(SaveState data) {
        saveData = data;
    }
    public void clearSave() {
        saveData = null;
    }

    @Override
    public void onActivate() {
        if (saveData == null) {
            mc.setScreen(new MinesweeperScreen(this, GuiThemes.get(), "Minesweeper"));
        } else {
            mc.setScreen(new MinesweeperScreen(this, GuiThemes.get(), "Minesweeper", saveData));
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.currentScreen instanceof MinesweeperScreen) mc.setScreen(null);
    }

    public record SaveState(
        WMinesweeper.Difficulty difficulty, int rows, int columns,
        int mines, int[][] grid, byte[][] state, long accumulatedMillis
    ) {}
}
