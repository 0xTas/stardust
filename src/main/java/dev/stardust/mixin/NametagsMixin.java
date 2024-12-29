package dev.stardust.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.jetbrains.annotations.Nullable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 *     Allows use of default font for Nametags even when custom font is chosen in the global config.
 **/
@Mixin(value = Nametags.class, remap = false)
public abstract class NametagsMixin extends Module {
    @Shadow
    @Final
    private SettingGroup sgGeneral;

    public NametagsMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Unique
    private @Nullable Setting<Boolean> forceDefaultFont = null;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Nametags;scale:Lmeteordevelopment/meteorclient/settings/Setting;", shift = At.Shift.AFTER))
    private void addDefaultFontSettings(CallbackInfo ci) {
        forceDefaultFont = sgGeneral.add(
            new BoolSetting.Builder()
                .name("force-default-font")
                .description("Force nametags to render using the default font, even if a custom GUI font is selected in your Meteor config.")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "renderNametagPlayer", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Nametags;drawBg(DDDD)V"))
    private void injectDefaultFontForPlayerNametags(CallbackInfo ci, @Local LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }

    @Inject(method = "renderNametagItem", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Nametags;drawBg(DDDD)V"))
    private void injectDefaultFontForItemNametags(CallbackInfo ci, @Local LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }

    @Inject(method = "renderGenericNametag", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Nametags;drawBg(DDDD)V"))
    private void injectDefaultFontForGenericNametags(CallbackInfo ci, @Local LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }

    @Inject(method = "renderTntNametag", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Nametags;drawBg(DDDD)V"))
    private void injectDefaultFontForTNTNametags(CallbackInfo ci, @Local LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }
}
