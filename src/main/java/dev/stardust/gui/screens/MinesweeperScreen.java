package dev.stardust.gui.screens;

import dev.stardust.modules.Minesweeper;
import org.jetbrains.annotations.Nullable;
import dev.stardust.gui.widgets.WMinesweeper;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class MinesweeperScreen extends WindowScreen {
    private final GuiTheme theme;
    private final Minesweeper module;
    private @Nullable Minesweeper.SaveState save = null;
    private @Nullable Cell<? extends WWidget> widget = null;

    public MinesweeperScreen(Minesweeper module, GuiTheme theme, String title) {
        super(theme, title);
        this.theme = theme;
        this.module = module;
    }

    public MinesweeperScreen(Minesweeper module, GuiTheme theme, String title, @Nullable Minesweeper.SaveState save) {
        super(theme, title);
        this.save = save;
        this.theme = theme;
        this.module = module;
    }

    @Override
    public void initWidgets() {
        if (save == null) {
            widget = add(new WMinesweeper(module, theme));
        } else {
            widget = add(new WMinesweeper(module, theme, save));
        }
    }

    @Override
    public void onClosed() {
        if (module.isActive()) module.toggle();
        if (widget != null && widget.widget() instanceof WMinesweeper minesweeper) {
            if (module.shouldSave.get() && minesweeper.shouldSaveGame()) {
                module.saveGame(minesweeper.saveGame());
            }
        }
    }
}
