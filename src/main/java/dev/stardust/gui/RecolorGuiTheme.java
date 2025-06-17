package dev.stardust.gui;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * Allows easily making themes that are recolored versions of the default Meteor Client theme.<br>
 * Credit to crosby for this code, originally from:
 * <a href="https://raw.githubusercontent.com/RacoonDog/Tokyo-Client/refs/heads/main/src/main/java/io/github/racoondog/tokyo/utils/RecolorGuiTheme.java">Tokyo-Client</a>
 *
 * <pre>{@code
 * public class ExampleTheme extends MeteorGuiTheme implements RecolorGuiTheme {
 *     @Override
 *     public String getName() {
 *         return "ExampleTheme";
 *     }
 *
 *     @Override
 *     public SettingColor getAccentColor() {
 *         return new SettingColor(255, 255, 0);
 *     }
 * }
 * }</pre>
 */
public interface RecolorGuiTheme {
    // Required
    String getName();

    // Category Icons
    default boolean getCategoryIcons() {
        return false;
    }

    // Colors
    default SettingColor getAccentColor() {
        return new SettingColor(145, 61, 226);
    }

    default SettingColor getCheckboxColor() {
        return new SettingColor(145, 61, 226);
    }

    default SettingColor getPlusColor() {
        return new SettingColor(50, 255, 50);
    }

    default SettingColor getMinusColor() {
        return new SettingColor(255, 50, 50);
    }

    default SettingColor getFavoriteColor() {
        return new SettingColor(250, 215, 0);
    }

    // Text
    default SettingColor getTextColor() {
        return new SettingColor(255, 255, 255);
    }

    default SettingColor getTextSecondaryColor() {
        return new SettingColor(150, 150, 150);
    }

    default SettingColor getTextHighlightColor() {
        return new SettingColor(45, 125, 245, 100);
    }

    default SettingColor getTitleTextColor() {
        return new SettingColor(255, 255, 255);
    }

    default SettingColor getLoggedInColor() {
        return new SettingColor(45, 255, 45);
    }

    default SettingColor getPlaceholderColor() {
        return new SettingColor(255, 255, 255, 20);
    }

    // Background
    default TriColorSetting getBackgroundColor() {
        return new TriColorSetting(
            new SettingColor(20, 20, 20, 200),
            new SettingColor(30, 30, 30, 200),
            new SettingColor(40, 40, 40, 200)
        );
    }

    default SettingColor getModuleBackground() {
        return new SettingColor(50, 50, 50);
    }

    // Outline
    default TriColorSetting getOutlineColor() {
        return new TriColorSetting(
            new SettingColor(0, 0, 0),
            new SettingColor(10, 10, 10),
            new SettingColor(20, 20, 20)
        );
    }

    // Separator
    default SettingColor getSeparatorText() {
        return new SettingColor(255, 255, 255);
    }

    default SettingColor getSeparatorCenter() {
        return new SettingColor(255, 255, 255);
    }

    default SettingColor getSeparatorEdges() {
        return new SettingColor(225, 225, 225, 150);
    }

    // Scrollbar
    default TriColorSetting getScrollbarColor() {
        return new TriColorSetting(
            new SettingColor(30, 30, 30, 200),
            new SettingColor(40, 40, 40, 200),
            new SettingColor(50, 50, 50, 200)
        );
    }

    // Slider
    default TriColorSetting getSliderHandle() {
        return new TriColorSetting(
            new SettingColor(130, 0, 255),
            new SettingColor(140, 30, 255),
            new SettingColor(150, 60, 255)
        );
    }

    default SettingColor getSliderLeft() {
        return new SettingColor(100, 35, 170);
    }

    default SettingColor getSliderRight() {
        return new SettingColor(50, 50, 50);
    }

    // Starscript
    default SettingColor getStarscriptText() {
        return new SettingColor(169, 183, 198);
    }

    default SettingColor getStarscriptBraces() {
        return new SettingColor(150, 150, 150);
    }

    default SettingColor getStarscriptParenthesis() {
        return new SettingColor(169, 183, 198);
    }

    default SettingColor getStarscriptDots() {
        return new SettingColor(169, 183, 198);
    }

    default SettingColor getStarscriptCommas() {
        return new SettingColor(169, 183, 198);
    }

    default SettingColor getStarscriptOperators() {
        return new SettingColor(169, 183, 198);
    }

    default SettingColor getStarscriptStrings() {
        return new SettingColor(106, 135, 89);
    }

    default SettingColor getStarscriptNumbers() {
        return new SettingColor(104, 141, 187);
    }

    default SettingColor getStarscriptKeywords() {
        return new SettingColor(204, 120, 50);
    }

    default SettingColor getStarscriptAccessedObjects() {
        return new SettingColor(152, 118, 170);
    }

    record TriColorSetting(SettingColor c1, SettingColor c2, SettingColor c3) {} //Modify ThreeStateColorSetting's colors without modifying group and name
}
