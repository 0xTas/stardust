package dev.stardust.gui.widgets.meteorites.entity;

import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import java.util.concurrent.ThreadLocalRandom;
import dev.stardust.gui.widgets.meteorites.WMeteorites;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import dev.stardust.gui.widgets.meteorites.entity.collision.CollisionUtil;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Meteorite extends Entity {
    public static final int SIZE_BOSS = 4;
    public static final int SIZE_SMALL = 1;
    public static final int SIZE_LARGE = 3;
    public static final int SIZE_MEDIUM = 2;
    public static final int TOTAL_SIZES = 4;
    public static final double MIN_SMALL_RADIUS = 8.0;
    public static final double MIN_LARGE_RADIUS = 36.0;
    public static final double MIN_MEDIUM_RADIUS = 18.0;
    public static final double SMALL_RADIUS_MULTIPLIER = 3.0;
    public static final double MEDIUM_RADIUS_MULTIPLIER = 6.0;
    public static final double LARGE_RADIUS_MULTIPLIER = 12.0;

    // collision hull-damage tuning
    public static final double RESTITUTION = 0.18;
    public static final double MIN_DAMAGE_IMPACT = 18.0;
    public static final double LETHAL_IMPACT_SPEED = 128.0;
    public static final double HULL_BASE_MULTIPLIER = 0.06;
    public static final double METEORITE_MASS_FACTOR = 0.04;
    public static final double PLAYER_KNOCKBACK_MULTIPLIER = 1.6;
    public static final double METEORITE_KNOCKBACK_MULTIPLIER = 0.42;

    // mass model
    public static final double MASS_EXPONENT = 1.666;
    public static final double SMALL_DAMAGE_BONUS = 4.20;
    public static final double LARGE_DAMAGE_PENALTY = 0.72;
    public static final double SMALL_RADIUS_THRESHOLD = 12.0;
    public static final double MAX_SINGLE_HIT_FRACTION = 0.969;

    // boss attributes
    public static final int BOSS_CHIP_STAGES = 4;
    public static final int BOSS_BASE_HP_PER_100PX = 17;
    public static final int BOSS_MIN_CHIP_FRAGMENTS = 2;
    public static final int BOSS_MAX_CHIP_FRAGMENTS = 6;
    public static final double HIT_FLASH_DURATION = 0.12;
    public static final double HIT_PULSE_DURATION = 0.16;
    public static final int BOSS_MIN_FINAL_FRAGMENTS = 5;
    public static final int BOSS_MAX_FINAL_FRAGMENTS = 10;
    public static final double BOSS_MIN_RADIUS_FACTOR = 0.2;
    public static final double BOSS_MAX_RADIUS_FACTOR = 0.42;

    public int hp;
    public int size;
    public int maxHp;
    private int stage;
    public double radius;
    public boolean isBoss;
    public double maxJitter;
    public double hitPulseTimer;
    public double hitFlashTimer;
    public double hitPulseStrength;

    // polygon shape (relative coords around center)
    public int vertCount;
    public double[] polyX;
    public double[] polyY;

    public double rot = 0;
    public double rotSpeed = 0;

    public void set(double x, double y, double vx, double vy, double radius, boolean boss) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.isBoss = boss;
        this.radius = radius;
        this.rot = Math.random() * Math.PI * 2;
        this.rotSpeed = (Math.random() - 0.5) * 1.5;
        // subtle bias so left-moving vs right-moving rocks tend to rotate opposite directions
        this.rotSpeed += Math.signum(this.vx) * (0.05 + Math.random() * 0.18);

        if (this.radius >= MIN_LARGE_RADIUS && !this.isBoss) {
            this.size = SIZE_LARGE;
        } else if (this.radius >= MIN_MEDIUM_RADIUS) {
            this.size = SIZE_MEDIUM;
        } else if (!this.isBoss) {
            this.size = SIZE_SMALL;
        } else {
            this.size = SIZE_BOSS;
        }

        // build polygon
        this.vertCount = 8 + (int) (Math.random() * 7);
        polyX = new double[vertCount];
        polyY = new double[vertCount];

        // jitter in range 0.72..=1.18 to favor convex-ish shapes
        double max = 0.0;
        for (int n = 0; n < vertCount; n++) {
            double a = (n / (double) vertCount) * Math.PI * 2;
            double jitter = 0.777 + Math.random() * 0.46;

            double r = radius * jitter;

            polyX[n] = Math.cos(a) * r;
            polyY[n] = Math.sin(a) * r;
            if (jitter > max) max = jitter;
        }

        this.maxJitter = max;
    }

    public void set(Meteorite other) {
        this.hp = other.hp;
        this.maxHp = other.maxHp;
        this.stage = other.stage;
        this.isBoss = other.isBoss;
        this.x = other.x; this.y = other.y;
        this.vx = other.vx; this.vy = other.vy;
        this.radius = other.radius; this.rot = other.rot; this.rotSpeed = other.rotSpeed;

        if (this.radius >= MIN_LARGE_RADIUS && !this.isBoss) {
            this.size = SIZE_LARGE;
        } else if (this.radius >= MIN_MEDIUM_RADIUS) {
            this.size = SIZE_MEDIUM;
        } else if (!this.isBoss) {
            this.size = SIZE_SMALL;
        } else {
            this.size = SIZE_BOSS;
        }

        if (other.vertCount > 0 && other.polyX != null) {
            this.vertCount = other.vertCount;
            this.polyX = new double[vertCount];
            this.polyY = new double[vertCount];
            System.arraycopy(other.polyX, 0, this.polyX, 0, vertCount);
            System.arraycopy(other.polyY, 0, this.polyY, 0, vertCount);
        } else {
            this.vertCount = 0;
            this.polyX = this.polyY = null;
        }
    }

    public void becomeBoss(double x, double y, double vx, double vy, double width, double height, int wave) {
        this.isBoss = true;
        double minDim = Math.min(width, height);
        double bossRadius = Math.max(
            minDim * BOSS_MIN_RADIUS_FACTOR,
            Math.min(
                minDim * BOSS_MAX_RADIUS_FACTOR,
                minDim * BOSS_MIN_RADIUS_FACTOR * (Math.max(wave, 10) / 10.0)
            )
        );

        this.set(x, y, vx, vy, bossRadius, true);

        // choose HP proportional to radius
        this.maxHp = Math.max(40, (int) Math.round((this.radius / 100.0) * BOSS_BASE_HP_PER_100PX * 10.0));

        this.stage = 0;
        this.hp = this.maxHp;
        // bias rotation slower for huge boss
        this.rotSpeed = (Math.random() - 0.5) * 0.2 + Math.signum(vx) * 0.07;

        this.vx = vx * 0.37;
        this.vy = vy * 0.37;
    }

    public void render(GuiRenderer renderer, int bx, int by, double width, double height) {
        double rExt = this.aabbRadius();
        // try offsets -1,0,+1 for both axes -> up to 9 copies to handle screen-edge-wrapping
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                double drawX = this.x + ox * width;
                double drawY = this.y + oy * height;
                if (drawX + rExt < 0 || drawX - rExt > width || drawY + rExt < 0 || drawY - rExt > height) continue;

                double screenX = bx + drawX;
                double screenY = by + drawY;

                double innerScale  = 0.96;
                double strokeScale = 1.06;
                Color innerColor = new Color(8, 8, 12);
                Color strokeColor = new Color(255, 255, 255);
                Color highlightColor = new Color(40, 40, 48);

                // visual damage feedback
                if (this.isBoss && (this.hitFlashTimer > 0 || this.hitPulseTimer > 0)) {
                    double flashProgress = hitFlashTimer / HIT_FLASH_DURATION;
                    flashProgress = MathHelper.clamp((float) flashProgress, 0f, 1f);

                    double fade = Math.pow(flashProgress, 0.6);
                    double pulseProgress = hitPulseTimer / HIT_PULSE_DURATION;
                    pulseProgress = MathHelper.clamp((float)pulseProgress, 0f, 1f);

                    double p = 1.0 - pulseProgress;
                    double pulseEase = Math.sin(p * Math.PI);

                    double maxScaleBump = 0.037 * hitPulseStrength;
                    double scaleBump = maxScaleBump * pulseEase;

                    strokeScale = strokeScale * (1.0 + scaleBump);
                    innerScale = innerScale * (1.0 - 0.02 * pulseEase);

                    int baseStrokeR = 255, baseStrokeG = 255, baseStrokeB = 255;
                    strokeColor = new Color(baseStrokeR, baseStrokeG, baseStrokeB, 255);

                    double tintAmount = 1.0 * fade * hitPulseStrength * 4.2;
                    int innerR = (int) Math.round(lerp(innerColor.r, highlightColor.r, tintAmount));
                    int innerG = (int) Math.round(lerp(innerColor.g, highlightColor.g, tintAmount));
                    int innerB = (int) Math.round(lerp(innerColor.b, highlightColor.b, tintAmount));
                    int innerA = innerColor.a;
                    innerColor = new Color(innerR, innerG, innerB, innerA);
                }

                // render polygon using triangle fan around center (0,0)
                // transform each vertex by rotation then translate to screenX/screenY
                double cos = Math.cos(this.rot);
                double sin = Math.sin(this.rot);

                if (this.vertCount >= 3) {
                    // fan triangles using scaled outer vertices
                    double v0x = screenX + ((this.polyX[0] * strokeScale) * cos - (this.polyY[0] * strokeScale) * sin);
                    double v0y = screenY + ((this.polyX[0] * strokeScale) * sin + (this.polyY[0] * strokeScale) * cos);

                    for (int v = 1; v < this.vertCount - 1; v++) {
                        double v1x = screenX + ((this.polyX[v] * strokeScale) * cos - (this.polyY[v] * strokeScale) * sin);
                        double v1y = screenY + ((this.polyX[v] * strokeScale) * sin + (this.polyY[v] * strokeScale) * cos);
                        double v2x = screenX + ((this.polyX[v + 1] * strokeScale) * cos - (this.polyY[v + 1] * strokeScale) * sin);
                        double v2y = screenY + ((this.polyX[v + 1] * strokeScale) * sin + (this.polyY[v + 1] * strokeScale) * cos);

                        renderer.triangle(v0x, v0y, v1x, v1y, v2x, v2y, strokeColor);
                    }

                    double iv0x = screenX + ((this.polyX[0] * innerScale) * cos - (this.polyY[0] * innerScale) * sin);
                    double iv0y = screenY + ((this.polyX[0] * innerScale) * sin + (this.polyY[0] * innerScale) * cos);

                    for (int v = 1; v < this.vertCount - 1; v++) {
                        double iv1x = screenX + ((this.polyX[v] * innerScale) * cos - (this.polyY[v] * innerScale) * sin);
                        double iv1y = screenY + ((this.polyX[v] * innerScale) * sin + (this.polyY[v] * innerScale) * cos);
                        double iv2x = screenX + ((this.polyX[v + 1] * innerScale) * cos - (this.polyY[v + 1] * innerScale) * sin);
                        double iv2y = screenY + ((this.polyX[v + 1] * innerScale) * sin + (this.polyY[v + 1] * innerScale) * cos);

                        renderer.triangle(iv0x, iv0y, iv1x, iv1y, iv2x, iv2y, innerColor);
                    }
                }
            }
        }
    }

    public void updatePhysics(
        double dt, double width, double height, double[] shipX, double[] shipY,
        int count, Ship player, Meteorite[] meteorites, WMeteorites widget
    ) {

        if (hitFlashTimer > 0.0) {
            hitFlashTimer -= dt;
            if (hitFlashTimer < 0.0) hitFlashTimer = 0.0;
        }
        if (hitPulseTimer > 0.0) {
            hitPulseTimer -= dt;
            if (hitPulseTimer <= 0.0) {
                hitPulseTimer = 0.0;
                hitPulseStrength = 0.0;
            }
        }

        this.x += this.vx * dt;
        this.y += this.vy * dt;

        this.wrap(width, height);
        this.updateRotation(dt);
        if (player.getPowerup().equals(Powerups.GRAVITY_WELL) && player.gravityWellDeployed) {
            double gx = player.gravityWellX;
            double gy = player.gravityWellY;

            double falloff = Ship.GW_FALLOFF;
            double falloff2 = falloff * falloff;
            double maxTurnPerTick = Ship.GW_MAX_TURN_RADS * dt;

            for (int n = 0; n < count; n++) {
                Meteorite meteorite2 = meteorites[n];

                // compute vector from meteorite -> well center
                // compute wrapped difference (well - meteorite) on both axes
                double dx = wrappedDelta(gx - meteorite2.x, width);
                double dy = wrappedDelta(gy - meteorite2.y, height);
                double dist2 = dx * dx + dy * dy;
                if (dist2 < 1e-8) continue;
                if (dist2 > falloff2) continue;
                double dist = Math.sqrt(dist2);

                // normalize radial vector (pointing from meteorite to well center)
                double rx = dx / dist;
                double ry = dy / dist;

                // tangent vector (perpendicular) for orbit
                double tx = -ry;

                // mostly tangent (orbit) + slight inward radial component
                double desiredX = tx + rx * Ship.GW_RADIAL_BIAS;
                double desiredY = rx + ry * Ship.GW_RADIAL_BIAS;

                // normalize desired direction
                double dLen = Math.hypot(desiredX, desiredY);
                if (dLen < 1e-8) continue;
                desiredX /= dLen;
                desiredY /= dLen;

                // scale effect by distance and mass
                double proximityFactor = 1.0 - (dist / falloff); // 1 at center -> 0 at edge
                if (proximityFactor < 0) proximityFactor = 0;
                double massScale = 1.0 / (1.0 + meteorite2.size * Ship.GW_METEORITE_SIZE_MASS_FACTOR);
                double effectScale = Ship.GW_STRENGTH * proximityFactor * massScale;

                // compute the current velocity direction & speed
                double vx = meteorite2.vx;
                double vy = meteorite2.vy;
                double speed = Math.hypot(vx, vy);
                if (speed < 1e-6) {
                    // if meteorite is nearly stationary, give it a small tangential kick
                    double kickSpeed = 42.0;
                    meteorite2.vx = desiredX * kickSpeed * effectScale;
                    meteorite2.vy = desiredY * kickSpeed * effectScale;
                    continue;
                }

                double curDirX = vx / speed;
                double curDirY = vy / speed;

                // compute signed angle from current direction -> desired direction
                // angle = atan2(cross, dot) where cross = cur x desired, dot = cur·desired
                double dot = curDirX * desiredX + curDirY * desiredY;
                dot = Math.max(-1.0, Math.min(1.0, dot));
                double cross = curDirX * desiredY - curDirY * desiredX;
                double angleToDesired = Math.atan2(cross, dot); // signed shortest angle

                // clamp angle change per tick based on effectScale
                double maxDelta = maxTurnPerTick * Math.max(0.2, effectScale);
                double deltaAngle = Math.max(-maxDelta, Math.min(maxDelta, angleToDesired));

                // rotate current velocity direction by deltaAngle
                double cosA = Math.cos(deltaAngle);
                double sinA = Math.sin(deltaAngle);
                double newDirX = curDirX * cosA - curDirY * sinA;
                double newDirY = curDirX * sinA + curDirY * cosA;

                // apply, keeping speed magnitude
                meteorite2.vx = newDirX * speed;
                meteorite2.vy = newDirY * speed;
                double newSpeed = Math.hypot(meteorite2.vx, meteorite2.vy);
                if (newSpeed > Ship.GW_MAX_METEORITE_SPEED) {
                    double s = Ship.GW_MAX_METEORITE_SPEED / newSpeed;
                    meteorite2.vx *= s;
                    meteorite2.vy *= s;
                }
            }
        }

        // skip player/meteorite collision checks if phase shifted
        if (player.getPowerup().equals(Powerups.PHASE_SHIFT) && player.phaseActive) return;

        // try wrap offsets -1,0,+1 (same as render) to find the nearest copy that might intersect
        boolean collidedAny = false;
        CollisionUtil.CollisionResult best = null;

        // cheap bounding radius for early-out
        double shipBoundRadius = 10; // conservative, roughly half the ship's extent
        double earlyR = this.aabbRadius() + shipBoundRadius;

        for (int ox = -1; ox <= 1 && !collidedAny; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                double drawX = this.x + ox * width;
                double drawY = this.y + oy * height;

                // cheap screen-space cull
                double dxC = drawX - player.x;
                double dyC = drawY - player.y;
                if (dxC*dxC + dyC*dyC > earlyR * earlyR) continue;

                // build meteorite world vertices transformed by rotation & translated to drawX/drawY
                int vc = this.vertCount;
                double[] mWorldX = new double[vc];
                double[] mWorldY = new double[vc];
                double cos = Math.cos(this.rot);
                double sin = Math.sin(this.rot);
                for (int v = 0; v < vc; v++) {
                    double rx = this.polyX[v];
                    double ry = this.polyY[v];
                    mWorldX[v] = drawX + (rx * cos - ry * sin);
                    mWorldY[v] = drawY + (rx * sin + ry * cos);
                }

                CollisionUtil.CollisionResult result = CollisionUtil.satPolygonCollision(
                    shipX, shipY, mWorldX, mWorldY, vc
                );

                if (result.collided) {
                    collidedAny = true;
                    best = result;
                    break;
                }
            }
        }

        if (!collidedAny) return;

        if (player.iFrames > 0
            || player.getPowerup().equals(Powerups.HIGH_TECH_HULL)
            || player.getPowerup().equals(Powerups.REINFORCED_HULL)) {

            // impact speed along normal (positive = closing)
            double nx = best.nx, ny = best.ny;
            double impactSpeed = Math.abs((this.vx - player.vx) * nx + (this.vy - player.vy) * ny);
            double meteoriteMass = Math.pow(this.radius, MASS_EXPONENT) * METEORITE_MASS_FACTOR + 1e-6;

            double invMassP = 1.0 / Ship.SHIP_MASS;
            double invMassM = 1.0 / meteoriteMass;

            // fast collisions are lethal regardless of hull or meteorite size
            if (impactSpeed >= LETHAL_IMPACT_SPEED && player.iFrames <= 0) {
                player.onHit(width, height, widget);
                return;
            }

            if (impactSpeed < MIN_DAMAGE_IMPACT) {
                this.x += nx * 2.0;
                this.y += ny * 2.0;
                this.vx += nx * 6.0;
                this.vy += ny * 6.0;

                if (player.iFrames <= 0) {
                    widget.playSound(
                        SoundEvents.ENTITY_PLAYER_SMALL_FALL,
                        ThreadLocalRandom.current().nextFloat(0.666f, 1.1337f)
                    );
                }
                player.iFrames = Ship.IFRAMES_ON_ASTEROID_COLLISION;

                return;
            }

            double baseDamage = HULL_BASE_MULTIPLIER * impactSpeed * (meteoriteMass * 0.5);

            double sizeBias = 1.0;
            if (this.radius < SMALL_RADIUS_THRESHOLD) {
                if (impactSpeed >= LETHAL_IMPACT_SPEED * 0.5) {
                    sizeBias = SMALL_DAMAGE_BONUS;
                } else {
                    sizeBias = SMALL_DAMAGE_BONUS * 0.5;
                }
            } else if (this.radius >= MIN_LARGE_RADIUS) sizeBias = LARGE_DAMAGE_PENALTY;

            double damage = baseDamage * sizeBias;

            damage = Math.max(1.0, damage);
            double maxPerHit = Ship.MAX_HULL_HEALTH * MAX_SINGLE_HIT_FRACTION;
            if (damage > maxPerHit) damage = maxPerHit;

            if (player.iFrames <= 0) {
                player.damageHull(damage);
                widget.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.9f);
            } else {
                widget.playSound(
                    SoundEvents.ENTITY_PLAYER_BIG_FALL, 0.7f,
                    widget.module.soundVolume.get().floatValue() * 0.66f
                );
            }

            // compute j using restitution and inverse masses
            double j = - (1.0 + RESTITUTION) * ( (this.vx - player.vx) * nx + (this.vy - player.vy) * ny );
            // normalize by (invMassP + invMassM) but careful: we want magnitude
            double denom = (invMassP + invMassM);
            if (denom <= 0) denom = 1e-6;
            j = j / denom;

            // ensure j is positive magnitude
            if (j < 0) j = -j;

            // player gets negative along normal (pushed away from meteorite)
            player.vx -= nx * j * invMassP * PLAYER_KNOCKBACK_MULTIPLIER;
            player.vy -= ny * j * invMassP * PLAYER_KNOCKBACK_MULTIPLIER;

            // meteorite gets small opposite impulse
            this.vx += nx * j * invMassM * METEORITE_KNOCKBACK_MULTIPLIER;
            this.vy += ny * j * invMassM * METEORITE_KNOCKBACK_MULTIPLIER;

            // small positional separation so they don't immediately re-collide
            double pushOut = best.penetration + 1.4;
            if (pushOut > 0) {
                this.x += nx * pushOut * 0.7;
                this.y += ny * pushOut * 0.7;
                player.x -= nx * pushOut * 0.3;
                player.y -= ny * pushOut * 0.3;
            }
            player.iFrames = Ship.IFRAMES_ON_ASTEROID_COLLISION;

            if (player.hull <= 0) {
                player.onHit(width, height, widget);
            } else {
                return;
            }
        }

        player.onHit(width, height, widget);
    }

    public boolean damageByBullet(int damageAmount, WMeteorites widget) {
        if (!isBoss) {
            return false;
        }

        this.hp -= damageAmount;
        widget.playSound(
            widget.rng.nextBoolean() ? SoundEvents.BLOCK_ANCIENT_DEBRIS_BREAK : SoundEvents.BLOCK_DEEPSLATE_BREAK,
            widget.rng.nextFloat(1.137f, 2.1f),
            widget.module.soundVolume.get().floatValue() * 0.42f
        );
        if (widget.rng.nextDouble() <= 0.1337) {
            widget.playSound(
                SoundEvents.BLOCK_ANVIL_LAND,
                widget.rng.nextFloat(3.333f, 4.420f),
                widget.module.soundVolume.get().floatValue() * 0.1337f
            );
        }

        double fraction = (double) this.hp / (double) this.maxHp;
        int newStage = (int) Math.floor((1.0 - fraction) * BOSS_CHIP_STAGES);
        if (newStage > stage) {
            for (int s = stage + 1; s <= newStage; s++) {
                int fragments = widget.rng.nextInt(BOSS_MIN_CHIP_FRAGMENTS, BOSS_MAX_CHIP_FRAGMENTS + 1);
                // scale fragment amount by stage & boss wave
                fragments = (int) Math.round(fragments * (1.0 + s * (widget.wave / 10.0)));
                this.chipOff(fragments, widget);
            }
            stage = newStage;
            widget.playSound(
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                widget.rng.nextFloat(0.8f, 1.1f),
                widget.module.soundVolume.get().floatValue() * 0.420f
            );
        }

        if (this.hp <= 0) {
            this.explode(widget);
            return true;
        }

        return false;
    }

    private void chipOff(int nFragments, WMeteorites widget) {
        if (nFragments <= 0) return;
        widget.playSound(
            WMeteorites.BREAK_SOUNDS.get(widget.rng.nextInt(WMeteorites.BREAK_SOUNDS.size())),
            ThreadLocalRandom.current().nextFloat(0.42f, 0.69f)
        );

        for (int n = 0; n < nFragments; n++) {
            double angle = widget.rng.nextDouble(0, Math.PI * 2.0);

            double fragRadius;
            double r = widget.rng.nextDouble();
            if (r < 0.25) {
                fragRadius = MIN_LARGE_RADIUS + widget.rng.nextDouble() * LARGE_RADIUS_MULTIPLIER;
            } else if (r < 0.45) {
                fragRadius = MIN_MEDIUM_RADIUS + widget.rng.nextDouble() * MEDIUM_RADIUS_MULTIPLIER;
            } else {
                fragRadius = MIN_SMALL_RADIUS + widget.rng.nextDouble() * SMALL_RADIUS_MULTIPLIER;
            }

            double spawnOffsetMin = 0.666;
            double spawnOffsetMax = 0.777;
            double speedMin = 47.0 + widget.wave;
            double speedMax = (100.0 * Math.min(1.0, this.radius / 120.0)) + widget.wave;
            widget.spawnChildMeteorite(this, angle, speedMin, speedMax, spawnOffsetMin, spawnOffsetMax, fragRadius);
        }
    }

    private void explode(WMeteorites widget) {
        widget.playSound(
            SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
            widget.rng.nextFloat(0.8f, 1.15f)
        );
        int finalCount = widget.rng.nextInt(BOSS_MIN_FINAL_FRAGMENTS, BOSS_MAX_FINAL_FRAGMENTS + 1) * (widget.wave / 10);

        for (int n = 0; n < finalCount; n++) {
            double angle = widget.rng.nextDouble(0, Math.PI * 2.0);

            double fragRadius;
            double r = widget.rng.nextDouble();
            if (r < 0.42) {
                fragRadius = MIN_LARGE_RADIUS + widget.rng.nextDouble() * LARGE_RADIUS_MULTIPLIER;
            } else if (r < 0.69) {
                fragRadius = MIN_MEDIUM_RADIUS + widget.rng.nextDouble() * MEDIUM_RADIUS_MULTIPLIER;

            } else {
                fragRadius = MIN_SMALL_RADIUS + widget.rng.nextDouble() * SMALL_RADIUS_MULTIPLIER;
            }

            double spawnOffsetMin = 0.069;
            double spawnOffsetMax = 0.777;
            double speedMin = 69.0 + widget.wave;
            double speedMax = (242.0 * Math.min(1.0, this.radius / 120.0)) + widget.wave;
            widget.spawnChildMeteorite(this, angle, speedMin, speedMax, spawnOffsetMin, spawnOffsetMax, fragRadius);
        }

        this.isBoss = false;
    }

    public void updateRotation(double dt) {
        rot += rotSpeed * dt;
    }

    public double aabbRadius() {
        return Math.max(radius * maxJitter, 4.0);
    }

    public static double wrappedDelta(double targetMinusSource, double span) {
        // targetMinusSource = (targetCoord - sourceCoord)
        // normalize into (-span/2, +span/2]
        double d = targetMinusSource;
        double half = span * 0.5;
        while (d >  half) d -= span;
        while (d <= -half) d += span;
        return d;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * MathHelper.clamp((float)t, 0f, 1f);
    }
}
