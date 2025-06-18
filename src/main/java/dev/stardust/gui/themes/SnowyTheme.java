package dev.stardust.gui.themes;

import dev.stardust.gui.RecolorGuiTheme;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

public class SnowyTheme extends MeteorGuiTheme implements RecolorGuiTheme {
    public static final SnowyTheme INSTANCE = new SnowyTheme();

    @Override
    public String getName() {
        return "Snowy";
    }

    @Override
    public boolean getCategoryIcons() {
        return true;
    }

    // Colors
    @Override
    public SettingColor getAccentColor() {
        return new SettingColor(231, 242, 247);
    }

    @Override
    public SettingColor getCheckboxColor() {
        return new SettingColor(231, 242, 247);
    }

    @Override
    public SettingColor getPlusColor() {
        return new SettingColor(222, 246, 255);
    }

    @Override
    public SettingColor getMinusColor() {
        return new SettingColor(95, 120, 137);
    }

    @Override
    public SettingColor getFavoriteColor() {
        return new SettingColor(154, 225, 255);
    }

    // Text
    @Override
    public SettingColor getTextSecondaryColor() {
        return new SettingColor(195, 224, 246);
    }

    @Override
    public SettingColor getTextHighlightColor() {
        return new SettingColor(171, 205, 255, 88);
    }

    @Override
    public SettingColor getTitleTextColor() {
        return new SettingColor(112, 199, 235);
    }

    @Override
    public SettingColor getLoggedInColor() {
        return new SettingColor(159, 238, 255);
    }

    // Background
    @Override
    public TriColorSetting getBackgroundColor() {
        return new TriColorSetting(new SettingColor(166, 179, 184, 200), new SettingColor(203, 212, 218, 200), new SettingColor(255, 255, 255, 200));
    }

    @Override
    public SettingColor getModuleBackground() {
        return new SettingColor(209, 214, 216, 169);
    }

    // Outline
    @Override
    public TriColorSetting getOutlineColor() {
        return new TriColorSetting(
            new SettingColor(220, 229, 235),
            new SettingColor(232, 246, 253),
            new SettingColor(201, 238, 255)
        );
    }

    // Separator
    @Override
    public SettingColor getSeparatorCenter() {
        return new SettingColor(191, 232, 255);
    }

    @Override
    public SettingColor getSeparatorEdges() {
        return new SettingColor(175, 230, 247, 208);
    }

    // Scrollbar
    @Override
    public RecolorGuiTheme.TriColorSetting getScrollbarColor() {
        return new TriColorSetting(new SettingColor(171, 214, 233, 200), new SettingColor(186, 229, 249, 200), new SettingColor(213, 240, 255, 200));
    }

    // Slider
    @Override
    public TriColorSetting getSliderHandle() {
        return new TriColorSetting(new SettingColor(204, 223, 237), new SettingColor(219, 238, 254), new SettingColor(255, 255, 255));
    }

    @Override
    public SettingColor getSliderLeft() {
        return new SettingColor(146, 217, 243);
    }

    @Override
    public SettingColor getSliderRight() {
        return new SettingColor(84, 127, 143);
    }

    // Starscript
    @Override
    public SettingColor getStarscriptText() {
        return new SettingColor(255, 255, 255);
    }

    @Override
    public SettingColor getStarscriptBraces() {
        return new SettingColor(146, 217, 243);
    }

    @Override
    public SettingColor getStarscriptParenthesis() {
        return new SettingColor(84, 127, 143);
    }

    @Override
    public SettingColor getStarscriptStrings() {
        return new SettingColor(178, 223, 255);
    }

    @Override
    public SettingColor getStarscriptNumbers() {
        return new SettingColor(95, 120, 137);
    }

    @Override
    public SettingColor getStarscriptKeywords() {
        return new SettingColor(50, 158, 204);
    }

    @Override
    public SettingColor getStarscriptAccessedObjects() {
        return new SettingColor(185, 222, 239);
    }
}
