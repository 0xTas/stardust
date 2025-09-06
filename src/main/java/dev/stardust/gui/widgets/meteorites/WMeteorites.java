package dev.stardust.gui.widgets.meteorites;

import java.util.*;
import static org.lwjgl.glfw.GLFW.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import dev.stardust.modules.Meteorites;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.sound.SoundInstance;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import dev.stardust.gui.widgets.meteorites.entity.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import dev.stardust.gui.widgets.meteorites.input.InputTracker;
import dev.stardust.gui.widgets.meteorites.render.HudRenderer;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import dev.stardust.gui.widgets.meteorites.render.StarfieldRenderer;
import dev.stardust.gui.widgets.meteorites.render.GravityWellRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import static dev.stardust.gui.widgets.meteorites.input.InputTracker.isKeyDown;
import static dev.stardust.gui.widgets.meteorites.input.InputTracker.isMouseDown;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class WMeteorites extends WWidget {
    private static final double UPDATE_RATE = 60.0;
    private static final int BREAKING_SAVE_VERSION = 1;
    private static final int BREAKING_SCORE_VERSION = 1;
    private static final long THRUST_SOUND_INTERVAL = 12_000;
    private static final double NS_PER_SEC = 1_000_000_000.0;
    private static final double DELTA_TIME = 0.016666666666666666;
    private static final double NS_PER_UPDATE = NS_PER_SEC / UPDATE_RATE;

    public final Ship player;
    public boolean CHEAT_MODE;
    public final Meteorites module;
    public final FieldSize fieldSize;
    public @Nullable HighScore highScore;
    public @Nullable String currentGameTip;
    public final StarfieldRenderer starfield;

    public int wave;
    private long pausedAt;
    private long lastSaved;
    public boolean isPaused;
    public boolean gameOver;
    public boolean gameBegan;
    public double accumulator;
    public double gameTimeSecs;
    public boolean mouseInBounds;
    public double aimMouseLocalX;
    public double aimMouseLocalY;
    public boolean hasNewHighScore;
    private final long starfieldSeed;

    public int meteoriteCount;
    private final int MIN_METEORITES;
    private final int MAX_METEORITES;
    public final Meteorite[] meteorites;

    private long timer = System.nanoTime();
    public final Random rng = ThreadLocalRandom.current();
    private final InputTracker inputTracker = new InputTracker();

    @SuppressWarnings({"unchecked"})
    private final List<Meteorite>[] renderBuckets = new ObjectArrayList[Meteorite.TOTAL_SIZES + 1];

    public int bulletCount;
    public final int MAX_BULLETS = 64;
    public Bullet[] bullets = new Bullet[MAX_BULLETS * 4];

    private long lastThrustSoundMs = 0;
    private @Nullable SoundInstance thrustInstance = null;
    public static final ReferenceList<SoundEvent> BREAK_SOUNDS = ReferenceList.of(
        SoundEvents.BLOCK_CALCITE_BREAK, SoundEvents.ENTITY_TURTLE_EGG_BREAK,
        SoundEvents.BLOCK_SUSPICIOUS_GRAVEL_BREAK, SoundEvents.BLOCK_ANCIENT_DEBRIS_BREAK,
        SoundEvents.BLOCK_TUFF_BREAK, SoundEvents.BLOCK_RESIN_BREAK, SoundEvents.BLOCK_DEEPSLATE_BREAK
    );

    public WMeteorites(Meteorites module) {
        for (int n = 0; n <= Meteorite.TOTAL_SIZES; n++)
            renderBuckets[n] = new ObjectArrayList<>();

        this.module = module;
        if (module.highScore != null && module.highScore.version() == BREAKING_SCORE_VERSION) {
            this.highScore = module.highScore;
        }

        if (module.saveGames.get() && module.saveData != null) {
            SaveData data = module.saveData; // if critical settings weren't changed, load save
            if (data.fieldSize().equals(module.canvasSize.get())
                && data.version() == BREAKING_SAVE_VERSION && data.cheating() == module.debug.get()) {
                this.CHEAT_MODE = data.cheating();

                this.wave = data.waves();
                this.player = data.player();
                this.bullets = data.bullets();
                this.starfieldSeed = data.seed();
                this.fieldSize = data.fieldSize();
                this.meteorites = data.meteorites();
                this.accumulator = data.accumulator();
                this.bulletCount = data.bulletCount();
                this.meteoriteCount = data.meteoriteCount();
                this.MIN_METEORITES = fieldSize.getMinAsteroids();
                this.MAX_METEORITES = fieldSize.getMaxAsteroids();
                long millisSincePaused = System.currentTimeMillis() - data.pausedAt();
                this.starfield = new StarfieldRenderer(player, fieldSize.getWidth(), fieldSize.getHeight(), starfieldSeed);

                isPaused = true;
                gameBegan = true;
                pausedAt = System.currentTimeMillis();
                player.lastHyperJump += millisSincePaused;
                if (player.getPowerup().equals(Powerups.STARDUST)) {
                    initStardustColor();
                } else if (player.getPowerup().equals(Powerups.MIDAS_TOUCH)) {
                    initMidasColor();
                }

                return;
            }
        }

        this.fieldSize = module.canvasSize.get();
        this.player = new Ship(fieldSize.getWidth() / 2.0, fieldSize.getHeight() / 2.0, fieldSize.getShipThrustAmount());

        this.MIN_METEORITES = module.canvasSize.get().getMinAsteroids();
        this.MAX_METEORITES = module.canvasSize.get().getMaxAsteroids();
        this.meteorites = new Meteorite[MAX_METEORITES];

        for (int n = 0; n < MAX_BULLETS * 4; n++) bullets[n] = new Bullet();
        for (int n = 0; n < MAX_METEORITES; n++) meteorites[n] = new Meteorite();

        this.starfieldSeed = System.currentTimeMillis() / 1000;
        this.starfield = new StarfieldRenderer(player, fieldSize.getWidth(), fieldSize.getHeight(), starfieldSeed);

        this.CHEAT_MODE = module.debug.get();

        initNewWave();
        isPaused = true;
        pausedAt = System.currentTimeMillis();
        if (module.gameTips.get()) cycleNewGameTip();
    }

    public @Nullable SettingColor prevShipColor = null;
    public @Nullable SettingColor prevFlameColor = null;
    public @Nullable SettingColor prevBulletColor = null;

    public boolean shouldRestoreColorSettings() {
        return (player.getPowerup().equals(Powerups.STARDUST)
            && prevShipColor != null && prevFlameColor != null && prevBulletColor != null)
            || (player.getPowerup().equals(Powerups.MIDAS_TOUCH) && prevBulletColor != null);
    }

    public boolean shouldSaveGame() {
        return module.saveGames.get() && player.lives > 0 && gameBegan;
    }

    public SaveData saveGame() {
        if (!isPaused) pausedAt = System.currentTimeMillis();
        return new SaveData(
            BREAKING_SAVE_VERSION, starfieldSeed, fieldSize, player, meteorites,
            bullets, CHEAT_MODE, wave, meteoriteCount, bulletCount, accumulator, pausedAt
        );
    }

    @Override
    protected void onCalculateSize() {
        width  = fieldSize.getWidth();
        height = fieldSize.getHeight();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        int bx = (int) x;
        int by = (int) y;
        aimMouseLocalX = mouseX - bx;
        aimMouseLocalY = mouseY - by;
        mouseInBounds = mouseX >= bx && mouseX <= bx + width && mouseY >= by && mouseY <= by + height;

        pollInput();
        if (!isPaused) {
            long now = System.nanoTime();
            double elapsedNanos = now - timer;

            timer = now;
            accumulator += elapsedNanos;
            while (accumulator >= NS_PER_UPDATE) {
                updatePhysics();
                accumulator -= NS_PER_UPDATE;
                gameTimeSecs += NS_PER_UPDATE / 1e9;
            }
        } else {
            timer = System.nanoTime();
        }

        // autosave once every minute in case of crashes
        if (shouldSaveGame() && System.currentTimeMillis() - lastSaved >= 60_000) {
            module.saveGame(saveGame());
            lastSaved = System.currentTimeMillis();
        }

        // panel background
        renderer.quad(bx - 2, by - 2, (int) width + 4, (int) height + 4, new Color(18, 18, 20));

        // game area background
        renderer.quad(bx, by, (int) width, (int) height, new Color(8, 8, 12));

        // scissorStart means don't render parts of the following objects that stray outside the bounds provided
        renderer.scissorStart(bx, by, (int) width, (int) height);

        if (module.renderStarfield.get()) {
            double starfieldDeltaTime = gameTimeSecs + (accumulator / 1e9);
            starfield.render(renderer, bx, by, player.x, player.y, width, height,starfieldDeltaTime);
        }

        // gravity well indicator
        if (player.getPowerup().equals(Powerups.GRAVITY_WELL) && player.lives > 0 && !isPaused && mouseInBounds) {
            double baseCx;
            double baseCy;
            if (player.gravityWellDeployed) {
                baseCx = bx + player.gravityWellX;
                baseCy = by + player.gravityWellY;
            } else {
                baseCx = bx + aimMouseLocalX;
                baseCy = by + aimMouseLocalY;
            }

            // try offsets -1..1 for x and y to draw wrapped copies
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    double drawCx = baseCx + ox * width;
                    double drawCy = baseCy + oy * height;

                    // quick cull
                    double maxDrawR = Ship.GW_FALLOFF * 1.2;
                    if (drawCx + maxDrawR < bx || drawCx - maxDrawR > bx + width || drawCy + maxDrawR < by || drawCy - maxDrawR > by + height)
                        continue;

                    GravityWellRenderer.renderBackground(
                        renderer, player.gravityWellDeployed,
                        drawCx, drawCy, module.gravityColor.get()
                    );
                }
            }
        }

        // draw meteorites in order by size, so larger rocks are always rendered above smaller rocks
        for (int n = 0; n <= Meteorite.TOTAL_SIZES; n++)
            renderBuckets[n].clear();

        for (int n = 0; n < meteoriteCount; n++) {
            Meteorite m = meteorites[n];
            int s = m.isBoss ? Meteorite.SIZE_BOSS : m.size;
            if (s < 1) s = 1;
            if (s > Meteorite.TOTAL_SIZES) s = Meteorite.TOTAL_SIZES;

            renderBuckets[s].add(m);
        }

        for (int size = 1; size <= Meteorite.TOTAL_SIZES; size++) {
            for (Meteorite m : renderBuckets[size]) {
                m.render(renderer, bx, by, width, height);
            }
        }

        for (int n = 0; n < bulletCount; n++) {
            Bullet bullet = bullets[n];
            bullet.render(renderer, bx, by, module.bulletColor.get());
        }

        boolean drawShip = true;
        if (player.iFrames > 0 || player.phaseActive) {
            drawShip = ((System.currentTimeMillis() / 120) & 1) == 0; // iFrame visual feedback
        }
        if (drawShip) {
            player.render(renderer, bx, by, module.shipColor.get(), module.flameColor.get());
        }

        // gravity well crosshair (drawn after meteorites so that it stays on top)
        if (player.getPowerup().equals(Powerups.GRAVITY_WELL) && player.lives > 0 && !isPaused && mouseInBounds) {
            double baseCx;
            double baseCy;
            if (player.gravityWellDeployed) {
                baseCx = bx + player.gravityWellX;
                baseCy = by + player.gravityWellY;
            } else {
                baseCx = bx + aimMouseLocalX;
                baseCy = by + aimMouseLocalY;
            }

            // cull radius
            double maxDrawR = Ship.GW_FALLOFF * 1.2;

            // try offsets -1..1 to draw screen-wrapped copies
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    double drawCx = baseCx + ox * width;
                    double drawCy = baseCy + oy * height;

                    // cull off-screen copies
                    if (drawCx + maxDrawR < bx || drawCx - maxDrawR > bx + width ||
                        drawCy + maxDrawR < by || drawCy - maxDrawR > by + height) {
                        continue;
                    }

                    GravityWellRenderer.renderReticle(renderer, player.gravityWellDeployed, drawCx, drawCy);
                }
            }
        }

        renderer.scissorEnd();
        HudRenderer.renderHud(renderer, theme,bx, by, width, height, this);
    }

    private void updatePhysics() {
        if (player.lives <= 0) {
            module.clearSave();
            if (thrustInstance != null) {
                mc.getSoundManager().stop(thrustInstance);
                thrustInstance = null;
            }
            return;
        }

        player.updatePhysics(DELTA_TIME, width, height, aimMouseLocalX, aimMouseLocalY, this);

        for (int n = 0; n < bulletCount; ) {
            Bullet bullet = bullets[n];
            if (!bullet.updatePhysics(DELTA_TIME, width, height, player, meteorites, meteoriteCount, this)) {
                bullets[n].set(bullets[bulletCount - 1]);
                bulletCount--;
            } else {
                n++;
            }
        }

        double[] shipX = new double[3];
        double[] shipY = new double[3];
        shipX[0] = player.x + Math.cos(player.angle) * 10;
        shipY[0] = player.y + Math.sin(player.angle) * 10;
        shipX[1] = player.x + Math.cos(player.angle + Math.PI * 0.75) * 8;
        shipY[1] = player.y + Math.sin(player.angle + Math.PI * 0.75) * 8;
        shipX[2] = player.x + Math.cos(player.angle - Math.PI * 0.75) * 8;
        shipY[2] = player.y + Math.sin(player.angle - Math.PI * 0.75) * 8;

        for (int n = 0; n < meteoriteCount; n++) {
            Meteorite meteorite = meteorites[n];
            meteorite.updatePhysics(
                DELTA_TIME, width, height, shipX, shipY,
                meteoriteCount, player, meteorites, this
            );
        }

        // wave cleared
        if (meteoriteCount == 0) {
            initNewWave();
        }
    }

    private void pollInput() {
        boolean left = isKeyDown(GLFW_KEY_A) || isKeyDown(GLFW_KEY_LEFT);
        boolean right = isKeyDown(GLFW_KEY_D) || isKeyDown(GLFW_KEY_RIGHT);
        boolean up = isKeyDown(GLFW_KEY_W) || isKeyDown(GLFW_KEY_UP);
        boolean down = isKeyDown(GLFW_KEY_S) || isKeyDown(GLFW_KEY_DOWN);
        boolean space = isKeyDown(GLFW_KEY_SPACE) || isMouseDown(GLFW_MOUSE_BUTTON_LEFT);
        boolean rKey = isKeyDown(GLFW_KEY_R);
        boolean mKey = isKeyDown(GLFW_KEY_M);
        boolean cKey = isKeyDown(GLFW_KEY_C);
        boolean iKey = isKeyDown(GLFW_KEY_I);
        boolean pKey = isKeyDown(GLFW_KEY_P);
        boolean nKey = isKeyDown(GLFW_KEY_N);
        boolean middleClick = isMouseDown(GLFW_MOUSE_BUTTON_MIDDLE);
        boolean rightClick = isMouseDown(GLFW_MOUSE_BUTTON_RIGHT) || isKeyDown(GLFW_KEY_H);

        if ((rKey && inputTracker.isNotHeld(GLFW_KEY_R) && player.lives <= 0)
            || (nKey && inputTracker.isNotHeld(GLFW_KEY_N) && isPaused)) {
            resetGame();
            return;
        }

        if (mKey && inputTracker.isNotHeld(GLFW_KEY_M)) {
            module.sounds.set(!module.sounds.get());
            if (!module.sounds.get()) killThrustInstance();
        }

        if (player.lives > 0) {
            if (cKey && inputTracker.isNotHeld(GLFW_KEY_C) && CHEAT_MODE) {
                player.cyclePowerup(this);
            }

            if (iKey && inputTracker.isNotHeld(GLFW_KEY_I) && CHEAT_MODE) {
                player.invulnerable = !player.invulnerable;
                if (!player.invulnerable) player.iFrames = Ship.IFRAMES_ON_HYPERJUMP;
            }

            if (middleClick && inputTracker.isNotHeld(GLFW_MOUSE_BUTTON_MIDDLE)) {
                module.mouseAim.set(!module.mouseAim.get());
            }

            if ((pKey && inputTracker.isNotHeld(GLFW_KEY_P) && !isPaused) || (rKey && inputTracker.isNotHeld(GLFW_KEY_R) && isPaused)) {
                isPaused = !isPaused;
                if (isPaused) {
                    pauseGame();
                } else {
                    gameBegan = true;
                    currentGameTip = null;
                    long millisSincePaused = System.currentTimeMillis() - pausedAt;
                    player.lastHyperJump += millisSincePaused;
                }
            }

            if (!isPaused) {
                player.rotLeft = left;
                player.rotRight = right;
                player.thrusting = up;

                if (space) {
                    if (player.shootCooldownTimer <= 0) {
                        player.tryToShoot(this);
                    }
                }

                // right-click ability
                if (rightClick && inputTracker.isNotHeld(GLFW_MOUSE_BUTTON_RIGHT)) {
                    if (player.getPowerup().equals(Powerups.PHASE_SHIFT)) {
                        if (player.phaseActive && player.phaseTimer <= player.phaseDuration) {
                            player.phaseActive = false; // take remaining duration off of phase cd
                            player.phaseCooldownTimer = player.phaseCooldown - (player.phaseDuration - player.phaseTimer);
                            player.iFrames = Ship.IFRAMES_ON_EXIT_PHASE;
                            playSound(SoundEvents.ENTITY_PLAYER_TELEPORT, 0.69f);
                        } else if (!player.phaseActive && player.phaseCooldownTimer <= 0) {
                            player.phaseTimer = 0;
                            player.phaseActive = true;
                            playSound(SoundEvents.ENTITY_PLAYER_TELEPORT, 0.42f);
                        } else {
                            playSound(SoundEvents.ENTITY_VILLAGER_NO, rng.nextFloat(0.969f, 1.1337f));
                        }
                    } else if (player.getPowerup().equals(Powerups.GRAVITY_WELL)) {
                        if (mouseInBounds) {
                            if (!player.gravityWellDeployed) {
                                if (player.gravityWellCdTimer <= 0) {
                                    player.gravityWellDeployed = true;
                                    player.gravityWellX = aimMouseLocalX;
                                    player.gravityWellY = aimMouseLocalY; // persist remaining duration if repositioned vv
                                    player.gravityWellTimer = player.gravityWellTimer > 0 ? player.gravityWellTimer : player.gravityWellDuration;
                                    playSound(SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, rng.nextFloat(0.969f, 1.1337f));
                                } else {
                                    playSound(SoundEvents.ENTITY_VILLAGER_NO, rng.nextFloat(0.969f, 1.1337f));
                                }
                            } else {
                                double dx = player.gravityWellX - aimMouseLocalX;
                                double dy = player.gravityWellY - aimMouseLocalY;

                                double distFromCenter = dx*dx + dy*dy;
                                if (distFromCenter < Ship.GW_FALLOFF * Ship.GW_FALLOFF) {
                                    player.gravityWellCdTimer = 0; // able to reposition immediately while deployed duration is > 0
                                    player.gravityWellDeployed = false;
                                    playSound(SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, rng.nextFloat(0.969f, 1.1337f));
                                }
                            }
                        }
                    } else if (player.getPowerup().equals(Powerups.CALIBRATED_FSD)
                        || (player.isShotgun && !player.isSniper && player.getPowerup().equals(Powerups.STARDUST))) {
                        boolean poweredUp = player.getPowerup().equals(Powerups.STARDUST);
                        if ((poweredUp && System.currentTimeMillis() - player.lastHyperJump >= 1000)
                            || (player.iFrames <= 0 && System.currentTimeMillis() - player.lastHyperJump >= 3.0 * 1000)) {
                            player.lastHyperJump = System.currentTimeMillis();
                            player.doHyperspaceJump(poweredUp, true, width, height, this);
                        } else {
                            playSound(SoundEvents.ENTITY_VILLAGER_NO, rng.nextFloat(0.969f, 1.1337f));
                        }
                    } else {
                        boolean poweredUp = player.getPowerup().equals(Powerups.SUPERCHARGED_FSD)
                            || (player.getPowerup().equals(Powerups.STARDUST) && !player.isShotgun && !player.isSniper);
                        if (poweredUp
                            || (player.iFrames <= 0 && player.score >= Ship.HYPERSPACE_SCORE_COST
                            && System.currentTimeMillis() - player.lastHyperJump >= (Ship.IFRAMES_ON_HYPERJUMP * 3) * 1000)
                        ) {
                            player.lastHyperJump = System.currentTimeMillis();
                            player.doHyperspaceJump(poweredUp, false, width, height, this);

                            if (!poweredUp) {
                                player.score -= Ship.HYPERSPACE_SCORE_COST;
                                playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE, rng.nextFloat(0.969f, 1.1337f));
                            }
                        } else {
                            playSound(SoundEvents.ENTITY_VILLAGER_NO, rng.nextFloat(0.969f, 1.1337f));
                        }
                    }
                }
            }
        }

        inputTracker.updateState(GLFW_KEY_W, up, this);
        inputTracker.updateState(GLFW_KEY_I, iKey, this);
        inputTracker.updateState(GLFW_KEY_C, cKey, this);
        inputTracker.updateState(GLFW_KEY_M, mKey, this);
        inputTracker.updateState(GLFW_KEY_N, nKey, this);
        inputTracker.updateState(GLFW_KEY_P, pKey, this);
        inputTracker.updateState(GLFW_KEY_R, rKey, this);
        inputTracker.updateState(GLFW_KEY_S, down, this);
        inputTracker.updateState(GLFW_KEY_A, left, this);
        inputTracker.updateState(GLFW_KEY_D, right, this);
        inputTracker.updateState(GLFW_KEY_SPACE, space, this);
        inputTracker.updateState(GLFW_MOUSE_BUTTON_RIGHT, rightClick, this);
        inputTracker.updateState(GLFW_MOUSE_BUTTON_MIDDLE, middleClick, this);
    }

    public void resetGame() {
        if (player.getPowerup().equals(Powerups.STARDUST)) {
            deInitStardustColor();
        } else if (player.getPowerup().equals(Powerups.MIDAS_TOUCH)) {
            deInitMidasColor();
        }

        for (Meteorite m : meteorites) m.isBoss = false;

        wave = 0;
        bulletCount = 0;
        isPaused = false;
        gameOver = false;
        meteoriteCount = 0;
        hasNewHighScore = false;

        player.score = 0;
        player.lives = 3;
        player.x = width / 2;
        player.y = height / 2;
        player.vx = player.vy = 0;
        player.pointsWithCurrentPower = 0;
        if (module.renderStarfield.get()) {
            starfield.resetInstant(player.x, player.y);
        }

        module.clearSave();
        player.resetPowerup();
        player.powerMap.clear();

        initNewWave();
    }

    private void initNewWave() {
        ++wave;
        boolean bossMeteoriteWave = wave % 10 == 0;
        if (wave % 6 == 0 && player.lives < 5) player.lives++;

        if (bossMeteoriteWave) {
            // set player pos to center so they don't get insta-gibbed by the giga meteor
            player.x = width / 2;
            player.y = height / 2;
            player.vx = player.vy = 0;
            if (module.renderStarfield.get()) {
                double dt = gameTimeSecs + (accumulator / 1e9);
                starfield.resetSmooth(player.x, player.y, 0.969, dt);
            }
            spawnMeteorite(true);
        } else {
            final int JITTER_RANGE = 2;
            final int EARLY_WAVE_LAST = 4;
            final double GROWTH_RATE = 1.420;
            final double SURGE_CHANCE   = 0.06;
            final double BREATHER_CHANCE = 0.08;
            final int BREATHER_AMOUNT = Math.max(1, (int) Math.round(2 + Math.sqrt(wave) * 0.5));
            final int SURGE_AMOUNT    = Math.max(2, (int) Math.round(2 + Math.sqrt(wave) * 0.9));
            final int EARLY_WAVE_SOFT_CAP = Math.max(MIN_METEORITES, Math.min(MAX_METEORITES, 6));
            int base = MIN_METEORITES + (int) Math.floor(GROWTH_RATE * Math.sqrt(Math.max(1, wave)));

            if (wave <= EARLY_WAVE_LAST) {
                base = Math.min(base, EARLY_WAVE_SOFT_CAP);
            }

            double r1 = rng.nextDouble();
            double r2 = rng.nextDouble();
            int jitter = (int) Math.round((r1 - r2) * JITTER_RANGE);

            int toSpawn = base + jitter;
            double specialRoll = rng.nextDouble();
            if (specialRoll < BREATHER_CHANCE) {
                toSpawn = Math.max(MIN_METEORITES, toSpawn - BREATHER_AMOUNT);
            } else if (specialRoll < BREATHER_CHANCE + SURGE_CHANCE) {
                toSpawn = Math.min(MAX_METEORITES, toSpawn + SURGE_AMOUNT);
            }

            toSpawn = Math.max(MIN_METEORITES, Math.min(MAX_METEORITES, toSpawn));

            for (int n = 0; n < toSpawn; n++) spawnMeteorite(false);
        }

        if (player.hasEntropy()
            || (wave % 3 == 0 && (player.getPowerup().equals(Powerups.NONE) || (player.getPowerup().equals(Powerups.STARDUST) && !CHEAT_MODE)))
            || (CHEAT_MODE && (module.randomizePowerups.get() || player.getPowerup().equals(Powerups.NONE)))) {

            player.gainNewPowerup(player.hasEntropy() ? player.getPowerup() : null, this);
            playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, rng.nextFloat(0.77f, 1.1337f));
        } else {
            playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, rng.nextFloat(0.77f, 1.1337f));
        }
    }

    private void spawnMeteorite(boolean boss) {
        if (meteoriteCount >= MAX_METEORITES) return;
        for (int attempt = 0; attempt < 8; attempt++) {
            double x, y;
            switch (rng.nextInt(4)) {
                case 0 -> {
                    x = -20;
                    y = rng.nextDouble() * height;
                }
                case 1 -> {
                    x = width + 20;
                    y = rng.nextDouble() * height;
                }
                case 2 -> {
                    y = -20;
                    x = rng.nextDouble() * width;
                }
                default -> {
                    y = height + 20;
                    x = rng.nextDouble() * width;
                }
            }

            double dxp = x - player.x;
            double dyp = y - player.y;
            double safeDistance = boss ? Ship.WAVE_SPAWN_SAFE_DIST * 2 : Ship.WAVE_SPAWN_SAFE_DIST;
            if (dxp * dxp + dyp * dyp >= safeDistance * safeDistance) {
                // compute velocity and spawn normally
                double angle = Math.atan2(player.y - y, player.x - x);
                double maxSpread = Math.toRadians(50.0);
                double ang = angle + (rng.nextDouble() * 2 - 1) * maxSpread;
                double baseSpeed = 28 + rng.nextDouble() * 60 + wave * 2;
                if (rng.nextDouble() < 0.06) baseSpeed *= 1.6;

                double perp = ang + Math.PI / 2.0;
                double perpStrength = 20.0 + rng.nextDouble() * 60.0;
                double perpFactor = (rng.nextDouble() * 2 - 1.0) * perpStrength;
                double vx = Math.cos(ang) * baseSpeed + Math.cos(perp) * perpFactor;
                double vy = Math.sin(ang) * baseSpeed + Math.sin(perp) * perpFactor;

                if (boss) {
                    Meteorite b = meteorites[meteoriteCount++];
                    b.becomeBoss(x, y, vx, vy, width, height, wave);
                } else {
                    double radius = Meteorite.MIN_LARGE_RADIUS + rng.nextDouble() * Meteorite.LARGE_RADIUS_MULTIPLIER;
                    Meteorite m = meteorites[meteoriteCount++];
                    m.set(x, y, vx, vy, radius, false);
                }
                return;
            }
        }

        // fallback: spawn off-screen at a corner far from the player
        double fx = (player.x < width / 2) ? width + 40 : -40;
        double fy = (player.y < height / 2) ? height + 40 : -40;
        double angle = Math.atan2(player.y - fy, player.x - fx);
        double vx = Math.cos(angle) * (30 + rng.nextDouble() * 40);
        double vy = Math.sin(angle) * (30 + rng.nextDouble() * 40);

        if (boss) {
            Meteorite b = meteorites[meteoriteCount++];
            b.becomeBoss(fx, fy, vx, vy, width, height, wave);
        } else {
            double radius = Meteorite.MIN_LARGE_RADIUS + rng.nextDouble() * Meteorite.LARGE_RADIUS_MULTIPLIER;
            Meteorite m = meteorites[meteoriteCount++];
            m.set(fx, fy, vx, vy, radius, false);
        }
    }

    private void spawnMeteoriteAt(double x, double y, double vx, double vy, double radius) {
        if (meteoriteCount >= MAX_METEORITES) return;
        Meteorite m = meteorites[meteoriteCount++];
        m.set(x, y, vx, vy, radius, false);
    }

    public void spawnChildMeteorite(
        Meteorite source, double angle,
        double baseSpeedMin, double baseSpeedMax,
        double spawnOffsetMin, double spawnOffsetMax, double fragRadius
    ) {
        if (meteoriteCount >= MAX_METEORITES) return;

        double fx = Math.cos(angle);
        double fy = Math.sin(angle);
        double offset = spawnOffsetMin + rng.nextDouble() * (spawnOffsetMax - spawnOffsetMin);
        double spawnDist = Math.max(1.0, source.radius * Math.max(0.6, offset));

        double sx = source.x + fx * spawnDist;
        double sy = source.y + fy * spawnDist;
        double speed = baseSpeedMin + rng.nextDouble() * (baseSpeedMax - baseSpeedMin);

        double vx = fx * speed;
        double vy = fy * speed;
        double inherit = 0.05 + rng.nextDouble() * 0.37;

        vx += source.vx * inherit;
        vy += source.vy * inherit;

        double perpAngle = angle + Math.PI * 0.5;
        double perpStrength = 6.0 + rng.nextDouble() * 48.0;
        double perp = (rng.nextDouble() * 2.0 - 1.0) * perpStrength;

        vx += Math.cos(perpAngle) * perp;
        vy += Math.sin(perpAngle) * perp;

        double maxSpeed = Meteorite.LETHAL_IMPACT_SPEED * 4;
        double ss = Math.hypot(vx, vy);
        if (ss > maxSpeed) {
            double s = maxSpeed / ss;
            vx *= s;
            vy *= s;
        }

        spawnMeteoriteAt(sx, sy, vx, vy, fragRadius);
    }

    public void splitMeteorite(int index) {
        if (index < 0 || index >= meteoriteCount) return;
        Meteorite m = meteorites[index];

        int size = m.size;
        double cx = m.x, cy = m.y;

        if (m.isBoss) {
            m.isBoss = false;
            // remove current by swapping last
            meteorites[index].set(meteorites[meteoriteCount - 1]);
            meteoriteCount--;
            return;
        }
        if (size > Meteorite.SIZE_SMALL) {
            int parts = 2 + rng.nextInt(2);
            for (int n = 0; n < parts; n++) {
                if (meteoriteCount >= MAX_METEORITES) break;
                Meteorite m2 = meteorites[meteoriteCount++];

                double aAng = rng.nextDouble() * Math.PI * 2;
                double speed = 20 + rng.nextDouble() * 90;
                double nvx = Math.cos(aAng) * speed + (rng.nextDouble() - 0.5) * 30;
                double nvy = Math.sin(aAng) * speed + (rng.nextDouble() - 0.5) * 30;
                double newRadius = (size == Meteorite.SIZE_LARGE) ? (18 + rng.nextDouble() * 6) : (8 + rng.nextDouble() * 3);

                m2.set(cx + Math.cos(aAng) * 6, cy + Math.sin(aAng) * 6, nvx, nvy, newRadius, false);
            }
        }

        meteorites[index].set(meteorites[meteoriteCount - 1]);
        meteoriteCount--;

        int luckyIndex = rng.nextInt(BREAK_SOUNDS.size());
        playSound(
            BREAK_SOUNDS.get(luckyIndex),
            rng.nextFloat(0.77f, 1.1337f),
            module.soundVolume.get().floatValue() * 0.5f
        );
    }

    public void clearMeteoritesAround(double cx, double cy) {
        for (int n = 0; n < meteoriteCount; n++) {
            Meteorite meteorite = meteorites[n];
            double dx = meteorite.x - cx;
            double dy = meteorite.y - cy;
            double d2 = dx*dx + dy*dy;
            double min = Ship.RESPAWN_SAFE_DIST + meteorite.radius;
            if (d2 <= min*min) {
                double d = Math.sqrt(Math.max(1e-6, d2));
                // push outwards along radial direction
                double nx = dx / d;
                double ny = dy / d;
                meteorite.x = cx + nx * (min + 4);
                meteorite.y = cy + ny * (min + 4);
                meteorite.vx += nx * 40;
                meteorite.vx += ny * 40;
                meteorite.wrap(width, height);
            }
        }
    }

    public void deInitMidasColor() {
        if (prevBulletColor != null) module.bulletColor.set(prevBulletColor);

        prevBulletColor = null;
    }

    public void initMidasColor() {
        prevBulletColor = module.bulletColor.get();
        if (prevBulletColor != null) module.bulletColor.set(new SettingColor(213, 213, 13));
    }

    public void deInitStardustColor() {
        if (prevShipColor != null) module.shipColor.set(prevShipColor);
        if (prevFlameColor != null) module.flameColor.set(prevFlameColor);
        if (prevBulletColor != null) module.bulletColor.set(prevBulletColor);

        prevShipColor = null;
        prevFlameColor = null;
        prevBulletColor = null;
    }

    public void initStardustColor() {
        prevShipColor = module.shipColor.get();
        prevFlameColor = module.flameColor.get();
        prevBulletColor = module.bulletColor.get();
        if (prevShipColor != null) module.shipColor.set(new SettingColor(prevShipColor.r, prevShipColor.g, prevShipColor.b, prevShipColor.a, true));
        if (prevFlameColor != null) module.flameColor.set(new SettingColor(prevFlameColor.r, prevFlameColor.g, prevFlameColor.b, prevFlameColor.a, true));
        if (prevBulletColor != null) module.bulletColor.set(new SettingColor(prevBulletColor.r, prevBulletColor.g, prevBulletColor.b, prevBulletColor.a, true));
    }

    public void updateHighScore() {
        if (!CHEAT_MODE && (highScore == null || highScore.isSurpassed(fieldSize, player.score))) {
            hasNewHighScore = true;

            int most = 0;
            Powerups bestPower = Powerups.NONE;
            for (Map.Entry<Powerups, Integer> powers : player.powerMap.entrySet()) {
                if (powers.getValue() > most) {
                    most = powers.getValue();
                    bestPower = powers.getKey();
                }
            }
            if (highScore == null) {
                Map<FieldSize, int[]> scores = new HashMap<>();
                scores.put(fieldSize, new int[]{player.score, wave, bestPower.ordinal()});
                highScore = new HighScore(BREAKING_SCORE_VERSION, scores);
            } else {
                highScore.update(fieldSize, player.score, wave, bestPower.ordinal());
            }

            module.saveHighScore(highScore);
        }
    }

    public void cycleNewGameTip() {
        currentGameTip = "Tip: " + Meteorites.GAMEPLAY_TIPS[rng.nextInt(Meteorites.GAMEPLAY_TIPS.length)];
    }

    public void playSound(SoundEvent sound, float pitch) {
        if (!module.sounds.get()) return;
        try {
            if (mc == null) return;
            if (mc.getSoundManager() == null) return;
            mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, module.soundVolume.get().floatValue()));
        } catch (Throwable ignored) {}
    }

    public void playSound(SoundEvent sound, float pitch, float volume) {
        if (!module.sounds.get()) return;
        try {
            if (mc == null) return;
            if (mc.getSoundManager() == null) return;
            mc.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
        } catch (Throwable ignored) {}
    }

    public void playThrustInstance() {
        if (module.sounds.get()) {
            long now = System.currentTimeMillis();
            if (thrustInstance == null || now - lastThrustSoundMs >= THRUST_SOUND_INTERVAL) {
                lastThrustSoundMs = now;
                try {
                    if (mc == null) return;
                    if (mc.getSoundManager() == null) return;
                    thrustInstance = PositionedSoundInstance.master(
                        SoundEvents.ITEM_ELYTRA_FLYING, 0.777f,
                        module.soundVolume.get().floatValue() * 0.69f
                    );
                    mc.getSoundManager().play(thrustInstance);
                } catch (Throwable ignored) {}
            }
        }
    }

    public void killThrustInstance() {
        if (thrustInstance != null) {
            mc.getSoundManager().stop(thrustInstance);
            thrustInstance = null;
        }
    }

    public void pauseGame() {
        isPaused = true;
        killThrustInstance();
        pausedAt = System.currentTimeMillis();
        if (module.gameTips.get()) cycleNewGameTip();
    }
}
