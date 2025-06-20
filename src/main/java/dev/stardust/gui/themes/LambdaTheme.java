package dev.stardust.gui.themes;

import dev.stardust.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

public class LambdaTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final LambdaTheme INSTANCE = new LambdaTheme();

    @Override
    public String getName() {
        return "Lambda";
    }

    // Colors
    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(108, 0, 43);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(158, 37, 85);
    }

    @Override
    public SettingColor getPlusColor() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getMinusColor() {
        return new SettingColor(222, 43, 79);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(158, 37, 85);
    }

    // Text
    @Override
    public SettingColor getTextColor() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(158, 37, 85);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(108, 0, 43, 137);
    }

    @Override
    public SettingColor getTitleTextColor() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(158, 37, 85);
    }

    // Background
    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(
            new SettingColor(31, 10, 18, 235),
            new SettingColor(58, 15, 32, 235),
            new SettingColor(108, 0, 43, 137)
        );
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(108, 0, 43, 137);
    }

    // Outline
    @Override
    public TriColorSetting getOutlineColor() {
        return new TriColorSetting(
            new SettingColor(78, 0, 31, 200),
            new SettingColor(78, 0, 31, 255),
            new SettingColor(120, 5, 51)
        );
    }

    // Separator
    @Override
    public SettingColor getSeparatorText() {
        return new SettingColor(158, 37, 85);
    }

    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(108, 0, 43);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(108, 0, 43, 137);
    }

    // Scrollbar
    @Override
    public RecolorGuiTheme.TriColorSetting getScrollbarColor() {
        return new TriColorSetting(
            new SettingColor(50, 11, 11, 200),
            new SettingColor(69, 13, 13, 200),
            new SettingColor(95, 12, 12, 200)
        );
    }

    // Slider
    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(
            new SettingColor(50, 11, 11),
            new SettingColor(69, 13, 13),
            new SettingColor(95, 12, 12)
        );
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(120, 5, 51);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(58, 15, 32, 235);
    }

    @Override
    public SettingColor getStarscriptText() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getStarscriptBraces() {
        return new SettingColor(108, 0, 43);
    }

    @Override
    public SettingColor getStarscriptParenthesis() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getStarscriptDots() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getStarscriptCommas() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getStarscriptOperators() {
        return new SettingColor(255, 205, 225);
    }

    @Override
    public SettingColor getStarscriptStrings() {
        return new SettingColor(158, 37, 85);
    }

    @Override
    public SettingColor getStarscriptNumbers() {
        return new SettingColor(130, 3, 54);
    }

    @Override
    public SettingColor getStarscriptKeywords() {
        return new SettingColor(130, 3, 54);
    }

    @Override
    public SettingColor getStarscriptAccessedObjects() {
        return new SettingColor(130, 3, 54);
    }
}
