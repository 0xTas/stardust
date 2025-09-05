package dev.stardust.gui.screens;

import dev.stardust.modules.Meteorites;
import org.jetbrains.annotations.Nullable;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.utils.Cell;
import dev.stardust.gui.widgets.meteorites.WMeteorites;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class MeteoritesScreen extends WindowScreen {
    private final Meteorites module;
    private @Nullable Cell<? extends WWidget> widget = null;

    public MeteoritesScreen(Meteorites module, GuiTheme theme, String title) {
        super(theme, title);
        this.module = module;
    }

    public @Nullable Cell<? extends WWidget> getWidget() {
        return widget;
    }

    @Override
    public void initWidgets() {
        widget = add(new WMeteorites(module));
    }

    @Override
    public void onClosed() {
        if (module.isActive()) module.toggle();
        if (widget != null && widget.widget() instanceof WMeteorites meteorites) {
            if (meteorites.shouldRestoreColorSettings()) {
                module.shipColor.set(meteorites.prevShipColor);
                module.flameColor.set(meteorites.prevFlameColor);
                module.bulletColor.set(meteorites.prevBulletColor);
            }
            if (meteorites.shouldSaveGame()) {
                module.saveGame(meteorites.saveGame());
            }
        }
    }
}
