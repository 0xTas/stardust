package dev.stardust.gui.themes;

import dev.stardust.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

public class MonochromeTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final MonochromeTheme INSTANCE = new MonochromeTheme();

    @Override
    public String getName() {
        return "Monochrome";
    }

    // Colors
    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(239, 243, 242);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getPlusColor() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getMinusColor() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(0, 0, 0);
    }

    // Text
    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(112, 112, 112);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(255, 255, 255, 100);
    }

    @Override
    public SettingColor getTitleTextColor() {
        return new SettingColor(0, 0, 0);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(255, 255, 255);
    }

    // Background
    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(new SettingColor(7, 7, 7, 169), new SettingColor(30, 30, 30, 200), new SettingColor(40, 40, 40, 200));
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(199, 199, 199, 169);
    }

    // Separator
    @Override
    public SettingColor getSeparatorText() {
        return new SettingColor(253, 253, 253, 236);
    }

    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(212, 212, 212);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(69, 69, 69, 150);
    }

    // Scrollbar
    @Override
    public RecolorGuiTheme.TriColorSetting getScrollbarColor() {
        return new TriColorSetting(new SettingColor(255, 255, 255, 200), new SettingColor(255, 255, 255), new SettingColor(255, 255, 255));
    }

    // Slider
    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(new SettingColor(37, 37, 37), new SettingColor(69, 69, 69), new SettingColor(255, 255, 255));
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(37, 37, 37);
    }

    // Starscript
    @Override
    public SettingColor getStarscriptText() {
        return new SettingColor(212, 212, 212);
    }

    @Override
    public SettingColor getStarscriptParenthesis() {
        return new SettingColor(69, 69, 69);
    }

    @Override
    public SettingColor getStarscriptDots() {
        return new SettingColor(137, 137, 137);
    }

    @Override
    public SettingColor getStarscriptCommas() {
        return new SettingColor(137, 137, 137);
    }

    @Override
    public SettingColor getStarscriptOperators() {
        return new SettingColor(137, 137, 137);
    }

    @Override
    public SettingColor getStarscriptStrings() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getStarscriptNumbers() {
        return new SettingColor(42, 42, 42);
    }

    @Override
    public SettingColor getStarscriptKeywords() {
        return new SettingColor(200, 200, 200);
    }

    @Override
    public SettingColor getStarscriptAccessedObjects() {
        return new SettingColor(69, 69, 69, 200);
    }
}
