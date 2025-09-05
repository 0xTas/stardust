package dev.stardust.mixin.meteor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.stardust.gui.screens.MeteoritesScreen;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.gui.utils.Cell;
import dev.stardust.gui.widgets.meteorites.WMeteorites;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Prevents accidental header clicks from collapsing or dragging the window while playing Meteorites.
 **/
@Mixin(targets = "meteordevelopment.meteorclient.gui.widgets.containers.WWindow$WHeader", remap = false)
public abstract class WHeaderMixin extends WContainer {
    @Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true)
    private void maybeCancelHeaderClick(double mouseX, double mouseY, int button, boolean used, CallbackInfoReturnable<Boolean> cir) {
        if (mc.currentScreen instanceof MeteoritesScreen meteorites) {
            Cell<? extends WWidget> widget = meteorites.getWidget();
            if (widget != null && widget.widget() instanceof WMeteorites mw) {
                if (!mw.isPaused && !mw.gameOver && mw.module.mouseAim.get()) {
                    cir.cancel();
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
