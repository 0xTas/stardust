package dev.stardust.gui.widgets.meteorites;

import java.util.Map;

public record HighScore(
    int version, Map<FieldSize, int[]> scores
) {
    public int getScore(FieldSize size) {
        return scores.getOrDefault(size, new int[]{0, 0, 0})[0];
    }

    public int getWave(FieldSize size) {
        return scores.getOrDefault(size, new int[]{0, 0, 0})[1];
    }

    public int getBestPowerOrdinal(FieldSize size) {
        return scores.getOrDefault(size, new int[]{0, 0, 0})[2];
    }

    public boolean isSurpassed(FieldSize size, int score) {
        return this.getScore(size) < score;
    }

    public void update(FieldSize size, int score, int wave, int bestPowerOrdinal) {
        scores.put(size, new int[]{score, wave, bestPowerOrdinal});
    }
}
