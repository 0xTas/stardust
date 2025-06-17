package dev.stardust.gui.themes;

import dev.stardust.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

public class StardustTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final StardustTheme INSTANCE = new StardustTheme();

    @Override
    public String getName() {
        return "Stardust";
    }

    @Override
    public boolean getCategoryIcons() {
        return true;
    }

    // Colors
    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(39, 171, 214);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(210, 149, 247);
    }

    @Override
    public SettingColor getPlusColor() {
        return new SettingColor(141, 237, 179);
    }

    @Override
    public SettingColor getMinusColor() {
        return new SettingColor(240, 128, 128);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(246, 237, 153);
    }

    // Text
    @Override
    public SettingColor getTextColor() {
        return new SettingColor(147, 129, 255);
    }

    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(89, 174, 245);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(45, 125, 245, 150);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(141, 237, 179);
    }

    // Background
    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(new SettingColor(4, 3, 14, 200), new SettingColor(43, 32, 86, 200), new SettingColor(77, 60, 160, 200));
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(0, 127, 255, 30);
    }

    // Separator
    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(89, 174, 245);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(45, 125, 245, 150);
    }

    // Scrollbar
    @Override
    public RecolorGuiTheme.TriColorSetting getScrollbarColor() {
        return new TriColorSetting(new SettingColor(22, 89, 190, 100), new SettingColor(45, 125, 245, 137), new SettingColor(89, 174, 245, 200));
    }

    // Slider
    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(new SettingColor(113, 42, 182), new SettingColor(169, 107, 247), new SettingColor(210, 149, 247));
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(193, 142, 253, 200);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(49, 37, 55, 200);
    }
}
