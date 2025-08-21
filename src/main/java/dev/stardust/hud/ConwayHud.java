package dev.stardust.hud;

import java.util.Set;
import java.util.HashSet;
import dev.stardust.Stardust;
import dev.stardust.util.MsgUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.StringIdentifiable;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import dev.stardust.mixin.meteor.accessor.SettingAccessor;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class ConwayHud extends HudElement {
    public static final HudElementInfo<ConwayHud> INFO = new HudElementInfo<>(Stardust.HUD_GROUP, "game-of-life", "Conway's Game of Life in your HUD.", ConwayHud::new);

    public ConwayHud() {
        super(INFO);
        if (this.deadColor.get().rainbow) RainbowColors.add(this.deadColor.get());
        if (this.aliveColor.get().rainbow) RainbowColors.add(this.aliveColor.get());
    }

    public enum Visibility {
        Always, Widgets, Windows
    }
    public enum ColorScheme {
        Dynamic, Bichromatic, Trippy, Stabilizing
    }
    public enum Ruleset implements StringIdentifiable {
        Random(""),
        Cyclic("C"),
        Standard("B3/S23"),
        HighLife("B36/S23"),
        Custom("B3/S137/R"),
        Mutation("B3/S23/R"),
        Amoeba("B35678/S5678"),
        Sunspots("B3678/S34678"),
        Stardust("B3678/S14568/R+");

        public final String rules;

        Ruleset(String rules) {
            this.rules = rules;
        }

        @Override
        public String asString() {
            return switch (this) {
                case Custom -> "Custom";
                case Cyclic -> "Cyclic";
                case Random -> "Random";
                case Standard -> "Standard(B3/S23)";
                case HighLife -> "HighLife(B36/S23)";
                case Amoeba -> "Amoeba(B35678/S5678)";
                case Mutation -> "Mutation(B3/S23/R)";
                case Stardust -> "Stardust(B3678/S14568/R+)";
                case Sunspots -> "Sunspots(B3678/S34678)";
            };
        }
    }

    private final SettingGroup sgRules = settings.createGroup("Rules");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Ruleset> rules = sgRules.add(
        new EnumSetting.Builder<Ruleset>()
            .name("simulation-rules")
            .defaultValue(Ruleset.Standard)
            .description(Ruleset.Standard.asString())
            .onChanged(it -> {
                switch (it) {
                    case Cyclic -> this.seedDensity.set(0.69);
                    case Amoeba -> this.seedDensity.set(0.469);
                    case Sunspots -> this.seedDensity.set(0.420);
                    case Stardust -> this.seedDensity.set(1.0);
                    default -> this.seedDensity.set(0.330);
                }
                if (it == Ruleset.Stardust) {
                    this.updateInterval.set(2);
                } else {
                    this.updateInterval.set(1);
                }
                firstTick = true;
                alterOwnDescription(it);
            })
            .build()
    );
    public final Setting<String> customRules = sgRules.add(
        new StringSetting.Builder()
            .name("custom-rules")
            .description("In the format \"B3/S23\" where B means birth, S means survive, and digits must be 1-8. Optional \"R\" flag to inject randomness.")
            .defaultValue(Ruleset.Custom.rules)
            .visible(() -> rules.get().equals(Ruleset.Custom) || rules.get().equals(Ruleset.Random))
            .onChanged(it -> firstTick = true)
            .build()
    );
    private final Setting<Integer> cyclicStates = sgRules.add(
        new IntSetting.Builder()
            .name("cyclic-max-states")
            .description("The max amount of states to use for cyclic automata. Half of these will be rendered as transparent.")
            .range(2, 32).noSlider()
            .defaultValue(8)
            .visible(() -> rules.get().equals(Ruleset.Cyclic) || rules.get().equals(Ruleset.Random))
            .build()
    );
    private final Setting<Integer> cyclicRate = sgRules.add(
        new IntSetting.Builder()
            .name("cyclic-threshold")
            .description("The threshold value to use for the cyclic automata ruleset. Higher values cause less change.")
            .range(1, 8).noSlider()
            .defaultValue(1)
            .visible(() -> rules.get().equals(Ruleset.Cyclic) || rules.get().equals(Ruleset.Random))
            .build()
    );
    private final Setting<Integer> cyclicDuration = sgRules.add(
        new IntSetting.Builder()
            .name("cyclic-duration")
            .description("The duration in seconds to allow any one cyclic simulation to live for.")
            .min(1).noSlider()
            .defaultValue(30)
            .visible(() -> rules.get().equals(Ruleset.Cyclic) || rules.get().equals(Ruleset.Random))
            .build()
    );
    private final Setting<Double> mutateAliveRate = sgRules.add(
        new DoubleSetting.Builder()
            .name("mutation-rate-alive")
            .description("How often to inject random life mutations when the randomize (R) rule is added to the Ruleset.")
            .range(0.0, 1.0).sliderRange(0.0, 0.001)
            .defaultValue(0.000069)
            .visible(() ->
                rules.get().equals(Ruleset.Mutation)
                    || rules.get().equals(Ruleset.Stardust) || rules.get().equals(Ruleset.Random)
                    || (rules.get().equals(Ruleset.Custom) && customRules.get().toUpperCase().contains("R"))
            )
            .build()
    );
    private final Setting<Double> mutateDeadRate = sgRules.add(
        new DoubleSetting.Builder()
            .name("mutation-rate-dead")
            .description("How often to inject random death mutations when the randomize (R) rule is added to the Ruleset.")
            .range(0.0, 1.0).sliderRange(0.0, 0.001)
            .defaultValue(0.000042)
            .visible(() ->
                rules.get().equals(Ruleset.Mutation)
                    || rules.get().equals(Ruleset.Stardust) || rules.get().equals(Ruleset.Random)
                    || (rules.get().equals(Ruleset.Custom) && customRules.get().toUpperCase().contains("R"))
            )
            .build()
    );
    private final Setting<Double> seedDensity = sgRules.add(
        new DoubleSetting.Builder()
            .name("seed-density")
            .description("The density of live cells when first seeding the simulation.")
            .range(0.0, 1.0).noSlider()
            .defaultValue(0.3333333333333333)
            .build()
    );
    public final Setting<Integer> cellSize = sgRules.add(
        new IntSetting.Builder()
            .name("cell-size")
            .min(1).noSlider()
            .defaultValue(5)
            .onChanged(it -> firstTick = true)
            .build()
    );
    public final Setting<Integer> gridSize = sgRules.add(
        new IntSetting.Builder()
            .name("grid-size")
            .min(10).noSlider()
            .defaultValue(420)
            .onChanged(it -> firstTick = true)
            .build()
    );
    public final Setting<Integer> maxAge = sgRules.add(
        new IntSetting.Builder()
            .name("maximum-age")
            .description("The maximum cell age to allow before resetting the game. Set to 0 to disable.")
            .min(0).noSlider()
            .defaultValue(1337)
            .build()
    );
    public final Setting<Integer> maxGeneration = sgRules.add(
        new IntSetting.Builder()
            .name("maximum-generation")
            .description("The maximum generation to allow before resetting the game. Set to 0 to disable.")
            .min(0).noSlider()
            .defaultValue(32767)
            .build()
    );
    private final Setting<Integer> updateInterval = sgRules.add(
        new IntSetting.Builder()
            .name("update-interval")
            .description("How often in ticks to update the simulation.")
            .min(1).sliderMax(20)
            .defaultValue(1)
            .build()
    );

    public final Setting<Visibility> visibility = sgGeneral.add(
        new EnumSetting.Builder<Visibility>()
            .name("visibility")
            .defaultValue(Visibility.Windows)
            .build()
    );
    private final Setting<ColorScheme> colorScheme = sgGeneral.add(
        new EnumSetting.Builder<ColorScheme>()
            .name("color-scheme")
            .defaultValue(ColorScheme.Stabilizing)
            .visible(() -> !rules.get().equals(Ruleset.Cyclic))
            .build()
    );
    public final Setting<SettingColor> aliveColor = sgGeneral.add(
        new ColorSetting.Builder()
            .name("alive-color")
            .description("The color to use for alive cells.")
            .defaultValue(new SettingColor(101, 17, 255, 142, true))
            .visible(() -> !colorScheme.get().equals(ColorScheme.Trippy) && !rules.get().equals(Ruleset.Cyclic))
            .build()
    );
    public final Setting<SettingColor> deadColor = sgGeneral.add(
        new ColorSetting.Builder()
            .name("dead-color")
            .description("The color to use for dead cells.")
            .defaultValue(new SettingColor(0, 0, 0, 0))
            .visible(() -> !colorScheme.get().equals(ColorScheme.Trippy) && !rules.get().equals(Ruleset.Cyclic))
            .build()
    );
    public final Setting<Integer> minAlpha = sgGeneral.add(
        new IntSetting.Builder()
            .name("minimum-opacity")
            .min(0).sliderMax(255)
            .defaultValue(69)
            .visible(() -> !colorScheme.get().equals(ColorScheme.Bichromatic))
            .build()
    );
    public final Setting<Integer> maxAlpha = sgGeneral.add(
        new IntSetting.Builder()
            .name("maximum-opacity")
            .min(0).sliderMax(255)
            .defaultValue(213)
            .visible(() -> !colorScheme.get().equals(ColorScheme.Bichromatic))
            .build()
    );

    private static final Integer DEAD = 0;
    private static final Integer ALIVE = 1;

    private int timer = 0;
    private int offset = 0;
    private int resetTimer = 0;
    private int generation = 0;
    private int biggestAge = 0;
    private long timestamp = 0L;
    private int CELL_SIZE = cellSize.get();
    private int GRID_SIZE = gridSize.get();
    private int[][] grid = new int[GRID_SIZE][GRID_SIZE];
    private int[][] buffer = new int[GRID_SIZE][GRID_SIZE];

    // See commands/Life.java
    public boolean isPaused = false;
    public boolean isVisible = true;
    public boolean firstTick = true;
    public Rules gameRules = Rules.STANDARD;

    public int getMaxAge() {
        return biggestAge;
    }
    public int getCellSize() {
        return CELL_SIZE;
    }
    public int getGridSize() {
        return GRID_SIZE;
    }
    public long getRuntime() {
        return System.currentTimeMillis() - timestamp;
    }
    public int getGeneration() {
        return generation;
    }
    public int[] getCellCount() {
        int dead = 0;
        int alive = 0;
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                if (grid[x][y] == DEAD) dead++;
                else alive++;
            }
        }

        return new int[] {dead, alive};
    }
    public void stepSimulation() {
        if (isPaused) tickSimulation();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();
        WButton reset = list.add(theme.button("New Game")).widget();
        WButton manualStep = list.add(theme.button("Step")).widget();
        WButton pauseResume = list.add(theme.button(isPaused ? "Resume" : "Pause")).widget();
        WButton visibilityBtn = list.add(theme.button(isVisible ? "Hide" : "Show")).widget();

        reset.action = () -> firstTick = true;
        manualStep.action = this::stepSimulation;
        pauseResume.action = () -> isPaused = !isPaused;
        visibilityBtn.action = () -> isVisible = !isVisible;

        return list;
    }

    @Override
    public void tick(HudRenderer hud) {
        if (shouldHide() || isPaused) return;

        if (firstTick) {
            if (rules.get().equals(Ruleset.Random)) {
                int luckyIndex = ThreadLocalRandom.current().nextInt(Ruleset.values().length);

                Ruleset rule = Ruleset.values()[luckyIndex];

                if (rule.equals(Ruleset.Random)) {
                    rule = Ruleset.Standard;
                }

                switch (rule) {
                    case Cyclic -> seedDensity.set(0.69);
                    case Amoeba -> seedDensity.set(0.469);
                    case Sunspots -> seedDensity.set(0.420);
                    case Stardust -> seedDensity.set(1.0);
                    default -> seedDensity.set(0.33);
                }

                if (rule.equals(Ruleset.Stardust)) {
                    this.updateInterval.set(2);
                } else {
                    this.updateInterval.set(1);
                }

                if (rule.equals(Ruleset.Custom)) {
                    gameRules = parseRules(rule, customRules.get());
                } else {
                    gameRules = parseRules(rule, rule.rules);
                }
            } else if (rules.get().equals(Ruleset.Custom)) {
                gameRules = parseRules(rules.get(), customRules.get());
            } else {
                gameRules = parseRules(rules.get(), rules.get().rules);
            }

            timer = 0;
            resetTimer = 0;
            generation = 0;
            biggestAge = 0;
            calculateSize();
            seedSimulation();
            firstTick = false;
            timestamp = System.currentTimeMillis();
            offset = ThreadLocalRandom.current().nextInt(256);
        }

        if (resetTimer > 0) {
            --resetTimer;
            if (resetTimer <= 1) {
                resetTimer = 0;
                firstTick = true;
            }
        }

        ++timer;
        if (timer >= updateInterval.get()) {
            timer = 0;
            tickSimulation();
        }
    }

    @Override
    public void render(HudRenderer hud) {
        if (shouldHide() || firstTick) return;
        if (gameRules.cyclic || !colorScheme.get().equals(ColorScheme.Bichromatic)) {
            for (int x = 0; x < GRID_SIZE; x++) {
                for (int y = 0; y < GRID_SIZE; y++) {
                    Color color;
                    if (gameRules.cyclic) {
                        if (grid[x][y] <= cyclicStates.get() / 2) {
                            color = deadColor.get();
                        } else color = getColorByValue(x, y);
                    } else if (grid[x][y] <= 0) {
                        color = deadColor.get();
                    } else if (colorScheme.get().equals(ColorScheme.Trippy)) {
                        color = getColorByValue(x, y);
                    } else if (colorScheme.get().equals(ColorScheme.Stabilizing)) {
                        if (grid[x][y] < minAlpha.get()) {
                            color = getColorByValue(x, y);
                        } else {
                            color = new Color(
                                aliveColor.get().r,
                                aliveColor.get().g,
                                aliveColor.get().b,
                                MathHelper.clamp(grid[x][y], minAlpha.get(), maxAlpha.get())
                            );
                        }
                    } else {
                        color = new Color(
                            aliveColor.get().r,
                            aliveColor.get().g,
                            aliveColor.get().b,
                            MathHelper.clamp(grid[x][y], minAlpha.get(), maxAlpha.get())
                        );
                    }
                    hud.quad(this.x + x * CELL_SIZE, this.y + y * CELL_SIZE, CELL_SIZE, CELL_SIZE, color);
                }
            }
        } else {
            for (int x = 0; x < GRID_SIZE; x++) {
                for (int y = 0; y < GRID_SIZE; y++) {
                    Color color = grid[x][y] >= 1 ? aliveColor.get() : deadColor.get();
                    hud.quad(this.x + x * CELL_SIZE, this.y + y * CELL_SIZE, CELL_SIZE, CELL_SIZE, color);
                }
            }
        }
    }

    private boolean shouldHide() {
        if (!isVisible) return true;
        switch (visibility.get()) {
            case Widgets -> {
                if (!(mc.currentScreen instanceof WidgetScreen)) return true;
            }
            case Windows -> {
                if (mc.currentScreen == null) return true;
            }
        }

        return false;
    }

    private boolean shouldMutate(int aliveNeighbors, boolean alive) {
        return (aliveNeighbors > 0
            || (rules.get().equals(Ruleset.Stardust)
            || (rules.get().equals(Ruleset.Custom) && customRules.get().toUpperCase().contains("R+"))))
            && ThreadLocalRandom.current().nextDouble() <= (alive ? mutateDeadRate.get() : mutateAliveRate.get());
    }

    private void calculateSize() {
        CELL_SIZE = cellSize.get();
        GRID_SIZE = gridSize.get();
        grid = new int[GRID_SIZE][GRID_SIZE];
        buffer = new int[GRID_SIZE][GRID_SIZE];
        setSize(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
    }

    private void tickSimulation() {
        boolean[][] mutated = new boolean[GRID_SIZE][GRID_SIZE];

        ++generation;
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                if (gameRules.cyclic) {
                    int current = grid[x][y];
                    int newState = (current + 1) % cyclicStates.get();
                    int neighbors = countNeighborsWithState(x, y, newState);

                    if (neighbors >= cyclicRate.get()) {
                        buffer[x][y] = newState;
                    } else {
                        buffer[x][y] = current;
                    }

                    if (resetTimer <= 0 && System.currentTimeMillis() - timestamp >= cyclicDuration.get() * 1000) {
                        resetTimer = 100;
                    }
                } else {
                    if (mutated[x][y]) continue;
                    boolean randomize = gameRules.randomize;
                    Set<Integer> birth = gameRules.birthSet;
                    Set<Integer> survival = gameRules.survivalSet;
                    int aliveNeighbors = countAliveNeighbors(x, y);
                    if (maxGeneration.get() > 0 && generation >= maxGeneration.get() && resetTimer <= 0) {
                        // Max generation triggers sunsetting the simulation
                        resetTimer = 100;
                    }

                    if (grid[x][y] != DEAD) {
                        if (grid[x][y] > biggestAge) biggestAge = grid[x][y];
                        if (maxAge.get() > 0 && biggestAge >= maxAge.get() && resetTimer <= 0) {
                            // Max age triggers sunsetting the simulation
                            resetTimer = 100;
                        }

                        if (randomize && shouldMutate(aliveNeighbors, true)) {
                            mutateNeighbors(
                                x, y, DEAD,
                                ThreadLocalRandom.current().nextInt(9), buffer, mutated
                            );
                        } else if (survival.contains(aliveNeighbors)) {
                            buffer[x][y] = grid[x][y] + ALIVE; // aging
                        } else {
                            buffer[x][y] = DEAD;
                        }
                    } else {
                        if (randomize && shouldMutate(aliveNeighbors, false)) {
                            mutateNeighbors(
                                x, y, ALIVE,
                                ThreadLocalRandom.current().nextInt(9), buffer, mutated
                            );
                        } else if (birth.contains(aliveNeighbors)) {
                            buffer[x][y] = ALIVE;
                        } else {
                            buffer[x][y] = DEAD;
                        }
                    }
                }
            }
        }

        int[][] temp = grid;
        grid = buffer;
        buffer = temp;
    }

    private void seedSimulation() {
        if (gameRules.cyclic) {
            for (int x = 0; x < GRID_SIZE; x++) {
                for (int y = 0; y < GRID_SIZE; y++) {
                    if (ThreadLocalRandom.current().nextDouble() < seedDensity.get()) {
                        grid[x][y] = ThreadLocalRandom.current().nextInt(cyclicStates.get());
                    } else {
                        grid[x][y] = DEAD;
                    }
                }
            }
        } else {
            for (int x = 0; x < GRID_SIZE; x++) {
                for (int y = 0; y < GRID_SIZE; y++) {
                    grid[x][y] = Math.random() < seedDensity.get() ? ALIVE : DEAD;
                }
            }
        }

    }

    private static final int[][] NEIGHBOR_OFFSETS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1},           {0, 1},
        {1, -1},  {1, 0},  {1, 1}
    };

    private int countAliveNeighbors(int x, int y) {
        int count = 0;
        for (int[] offset : NEIGHBOR_OFFSETS) {
            int nx = (x + offset[0] + GRID_SIZE) % GRID_SIZE;
            int ny = (y + offset[1] + GRID_SIZE) % GRID_SIZE;
            if (grid[nx][ny] != DEAD) count++;
        }

        return count;
    }

    private int countNeighborsWithState(int x, int y, int state) {
        int count = 0;
        for (int[] offset : NEIGHBOR_OFFSETS) {
            int nx = (x + offset[0] + GRID_SIZE) % GRID_SIZE;
            int ny = (y + offset[1] + GRID_SIZE) % GRID_SIZE;
            if (grid[nx][ny] == state) count++;
        }

        return count;
    }

    private void mutateNeighbors(int x, int y, int mutation, int amount, int[][] buffer, boolean[][] mutated) {
        int mutations = 0;
        for (int[] offset : NEIGHBOR_OFFSETS) {
            int nx = (x + offset[0] + GRID_SIZE) % GRID_SIZE;
            int ny = (y + offset[1] + GRID_SIZE) % GRID_SIZE;

            if (!mutated[nx][ny]) {
                mutated[nx][ny] = true;
                buffer[nx][ny] = mutation;
                if (++mutations >= amount) break;
            }
        }
    }

    private Color getColorByValue(int x, int y) {
        float hue = ((grid[x][y] + offset) * 0.6180339887f) % 1.0f;

        float saturation = 0.8f;
        float brightness = 0.9f;
        java.awt.Color temp = java.awt.Color.getHSBColor(hue, saturation, brightness);
        return new Color(
            temp.getRed() / 255f,
            temp.getGreen() / 255f,
            temp.getBlue() / 255f,
            MathHelper.clamp(grid[x][y], minAlpha.get(), maxAlpha.get())
        );
    }

    private Rules parseRules(Ruleset set, String rules) {
        if (rules.toUpperCase().startsWith("C")) {
            return new Rules(set, new HashSet<>(), new HashSet<>(), false, true);
        }

        if (rules.isBlank() || !rules.contains("/")) {
            MsgUtil.updateModuleMsg(
                "Invalid rule syntax§c..! §7Cannot parse: \"§c" + rules + "§7\"§c..!",
                "Conway", "invalidRuleset".hashCode()
            );
            return Rules.STANDARD;
        }

        String[] parts = rules.toUpperCase().split("/");

        boolean isCyclic = false;
        boolean hasRandom = false;
        Set<Integer> birthSet = new HashSet<>();
        Set<Integer> survivalSet = new HashSet<>();
        for (String part : parts) {
            if (part.startsWith("B")) {
                for (char c : part.toCharArray()) {
                    if (Character.isDigit(c)) {
                        int value = Integer.parseInt(String.valueOf(c));
                        if (value < 9) {
                            birthSet.add(value);
                        }
                    }
                }
            } else if (part.startsWith("S")) {
                for (char c : part.toCharArray()) {
                    if (Character.isDigit(c)) {
                        int value = Integer.parseInt(String.valueOf(c));
                        if (value < 9) {
                            survivalSet.add(value);
                        }
                    }
                }
            } else if (part.startsWith("R")) {
                hasRandom = true;
            } else if (part.startsWith("C")) {
                isCyclic = true;
            }
        }

        if (birthSet.contains(0)) {
            birthSet.clear();
        }
        if (survivalSet.contains(0)) {
            survivalSet.clear();
        }

        if (isCyclic) {
            return new Rules(set, birthSet, survivalSet, hasRandom, true);
        } else if (birthSet.isEmpty() && survivalSet.isEmpty()) {
            MsgUtil.updateModuleMsg(
                "Invalid Ruleset§c..! §7The birth & survival sets are both empty§c..!",
                "Conway", "badConwayRules".hashCode()
            );
            return Rules.STANDARD;
        } else if (birthSet.isEmpty()) {
            MsgUtil.updateModuleMsg(
                "Empty birth set§c..! §7The simulation may not work, or may be short-lived§c..!",
                "Conway", "badConwayRules".hashCode()
            );
            return new Rules(
                set, birthSet, survivalSet, hasRandom, false
            );
        } else if (survivalSet.isEmpty()) {
            MsgUtil.updateModuleMsg(
                "Empty survival set§c..! §7The simulation may not work, or may be short-lived§c..!",
                "Conway", "badConwayRules".hashCode()
            );
            return new Rules(
                set, birthSet, survivalSet, hasRandom, false
            );
        }

        return new Rules(set, birthSet, survivalSet, hasRandom, false);
    }

    private void alterOwnDescription(Ruleset set) {
        ((SettingAccessor) rules).setDescription(set.asString());
    }

    public record Rules(Ruleset rules, Set<Integer> birthSet, Set<Integer> survivalSet, boolean randomize, boolean cyclic) {
        public static Rules STANDARD = new Rules(
            Ruleset.Standard, Set.of(3), Set.of(2, 3), false, false
        );
    }
}
