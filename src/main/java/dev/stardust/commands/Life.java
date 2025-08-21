package dev.stardust.commands;

import java.text.DecimalFormat;
import dev.stardust.util.MsgUtil;
import dev.stardust.hud.ConwayHud;
import dev.stardust.util.StardustUtil;
import net.minecraft.command.CommandSource;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Life extends Command {
    public Life() {
        super("life", "Companion command for the Conway's Game of Life HUD Module.");
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private int timer = 0;
    private int monRate = 20;
    private boolean monitoring = false;

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        timer = 0;
        monitoring = false;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        timer = 0;
        monitoring = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate() || !monitoring) return;

        ++timer;
        if (timer >= monRate) {
            timer = 0;
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    if (!hud.firstTick) {
                        MsgUtil.updateMsg(
                            getSimulationStats(hud),
                            "monitorConway".hashCode()
                        );
                    }
                }
            });
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("new").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.firstTick = true;
                    MsgUtil.sendModuleMsg("Simulation reset§a..!", "Conway");
                }
            });

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("pause").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.isPaused = !hud.isPaused;
                    MsgUtil.sendModuleMsg("Simulation " + (hud.isPaused ? "§cpaused§7." : "§aresumed§7."), "Conway");
                }
            });

            return SINGLE_SUCCESS;
        }));
        builder.then(literal("resume").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.isPaused = !hud.isPaused;
                    MsgUtil.sendModuleMsg("Simulation " + (hud.isPaused ? "§cpaused§7." : "§aresumed§7."), "Conway");
                }
            });

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("hide").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.isVisible = !hud.isVisible;
                    MsgUtil.sendModuleMsg("Simulation " + (hud.isVisible ? "§ashown§7." : "§chidden§7."), "Conway");
                }
            });

            return SINGLE_SUCCESS;
        }));
        builder.then(literal("show").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.isVisible = !hud.isVisible;
                    MsgUtil.sendModuleMsg("Simulation " + (hud.isVisible ? "§ashown§7." : "§chidden§7."), "Conway");
                }
            });

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("visibility").then(literal("always").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.visibility.set(ConwayHud.Visibility.Always);
                    MsgUtil.sendModuleMsg("Successfully updated visibility.", "Conway");
                }
            });

            return SINGLE_SUCCESS;
        })));
        builder.then(literal("visibility").then(literal("windows").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.visibility.set(ConwayHud.Visibility.Windows);
                    MsgUtil.sendModuleMsg("Successfully updated visibility.", "Conway");
                }
            });

            return SINGLE_SUCCESS;
        })));
        builder.then(literal("visibility").then(literal("widgets").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    hud.visibility.set(ConwayHud.Visibility.Widgets);
                    MsgUtil.sendModuleMsg("Successfully updated visibility.", "Conway");
                }
            });

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("step").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    if (!hud.isPaused) {
                        MsgUtil.sendModuleMsg("Simulation must be paused to step manually§c..!", "Conway");
                    } else {
                        hud.stepSimulation();
                    }
                }
            });

            return SINGLE_SUCCESS;
        }));
        builder.then(literal("step").then(argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
            int step = IntegerArgumentType.getInteger(ctx, "amount");
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    if (!hud.isPaused) {
                        MsgUtil.sendModuleMsg("Simulation must be paused to step manually§c..!", "Conway");
                    } else {
                        for (int n = 0; n < step; n++) {
                            hud.stepSimulation();
                        }
                    }
                }
            });

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("stats").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud hud) {
                    MsgUtil.sendModuleMsg("\n" + getSimulationStats(hud), "Conway");
                }
            });

            return SINGLE_SUCCESS;
        }));
        builder.then(literal("monitor").executes(ctx -> {
            Hud.get().forEach(element -> {
                if (element instanceof ConwayHud) {
                    monitoring = !monitoring;
                }
            });

            return SINGLE_SUCCESS;
        }));
        builder.then(literal("monitor").then(argument("tickRate", IntegerArgumentType.integer(1)).executes(ctx -> {
            monRate = IntegerArgumentType.getInteger(ctx, "tickRate");

            return SINGLE_SUCCESS;
        })));
    }

    private String getSimulationStats(ConwayHud hud) {
        ConwayHud.Ruleset rules = hud.gameRules.rules();

        String name;
        if (rules.equals(ConwayHud.Ruleset.Custom)) {
            name = "Custom(" + hud.customRules.get().toUpperCase() + ")";
        } else {
            name = rules.asString();
        }
        StringBuilder sb = new StringBuilder();

        sb.append("§7Ruleset§f: §7");

        boolean inBracket = false;
        for (char c : name.toCharArray()) {
            if (c == '(') {
                inBracket = true;
                sb.append("§8§o").append(c);
            } else if (c == ')') {
                inBracket = false;
                sb.append("§8§o").append(c).append("§7§o");
            } else if (inBracket && Character.isDigit(c) || c == '+') {
                sb.append(StardustUtil.rCC()).append("§o").append(c);
            } else if (inBracket && c == 'B' || c == 'S' || c == 'C' || c == 'R') {
                sb.append("§f§o").append(c);
            } else if (inBracket && c == '/') {
                sb.append("§8§o").append(c);
            } else {
                sb.append(c);
            }
        }

        DecimalFormat df = new DecimalFormat("#.##");
        sb.append("\n§7Cell size§f: ").append("§e§o").append(hud.getCellSize());
        sb.append("\n§7Grid size§f: ").append("§e§o").append(hud.getGridSize());
        sb.append("\n§7Generation§f: ").append("§5§o").append(hud.getGeneration());
        sb.append("\n§7Oldest cell§f: ").append("§3§o").append(hud.getMaxAge());

        int[] cellCount = hud.getCellCount();
        int totalCells = hud.getGridSize() * hud.getGridSize();
        double percentCellsAlive = (cellCount[1] / (double) totalCells) * 100.0;
        sb.append("\n§7Survival rate§f: ").append("§a§o").append(df.format(percentCellsAlive)).append("§8§o%");
        sb.append("\n§7Simulation runtime§f: ").append("§6§o").append(hud.getRuntime() / 1000).append(" seconds");
        sb.append("\n§7Cell count §8§o[§a§oalive§8§o/§c§odead§8§o]§f: §8§o[").append("§a§o")
            .append(cellCount[1]).append("§8§o/").append("§c§o").append(cellCount[0]).append("§8§o]");

        return sb.toString();
    }
}
