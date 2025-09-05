package dev.stardust.gui.widgets.meteorites.render;

import java.util.Random;
import net.minecraft.util.math.MathHelper;
import dev.stardust.gui.widgets.meteorites.entity.Ship;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class StarfieldRenderer {
    private static final int STAR_SEGMENTS = 8;
    private static final int MIN_DUST_COUNT = 13;
    private static final int MAX_DUST_COUNT = 37;
    private static final int MIN_STARS_LAYER1 = 80;
    private static final int MAX_STARS_LAYER1 = 120;
    private static final int MIN_STARS_LAYER0 = 120;
    private static final int MAX_STARS_LAYER0 = 250;
    private static final double DUST_PARALLAX = 0.42;
    private static final double LAYER1_PARALLAX = 0.17;
    private static final double LAYER0_PARALLAX = 0.069;

    private final Dust[] dustLayer;
    private final Star[] starsLayer0;
    private final Star[] starsLayer1;

    private double worldX;
    private double worldY;
    private double prevPlayerX;
    private double prevPlayerY;
    private double resetDurationSec;
    private double resetStartTimeSec;
    private boolean resetting = false;
    private double resetToX, resetToY;
    private double resetFromX, resetFromY;
    private final int dustAmount, stars0Amount, stars1Amount;

    public StarfieldRenderer(Ship player, double width, double height, long seed) {
        this.worldX = player.x;
        this.worldY = player.y;
        this.prevPlayerX = worldX;
        this.prevPlayerY = worldY;
        Random rng = new Random(seed);

        this.dustAmount = rng.nextInt(MIN_DUST_COUNT, MAX_DUST_COUNT);
        this.stars0Amount = rng.nextInt(MIN_STARS_LAYER0, MAX_STARS_LAYER0);
        this.stars1Amount = rng.nextInt(MIN_STARS_LAYER1, MAX_STARS_LAYER1);

        dustLayer = new Dust[dustAmount];
        starsLayer0 = new Star[stars0Amount];
        starsLayer1 = new Star[stars1Amount];

        // tiny distant stars
        for (int n = 0; n < stars0Amount; n++) {
            Star s = new Star();
            s.x = rng.nextDouble() * width;
            s.y = rng.nextDouble() * height;
            s.size  = 1.0 + rng.nextDouble() * 1.6;
            s.alpha = 0.06 + rng.nextDouble() * 0.18;
            s.phase = rng.nextDouble() * Math.PI * 2.0;
            s.hue   = (rng.nextFloat() * 0.05f) + 0.05f;

            starsLayer0[n] = s;
        }

        // closer stars / twinkling
        for (int n = 0; n < stars1Amount; n++) {
            Star s = new Star();
            s.x = rng.nextDouble() * width;
            s.y = rng.nextDouble() * height;

            s.size  = 1.5 + rng.nextDouble() * 2.6;
            s.alpha = 0.12 + rng.nextDouble() * 0.36;
            s.phase = rng.nextDouble() * Math.PI * 2.0;
            s.hue   = (rng.nextFloat() * 0.08f) + 0.02f;

            starsLayer1[n] = s;
        }

        // subtle dust & debris
        for (int n = 0; n < dustAmount; n++) {
            Dust d = new Dust();
            d.x = rng.nextDouble() * width;
            d.y = rng.nextDouble() * height;

            d.size = 4.0 + rng.nextDouble() * 8.0;
            d.alpha = 0.06 + rng.nextDouble() * 0.14;

            d.vx = (rng.nextDouble() - 0.5) * 7.0;
            d.vy = (rng.nextDouble() - 0.5) * 7.0;
            d.phase = rng.nextDouble() * Math.PI * 2.0;

            dustLayer[n] = d;
        }
    }

    public void resetInstant(double worldXTarget, double worldYTarget) {
        worldX = worldXTarget;
        worldY = worldYTarget;
        resetting = false;
    }

    public void resetSmooth(double worldXTarget, double worldYTarget, double durationSec, double timeSec) {
        double curX = this.worldX;
        double curY = this.worldY;
        if (this.resetting) {
            double t = (timeSec - this.resetStartTimeSec) / Math.max(1e-9, this.resetDurationSec);
            t = MathHelper.clamp((float) t, 0f, 1f);

            double ease = easeInOutCubic(t);
            curX = lerp(this.resetFromX, this.resetToX, ease);
            curY = lerp(this.resetFromY, this.resetToY, ease);
        }

        this.resetFromX = curX;
        this.resetFromY = curY;
        this.resetToX = worldXTarget;
        this.resetToY = worldYTarget;
        this.resetStartTimeSec = timeSec;
        this.resetDurationSec = Math.max(1e-6, durationSec);
        this.resetting = true;
    }


    public void render(GuiRenderer renderer, int bx, int by, double px, double py, double width, double height, double time) {
        int w = (int) width;
        int h = (int) height;
        double rawDx = px - prevPlayerX;
        double rawDy = py - prevPlayerY;
        double dx = wrappedDelta(rawDx, width);
        double dy = wrappedDelta(rawDy, height);

        worldX += dx;
        worldY += dy;
        prevPlayerX = px;
        prevPlayerY = py;

        if (resetting) {
            double t = (time - resetStartTimeSec) / resetDurationSec;
            t = MathHelper.clamp((float) t, 0f, 1f);

            double ease = easeInOutCubic(t);
            worldX = lerp(resetFromX, resetToX, ease);
            worldY = lerp(resetFromY, resetToY, ease);

            if (t >= 1.0 - 1e-9) {
                resetting = false;
            }
        }

        renderStarsLayer(
            renderer, starsLayer0, stars0Amount,
            bx, by, w, h, LAYER0_PARALLAX, worldX, worldY, time, false
        );

        renderStarsLayer(
            renderer, starsLayer1, stars1Amount,
            bx, by, w, h, LAYER1_PARALLAX, worldX, worldY, time, true
        );

        renderDustLayer(renderer, dustLayer, bx, by, w, h, worldX, worldY, time);
    }

    private void renderStarsLayer(
        GuiRenderer renderer, Star[] stars, int count, int bx, int by,
        int width, int height, double parallax, double worldX, double worldY, double timeSec, boolean twinkle
    ) {

        // offset based on player position to create parallax; negative so stars move in opposing fashion
        double ox = -worldX * parallax;
        double oy = -worldY * parallax;

        // reduce ox/oy into positive modulo
        ox = mod(ox, width);
        oy = mod(oy, height);
        for (int n = 0; n < count; n++) {
            Star s = stars[n];
            double a = s.alpha;

            if (twinkle) {
                double tw = 0.6 + 0.4 * Math.sin(timeSec * 3.0 + s.phase);
                a = a * (0.7 + 0.6 * tw);
            }

            // wrap positions to maintain visual continuity
            double wx = (s.x + ox) % width;
            if (wx < 0) wx += width;
            double wy = (s.y + oy) % height;
            if (wy < 0) wy += height;

            Color color = new Color(
                255, 255, 255,
                Math.round(255 * MathHelper.clamp((float) a, 0f, 1f))
            );

            double sr = s.size;
            drawCircleWrapped(renderer, bx, by, width, height, wx, wy, sr, color);
        }
    }

    private void renderDustLayer(
        GuiRenderer renderer, Dust[] dust, int bx, int by,
        int width, int height, double worldX, double worldY, double timeSec
    ) {

        double ox = -worldX * DUST_PARALLAX;
        double oy = -worldY * DUST_PARALLAX;

        ox = mod(ox, width);
        oy = mod(oy, height);

        for (int n = 0; n < dustAmount; n++) {
            Dust d = dust[n];

            // drift animation
            double dx = d.x + d.vx * (timeSec * 0.42);
            double dy = d.y + d.vy * (timeSec * 0.42);
            double wx = (dx + ox) % width; if (wx < 0) wx += width;
            double wy = (dy + oy) % height; if (wy < 0) wy += height;

            // subtle breathing
            double pulse = 0.85 + 0.15 * Math.sin(timeSec * 1.1 + d.phase);

            Color col = new Color(
                220, 220, 240,
                Math.round(255 * MathHelper.clamp((float)(d.alpha * pulse), 0f, 0.42f))
            );

            drawCircleWrapped(renderer, bx, by, width, height, wx, wy, d.size * pulse, col);
        }
    }

    private void drawCircleWrapped(
        GuiRenderer renderer, int bx, int by,
        int width, int height, double centerX, double centerY, double w, Color color
    ) {
        // inner fill
        drawFilledCircleWrapped(
            renderer, bx, by, width, height,
            centerX, centerY, Math.max(0.5, w * 0.5), color
        );

        // ring with slightly lower alpha
        int ringAlpha = Math.max(8, (int)(color.a * 0.30));
        double ringRadius = Math.max(1.0, (w * 0.5) * 1.8);
        Color ringColor = new Color(color.r, color.g, color.b, ringAlpha);

        drawRingWrapped(
            renderer, bx, by,
            width, height, centerX, centerY, ringRadius, ringColor
        );
    }

    private void drawFilledCircleWrapped(
        GuiRenderer renderer, int bx, int by, int width, int height,
        double centerX, double centerY, double radius, Color color
    ) {
        // draw center copy and up to 8 neighbors to account for wrapping
        for (int offX = -1; offX <= 1; offX++) {
            for (int offY = -1; offY <= 1; offY++) {
                double sx = centerX + offX * width;
                double sy = centerY + offY * height;
                if (sx + radius < 0 || sx - radius > width || sy + radius < 0 || sy - radius > height) continue;

                double cx = bx + sx;
                double cy = by + sy;

                // create triangle-fan: center -> perimeter
                double angleStep = 2.0 * Math.PI / STAR_SEGMENTS;

                // build triangles (center, v_i, v_i+1)
                double prevX = cx + Math.cos(0) * radius;
                double prevY = cy + Math.sin(0) * radius;
                for (int n = 1; n <= STAR_SEGMENTS; n++) {
                    double ang = n * angleStep;
                    double vx = cx + Math.cos(ang) * radius;
                    double vy = cy + Math.sin(ang) * radius;

                    renderer.triangle(
                        cx, cy,
                        prevX, prevY,
                        vx, vy, color
                    );

                    prevX = vx;
                    prevY = vy;
                }
            }
        }
    }

    private void drawRingWrapped(
        GuiRenderer renderer, int bx, int by, int width, int height,
        double centerX, double centerY, double outerRadius, Color ringColor
    ) {
        // ring rendered as outer fan minus inner fan, approximated by triangles
        // thin triangular geometry: for each segment, draw triangle (outer_i, outer_i+1, inner_i)
        double thickness = Math.max(0.5, outerRadius * 0.45);
        double innerRadius = Math.max(0.5, outerRadius - thickness);

        for (int offX = -1; offX <= 1; offX++) {
            for (int offY = -1; offY <= 1; offY++) {
                double sx = centerX + offX * width;
                double sy = centerY + offY * height;
                if (sx + outerRadius < 0 || sx - outerRadius > width || sy + outerRadius < 0 || sy - outerRadius > height) continue;

                double cx = bx + sx;
                double cy = by + sy;
                double angleStep = 2.0 * Math.PI / STAR_SEGMENTS;

                // precompute first pair
                double oPrevX = cx + Math.cos(0) * outerRadius;
                double oPrevY = cy + Math.sin(0) * outerRadius;
                double iPrevX = cx + Math.cos(0) * innerRadius;
                double iPrevY = cy + Math.sin(0) * innerRadius;

                for (int n = 1; n <= STAR_SEGMENTS; n++) {
                    double ang = n * angleStep;
                    double oX = cx + Math.cos(ang) * outerRadius;
                    double oY = cy + Math.sin(ang) * outerRadius;
                    double iX = cx + Math.cos(ang) * innerRadius;
                    double iY = cy + Math.sin(ang) * innerRadius;

                    // two triangles for the quad
                    renderer.triangle(
                        oPrevX, oPrevY,
                        iPrevX, iPrevY,
                        oX, oY, ringColor
                    );
                    renderer.triangle(
                        oX, oY,
                        iPrevX, iPrevY,
                        iX, iY, ringColor
                    );

                    oPrevX = oX; oPrevY = oY;
                    iPrevX = iX; iPrevY = iY;
                }
            }
        }
    }

    private static double wrappedDelta(double targetMinusSource, double span) {
        double d = targetMinusSource;
        double half = span * 0.5;
        while (d >  half) d -= span;
        while (d <= -half) d += span;
        return d;
    }

    private static double mod(double v, double m) {
        double r = v % m;
        if (r < 0) r += m;

        return r;
    }

    private static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static class Star {
        double x, y;
        double size;
        double alpha;
        double phase;
        float hue; // unused until I feel like it
    }
    private static class Dust {
        double x, y;
        double size;
        double alpha;
        double vx, vy;
        double phase;
    }
}
