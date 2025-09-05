package dev.stardust.gui.widgets.meteorites.entity.collision;

public class CollisionUtil {
    public static class CollisionResult {
        public boolean collided;
        public double nx, ny; // normal pointing from ship -> meteorite, unit length
        public double penetration; // overlap depth (positive)
    }

    /**
     * SAT test: polygon A vs polygon B.
     * Expects vertices arrays length = vertCount.
     * Returns CollisionResult and axis/penetration in result.
     */
    public static CollisionResult satPolygonCollision(
        double[] ax, double[] ay,
        double[] bx, double[] by, int bc
    ) {
        CollisionResult res = new CollisionResult();

        double bestAx = 0;
        double bestAy = 0;
        res.collided = true;
        double minOverlap = Double.POSITIVE_INFINITY;

        double[] proj = new double[2];
        double[] proj2 = new double[2];

        // Test axes from A (edge normals)
        double[] normalTmp = new double[2];
        for (int i = 0, j = 3 - 1; i < 3; j = i++) {
            edgeNormal(j, i, ax, ay, normalTmp); // normalized
            projectPolygon(ax, ay, 3, normalTmp[0], normalTmp[1], proj);
            projectPolygon(bx, by, bc, normalTmp[0], normalTmp[1], proj2);

            double[] ov = new double[1];
            if (!intervalOverlap(proj[0], proj[1], proj2[0], proj2[1], ov)) {
                res.collided = false; return res; // separating axis found
            }

            double overlap = ov[0];
            if (overlap < minOverlap) {
                minOverlap = overlap;
                bestAx = normalTmp[0];
                bestAy = normalTmp[1];
            }
        }

        // Test axes from B
        for (int i = 0, j = bc - 1; i < bc; j = i++) {
            edgeNormal(j, i, bx, by, normalTmp);
            projectPolygon(ax, ay, 3, normalTmp[0], normalTmp[1], proj);
            projectPolygon(bx, by, bc, normalTmp[0], normalTmp[1], proj2);

            double[] ov = new double[1];
            if (!intervalOverlap(proj[0], proj[1], proj2[0], proj2[1], ov)) {
                res.collided = false; return res;
            }

            double overlap = ov[0];
            if (overlap < minOverlap) {
                minOverlap = overlap;
                bestAx = normalTmp[0];
                bestAy = normalTmp[1];
            }
        }

        // If we get here, no separating axis found -> collision.
        // bestAx, bestAy is the axis with the smallest overlap. We need normal pointing from ship -> meteorite.
        res.penetration = minOverlap;

        // We don't yet know direction. Compute polygon centroids and orient normal from A->B
        double acx = 0;
        double acy = 0;
        for (int i = 0; i < 3; i++) {
            acx += ax[i];
            acy += ay[i];
        }
        acx /= 3;
        acy /= 3;

        double bcx = 0;
        double bcy = 0;
        for (int i = 0; i < bc; i++) {
            bcx += bx[i];
            bcy += by[i];
        }
        bcx /= bc;
        bcy /= bc;

        double dirX = bcx - acx;
        double dirY = bcy - acy;
        if (dot(dirX, dirY, bestAx, bestAy) < 0) {
            // flip direction so it points from A -> B
            bestAx = -bestAx; bestAy = -bestAy;
        }

        res.nx = bestAx;
        res.ny = bestAy;
        return res;
    }

    // project polygon vertices onto axis (mx,my). returns [min,max] in out[0], out[1]
    public static void projectPolygon(double[] vertsX, double[] vertsY, int vertCount, double mx, double my, double[] out) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < vertCount; i++) {
            double p = dot(vertsX[i], vertsY[i], mx, my);
            if (p < min) min = p;
            if (p > max) max = p;
        }
        out[0] = min;
        out[1] = max;
    }

    // test overlap between two intervals [minA,maxA] and [minB,maxB]; return overlap > 0 and write overlap to out[0]
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean intervalOverlap(double minA, double maxA, double minB, double maxB, double[] out) {
        double overlap = Math.min(maxA, maxB) - Math.max(minA, minB);
        out[0] = overlap;
        return overlap > 0;
    }

    // compute perpendicular (normal) for an edge (x1,y1)->(x2,y2) and normalize it
    public static void edgeNormal(int i1, int i2, double[] vertsX, double[] vertsY, double[] out) {
        double ex = vertsX[i2] - vertsX[i1];
        double ey = vertsY[i2] - vertsY[i1];
        // perpendicular (ny, -nx) or (-ey, ex) is fine — we normalize
        double nx = -ey;
        double len = Math.hypot(nx, ex);
        if (len == 0) {
            out[0]=1;
            out[1]=0;
            return;
        }

        out[0] = nx / len;
        out[1] = ex / len;
    }

    public static double dot(double ax, double ay, double bx, double by) {
        return ax*bx + ay*by;
    }

    public static double distToSegmentSquared(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            double ddx = px - x1, ddy = py - y1;
            return ddx*ddx + ddy*ddy;
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / (dx*dx + dy*dy);

        if (t < 0) t = 0;
        else if (t > 1) t = 1;

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        double ddx = px - projX, ddy = py - projY;

        return ddx*ddx + ddy*ddy;
    }
}
