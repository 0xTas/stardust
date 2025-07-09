package dev.stardust.gui.themes;

import dev.stardust.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

public class DarkTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final DarkTheme INSTANCE = new DarkTheme();

    @Override
    public String getName() {
        return "Dark";
    }

    @Override
    public boolean getCategoryIcons() {
        return true;
    }

    // Colors
    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(37, 37, 37);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(37, 37, 37);
    }

    // Text
    @Override
    public SettingColor getTextColor() {
        return new SettingColor(176, 176, 176);
    }

    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(123, 123, 123);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(130, 130, 130, 100);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(45, 225, 45);
    }

    @Override
    public SettingColor getFavoriteColor() { return new SettingColor(0, 0, 0); }

    // Background
    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(
            new SettingColor(0, 0, 0, 200),
            new SettingColor(13, 13, 13, 200),
            new SettingColor(37, 37, 37, 200)
        );
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(42, 42, 42);
    }

    // Separator
    @Override
    public SettingColor getSeparatorText() {
        return new SettingColor(165, 165, 165);
    }

    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(197, 197, 197);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(25, 25, 25, 150);
    }

    // Slider
    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(
            new SettingColor(37, 37, 37),
            new SettingColor(69, 69, 69),
            new SettingColor(101, 101, 101)
        );
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(50, 47, 54);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(21, 20, 23);
    }

    // Starscript
    @Override
    public SettingColor getStarscriptText() {
        return new SettingColor(169, 169, 169);
    }

    @Override
    public SettingColor getStarscriptBraces() {
        return new SettingColor(37, 37, 37, 200);
    }

    @Override
    public SettingColor getStarscriptParenthesis() {
        return new SettingColor(169, 169, 169);
    }

    @Override
    public SettingColor getStarscriptDots() {
        return new SettingColor(169, 169, 169);
    }

    @Override
    public SettingColor getStarscriptCommas() {
        return new SettingColor(169, 169, 169);
    }

    @Override
    public SettingColor getStarscriptOperators() {
        return new SettingColor(169, 169, 169);
    }

    @Override
    public SettingColor getStarscriptStrings() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getStarscriptNumbers() {
        return new SettingColor(208, 194, 231);
    }

    @Override
    public SettingColor getStarscriptKeywords() {
        return new SettingColor(49, 40, 59);
    }
}
