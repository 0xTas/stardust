package dev.stardust.mixin.meteor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.stardust.gui.screens.MeteoritesScreen;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.gui.utils.Cell;
import dev.stardust.gui.widgets.meteorites.WMeteorites;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Pauses Meteorites if the triangle header button is used to collapse the window while playing.
 **/
@Mixin(value = WWindow.class, remap = false)
public abstract class WWindowMixin extends WVerticalList {
    @Inject(method = "setExpanded", at = @At("HEAD"))
    private void injectSetExpanded(boolean expanded, CallbackInfo ci) {
        if (expanded) return;
        if (!(mc.currentScreen instanceof MeteoritesScreen meteorites)) return;

        Cell<? extends WWidget> widget = meteorites.getWidget();
        if (widget != null && widget.widget() instanceof WMeteorites mw) {
            if (!mw.isPaused && !mw.gameOver && mw.module.mouseAim.get()) {
                mw.pauseGame();
            }
        }
    }
}
