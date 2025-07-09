package dev.stardust.gui.themes;

import dev.stardust.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

public class MidnightTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final MidnightTheme INSTANCE = new MidnightTheme();

    @Override
    public String getName() {
        return "Midnight";
    }

    @Override
    public boolean getCategoryIcons() {
        return true;
    }

    // Colors
    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(19, 6, 42);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(90, 116, 255);
    }

    @Override
    public SettingColor getPlusColor() {
        return new SettingColor(0, 105, 104);
    }

    @Override
    public SettingColor getMinusColor() {
        return new SettingColor(0, 79, 141);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(0, 176, 250);
    }

    // Text
    @Override
    public SettingColor getTextColor() {
        return new SettingColor(0, 133, 220);
    }

    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(71, 44, 225);
    }

    @Override
    public SettingColor getTitleTextColor() {
        return new SettingColor(90, 116, 255);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(71, 44, 225);
    }

    // Background
    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(
            new SettingColor(10, 0, 28, 213),
            new SettingColor(7, 0, 20, 213),
            new SettingColor(4, 0, 11, 213)
        );
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(22, 0, 64);
    }

    // Outline
    @Override
    public TriColorSetting getOutlineColor() {
        return new TriColorSetting(
            new SettingColor(22, 0, 64),
            new SettingColor(22, 0, 64),
            new SettingColor(22, 0, 64)
        );
    }

    // Separator
    @Override
    public SettingColor getSeparatorText() {
        return new SettingColor(90, 116, 255);
    }

    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(90, 116, 255);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(10, 0, 28, 217);
    }

    // Scrollbar
    @Override
    public TriColorSetting getScrollbarColor() {
        return new TriColorSetting(
            new SettingColor(13, 63, 132),
            new SettingColor(1, 43, 100),
            new SettingColor(0, 32, 77)
        );
    }

    // Slider
    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(
            new SettingColor(13, 63, 132),
            new SettingColor(1, 43, 100),
            new SettingColor(0, 32, 77)
        );
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(0, 63, 152);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(0, 17, 41);
    }

    // Starscript
    @Override
    public SettingColor getStarscriptText() {
        return new SettingColor(0, 133, 220);
    }

    @Override
    public SettingColor getStarscriptBraces() {
        return new SettingColor(71, 44, 225);
    }

    @Override
    public SettingColor getStarscriptParenthesis() {
        return new SettingColor(0, 133, 220);
    }

    @Override
    public SettingColor getStarscriptDots() {
        return new SettingColor(0, 133, 220);
    }

    @Override
    public SettingColor getStarscriptCommas() {
        return new SettingColor(0, 133, 220);
    }

    @Override
    public SettingColor getStarscriptOperators() {
        return new SettingColor(0, 133, 220);
    }

    @Override
    public SettingColor getStarscriptStrings() {
        return new SettingColor(0, 105, 104);
    }

    @Override
    public SettingColor getStarscriptNumbers() {
        return new SettingColor(0, 133, 220);
    }

    @Override
    public SettingColor getStarscriptKeywords() {
        return new SettingColor(0, 176, 250);
    }

    @Override
    public SettingColor getStarscriptAccessedObjects() {
        return new SettingColor(71, 44, 225);
    }
}
