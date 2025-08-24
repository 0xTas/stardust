package dev.stardust.gui.widgets;

import java.util.Deque;
import java.util.Random;
import java.util.ArrayDeque;
import net.minecraft.sound.SoundEvents;
import dev.stardust.modules.Minesweeper;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class WMinesweeper extends WWidget {
    public enum Difficulty {
        Easy(new int[]{12, 12, 10}),
        Normal(new int[]{16, 16, 40}),
        Hard(new int[]{30, 16, 99}),
        Insane(new int[]{30, 24, 137}),
        Custom(new int[]{37, 37, 169});

        public final int[] values;

        Difficulty(int[] values) {
            this.values = values;
        }

        public int getMines() {
            return values[2];
        }
        public int getRows() {
            return values[1];
        }
        public int getColumns() {
            return values[0];
        }
    }

    public enum ColorSchemes {
        Classic, Themed, Custom
    }

    public static class ColorScheme {
        public Color wonTextColor;
        public Color lostTextColor;
        public Color cellBorderColor;
        public Color mineTextColor;
        public Color mineCellColor;
        public Color runtimeColor;
        public Color resetTextColor;
        public Color statusBarColor;
        public Color backgroundColor;
        public Color mineCountColor;
        public Color hiddenCellColor;
        public Color flaggedCellColor;
        public Color flaggedCellTextColor;
        public Color resetButtonColor;
        public Color resetHoveredColor;
        public Color revealedCellColor;
        public Color textShadowColor;

        public ColorScheme(Minesweeper module, GuiTheme theme) {
            ColorSchemes scheme = module.colorScheme.get();

            boolean assignDefaults = false;
            switch (scheme) {
                case Themed -> {
                    if (theme instanceof MeteorGuiTheme guiTheme) {
                        this.wonTextColor = guiTheme.plusColor.get();
                        this.lostTextColor = guiTheme.minusColor.get();
                        this.cellBorderColor = guiTheme.outlineColor.get();
                        this.mineTextColor = guiTheme.minusColor.get();
                        this.mineCellColor = guiTheme.backgroundColor.get();
                        this.runtimeColor = guiTheme.titleTextColor.get();
                        this.resetTextColor = guiTheme.textSecondaryColor.get();
                        this.statusBarColor = guiTheme.accentColor.get();
                        this.backgroundColor = guiTheme.backgroundColor.get();
                        this.mineCountColor = guiTheme.titleTextColor.get();
                        this.hiddenCellColor = guiTheme.moduleBackground.get();
                        this.flaggedCellColor = guiTheme.accentColor.get();
                        this.flaggedCellTextColor = guiTheme.textColor.get();
                        this.resetButtonColor = guiTheme.backgroundColor.get();
                        this.resetHoveredColor = guiTheme.backgroundColor.get(false, true, false);
                        this.revealedCellColor = guiTheme.backgroundColor.get();
                        this.textShadowColor = guiTheme.backgroundColor.get(false, true, false);
                    } else {
                        assignDefaults = true;
                    }
                }
                case Custom -> {
                    this.wonTextColor = module.wonColor.get();
                    this.lostTextColor = module.lostColor.get();
                    this.cellBorderColor = module.cellBorderColor.get();
                    this.mineTextColor = module.mineTextColor.get();
                    this.mineCellColor = module.mineCellColor.get();
                    this.runtimeColor = module.timerColor.get();
                    this.resetTextColor = module.resetTextColor.get();
                    this.statusBarColor = module.statusBarColor.get();
                    this.backgroundColor = module.backgroundColor.get();
                    this.mineCountColor = module.mineCountColor.get();
                    this.hiddenCellColor = module.hiddenCellColor.get();
                    this.flaggedCellColor = module.flaggedCellColor.get();
                    this.flaggedCellTextColor = module.flaggedCellTextColor.get();
                    this.resetButtonColor = module.resetButtonColor.get();
                    this.resetHoveredColor = module.resetHoveredColor.get();
                    this.revealedCellColor = module.revealedCellColor.get();
                    this.textShadowColor = module.textShadowColor.get();
                }
                default -> assignDefaults = true;
            }

            if (assignDefaults) {
                this.wonTextColor = new Color(0, 255, 0);
                this.lostTextColor = new Color(255, 0, 0);
                this.cellBorderColor = new Color(0, 0, 0);
                this.mineTextColor = new Color(255, 0, 0);
                this.textShadowColor = new Color(0, 0, 0);
                this.mineCellColor = new Color(13, 13, 13);
                this.runtimeColor = new Color(255, 255, 0);
                this.resetTextColor = new Color(20, 20, 20);
                this.statusBarColor = new Color(42, 42, 42);
                this.backgroundColor = new Color(51, 51, 51);
                this.mineCountColor = new Color(255, 255, 255);
                this.hiddenCellColor = new Color(176, 176, 176);
                this.flaggedCellColor = new Color(255, 204, 102);
                this.flaggedCellTextColor = new Color(169, 0, 0);
                this.resetButtonColor = new Color(200, 200, 200);
                this.resetHoveredColor = new Color(230, 230, 230);
                this.revealedCellColor = new Color(248, 248, 248);
            }
        }
    }

    private static final int TEXT_Y_OFFSET = 1;

    private int rows;
    private int cols;
    private int mines;
    private final int cellSize;
    private final Minesweeper module;
    private final Difficulty difficulty;
    private final ColorScheme colorScheme;
    private final Random random = new Random();

    private long gameEnd = 0L;
    private long gameStart = 0L;
    private long accumulated = 0L;
    private boolean resetHover = false;
    private final int statusHeight = 32;

    private int[][] grid;
    private byte[][] state;

    private boolean firstClick = true;
    private boolean gameOver = false;
    private boolean gameWon = false;

    // See MinesweeperScreen.java
    public boolean shouldSaveGame() {
        return !gameOver && !firstClick;
    }
    public Minesweeper.SaveState saveGame() {
        return new Minesweeper.SaveState(
            difficulty, rows, cols, mines, grid, state, System.currentTimeMillis() - gameStart
        );
    }

    public WMinesweeper(Minesweeper module, GuiTheme theme) {
        this.module = module;
        this.cellSize = module.cellSize.get();
        this.difficulty = module.difficulty.get();
        this.colorScheme = new ColorScheme(module, theme);

        initEmpty();
    }

    public WMinesweeper(Minesweeper module, GuiTheme theme, Minesweeper.SaveState save) {
        this.module = module;
        this.cellSize = module.cellSize.get();
        this.difficulty = module.difficulty.get();
        this.colorScheme = new ColorScheme(module, theme);

        // If core settings were changed, start fresh
        if (difficulty != save.difficulty()) {
            initEmpty();
        } else if (difficulty.equals(Difficulty.Custom)) {
            if (module.rows.get() != save.rows()
                || module.columns.get() != save.columns()
                || module.mines.get() != save.mines()) {
                initEmpty();
            } else loadSave(save);
        } else loadSave(save);
    }

    private void loadSave(Minesweeper.SaveState save) {
        this.rows = save.rows();
        this.cols = save.columns();
        this.mines = Math.min(save.mines(), rows * cols - 1);
        accumulated = save.accumulatedMillis();
        gameStart = System.currentTimeMillis();
        grid = save.grid();
        state = save.state();
        firstClick = false;
        gameOver = false;
        gameWon = false;
    }

    private void initEmpty() {
        if (difficulty.equals(Difficulty.Custom)) {
            this.rows = module.rows.get();
            this.cols = module.columns.get();
            this.mines = Math.min(module.mines.get(), rows * cols - 1);
        } else {
            this.rows = difficulty.getRows();
            this.cols = difficulty.getColumns();
            this.mines = Math.min(difficulty.getMines(), rows * cols - 1);
        }
        gameStart = 0;
        grid = new int[rows][cols];
        state = new byte[rows][cols];
        firstClick = true;
        gameOver = false;
        gameWon = false;
    }

    private void placeMines(int exX, int exY) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = 0;
                state[r][c] = 0;
            }
        }

        int placed = 0;
        while (placed < mines) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            if ((r == exX && c == exY) || grid[r][c] == -1) continue;

            grid[r][c] = -1;
            placed++;

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = r + dr, nc = c + dc;
                    if (inBounds(nr, nc) && grid[nr][nc] != -1) grid[nr][nc]++;
                }
            }
        }
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && c >= 0 && r < rows && c < cols;
    }

    public void revealCell(int r, int c) {
        if (!inBounds(r, c) || state[r][c] == 1 || state[r][c] == 2 || gameOver) return;

        if (firstClick) {
            // Lazy-place mines to guarantee the first click is safe
            placeMines(r, c);
            firstClick = false;
        }

        if (grid[r][c] == -1) {
            if (!gameOver) {
                module.clearSave();
                if (module.gameSounds.get()) mc.getSoundManager().play(
                    PositionedSoundInstance.master(
                        SoundEvents.ENTITY_VILLAGER_NO,
                        ThreadLocalRandom.current().nextFloat(0.77f, 1.1337f)
                    )
                );
            }

            revealAllMines();
            gameOver = true;
            gameWon = false;
            gameEnd = System.currentTimeMillis();
            return;
        }

        floodReveal(r, c);

        if (checkWin()) {
            if (!gameOver) {
                module.clearSave();
                if (module.gameSounds.get()) mc.getSoundManager().play(
                    PositionedSoundInstance.master(
                        SoundEvents.ENTITY_VILLAGER_YES,
                        ThreadLocalRandom.current().nextFloat(0.77f, 1.1337f)
                    )
                );
            }

            gameOver = true;
            gameWon = true;
            gameEnd = System.currentTimeMillis();
        }

    }

    private void revealAllMines() {
        for (int rr = 0; rr < rows; rr++) for (int cc = 0; cc < cols; cc++)
            if (grid[rr][cc] == -1) state[rr][cc] = 1;
    }

    private void floodReveal(int sr, int sc) {
        // iterative flood-fill using a stack
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{sr, sc});

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int r = p[0], c = p[1];

            if (!inBounds(r, c)) continue;
            if (state[r][c] == 1 || state[r][c] == 2) continue;

            state[r][c] = 1;
            if (grid[r][c] == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (inBounds(nr, nc) && state[nr][nc] == 0) {
                            stack.push(new int[] {nr, nc});
                        }
                    }
                }
            }
        }
    }

    public void toggleFlag(int r, int c) {
        if (!inBounds(r, c) || state[r][c] == 1 || gameOver) return;
        state[r][c] = (state[r][c] == 2) ? (byte)0 : (byte)2;
    }

    public void reset() {
        initEmpty();
        module.clearSave();
    }

    private boolean checkWin() {
        int revealed = 0;
        int total = rows * cols;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (state[r][c] == 1) revealed++;
            }
        }
        return (revealed == total - mines);
    }

    @Override
    protected void onCalculateSize() {
        width = cols * cellSize;
        height = statusHeight + rows * cellSize;
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        double localX = mouseX - x;
        double localY = mouseY - y;

        if (localY >= 0 && localY < statusHeight) {
            int resetW = 64;
            int resetX = (int) width - resetW - 6;
            int resetY = 3;
            int resetH = statusHeight - 6;

            if (localX >= resetX && localX <= resetX + resetW && localY >= resetY && localY <= resetY + resetH) {
                reset();
                return true;
            }

            return false;
        }

        if (gameOver) return false;
        int boardLocalX = (int) localX;
        int boardLocalY = (int) (localY - statusHeight);

        int c = boardLocalX/ cellSize;
        int r = boardLocalY / cellSize;

        if (r < 0 || c < 0 || r >= rows || c >= cols) return false;

        if (button == 0) { // left click
            if (firstClick && gameStart == 0) gameStart = System.currentTimeMillis();
            revealCell(r, c);
            return true;
        } else if (button == 1) { // right click
            toggleFlag(r, c);
            return true;
        }

        return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        super.onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);

        double localX = mouseX - x;
        double localY = mouseY - y;

        int statusW = (int) width;
        int resetW = 64;
        int resetX = statusW - resetW - 6;
        int resetY = 3;
        int resetH = statusHeight - 6;

        resetHover = (localX >= resetX && localX <= resetX + resetW && localY >= resetY && localY <= resetY + resetH);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        // board background
        int bx = (int) x;
        int by = (int) y;
        renderer.quad(bx - 2, by - 2, (int)width + 4, (int)height + 4, colorScheme.backgroundColor);

        // status bar
        int statusW = (int) width;
        int statusH = statusHeight;
        renderer.quad(bx, by, statusW, statusH, colorScheme.statusBarColor);

        // timer
        long elapsedSec = 0L;
        if (!firstClick && gameStart > 0 && !gameOver) {
            elapsedSec = (System.currentTimeMillis() - (gameStart - accumulated)) / 1000;
        } else if (gameOver) elapsedSec = (gameEnd - (gameStart - accumulated)) / 1000;

        int timerTextWidth = 40;
        String timerText = String.format("%02d:%02d", (elapsedSec / 60), (elapsedSec % 60));
        double timerX = bx + ((double) statusW / 2) - ((double) timerTextWidth / 2);
        renderer.text(timerText, timerX - 1, by + 4, colorScheme.textShadowColor, false);
        renderer.text(timerText, timerX + 1, by + 4, colorScheme.textShadowColor, false);
        renderer.text(timerText, timerX, by + 5, colorScheme.textShadowColor, false);
        renderer.text(timerText, timerX, by + 4, colorScheme.runtimeColor, false);

        // reset button
        int resetW = 64;
        int resetH = statusH - 6;
        int resetX = bx + statusW - resetW - 6;
        int resetY = by + 3;

        Color resetBg = resetHover ? colorScheme.resetHoveredColor : colorScheme.resetButtonColor;
        renderer.quad(resetX, resetY, resetW, resetH, resetBg);
        renderer.text("Reset", resetX + 9, resetY + 3, colorScheme.textShadowColor, false);
        renderer.text("Reset", resetX + 11, resetY + 3, colorScheme.textShadowColor, false);
        renderer.text("Reset", resetX + 10, resetY + 4, colorScheme.textShadowColor, false);
        renderer.text("Reset", resetX + 10, resetY + 3, colorScheme.resetTextColor, false);

        int boardY = by + statusH;

        // cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = bx + c * cellSize;
                int sy = boardY + r * cellSize;
                int inner = 1;
                int w = cellSize - inner * 2;
                int h = cellSize - inner * 2;

                byte s = state[r][c];
                if (s == 0) { // hidden
                    renderer.quad(sx + inner, sy + inner, w, h, colorScheme.hiddenCellColor);
                } else if (s == 2) { // flagged
                    renderer.quad(sx + inner, sy + inner, w, h, colorScheme.flaggedCellColor);
                    renderer.text("?", sx + inner + 2, sy + inner + TEXT_Y_OFFSET, colorScheme.textShadowColor, false);
                    renderer.text("?", sx + inner + 4, sy + inner + TEXT_Y_OFFSET, colorScheme.textShadowColor, false);
                    renderer.text("?", sx + inner + 3, sy + inner + TEXT_Y_OFFSET + 1, colorScheme.textShadowColor, false);
                    renderer.text("?", sx + inner + 3, sy + inner + TEXT_Y_OFFSET, colorScheme.flaggedCellTextColor, false);
                } else { // revealed
                    if (grid[r][c] == -1) { // mine
                        renderer.quad(sx + inner, sy + inner, w, h, colorScheme.mineCellColor);
                        renderer.text("X", sx + inner + 2, sy + inner + TEXT_Y_OFFSET, colorScheme.textShadowColor, false);
                        renderer.text("X", sx + inner + 4, sy + inner + TEXT_Y_OFFSET, colorScheme.textShadowColor, false);
                        renderer.text("X", sx + inner + 3, sy + inner + TEXT_Y_OFFSET + 1, colorScheme.textShadowColor, false);
                        renderer.text("X", sx + inner + 3, sy + inner + TEXT_Y_OFFSET, colorScheme.mineTextColor, false);
                    } else if (grid[r][c] == 0) { // empty cell
                        renderer.quad(sx + inner, sy + inner, w, h, colorScheme.revealedCellColor);
                    } else {
                        renderer.quad(sx + inner, sy + inner, w, h, colorScheme.revealedCellColor);
                        Color color = numberColor(grid[r][c]);
                        String txt = Integer.toString(grid[r][c]);
                        renderer.text(txt, sx + inner + 2, sy + inner + TEXT_Y_OFFSET, colorScheme.textShadowColor, false);
                        renderer.text(txt, sx + inner + 4, sy + inner + TEXT_Y_OFFSET, colorScheme.textShadowColor, false);
                        renderer.text(txt, sx + inner + 3, sy + inner + TEXT_Y_OFFSET + 1, colorScheme.textShadowColor, false);
                        renderer.text(txt, sx + inner + 3, sy + inner + TEXT_Y_OFFSET, color, false);
                    }
                }

                renderer.quad(sx, sy, cellSize, 1, colorScheme.cellBorderColor);
                renderer.quad(sx, sy, 1, cellSize, colorScheme.cellBorderColor);
                renderer.quad(sx, sy + cellSize, cellSize, 1, colorScheme.cellBorderColor);
                renderer.quad(sx + cellSize - 1, sy, 1, cellSize, colorScheme.cellBorderColor);
            }
        }

        if (gameOver) {
            String gameOverText = gameWon ? "YOU WIN! 8)" : "GAME OVER X)";
            renderer.text(gameOverText, bx + 5, by + 4, colorScheme.textShadowColor, false);
            renderer.text(gameOverText, bx + 7, by + 4, colorScheme.textShadowColor, false);
            renderer.text(gameOverText, bx + 6, by + 5, colorScheme.textShadowColor, false);
            renderer.text(gameOverText, bx + 6, by + 4, gameWon ? colorScheme.wonTextColor : colorScheme.lostTextColor, false);
        } else {
            int flags = countFlags();
            int minesLeft = Math.max(0, mines - flags);
            String minesText = "Mines: " + minesLeft;
            renderer.text(minesText, bx + 5, by + 4, colorScheme.textShadowColor, false);
            renderer.text(minesText, bx + 7, by + 4, colorScheme.textShadowColor, false);
            renderer.text(minesText, bx + 6, by + 5, colorScheme.textShadowColor, false);
            renderer.text(minesText, bx + 6, by + 4, colorScheme.mineCountColor, false);
        }
    }

    private Color numberColor(int n) {
        return switch (n) {
            case 1 -> new Color(0, 0, 255); // blue
            case 2 -> new Color(0, 128, 0); // green
            case 3 -> new Color(255, 0, 0); // red
            case 4 -> new Color(0, 0, 128); // dark blue
            case 5 -> new Color(128, 0, 0); // dark red
            case 6 -> new Color(0, 128, 128); // teal
            case 7 -> new Color(0, 0, 0); // black
            case 8 -> new Color(128, 128, 128); // gray
            default -> new Color(0, 0, 0);
        };
    }

    private int countFlags() {
        int flags = 0;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) if (state[r][c] == 2) flags++;
        return flags;
    }
}

