package dev.stardust.gui.widgets.meteorites.render;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.text.DecimalFormat;
import org.jetbrains.annotations.NotNull;
import meteordevelopment.meteorclient.gui.GuiTheme;
import dev.stardust.gui.widgets.meteorites.WMeteorites;
import dev.stardust.gui.widgets.meteorites.entity.Ship;
import dev.stardust.gui.widgets.meteorites.entity.Powerups;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class HudRenderer {
    private static final int PAD_Y = 8;
    private static final int PAD_X = 12;
    private static final double TITLE_SCALE = 1.25;
    private static final double NORMAL_SCALE = 1.0;
    private static final int GAP_INLINE_TO_SUB = 6;
    private static final int GAP_TITLE_TO_INLINE = 6;
    private static final DecimalFormat displayFormat = new DecimalFormat("#.##");

    public static void renderHud(
        GuiRenderer renderer, GuiTheme theme,
        int bx, int by,double width, double height, WMeteorites widget
    ) {
        Color textColor = new Color(8, 8, 12);
        Color shadowColor = new Color(255, 255, 1255);

        if (widget.CHEAT_MODE) {
            String cheatText = "CHEAT";
            Color cheatShadow = new Color(69, 0, 0);
            double cheatWidth = theme.textRenderer().getWidth(cheatText);
            renderer.text("CHEAT", bx + (int)width - cheatWidth - 6 - 1, by + 6, cheatShadow, false);
            renderer.text("CHEAT", bx + (int)width - cheatWidth - 6 + 1, by + 6, cheatShadow, false);
            renderer.text("CHEAT", bx + (int)width - cheatWidth - 6, by + 5, cheatShadow, false);
            renderer.text("CHEAT", bx + (int)width - cheatWidth - 6, by + 7, cheatShadow, false);
            renderer.text("CHEAT", bx + (int)width - cheatWidth - 6, by + 6, new Color(255, 0, 0), false);
        }

        int[] yOffsets = {6, 22, 38, 54};
        List<String> hudText = getHudStrings(widget);
        hudText.sort(Comparator.comparingDouble(str -> theme.textRenderer().getWidth((String) str)).reversed());

        for (int n = 0; n < 4; n++) {
            int yOffset = yOffsets[n];
            String text = hudText.get(n);
            renderer.text(text, bx + 5, by + yOffset, shadowColor, false);
            renderer.text(text, bx + 7, by + yOffset, shadowColor, false);
            renderer.text(text, bx + 6, by + yOffset - 1, shadowColor, false);
            renderer.text(text, bx + 6, by + yOffset + 1, shadowColor, false);
            renderer.text(text, bx + 6, by + yOffset, textColor, false);
        }

        if (widget.player.lives <= 0) {
            renderGameOverPopup(renderer, bx, by, width, height, widget);
        } else if (widget.isPaused) {
            renderPausePopup(renderer, bx, by, width, height, widget);
        }
    }

    private static void renderPausePopup(
        GuiRenderer renderer, int bx, int by,
        double width, double height, WMeteorites widget
    ) {
        String tip = widget.currentGameTip;
        String title = widget.gameBegan ? "PAUSED" : "METEORITES";
        String subtitle = widget.gameBegan ? "Press R to resume | Press N to restart" : "Press R to start";

        List<String> subs = new ArrayList<>();
        if (!widget.gameBegan) subs.add("by <0xTas>");
        subs.add(subtitle);
        if (tip != null && !tip.isEmpty()) subs.add(tip);

        List<Color> subCols = new ArrayList<>();
        if (!widget.gameBegan) subCols.add(new Color(80, 255, 80));
        subCols.add(new Color(200, 200, 200));

        drawPopup(
            renderer, bx, by, width, height,
            title, widget.gameBegan ? new Color(255, 80, 80) : new Color(80, 255, 80),
            null, null, subs, subCols
        );
    }

    private static void renderGameOverPopup(
        GuiRenderer renderer, int bx, int by,
        double width, double height, WMeteorites widget
    ) {
        String inlineLeft, inlineRight;
        String separator = " | ";

        if (widget.highScore != null && !widget.CHEAT_MODE) {
            int pts = widget.highScore.getScore(widget.fieldSize);
            int maxWave = widget.highScore.getWave(widget.fieldSize);
            String bestPower = Powerups.values()[widget.highScore.getBestPowerOrdinal(widget.fieldSize)].asString();

            inlineRight = "Favorite power: " + bestPower;
            inlineLeft = widget.hasNewHighScore
                ? String.format("Wave %d: %,d pts", maxWave, pts)
                : String.format("Highscore: wave %d - %,d pts", maxWave, pts);
        } else {
            int most = 0;
            Powerups bestPower = Powerups.NONE;
            for (Map.Entry<Powerups, Integer> powers : widget.player.powerMap.entrySet()) {
                if (powers.getValue() > most) {
                    most = powers.getValue();
                    bestPower = powers.getKey();
                }
            }

            int pts = widget.player.score;
            inlineLeft = String.format("Wave: %d, Score: %,d", widget.wave, pts);
            inlineRight = "Favorite power: " + bestPower.asString();
        }

        String[] inlineTriple = new String[] { inlineLeft, separator, inlineRight };
        Color[] inlineColors = new Color[] {
            widget.hasNewHighScore ? new Color(13, 225, 13) : new Color(169, 169, 0),
            new Color(200, 200, 200),
            new Color(180, 220, 255)
        };

        List<String> subs = new ArrayList<>();
        subs.add("Press R to restart");
        List<Color> subCols = new ArrayList<>();
        subCols.add(new Color(200, 200, 200));

        Color titleColor = widget.hasNewHighScore ? new Color(80, 255, 80) : new Color(255, 80, 80);

        drawPopup(
            renderer, bx, by, width, height,
            widget.hasNewHighScore ? "NEW HIGH SCORE" : "GAME OVER",
            titleColor, inlineTriple, inlineColors, subs, subCols
        );
    }

    private static void drawPopup(
        GuiRenderer renderer, int bx, int by, double width, double height,
        String title, Color titleColor, String[] inlineTriple, Color[] inlineColors, List<String> subtitles, List<Color> subColors
    ) {
        // beware: here be HUD code
        TextRenderer tr = renderer.theme.textRenderer();
        int titleW = scaledTextWidth(renderer, tr, title, TITLE_SCALE);
        int titleH = scaledTextHeight(renderer, tr, TITLE_SCALE);

        int maxSubW = 0;
        int subH = scaledTextHeight(renderer, tr, NORMAL_SCALE);
        if (subtitles != null) {
            for (String s : subtitles) {
                if (s == null) continue;
                maxSubW = Math.max(maxSubW, scaledTextWidth(renderer, tr, s, NORMAL_SCALE));
            }
        }

        int inlineH = 0;
        int inlineCombinedW = 0;
        if (inlineTriple != null) {
            int leftW = scaledTextWidth(renderer, tr, inlineTriple[0], NORMAL_SCALE);
            int sepW  = scaledTextWidth(renderer, tr, inlineTriple[1], NORMAL_SCALE);
            int rightW = scaledTextWidth(renderer, tr, inlineTriple[2], NORMAL_SCALE);
            inlineCombinedW = leftW + sepW + rightW;
            inlineH = subH;
        }

        int subtitleAmount = subtitles == null ? 0 : subtitles.size();
        int maxInnerWidth = Math.max(titleW, Math.max(maxSubW, inlineCombinedW));

        int quadW = maxInnerWidth + PAD_X * 2;
        int quadH = titleH + PAD_Y * 2 + (subH * subtitleAmount);

        if (inlineCombinedW > 0) {
            quadH += inlineH + GAP_TITLE_TO_INLINE + GAP_INLINE_TO_SUB;
        }

        int centerX = bx + (int) (width / 2.0);
        int centerY = by + (int) (height / 2.0) - 8;

        int quadX = centerX - quadW / 2;
        int quadY = centerY - (titleH / 2) - PAD_Y;
        renderer.quad(quadX, quadY, quadW, quadH, new Color(0, 0, 0, 180));

        int titleY = quadY + (PAD_Y / 2);
        int titleX = centerX - titleW / 2;
        Color strokeColor = new Color(0, 0, 0);
        drawText(renderer, title, titleX + 1, titleY, strokeColor, true);
        drawText(renderer, title, titleX - 1, titleY, strokeColor, true);
        drawText(renderer, title, titleX, titleY + 1, strokeColor, true);
        drawText(renderer, title, titleX, titleY - 1, strokeColor, true);
        drawText(renderer, title, titleX, titleY, titleColor != null ? titleColor : new Color(255, 80, 80), true);

        int currentY = titleY + titleH;

        if (inlineCombinedW > 0) {
            currentY += GAP_TITLE_TO_INLINE;
            int inlineXStart = centerX - inlineCombinedW / 2;

            Color leftCol = inlineColors != null && inlineColors.length > 0 ? inlineColors[0] : new Color(200, 200, 200);
            drawText(renderer, inlineTriple[0], inlineXStart, currentY, leftCol, false);

            int leftW = scaledTextWidth(renderer, tr, inlineTriple[0], NORMAL_SCALE);
            int sepX = inlineXStart + leftW;

            Color sepCol = inlineColors != null && inlineColors.length > 1 ? inlineColors[1] : new Color(200, 200, 200);
            drawText(renderer, inlineTriple[1], sepX, currentY, sepCol, false);

            int sepW = scaledTextWidth(renderer, tr, inlineTriple[1], NORMAL_SCALE);
            int rightX = sepX + sepW;

            Color rightCol = inlineColors != null && inlineColors.length > 2 ? inlineColors[2] : new Color(200, 200, 200);
            drawText(renderer, inlineTriple[2], rightX, currentY, rightCol, false);

            currentY += inlineH + GAP_INLINE_TO_SUB;
        }

        if (subtitles != null) {
            for (int n = 0; n < subtitles.size(); n++) {
                String s = subtitles.get(n);
                if (s == null) continue;

                Color c = (subColors != null && n < subColors.size()) ? subColors.get(n) : new Color(200, 200, 200);
                int sw = scaledTextWidth(renderer, tr, s, NORMAL_SCALE);
                int sx = centerX - sw / 2;

                drawText(renderer, s, sx, currentY, c, false);

                currentY += subH;
            }
        }
    }

    private static int scaledTextWidth(GuiRenderer renderer, TextRenderer tr, String s, double scale) {
        if (s == null || s.isEmpty()) return 0;
        return (int) Math.round(tr.getWidth(s) * renderer.theme.scale(scale));
    }

    private static int scaledTextHeight(GuiRenderer renderer, TextRenderer tr, double scale) {
        return (int) Math.round(tr.getHeight() * renderer.theme.scale(scale));
    }

    private static void drawText(GuiRenderer renderer, String text, int x, int y, Color color, boolean title) {
        renderer.text(text, x, y, color, title);
    }

    private static @NotNull List<String> getHudStrings(WMeteorites widget) {
        Ship player = widget.player;
        List<String> hudText = new ArrayList<>();
        String waveText = "Wave: " + widget.wave;
        String livesText = "Lives: " + player.lives;
        String scoreText = "Points: " + String.format("%,d", player.score);
        String powerText = "Powerup: ";

        if (player.hasEntropy()) {
            powerText += "Entropy(" + player.getPowerup().asString() + ")";
        } else {
            powerText += player.getPowerup().asString();
        }

        switch (player.getPowerup()) {
            case MIDAS_TOUCH -> powerText += " [$$$]";

            case CALIBRATED_FSD -> {
                long now = System.currentTimeMillis();
                if (now - player.lastHyperJump >= Ship.CALIBRATED_WARP_COOLDOWN * 1000) {
                    powerText += " [READY]";
                } else {
                    double cd = Ship.CALIBRATED_WARP_COOLDOWN * 1000.0 - (now - player.lastHyperJump);
                    powerText += " [COOLING: " + displayFormat.format(Math.max(0, cd / 1000.0)) + "]";
                }
            }

            case GRAVITY_WELL -> {
                if (player.gravityWellDeployed) {
                    powerText += " [DEPLOYED: " + displayFormat.format(Math.max(player.gravityWellTimer, 0)) + "]";
                } else if (player.gravityWellCdTimer <= 0) {
                    powerText += " [READY]";
                } else {
                    powerText += " [COOLING: " + displayFormat.format(Math.max(player.gravityWellCdTimer, 0)) + "]";
                }
            }

            case PHASE_SHIFT -> {
                if (player.phaseActive) {
                    powerText += " [" + "ACTIVE: " + displayFormat.format(Math.max(player.phaseDuration - player.phaseTimer, 0)) + "]";
                } else if (player.phaseCooldownTimer <= 0) {
                    powerText += " [READY]";
                } else {
                    powerText += " [COOLING]";
                }
            }

            case STARDUST -> powerText += " [" + (player.isShotgun ? "SHOTGUN" : player.isSniper ? "SNIPER" : "HELL") + "]";

            case HIGH_TECH_HULL, REINFORCED_HULL -> powerText += " [" + (player.isHullFull() ? "FULL" : (int) Math.ceil(player.hull)) + "]";
        }

        hudText.add(waveText);
        hudText.add(scoreText);
        hudText.add(powerText);
        hudText.add(livesText);
        return hudText;
    }
}
