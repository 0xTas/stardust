package dev.stardust.modules;

import dev.stardust.Stardust;
import net.minecraft.text.Text;
import dev.stardust.util.StardustUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import dev.stardust.mixin.GameOptionsAccessor;
import net.minecraft.client.option.GameOptions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.option.SimpleOption;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 */
public class AutoDrawDistance extends Module {

    private final Setting<Integer> fpsTarget = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("FPS Target")
            .description("Best if you cap your FPS to your monitor's refresh rate (or lower) and use that value.")
            .range(30, 240)
            .sliderRange(30, 240)
            .defaultValue(60)
            .build()
    );

    private final Setting<Integer> minDistance = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Minimum Render Distance")
            .description("The minimum desired draw distance.")
            .range(2, 8)
            .sliderRange(2, 8)
            .defaultValue(4)
            .build()
    );

    private final Setting<Integer> maxDistance = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Maximum Render Distance")
            .description("The maximum desired draw distance.")
            .range(10, 64)
            .sliderRange(10, 32)
            .defaultValue(12)
            .build()
    );

    private final Setting<Integer> sweetSpotDelay = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Increase Delay")
            .description("Delay before trying to increase render distance when FPS target is satisfied.")
            .range(20, 1000)
            .sliderRange(100, 1000)
            .defaultValue(420)
            .build()
    );

    private final Setting<Boolean> verbose = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Debug")
            .description("Output to chat whenever view distance adjustments are made.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> reportFPS = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Report FPS")
            .description("Report average FPS numbers along with debug messages.")
            .defaultValue(false)
            .visible(verbose::get)
            .build()
    );

    private int justIncreased = 0;
    private int sweetSpotCounter = 0;
    private int totalTicksEnabled = 0;
    private boolean sweetSpot = false;
    private final IntArrayList fpsData = new IntArrayList();

    public AutoDrawDistance() {
        super(
            Stardust.CATEGORY,
            "AutoDrawDistance",
            "Automatically adjusts your render distance to maintain an FPS target."
        );
    }

    private void updateDrawDistance(int distance) {
        boolean bl = mc.is64Bit();
        boolean bl2 = bl && Runtime.getRuntime().maxMemory() >= 1000000000L;
        SimpleOption<Integer> viewDistance = new SimpleOption<>(
            "options.renderDistance", SimpleOption.emptyTooltip(),
            (optionText, value) -> GameOptions.getGenericValueText(optionText, Text.translatable("options.chunks", value)), // yikes
            new SimpleOption.ValidatingIntSliderCallbacks(2, bl2 ? 32 : 16),
            distance, value -> MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate());

        ((GameOptionsAccessor) mc.options).setViewDistance(viewDistance);

        mc.options.sendClientSettings();
        if (verbose.get() && !(mc.player == null)) {
            mc.player.sendMessage(
                Text.of(
                    "§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Updated view distance to§8: §2"+distance+"§7."
                )
            );
        }
    }

    private void tryLowerDrawDistanceDynamic(int drawDistance, int reduceAmount) {
        if (reduceAmount <= 0) return;

        if (drawDistance - reduceAmount < minDistance.get()) {
            tryLowerDrawDistanceDynamic(drawDistance, reduceAmount-1);
        } else {
            updateDrawDistance(drawDistance - reduceAmount);
        }
    }


    @EventHandler
    public void onDeactivate() {
        this.sweetSpotCounter = 0;
        this.totalTicksEnabled = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!this.isActive()) return;
        if (fpsData.size() > 500) fpsData.clear();
        if (this.totalTicksEnabled >= 65535) this.totalTicksEnabled = 0;

        if (sweetSpot) this.sweetSpotCounter++; else sweetSpotCounter = 0;
        if (this.sweetSpotCounter >= sweetSpotDelay.get()) {
            sweetSpot = false;
            sweetSpotCounter = 0;
        }

        int currentFps = mc.getCurrentFps();

        fpsData.add(currentFps);
        this.totalTicksEnabled++;
        if (this.totalTicksEnabled % 10 == 0) {
            int drawDistance = mc.options.getViewDistance().getValue();

            int averageFps = 0;
            for (int point : fpsData) {
                averageFps += point;
            }
            averageFps = averageFps / fpsData.size();

            int closeEnough = 5;
            if (justIncreased > 0) {
                if (justIncreased >= 3 && averageFps >= fpsTarget.get() - closeEnough) {
                    sweetSpot = true;
                    justIncreased = 0;
                    sweetSpotCounter = 0;
                    if (verbose.get() && mc.player != null) {
                        if (reportFPS.get()) {
                            mc.player.sendMessage(
                                Text.of(
                                    "§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Average FPS§8: §2"+averageFps
                                ));
                        }

                        mc.player.sendMessage(
                            Text.of(
                                "§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Entered a §2sweet spot§8!"
                            ));
                    }
                } else {
                    justIncreased++;
                }
                return;
            } else if (sweetSpot) {
                if (averageFps >= fpsTarget.get() - closeEnough) return;
            }

            if (averageFps >= fpsTarget.get() - closeEnough) {
                if (drawDistance >= maxDistance.get()) return;

                justIncreased++;
                fpsData.clear();
                if (verbose.get() && reportFPS.get() && mc.player != null) {
                    mc.player.sendMessage(
                        Text.of(
                            "§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Average FPS§8: §2"+averageFps
                        ));
                }

                updateDrawDistance(drawDistance + 1);
            } else {
                int targetFps = fpsTarget.get();
                if (targetFps - averageFps <= closeEnough) return;
                if (drawDistance <= minDistance.get()) return;

                if (verbose.get() && reportFPS.get() && mc.player != null) {
                    mc.player.sendMessage(
                        Text.of(
                            "§8<"+ StardustUtil.rCC()+"§o✨§r§8> §7Average FPS§8: §2"+averageFps
                        ));
                }

                fpsData.clear();
                if (this.totalTicksEnabled < 250) return;

                int diff = targetFps - averageFps;
                if (diff <= 15) {
                    tryLowerDrawDistanceDynamic(drawDistance, 2);
                } else if (diff <= 30) {
                    tryLowerDrawDistanceDynamic(drawDistance, 4);
                } else if (diff <= 40) {
                    tryLowerDrawDistanceDynamic(drawDistance, 6);
                } else if (diff <= 50) {
                    tryLowerDrawDistanceDynamic(drawDistance, 8);
                } else if (diff <= 60) {
                    tryLowerDrawDistanceDynamic(drawDistance, 10);
                }
            }
        }
    }
}
