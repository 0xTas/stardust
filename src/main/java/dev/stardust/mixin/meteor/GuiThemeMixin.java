package dev.stardust.mixin.meteor;

import dev.stardust.gui.RecolorGuiTheme;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.gui.GuiTheme;
import org.spongepowered.asm.mixin.injection.Redirect;
import meteordevelopment.meteorclient.utils.misc.ISerializable;

/**
 * Credit to crosby for this mixin from <a href="https://github.com/RacoonDog/Tokyo-Client">Tokyo-Client</a>
 * Allows custom themes implementing RecolorGuiTheme to display their custom names.
 **/
@Mixin(value = GuiTheme.class, remap = false)
public abstract class GuiThemeMixin implements ISerializable<GuiTheme> {
    @Shadow @Final @Mutable public String name;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/gui/GuiTheme;name:Ljava/lang/String;"))
    private void rename(GuiTheme instance, String value) {
        if (instance instanceof RecolorGuiTheme theme) name = theme.getName();
        else name = value;
    }
}
