package dev.stardust.gui.widgets.meteorites.entity;

import dev.stardust.gui.widgets.meteorites.WMeteorites;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import dev.stardust.gui.widgets.meteorites.entity.collision.CollisionUtil;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Bullet extends Entity {
    public static final double BULLET_SPEED = 280;
    public static final double HOMING_RANGE = 69.0;
    public static final double BULLET_LIFETIME = 1.2;
    public static final double HOMING_STRENGTH = 0.0969;

    public double angle;
    public double life = 0;
    public BulletTypes type;
    public void set(BulletTypes type, double x, double y, double vx, double vy, double life, double angle) {
        this.type = type; this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.angle = angle;
    }
    public void set(Bullet other) {
        this.type = other.type;
        this.x = other.x; this.y = other.y; this.vx = other.vx; this.vy = other.vy; this.life = other.life; this.angle = other.angle;
    }

    public void render(GuiRenderer renderer, int bx, int by, SettingColor bulletColor) {
        double thickness = 2.0;
        double baseLength = 2.0;
        if (this.type == BulletTypes.SNIPER) {
            double speed = Math.hypot(this.vx, this.vy);
            double speedFactor = Math.min(3.0, speed / 50.0);
            baseLength = 4.0 * speedFactor;
        }

        // get orientation; if velocity is zero, fall back to angle field
        double angle = Math.atan2(this.vy, this.vx);
        if (Double.isNaN(angle) || (Math.abs(this.vx) < 1e-6 && Math.abs(this.vy) < 1e-6)) {
            angle = this.angle;
        }
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double hx = cos * baseLength;
        double hy = sin * baseLength;
        double px = -sin * thickness;
        double py = cos * thickness;

        double fx = this.x + hx;
        double fy = this.y + hy;
        double bx2 = this.x - hx;
        double by2 = this.y - hy;

        double flx = fx + px, fly = fy + py;
        double frx = fx - px, fry = fy - py;
        double brx = bx2 - px, bry = by2 - py;
        double blx = bx2 + px, bly = by2 + py;

        float screenBx = (float) bx;
        float screenBy = (float) by;

        // draw as two triangles (fl, fr, br) + (fl, br, bl)
        renderer.triangle((float)(flx + screenBx), (float)(fly + screenBy),
            (float)(frx + screenBx), (float)(fry + screenBy),
            (float)(brx + screenBx), (float)(bry + screenBy), bulletColor);

        renderer.triangle((float)(flx + screenBx), (float)(fly + screenBy),
            (float)(brx + screenBx), (float)(bry + screenBy),
            (float)(blx + screenBx), (float)(bly + screenBy), bulletColor);
    }

    // returns false if a bullet is to be removed
    public boolean updatePhysics(double dt, double width, double height,
                                 Ship player, Meteorite[] meteorites, int meteoriteCount, WMeteorites widget) {
        this.life -= dt;
        if (this.life <= 0) {
            return false;
        }

        if (player.getPowerup().equals(Powerups.HOMING_SHOTS)
            || (player.getPowerup().equals(Powerups.STARDUST) && player.isSniper)) {
            // find nearest meteorite within range
            Meteorite target = null;
            double bestDist2 = Bullet.HOMING_RANGE * Bullet.HOMING_RANGE;
            for (int mi = 0; mi < meteoriteCount; mi++) {
                Meteorite meteorite = meteorites[mi];
                double dx = meteorite.x - this.x;
                double dy = meteorite.y - this.y;
                double distFromCenter = Math.sqrt(dx*dx + dy*dy);

                double distFromEdge = Math.max(0.0, distFromCenter - meteorite.radius);
                if (distFromEdge * distFromEdge < bestDist2) {
                    bestDist2 = distFromEdge * distFromEdge;
                    target = meteorite;
                }
            }

            if (target != null) {
                // desired velocity toward meteorite
                double tx = target.x - this.x;
                double ty = target.y - this.y;
                double len = Math.hypot(tx, ty);
                if (len > 1e-6) {
                    boolean sniper = player.getPowerup().equals(Powerups.SNIPER)
                        || (player.getPowerup().equals(Powerups.STARDUST) && player.isSniper);
                    double multiplier = width >= 1280 ? 6.666 : 4.20;
                    double speed = sniper ? Bullet.BULLET_SPEED * multiplier : Bullet.BULLET_SPEED;

                    tx /= len; ty /= len;
                    double desiredVx = tx * speed;
                    double desiredVy = ty * speed;

                    // nudge bullet velocity towards target
                    double steer = sniper ? Bullet.HOMING_STRENGTH * 4.20 : Bullet.HOMING_STRENGTH;
                    this.vx += (desiredVx - this.vx) * steer;
                    this.vy += (desiredVy - this.vy) * steer;

                    // re-normalize to exact speed to avoid slowing down
                    double nv = Math.hypot(this.vx, this.vy);
                    if (nv > 1e-6) {
                        this.vx = (this.vx / nv) * speed;
                        this.vy = (this.vy / nv) * speed;
                    }
                }
            }
        }

        this.x += this.vx * dt;
        this.y += this.vy * dt;
        this.wrap(width, height);

        // build bullet segment endpoints, same as renderer
        double speed = Math.hypot(this.vx, this.vy);
        double angle = Math.atan2(this.vy, this.vx);
        if (Double.isNaN(angle) || (Math.abs(this.vx) < 1e-6 && Math.abs(this.vy) < 1e-6)) angle = this.angle;

        double halfLength = 2.0;
        if (this.type == BulletTypes.SNIPER) {
            double speedFactor = Math.min(3.0, speed / 50.0);
            halfLength = 4.0 * speedFactor;
        }
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double hx = cos * halfLength;
        double hy = sin * halfLength;

        double segMx = this.x + hx;
        double segMy = this.y + hy;
        double segBx = this.x - hx;
        double segBy = this.y - hy;

        double bulletThickness = 2.0;

        boolean removed = false;
        for (int midx = 0; midx < meteoriteCount; midx++) {
            Meteorite meteorite = meteorites[midx];

            double mx = meteorite.x;
            double my = meteorite.y;

            int ox = (int) Math.round((this.x - mx) / width);
            int oy = (int) Math.round((this.y - my) / height);
            double drawMx = mx + ox * width;
            double drawMy = my + oy * height;

            double d2 = CollisionUtil.distToSegmentSquared(
                drawMx, drawMy, segMx,
                segMy, segBx, segBy
            );

            double collisionRadius = meteorite.radius + bulletThickness;
            if (d2 <= collisionRadius * collisionRadius) {
                if (meteorite.isBoss) {
                    int dmg = this.type.equals(BulletTypes.SNIPER)
                        ? widget.rng.nextInt(10, 40)
                        : widget.rng.nextInt(1, 4);
                    boolean dead = meteorite.damageByBullet(dmg, widget);
                    if (dead) {
                        boolean dbl = player.getPowerup().equals(Powerups.DOUBLE_POINTS);
                        player.score += (dbl ? 200 : 100) * meteorite.size + meteorite.maxHp;
                        player.pointsWithCurrentPower += 100 * meteorite.size + meteorite.maxHp;

                        widget.splitMeteorite(midx);
                    } else {
                        meteorite.hitFlashTimer = Meteorite.HIT_FLASH_DURATION;
                        double strength = Math.min(1.0, dmg * (dmg > 10 ? 0.02 : 0.04));
                        meteorite.hitPulseStrength = Math.max(meteorite.hitPulseStrength, strength);
                        meteorite.hitPulseTimer = Meteorite.HIT_PULSE_DURATION * (0.8 + 0.8 * strength);
                    }
                    removed = !(widget.rng.nextInt(69) > 42
                        && (player.getPowerup().equals(Powerups.PIERCING_SHOTS) || this.type == BulletTypes.SNIPER)
                    );
                } else {
                    boolean dbl = player.getPowerup().equals(Powerups.DOUBLE_POINTS);

                    int baseScore = dbl ? 20 : 10;
                    player.score += baseScore * meteorite.size;
                    player.pointsWithCurrentPower += 10 * meteorite.size; // remove natural bias from double points power

                    widget.splitMeteorite(midx);
                    boolean bulletRemoves = !(widget.rng.nextInt(4) <= 2
                        && (player.getPowerup().equals(Powerups.PIERCING_SHOTS) || player.getPowerup().equals(Powerups.STARDUST))
                    );

                    if (this.type == BulletTypes.SNIPER) {
                        bulletRemoves = false;
                    }

                    if (bulletRemoves) {
                        removed = true;
                    }
                }
                break;
            }
        }

        return !removed;
    }
}
