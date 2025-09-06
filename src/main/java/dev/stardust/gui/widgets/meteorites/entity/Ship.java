package dev.stardust.gui.widgets.meteorites.entity;

import java.util.*;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import dev.stardust.gui.widgets.meteorites.WMeteorites;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Ship extends Entity {
    public static final double SHIP_MASS = 0.25;
    public static final double SHIP_FRICTION = 0.98;
    public static final double SHOOT_COOLDOWN = 0.18;
    public static final double MAX_HULL_HEALTH = 100.0;
    public static final double SHIP_ROT_SPEED = Math.toRadians(250);

    public static final int HYPERSPACE_ATTEMPTS = 25;
    public static final double RESPAWN_SAFE_DIST = 80;
    public static final int SAFE_RESPAWN_ATTEMPTS = 50;
    public static final int HYPERSPACE_SCORE_COST = 1000;
    public static final double HYPERSPACE_SAFE_DIST = 90;
    public static final double WAVE_SPAWN_SAFE_DIST = 140;
    public static final double CALIBRATED_WARP_COOLDOWN = 3.0;

    public static final double IFRAMES_ON_RESPAWN = 1.8;
    public static final double IFRAMES_ON_HYPERJUMP = 1.2;
    public static final double IFRAMES_ON_EXIT_PHASE = 0.42;
    public static final double IFRAMES_ON_ASTEROID_COLLISION = 0.42;

    public static final double GW_FALLOFF = 69.0;
    public static final double GW_STRENGTH = 42.69;
    public static final double GW_RADIAL_BIAS = 0.069; // small inward pull vs tangential (0 = pure orbit)
    public static final double GW_MAX_TURN_RADS = Math.PI * 2.0;
    public static final double GW_METEORITE_SIZE_MASS_FACTOR = 0.75;
    public static final double GW_MAX_METEORITE_SPEED = Meteorite.LETHAL_IMPACT_SPEED;

    public static final double SHOTGUN_SPREAD_DEGREES = 30.0;
    public static final double SHOTGUN_SPEED_MULTIPLIER = 2.0;
    public static final double SHOTGUN_LIFETIME_MULTIPLIER = 1.0 / 3.0;
    public static final double SHOTGUN_JITTER_RAD = Math.toRadians(6.0);
    public static final double SHOTGUN_SPREAD_RAD = Math.toRadians(SHOTGUN_SPREAD_DEGREES);

    public static final int MIDAS_TOUCH_BULLET_COST = 50;
    public static final int MIDAS_TOUCH_REWARD_MULTIPLIER = 4;

    Powerups powerUp;
    public long lastHyperJump;
    private boolean jumpCooling;
    public long lastMidasRejectSound;
    public double shootCooldownTimer;
    public int pointsWithCurrentPower;
    public double angle = -Math.PI / 2; // facing upwards
    public double hull = MAX_HULL_HEALTH;
    public final Map<Powerups, Integer> powerMap = new HashMap<>();
    public boolean rotLeft, rotRight, thrusting, entropy, invulnerable, isShotgun, isSniper;

    public double phaseTimer;
    public boolean phaseActive;
    public double phaseCooldownTimer;
    public final double phaseDuration = 2.0;
    public final double phaseCooldown = 2.0 + IFRAMES_ON_EXIT_PHASE;

    public double gravityWellX;
    public double gravityWellY;
    public double gravityWellTimer;
    public double gravityWellCdTimer;
    public boolean gravityWellDeployed;
    public final double gravityWellCooldown = 5.0;
    public final double gravityWellDuration = 10.0;

    public int lives = 3;
    public int score = 0;
    public double iFrames;
    public final double thrustStrength;

    public Ship(double x, double y, double thrust) {
        this.x = x;
        this.y = y;
        powerUp = Powerups.NONE;
        this.thrustStrength = thrust;
    }

    public Powerups getPowerup() {
        return powerUp;
    }
    public void setPowerup(Powerups powerUp) {
        this.powerUp = powerUp;
    }

    public void resetHull() {
        hull = MAX_HULL_HEALTH;
    }
    public boolean isHullFull() {
        return hull >= 99.45;
    }
    public void damageHull(double dmg) {
        hull -= dmg;
        if (hull < 0) hull = 0;
    }
    public void healHull(double health) {
        hull += health;
        if (hull > MAX_HULL_HEALTH) hull = MAX_HULL_HEALTH;
    }

    public boolean hasEntropy() {
        return entropy;
    }
    public void setEntropy(boolean entropy) {
        this.entropy = entropy;
    }

    public void resetPhase() {
        phaseTimer = 0;
        phaseActive = false;
        phaseCooldownTimer = 0;
    }

    public void creditPowerup() {
        powerMap.computeIfPresent(
            hasEntropy() ? Powerups.ENTROPY : getPowerup(),
            (k, pointsEarned) -> pointsEarned + pointsWithCurrentPower
        );
        powerMap.computeIfAbsent(
            hasEntropy() ? Powerups.ENTROPY : getPowerup(),
            pwr -> pointsWithCurrentPower
        );
        pointsWithCurrentPower = 0;
    }

    public void resetPowerup() {
        resetGun();
        resetHull();
        resetPhase();
        setEntropy(false);
        resetGravityWell();
        setPowerup(Powerups.NONE);
    }

    public void resetGun() {
        isSniper = false;
        isShotgun = false;
    }

    public void resetGravityWell() {
        gravityWellX = 0;
        gravityWellY = 0;
        gravityWellTimer = 0;
        gravityWellCdTimer = 0;
        gravityWellDeployed = false;
    }

    public void render(GuiRenderer renderer, int bx, int by, SettingColor shipColor, SettingColor flameColor) {
        double sx = this.x + bx;
        double sy = this.y + by;
        double angle = this.angle;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double s1x = sx + cos * 10;
        double s1y = sy + sin * 10;
        double s2x = sx + Math.cos(angle + Math.PI * 0.75) * 8;
        double s2y = sy + Math.sin(angle + Math.PI * 0.75) * 8;
        double s3x = sx + Math.cos(angle - Math.PI * 0.75) * 8;
        double s3y = sy + Math.sin(angle - Math.PI * 0.75) * 8;

        if (this.thrusting) {
            boolean strong = this.getPowerup().equals(Powerups.THRUSTER_UPGRADES) || this.getPowerup().equals(Powerups.STARDUST);
            double thrustStrength = strong ? 12 : 10;
            double fx = sx - cos * thrustStrength;
            double fy = sy - sin * thrustStrength;
            renderer.triangle(fx, fy, s2x, s2y, s3x, s3y, flameColor);
        }
        renderer.triangle(s1x, s1y, s2x, s2y, s3x, s3y, shipColor);
    }

    public void updatePhysics(
        double dt, double width, double height,
        double aimMouseLocalX, double aimMouseLocalY, WMeteorites widget
    ) {

        boolean strong = this.getPowerup().equals(Powerups.THRUSTER_UPGRADES)
            || this.getPowerup().equals(Powerups.STARDUST);
        double rotSpeed = strong ? Math.toRadians(269) : Ship.SHIP_ROT_SPEED;
        if (this.rotLeft) this.angle -= rotSpeed * dt;
        if (this.rotRight) this.angle += rotSpeed * dt;

        if (widget.module.mouseAim.get() && !this.rotLeft && !this.rotRight
            && (widget.mouseInBounds || widget.module.mouseAimOutside.get())) {
            double target = Math.atan2(aimMouseLocalY - this.y, aimMouseLocalX - this.x);

            if (this.getPowerup().equals(Powerups.PRECISION_AIM)
                || this.getPowerup().equals(Powerups.STARDUST) || this.getPowerup().equals(Powerups.SNIPER)) {
                this.angle = target;
            } else {
                double delta = shortestAngleDiff(target, this.angle);
                double maxTurn = Ship.SHIP_ROT_SPEED * dt;
                if (Math.abs(delta) <= maxTurn) {
                    this.angle = target;
                } else {
                    this.angle += Math.signum(delta) * maxTurn;
                }
            }
        }

        if (this.invulnerable) this.iFrames = Double.MAX_VALUE;
        if (this.iFrames > 0) {
            this.iFrames -= dt;
            if (this.iFrames < 0) this.iFrames = 0;
        }

        if (this.shootCooldownTimer > 0) {
            this.shootCooldownTimer -= dt;
        }

        if (this.getPowerup().equals(Powerups.HIGH_TECH_HULL)) {
            this.healHull(dt);
        }

        if (this.getPowerup().equals(Powerups.GRAVITY_WELL)) {
            if (this.gravityWellDeployed) {
                this.gravityWellTimer -= dt;
                if (this.gravityWellTimer <= 0) {
                    this.gravityWellDeployed = false;
                    this.gravityWellCdTimer = this.gravityWellCooldown;
                    widget.playSound(
                        SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP,
                        widget.rng.nextFloat(0.77f, 1.1337f)
                    );
                }
            } else if (this.gravityWellTimer <= 0 && this.gravityWellCdTimer > 0) {
                this.gravityWellCdTimer -= dt;
                if (this.gravityWellCdTimer <= 0) {
                    this.gravityWellCdTimer = 0;
                    widget.playSound(
                        SoundEvents.BLOCK_VAULT_INSERT_ITEM,
                        widget.rng.nextFloat(0.77f, 1.1337f)
                    );
                }
            }
        }

        if (this.getPowerup().equals(Powerups.PHASE_SHIFT)) {
            if (this.phaseActive) {
                this.phaseTimer += dt;
                if (this.phaseTimer >= this.phaseDuration) {
                    this.phaseTimer = 0.0;
                    this.phaseActive = false;
                    this.iFrames = Ship.IFRAMES_ON_EXIT_PHASE;
                    this.phaseCooldownTimer = this.phaseCooldown;
                    widget.playSound(SoundEvents.ENTITY_PLAYER_TELEPORT, 0.69f);
                }
            } else if (this.phaseCooldownTimer >= 0) {
                this.phaseCooldownTimer -= dt;
                if (this.phaseCooldownTimer <= 0) {
                    widget.playSound(
                        SoundEvents.BLOCK_VAULT_INSERT_ITEM,
                        widget.rng.nextFloat(0.77f, 1.1337f)
                    );
                }
            }
        }

        if (this.jumpCooling) {
            if (this.getPowerup().equals(Powerups.CALIBRATED_FSD)) {
                if (System.currentTimeMillis() - this.lastHyperJump >= CALIBRATED_WARP_COOLDOWN * 1000) {
                    this.jumpCooling = false;
                }
            } else if (this.isShotgun && this.getPowerup().equals(Powerups.STARDUST)) {
                if (System.currentTimeMillis() - this.lastHyperJump >= 1000) {
                    this.jumpCooling = false;
                }
            } else if (!this.getPowerup().equals(Powerups.SUPERCHARGED_FSD)) {
                if (System.currentTimeMillis() - this.lastHyperJump >= (IFRAMES_ON_HYPERJUMP * 3) * 1000) {
                    this.jumpCooling = false;
                }
            }
            if (!this.jumpCooling) {
                widget.playSound(
                    SoundEvents.BLOCK_VAULT_INSERT_ITEM,
                    widget.rng.nextFloat(0.77f, 1.1337f)
                );
            }
        }

        if (this.thrusting) {
            double thrustStrength = strong ? this.thrustStrength * 2 : this.thrustStrength;
            this.vx += Math.cos(this.angle) * thrustStrength * dt;
            this.vy += Math.sin(this.angle) * thrustStrength * dt;

            widget.playThrustInstance();
        } else {
            widget.killThrustInstance();
        }

        this.vx *= Math.pow(Ship.SHIP_FRICTION, dt * 60.0);
        this.vy *= Math.pow(Ship.SHIP_FRICTION, dt * 60.0);

        this.x += this.vx * dt;
        this.y += this.vy * dt;
        this.wrap(width, height);
    }

    public void onHit(double width, double height, WMeteorites widget) {
        this.lives--;
        if (this.lives > 0) {
            if (!widget.CHEAT_MODE) {
                this.setEntropy(false);
                if (this.getPowerup().equals(Powerups.STARDUST)) {
                    widget.deInitStardustColor();
                } else if (this.getPowerup().equals(Powerups.MIDAS_TOUCH)) {
                    widget.deInitMidasColor();
                }
                this.creditPowerup();
                this.resetPowerup();
            } else {
                this.resetHull();
                this.resetPhase();
                this.resetGravityWell();
            }

            double[] safe = this.findSafePosition(
                width, height, Ship.RESPAWN_SAFE_DIST,
                30, Ship.SAFE_RESPAWN_ATTEMPTS, widget
            );

            if (safe != null) {
                this.x = safe[0];
                this.y = safe[1];
            } else {
                this.x = width / 2;
                this.y = height / 2;
                widget.clearMeteoritesAround(this.x, this.y);
            }

            this.vx = 0;
            this.vy = 0;
            if (widget.module.renderStarfield.get()) {
                double dt = widget.gameTimeSecs + (widget.accumulator / 1e9);
                widget.starfield.resetSmooth(this.x, this.y, 0.69, dt);
            }

            this.iFrames = Ship.IFRAMES_ON_RESPAWN;
            if (!getPowerup().equals(Powerups.CALIBRATED_FSD) && (!getPowerup().equals(Powerups.STARDUST) || !isSniper)) {
                this.lastHyperJump = System.currentTimeMillis();
            }
            widget.playSound(SoundEvents.ENTITY_PLAYER_HURT, widget.rng.nextFloat(0.77f, 1.1337f));
        } else {
            this.vx = this.vy = 0;
            widget.gameOver = true;
            widget.playSound(SoundEvents.ENTITY_PLAYER_DEATH, widget.rng.nextFloat(0.77f, 1.1337f));
            widget.playSound(
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                widget.rng.nextFloat(0.77f, 1.1337f)
            );

            this.creditPowerup();
            widget.updateHighScore();
        }
    }

    public void tryToShoot(WMeteorites widget) {
        if (this.getPowerup().equals(Powerups.MIDAS_TOUCH)) {
            if (this.score < MIDAS_TOUCH_BULLET_COST) {
                long now = System.currentTimeMillis();
                if (now - this.lastMidasRejectSound >= 1337) {
                    this.lastMidasRejectSound = now;
                    widget.playSound(
                        SoundEvents.ENTITY_VILLAGER_NO,
                        widget.rng.nextFloat(0.969f, 1.1337f)
                    );
                }
                return;
            }
        }

        boolean sniper = this.getPowerup().equals(Powerups.SNIPER)
            || (this.getPowerup().equals(Powerups.STARDUST) && this.isSniper);
        boolean shotgun = this.getPowerup().equals(Powerups.SHOTGUN)
            || (this.getPowerup().equals(Powerups.STARDUST) && this.isShotgun);
        boolean hell = this.getPowerup().equals(Powerups.BULLET_HELL)
            || (this.getPowerup().equals(Powerups.STARDUST) && !shotgun && !sniper);

        int capacityLimit = hell ? widget.MAX_BULLETS * 4 : shotgun ? widget.MAX_BULLETS * 2 : widget.MAX_BULLETS;
        if (widget.bulletCount >= capacityLimit) return;
        int availableSlots = capacityLimit - widget.bulletCount;

        int pelletCount = 1;
        if (shotgun) {
            pelletCount = widget.rng.nextInt(4, 13);
        }

        if (pelletCount > availableSlots) pelletCount = availableSlots;
        if (pelletCount <= 0) return;

        if (!shotgun) {
            Bullet b = widget.bullets[widget.bulletCount++];
            double multiplier = widget.width >= 1280 ? 6.666 : 4.20;
            double speed = sniper ? Bullet.BULLET_SPEED * multiplier : Bullet.BULLET_SPEED;
            double bulletLifetime = hell ? Bullet.BULLET_LIFETIME * 4 : Bullet.BULLET_LIFETIME;

            if (hell) speed *= 1.5;
            if (this.getPowerup().equals(Powerups.HOMING_SHOTS)) bulletLifetime *= 2;
            else if (this.getPowerup().equals(Powerups.STARDUST) && this.isSniper) bulletLifetime *= 1.337;

            b.set(
                sniper ? BulletTypes.SNIPER : BulletTypes.NORMAL,
                this.x + Math.cos(this.angle) * 12,
                this.y + Math.sin(this.angle) * 12,
                Math.cos(this.angle) * speed + this.vx,
                Math.sin(this.angle) * speed + this.vy,
                bulletLifetime, this.angle
            );

            boolean rapid = this.getPowerup().equals(Powerups.RAPID_FIRE)
                || (this.getPowerup().equals(Powerups.STARDUST) && !this.isSniper);
            this.shootCooldownTimer = rapid
                ? Ship.SHOOT_COOLDOWN / 2.0
                : sniper ? Ship.SHOOT_COOLDOWN * 4.2 : Ship.SHOOT_COOLDOWN;

            widget.playSound(
                SoundEvents.ITEM_CROSSBOW_SHOOT,
                widget.rng.nextFloat(3.3333f, 4.2f),
                widget.module.soundVolume.get().floatValue() * (sniper ? 0.69f : 0.333f)
            );

            if (this.getPowerup().equals(Powerups.MIDAS_TOUCH)) {
                this.score -= MIDAS_TOUCH_BULLET_COST;
            }
            return;
        }

        final double baseAngle = this.angle;
        final double recoilPerSpeed = this.getPowerup().equals(Powerups.STARDUST) ? 0.00666 : 0.00969;

        if (pelletCount == 1) {
            Bullet b = widget.bullets[widget.bulletCount++];
            double speed = Bullet.BULLET_SPEED * Ship.SHOTGUN_SPEED_MULTIPLIER * (0.9 + widget.rng.nextDouble() * 0.2);
            b.set(BulletTypes.NORMAL,this.x + Math.cos(baseAngle) * 12, this.y + Math.sin(baseAngle) * 12,
                Math.cos(baseAngle) * speed + this.vx, Math.sin(baseAngle) * speed + this.vy,
                Bullet.BULLET_LIFETIME * Ship.SHOTGUN_LIFETIME_MULTIPLIER, this.angle);
        } else {
            double spacing = Ship.SHOTGUN_SPREAD_RAD / Math.max(1, pelletCount - 1);
            double half = Ship.SHOTGUN_SPREAD_RAD * 0.5;

            double sumBulletSpeed = 0.0;
            for (int n = 0; n < pelletCount; n++) {
                double angle = baseAngle - half + n * spacing + (widget.rng.nextDouble() * 2.0 - 1.0) * Ship.SHOTGUN_JITTER_RAD;

                double speed = Bullet.BULLET_SPEED * (Ship.SHOTGUN_SPEED_MULTIPLIER * (0.85 + widget.rng.nextDouble() * 0.3));
                sumBulletSpeed += speed;

                Bullet b = widget.bullets[widget.bulletCount++];
                b.set(BulletTypes.NORMAL,
                    this.x + Math.cos(angle) * 12,
                    this.y + Math.sin(angle) * 12,
                    Math.cos(angle) * speed + this.vx,
                    Math.sin(angle) * speed + this.vy,
                    Bullet.BULLET_LIFETIME * Ship.SHOTGUN_LIFETIME_MULTIPLIER, this.angle
                );
            }

            double avgSpeed = sumBulletSpeed / pelletCount;
            double recoil = avgSpeed * pelletCount * recoilPerSpeed;

            this.vx -= Math.cos(baseAngle) * recoil;
            this.vy -= Math.sin(baseAngle) * recoil;
        }

        boolean rapid = this.getPowerup().equals(Powerups.RAPID_FIRE)
            || this.getPowerup().equals(Powerups.STARDUST);
        this.shootCooldownTimer = rapid ? Ship.SHOOT_COOLDOWN / 2.0 : Ship.SHOOT_COOLDOWN * 5.25;

        float pitch = widget.rng.nextFloat(1.666f, 2.1337f);
        float pitch2 = widget.rng.nextFloat(1.666f, 2.1337f);
        widget.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, pitch, widget.module.soundVolume.get().floatValue() * 0.42f);
        widget.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, pitch2, widget.module.soundVolume.get().floatValue() * 0.69f);
    }

    public void doHyperspaceJump(boolean poweredUp, boolean calibrated, double width, double height, WMeteorites widget) {
        if (calibrated) {
            this.x = widget.aimMouseLocalX;
            this.y = widget.aimMouseLocalY;
        } else {
            double[] safe = findSafePosition(
                width, height, Ship.HYPERSPACE_SAFE_DIST, 25, Ship.HYPERSPACE_ATTEMPTS, widget
            );

            if (safe != null) {
                this.x = safe[0];
                this.y = safe[1];
            } else {
                this.x = width / 2;
                this.y = height / 2;
            }
        }

        jumpCooling = true;
        if (widget.module.renderStarfield.get()) {
            double dt = widget.gameTimeSecs + (widget.accumulator / 1e9);
            widget.starfield.resetSmooth(this.x, this.y, 0.45, dt);
        }

        this.vx = (widget.rng.nextDouble() - 0.5) * 60;
        this.vy = (widget.rng.nextDouble() - 0.5) * 60;

        widget.playSound(
            SoundEvents.ENTITY_PLAYER_TELEPORT,
            widget.rng.nextFloat(0.8f, 1.1337f)
        );

        if (!poweredUp) this.iFrames = Ship.IFRAMES_ON_HYPERJUMP;
    }

    public double[] findSafePosition(
        double width, double height, double minDistFromPlayer,
        double minDistFromMeteorites, int attempts, WMeteorites widget
    ) {

        for (int n = 0; n < attempts; n++) {
            double sx = 20 + widget.rng.nextDouble() * (width - 40);
            double sy = 20 + widget.rng.nextDouble() * (height - 40);

            double dxp = sx - this.x;
            double dyp = sy - this.y;
            if (dxp * dxp + dyp * dyp < minDistFromPlayer * minDistFromPlayer) continue;

            boolean ok = true;
            for (int i = 0; i < widget.meteoriteCount; i++) {
                Meteorite meteorite = widget.meteorites[i];
                double dx = sx - meteorite.x;
                double dy = sy - meteorite.y;
                double minAllowed = (minDistFromMeteorites + meteorite.radius);
                if (dx * dx + dy * dy < minAllowed * minAllowed) {
                    ok = false; break;
                }
            }

            if (!ok) continue;
            return new double[]{sx, sy};
        }

        return null;
    }

    public void cyclePowerup(WMeteorites widget) {
        this.resetGun();
        this.resetHull();
        this.resetPhase();
        this.creditPowerup();
        this.resetGravityWell();

        Powerups[] vals = Powerups.values();
        int len = vals.length;
        int current = this.hasEntropy() ? Powerups.ENTROPY.ordinal() : this.getPowerup().ordinal();
        int start = (current + 1) % len;

        if (this.hasEntropy()) {
            this.setEntropy(false);
        } else if (this.getPowerup().equals(Powerups.STARDUST)) {
            widget.deInitStardustColor();
        } else if (this.getPowerup().equals(Powerups.MIDAS_TOUCH)) {
            widget.deInitMidasColor();
        }

        int foundIdx = -1;
        for (int n = 0; n < len; n++) {
            int idx = (start + n) % len;
            Powerups p = vals[idx];
            if (!widget.module.mouseAim.get() &&
                (p == Powerups.GRAVITY_WELL || p == Powerups.PRECISION_AIM || p == Powerups.CALIBRATED_FSD)) continue;

            foundIdx = idx;
            break;
        }

        Powerups nextPower = vals[foundIdx];
        if (nextPower == Powerups.ENTROPY) {
            List<Powerups> valid = new ArrayList<>();
            for (Powerups p : vals) {
                if (p == Powerups.NONE) continue;
                if (p == Powerups.ENTROPY) continue;
                if (p == Powerups.STARDUST) continue;
                if (!widget.module.mouseAim.get()
                    && (p == Powerups.GRAVITY_WELL || p == Powerups.PRECISION_AIM || p == Powerups.CALIBRATED_FSD)) continue;
                valid.add(p);
            }

            Powerups chosen = valid.get(widget.rng.nextInt(valid.size()));
            this.setEntropy(true);
            this.setPowerup(chosen);

            return;
        }

        if (nextPower == Powerups.MIDAS_TOUCH) {
            widget.initMidasColor();
        }

        if (nextPower == Powerups.STARDUST) {
            widget.initStardustColor();
            if (widget.rng.nextBoolean()) {
                this.isSniper = widget.rng.nextInt(69) > 42;
            } else {
                this.isShotgun = widget.rng.nextInt(69) > 42;
            }
        }

        this.setPowerup(nextPower);
    }

    public void gainNewPowerup(@Nullable Powerups excludeLast, WMeteorites widget) {
        this.resetGun();
        this.resetHull();
        this.resetPhase();
        this.resetGravityWell();
        if (!this.hasEntropy()) {
            this.creditPowerup();
        }
        if (this.getPowerup().equals(Powerups.STARDUST)) {
            widget.deInitStardustColor();
        } else if (this.getPowerup().equals(Powerups.MIDAS_TOUCH)) {
            widget.deInitMidasColor();
        }

        if (!this.hasEntropy()) {
            boolean hyperLucky = widget.wave % 9 == 0 // boosted chance directly before a boss wave
                ? widget.rng.nextInt(widget.CHEAT_MODE ? 7 : 69) <= widget.wave % 3
                : widget.rng.nextInt(widget.CHEAT_MODE ? 10 : 100) <= widget.wave % 3;
            if (hyperLucky && (excludeLast != Powerups.STARDUST || !this.getPowerup().equals(Powerups.STARDUST))) {
                widget.initStardustColor();
                this.setPowerup(Powerups.STARDUST);

                if (widget.rng.nextBoolean()) {
                    this.isSniper = widget.rng.nextInt(69) > 42;
                } else {
                    this.isShotgun = widget.rng.nextInt(69) > 42;
                }
                return;
            }
        }

        List<Powerups> valid = new ArrayList<>();
        for (Powerups power : Powerups.values()) {
            if (power.equals(excludeLast)) continue;
            if (power.equals(Powerups.NONE)) continue;
            if (power.equals(Powerups.STARDUST)) continue;
            if (this.hasEntropy() && power.equals(Powerups.ENTROPY)) continue;
            if (power.equals(Powerups.GRAVITY_WELL) && !widget.module.mouseAim.get()) continue;
            if (power.equals(Powerups.PRECISION_AIM) && !widget.module.mouseAim.get()) continue;
            if (power.equals(Powerups.CALIBRATED_FSD) && !widget.module.mouseAim.get()) continue;

            valid.add(power);
        }

        int luckyIndex = widget.rng.nextInt(valid.size());
        Powerups powerup = valid.get(luckyIndex);

        if (powerup.equals(Powerups.ENTROPY)) {
            valid.remove(Powerups.ENTROPY);
            int newLuck = widget.rng.nextInt(valid.size());
            this.setEntropy(true);
            this.setPowerup(valid.get(newLuck));
        } else this.setPowerup(powerup);
    }

    private double shortestAngleDiff(double target, double src) {
        double a = (target - src) % (Math.PI * 2);
        if (a <= -Math.PI) a += Math.PI * 2;
        if (a > Math.PI) a -= Math.PI * 2;
        return a;
    }
}
