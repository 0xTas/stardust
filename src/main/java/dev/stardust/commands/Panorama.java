package dev.stardust.commands;

import java.io.File;
import java.nio.file.*;
import dev.stardust.Stardust;
import javax.imageio.ImageIO;
import net.minecraft.text.Text;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.command.CommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import meteordevelopment.orbit.EventHandler;
import java.util.concurrent.ThreadLocalRandom;
import java.nio.file.attribute.BasicFileAttributes;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.util.ScreenshotRecorder;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.files.StreamUtils;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Panorama extends Command {
    public Panorama() {
        super("panorama", "Takes a panorama and saves it to a custom resource pack for the main menu screen.", "take");
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private int timer = 10;
    private int screenshot = 0;
    private float preYaw = 69f;
    private float prevYaw = 69f;
    private float prePitch = 69f;
    private float prevPitch = 69f;
    private int preWidth = 42069;
    private int preHeight = 42069;

    private boolean isWarming = false;
    private boolean takingPanorama = false;
    private boolean readyToAssemble = false;
    @Nullable
    private Path currentPanoramaDir = null;
    @Nullable
    private MinecraftClient instance = null;

    private void takeWarmedScreenshot() {
        if (currentPanoramaDir == null || instance == null) return;

        ScreenshotRecorder.saveScreenshot(
            currentPanoramaDir.toFile(),
            "panorama_"+screenshot+".png",
            instance.getFramebuffer(), msg -> {}
        );

        ++screenshot;
        isWarming = false;
    }

    private void startPanoramaProcess(String name) {
        Path panoramaDir = FabricLoader.getInstance().getGameDir().resolve("meteor-client/panoramas/"+name);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        try {
            //noinspection ResultOfMethodCallIgnored
            panoramaDir.toFile().mkdirs();
        }catch (Exception err) {
            err.printStackTrace();
        }

        instance = mc;
        screenshot = 0;
        preYaw = mc.player.getYaw();
        prevYaw = mc.player.prevYaw;
        prePitch = mc.player.getPitch();
        prevPitch = mc.player.prevPitch;
        currentPanoramaDir = panoramaDir;
        preWidth = mc.getWindow().getFramebufferWidth();
        preHeight = mc.getWindow().getFramebufferHeight();

        instance.getWindow().setFramebufferWidth(4096);
        instance.getWindow().setFramebufferHeight(4096);
        instance.getFramebuffer().resize(4096, 4096, MinecraftClient.IS_SYSTEM_MAC);

        takingPanorama = true;
    }

    private void assembleResourcePack() {
        if (instance == null || currentPanoramaDir == null) return;
        Path resourcePacks = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
        Path screenshotsFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client/panoramas/"+currentPanoramaDir.getFileName()+"/screenshots");
        Path customBaseFolder = resourcePacks.resolve(currentPanoramaDir.getFileName());
        Path customPackFolder = customBaseFolder.resolve("assets/minecraft/textures/gui/title/background/");
        //noinspection ResultOfMethodCallIgnored
        customPackFolder.toFile().mkdirs();

        String mcMeta = "{"+"\n"+"    \"pack\": {\"pack_format\": 15,\"description\": \"\\u00A73"+currentPanoramaDir.getFileName()+"_panorama\\n\\u00A72\\u00A7oGenerated by Stardust\\u00A7d\\u00A7o\\u2728\"}"+"\n"+"}";
        try {
            int luckyNumber = ThreadLocalRandom.current().nextInt(6);
            Files.walkFileTree(screenshotsFolder, new SimpleFileVisitor<>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    StreamUtils.copy(file.toFile(), customPackFolder.resolve(file.getFileName()).toFile());

                    // Pick a random pack.png from the screenshot set
                    if (file.getFileName().toString().contains(String.valueOf(luckyNumber))) {
                        StreamUtils.copy(file.toFile(), customBaseFolder.resolve("pack.png").toFile());
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            File packMeta = customBaseFolder.resolve("pack.mcmeta").toFile();
            if (!packMeta.createNewFile()) {
                error("[Stardust] Failed to assemble custom resource pack! Does a pack of that name already exist?");
                Stardust.LOG.error("[Stardust] Failed to assemble custom resource pack.");
                return;
            }
            Files.write(packMeta.toPath(), mcMeta.getBytes());

            // Remove the ugly white-to-black vignette overlay that Minecraft uses for its menu panoramas by default.
            // You can restore the original look by deleting panorama_overlay.png from your generated resource pack.
            BufferedImage transparentOverlay = new BufferedImage(16, 128, BufferedImage.TYPE_INT_ARGB);
            File overlayImage = customPackFolder.resolve("panorama_overlay.png").toFile();
            ImageIO.write(transparentOverlay, "PNG", overlayImage);
        } catch (Exception err) {
            Stardust.LOG.error("[Stardust] "+err);
            error(err.toString());
        }

        readyToAssemble = false;
        if (instance.player != null) {
            instance.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
            instance.player.sendMessage(Text.of("§8<" + StardustUtil.rCC() + "✨§8> §3§oYour resource pack is ready to be enabled§f§o!"));
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            argument("name", StringArgumentType.word()).executes(ctx -> {
                String name = ctx.getArgument("name", String.class);

                startPanoramaProcess(name);
                return SINGLE_SUCCESS;
            })
        );
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (instance == null || instance.player == null || currentPanoramaDir == null) return;

        if (timer > 0) {
            --timer;
            return;
        } else {
            timer = 10;
        }

        if (readyToAssemble) {
            assembleResourcePack();
        }
        if (!takingPanorama) return;

        // When using Iris mod, the vanilla takePanorama impl will produce blank screenshots.
        // Original issue: https://github.com/IrisShaders/Iris/issues/2196#issuecomment-1873043947
        // To overcome this, I've reimplemented the vanilla takePanorama method in a way that works with Iris.
        switch (screenshot) {
            case 0 -> {
                if (!isWarming) {
                    instance.gameRenderer.setRenderingPanorama(true);
                    instance.gameRenderer.setBlockOutlineEnabled(false);
                    instance.worldRenderer.reloadTransparencyPostProcessor();
                    if (!instance.options.hudHidden) instance.options.hudHidden = true;
                    instance.player.setYaw(preYaw);
                    instance.player.setPitch(0f);
                    isWarming = true;
                } else takeWarmedScreenshot();
            }
            case 1 -> {
                if (!isWarming) {
                    instance.player.setYaw((preYaw + 90f) % 360f);
                    instance.player.setPitch(0f);
                    isWarming = true;
                } else takeWarmedScreenshot();
            }
            case 2 -> {
                if (!isWarming) {
                    instance.player.setYaw((preYaw + 180f) % 360f);
                    instance.player.setPitch(0f);
                    isWarming = true;
                } else takeWarmedScreenshot();
            }
            case 3 -> {
                if (!isWarming) {
                    instance.player.setYaw((preYaw - 90f) % 360f);
                    instance.player.setPitch(0f);
                    isWarming = true;
                } else takeWarmedScreenshot();
            }
            case 4 -> {
                if (!isWarming) {
                    instance.player.setYaw(preYaw);
                    instance.player.setPitch(-90f);
                    isWarming = true;
                } else takeWarmedScreenshot();
            }
            default -> {
                if (!isWarming) {
                    instance.player.setYaw(preYaw);
                    instance.player.setPitch(90f);
                    isWarming = true;
                } else {
                    takeWarmedScreenshot();
                    takingPanorama = false;
                    instance.player.setYaw(preYaw);
                    instance.player.setPitch(prePitch);
                    instance.player.prevYaw = prevYaw;
                    instance.player.prevPitch = prevPitch;
                    instance.gameRenderer.setRenderingPanorama(false);
                    instance.gameRenderer.setBlockOutlineEnabled(true);
                    instance.getWindow().setFramebufferWidth(preWidth);
                    instance.getWindow().setFramebufferHeight(preHeight);
                    if (instance.options.hudHidden) instance.options.hudHidden = false;
                    instance.getFramebuffer().resize(preWidth, preHeight, MinecraftClient.IS_SYSTEM_MAC);
                    instance.worldRenderer.reloadTransparencyPostProcessor();

                    timer = 100; // wait a few seconds for the screenshot files to get fully written to disk,
                    readyToAssemble = true; // and then copy them into a resource pack (this avoids copying empty files.)
                    instance.player.sendMessage(
                        Text.of("§8<§2§o✨§8> §8§oFinalizing resource pack§2§o, §8§oplease wait§2§o...")
                    );
                }
            }
        }
    }
}