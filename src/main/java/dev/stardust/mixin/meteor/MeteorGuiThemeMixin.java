package dev.stardust.mixin.meteor;

import dev.stardust.Stardust;
import dev.stardust.gui.RecolorGuiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import meteordevelopment.meteorclient.settings.BoolSetting;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

/**
 * Credit to crosby for this mixin from <a href="https://github.com/RacoonDog/Tokyo-Client">Tokyo-Client</a>
 * Replaces default values with the overrides from the RecolorGuiTheme interface.
 **/
@Mixin(value = MeteorGuiTheme.class, remap = false)
public abstract class MeteorGuiThemeMixin {
    // Category Icons
    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=category-icons")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/settings/SettingGroup;add(Lmeteordevelopment/meteorclient/settings/Setting;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0))
    private Setting<Boolean> categoryIcons(Setting<Boolean> original) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return new BoolSetting.Builder()
                .name("category-icons")
                .description("Adds item icons to module categories.")
                .defaultValue(RecolorGuiTheme.getCategoryIcons())
                .build();
        }
        return original;
    }

    // Colors
    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=accent")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 2)
    private SettingColor accentColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getAccentColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=checkbox")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 2)
    private SettingColor checkboxColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getCheckboxColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=plus")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 2)
    private SettingColor plusColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getPlusColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=minus")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 2)
    private SettingColor minusColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getMinusColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=favorite")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 2)
    private SettingColor favoriteColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getFavoriteColor();
        }
        return color;
    }

    // Text
    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=text")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor textColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getTextColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=text-secondary-text")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor textSecondaryColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getTextSecondaryColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=text-highlight")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor textHighlightColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getTextHighlightColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=title-text")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor titleTextColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getTitleTextColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=logged-in-text")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor loggedInColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getLoggedInColor();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=placeholder")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor placeholderColor(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getPlaceholderColor();
        }
        return color;
    }

    // Background
    @ModifyArgs(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=background")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme$ThreeStateColorSetting;<init>(Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)V", ordinal = 0))
    private void backgroundColor(Args args) {
        if (args.get(0) instanceof RecolorGuiTheme RecolorGuiTheme) {
            RecolorGuiTheme.TriColorSetting triColorSetting = RecolorGuiTheme.getBackgroundColor();
            Stardust.LOG.info("[Stardust] Setting theme background color!");
            args.set(3, triColorSetting.c1());
            args.set(4, triColorSetting.c2());
            args.set(5, triColorSetting.c3());
        }
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=module-background")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor moduleBackground(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getModuleBackground();
        }
        return color;
    }

    // Outline
    @ModifyArgs(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=outline")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme$ThreeStateColorSetting;<init>(Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)V", ordinal = 1))
    private void outlineColor(Args args) {
        if (args.get(0) instanceof RecolorGuiTheme RecolorGuiTheme) {
            RecolorGuiTheme.TriColorSetting triColorSetting = RecolorGuiTheme.getOutlineColor();
            args.set(3, triColorSetting.c1());
            args.set(4, triColorSetting.c2());
            args.set(5, triColorSetting.c3());
        }
    }

    // Separator
    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=separator-text")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor separatorText(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getSeparatorText();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=separator-center")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor separatorCenter(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getSeparatorCenter();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=separator-edges")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor separatorEdges(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getSeparatorEdges();
        }
        return color;
    }

    // Scrollbar
    @ModifyArgs(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=Scrollbar")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme$ThreeStateColorSetting;<init>(Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)V", ordinal = 2))
    private void scrollbarColor(Args args) {
        if (args.get(0) instanceof RecolorGuiTheme RecolorGuiTheme) {
            RecolorGuiTheme.TriColorSetting triColorSetting = RecolorGuiTheme.getScrollbarColor();
            args.set(3, triColorSetting.c1());
            args.set(4, triColorSetting.c2());
            args.set(5, triColorSetting.c3());
        }
    }

    // Slider
    @ModifyArgs(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=slider-handle")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme$ThreeStateColorSetting;<init>(Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)V", ordinal = 0))
    private void sliderHandle(Args args) {
        if (args.get(0) instanceof RecolorGuiTheme RecolorGuiTheme) {
            RecolorGuiTheme.TriColorSetting triColorSetting = RecolorGuiTheme.getSliderHandle();
            args.set(3, triColorSetting.c1());
            args.set(4, triColorSetting.c2());
            args.set(5, triColorSetting.c3());
        }
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=slider-left")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor sliderLeft(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getSliderLeft();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=slider-right")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor sliderRight(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getSliderRight();
        }
        return color;
    }

    // Starscript
    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-text")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptText(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptText();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-braces")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptBraces(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptBraces();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-parenthesis")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptParenthesis(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptParenthesis();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-dots")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptDots(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptDots();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-commas")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptCommas(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptCommas();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-operators")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptOperators(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptOperators();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-strings")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptStrings(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptStrings();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-numbers")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptNumbers(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptNumbers();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-keywords")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptKeywords(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptKeywords();
        }
        return color;
    }

    @ModifyArg(method = "<init>", slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=starscript-accessed-objects")), at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme;color(Lmeteordevelopment/meteorclient/settings/SettingGroup;Ljava/lang/String;Ljava/lang/String;Lmeteordevelopment/meteorclient/utils/render/color/SettingColor;)Lmeteordevelopment/meteorclient/settings/Setting;", ordinal = 0), index = 3)
    private SettingColor starscriptAccessedObjects(SettingColor color) {
        if (this instanceof RecolorGuiTheme RecolorGuiTheme) {
            return RecolorGuiTheme.getStarscriptAccessedObjects();
        }
        return color;
    }
}
