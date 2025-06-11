package dev.stardust.mixin;

import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(SplashTextRenderer.class)
public class SplashTextRendererMixin {
    @Unique private int trackAlpha = 0;

    @Inject(method = "render", at = @At("HEAD"))
    private void mixinRender(DrawContext context, int width, TextRenderer textRenderer, int alpha, CallbackInfo ci) {
        this.trackAlpha = alpha;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"), index = 4)
    private int modifyRenderArg(int color) {
        return StardustConfig.greenSplashTextSetting.get() ? 0x54FB54 | this.trackAlpha : color;
    }
}
