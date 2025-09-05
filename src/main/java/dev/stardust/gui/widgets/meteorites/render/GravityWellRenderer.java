package dev.stardust.gui.widgets.meteorites.render;

import net.minecraft.util.math.MathHelper;
import dev.stardust.gui.widgets.meteorites.entity.Ship;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class GravityWellRenderer {
    public static void renderBackground(
        GuiRenderer renderer, boolean deployed,
        double cx, double cy, Color gravityColor
    ) {
        // pulse animation
        double t = System.nanoTime() * 1e-9;
        float pulse = (float)(0.9 + 0.15 * Math.sin(t * Math.PI * 1.5));
        pulse = MathHelper.clamp(pulse, 0.13f, 1.0f);

        int segments = 28;

        // outer faint band
        double gR1 = Math.max(6.0, Ship.GW_FALLOFF * 0.30);
        Color c1 = new Color(gravityColor.r, gravityColor.g, gravityColor.b, (int)((deployed ? 18 : 9) * pulse));
        drawFilledCircleFan(renderer, cx, cy, gR1 * 1.6, segments, c1);

        // mid glow
        double gR2 = Math.max(4.0, Ship.GW_FALLOFF * 0.20);
        Color c2 = new Color(gravityColor.r, gravityColor.g, gravityColor.b, (int)((deployed ? 36 : 18) * pulse));
        drawFilledCircleFan(renderer, cx, cy, gR2 * 1.25, segments, c2);

        // tight inner glow
        double gR3 = Math.max(2.0, Ship.GW_FALLOFF * 0.08);
        Color c3 = new Color(gravityColor.r, gravityColor.g, gravityColor.b, (int)((deployed ? 60 : 30) * pulse));
        drawFilledCircleFan(renderer, cx, cy, gR3, segments, c3);

        // thin outline ring
        int ringSegments = 36;
        double ringThickness = Math.max(2.0, Ship.GW_FALLOFF * 0.03);
        double outerR = Ship.GW_FALLOFF + ringThickness * 0.5;
        double innerR = Ship.GW_FALLOFF - ringThickness * 0.5;
        Color ringCol = new Color(gravityColor.r, gravityColor.g, gravityColor.b, (int)((deployed ? 80 : 40) * pulse));
        double step = Math.PI * 2.0 / ringSegments;
        for (int n = 0; n < ringSegments; n++) {
            double a0 = n * step;
            double a1 = (n + 1) * step;
            double ox0 = Math.cos(a0), oy0 = Math.sin(a0);
            double ox1 = Math.cos(a1), oy1 = Math.sin(a1);

            double outerX0 = cx + ox0 * outerR, outerY0 = cy + oy0 * outerR;
            double outerX1 = cx + ox1 * outerR, outerY1 = cy + oy1 * outerR;
            double innerX0 = cx + ox0 * innerR, innerY0 = cy + oy0 * innerR;
            double innerX1 = cx + ox1 * innerR, innerY1 = cy + oy1 * innerR;

            // two triangles to form the quad segment
            renderer.triangle(outerX0, outerY0, innerX0, innerY0, outerX1, outerY1, ringCol);
            renderer.triangle(innerX0, innerY0, outerX1, outerY1, innerX1, innerY1, ringCol);
        }
    }

    public static void renderReticle(GuiRenderer renderer, boolean deployed, double cx, double cy) {
        Color crossColor = new Color(230, 230, 255, (deployed ? 137 : 69));
        double crossHalf = Math.max(6.0, Ship.GW_FALLOFF * 0.03);
        renderer.quad(cx - crossHalf, cy - 1.0, crossHalf * 2.0, 2.0, crossColor);
        renderer.quad(cx - 1.0, cy - crossHalf, 2.0, crossHalf * 2.0, crossColor);
        renderer.quad(cx - 1.5, cy - 1.5, 3.0, 3.0, new Color(255, 255, 255, (deployed ? 220 : 110)));
    }

    private static void drawFilledCircleFan(
        GuiRenderer renderer, double cx, double cy, double radius, int segments, Color color
    ) {
        if (segments < 6) segments = 6;
        // center vertex
        double[] vx = new double[segments];
        double[] vy = new double[segments];
        double step = Math.PI * 2.0 / segments;
        for (int n = 0; n < segments; n++) {
            double a = n * step;
            vx[n] = cx + Math.cos(a) * radius;
            vy[n] = cy + Math.sin(a) * radius;
        }

        // fan triangles (center, i, i+1)
        for (int n = 0; n < segments; n++) {
            int j = (n + 1) % segments;
            renderer.triangle(cx, cy, vx[n], vy[n], vx[j], vy[j], color);
        }
    }
}
