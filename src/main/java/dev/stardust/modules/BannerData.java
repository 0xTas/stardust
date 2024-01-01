package dev.stardust.modules;

import java.util.List;
import java.util.Optional;
import dev.stardust.Stardust;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.block.entity.*;
import net.minecraft.nbt.NbtCompound;
import dev.stardust.util.StardustUtil;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import dev.stardust.util.StardustUtil.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.registry.entry.RegistryEntry;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;


/**
* @author Tas [0xTas] <root@0xTas.dev>
*/
public class BannerData extends Module {
    public BannerData() {
        super(
            Stardust.CATEGORY, "BannerData", "View fancy-formatted NBT data for banners."
        );
    }

    private final Setting<StardustUtil.TextFormat> textFormatSetting = settings.getDefaultGroup().add(
        new EnumSetting.Builder<TextFormat>()
            .name("Text Formatting")
            .description("Apply formatting to displayed NBT text.")
            .defaultValue(StardustUtil.TextFormat.Italic)
            .build()
    );

    private final Setting<TextColor> flairColor = settings.getDefaultGroup().add(
        new EnumSetting.Builder<TextColor>()
            .name("Accent Color")
            .defaultValue(TextColor.Random)
            .build()
    );

    private final Setting<Boolean> copyToClipboard = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("Copy to Clipboard")
        .description("Copy NBT data to your clipboard.")
        .defaultValue(false)
        .build()
    );

    private int totalTicksEnabled = 0;
    private BlockPos lastEventPos = new BlockPos(0,0,0);

    private String patternNameFromID(String id) {
        return switch (id) {
            case "b" -> "Base";
            case "bs" -> "Base Fess (Bottom Stripe)";
            case "ts" -> "Chief (Top Stripe)";
            case "ls" -> "Pale Dexter (Left Stripe)";
            case "rs" -> "Pale Sinister (Right Stripe)";
            case "cs" -> "Pale (Center Vertical Stripe)";
            case "ms" -> "Fess (Middle Horizontal Stripe)";
            case "drs" -> "Bend (Down Right Stripe)";
            case "dls" -> "Bend Sinister (Down Left Stripe)";
            case "ss" -> "Paly (Small Vertical Stripes)";
            case "cr" -> "Saltire (Diagonal Cross [X])";
            case "sc" -> "Cross (Square Cross [+])";
            case "ld" -> "Per Bend Sinister (Left of Diagonal)";
            case "rud" -> "Per Bend (Right of Upside-down Diagonal)";
            case "lud" -> "Per Bend Inverted (Left of Upside-Down Diagonal)";
            case "rd" -> "Per Bend Sinister Inverted (Right of Diagonal)";
            case "vh" -> "Per Pale (Vertical Half Left)";
            case "vhr" -> "Per Pale Inverted (Vertical Half Right)";
            case "hh" -> "Per Fess (Horizontal Half Top)";
            case "hhb" -> "Per Fess Inverted (Horizontal Half Bottom)";
            case "bl" -> "Base Dexter Canton (Bottom Left Corner)";
            case "br" -> "Base Sinister Canton (Bottom Right Corner)";
            case "tl" -> "Chief Dexter Canton (Top Left Corner)";
            case "tr" -> "Chief Sinister Canton (Top Right Corner)";
            case "bt" -> "Chevron (Bottom Triangle)";
            case "tt" -> "Inverted Chevron (Top Triangle)";
            case "bts" -> "Base Indented (Bottom Sawtooth)";
            case "tts" -> "Chief Indented (Top Sawtooth)";
            case "mc" -> "Roundel (Middle Circle)";
            case "mr" -> "Lozenge (Middle Rhombus)";
            case "bo" -> "Bordure (Border)";
            case "cbo" -> "Bordure Indented (Curly Border)";
            case "bri" -> "Field Masoned (Brick)";
            case "gra" -> "Gradient";
            case "gru" -> "Base Gradient";
            case "cre" -> "Creeper Charge";
            case "sku" -> "Skull Charge";
            case "flo" -> "Flower Charge";
            case "moj" -> "Thing (Mojang)";
            case "glb" -> "Globe";
            case "pig" -> "Snout (Piglin)";
            default -> "Oasis Sigil";
        };
    }


    @Override
    public void onDeactivate() {
        this.totalTicksEnabled = 0;
    }

    @EventHandler
    private void onRightClickBlock(InteractBlockEvent event) {
        if (mc.world == null || mc.player == null) return;

        BlockHitResult result = event.result;
        if (this.isActive() && result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = result.getBlockPos();
            BlockEntity blockEntity = mc.world.getBlockEntity(pos);

            if (blockEntity == null) return;
            // InteractBlockEvents fire twice sometimes, so we must cull the extra ones manually.
            if (this.lastEventPos == pos) return;

            if (blockEntity instanceof BannerBlockEntity banner) {
                StringBuilder customName = new StringBuilder();
                if (banner.getCustomName() != null) {
                    banner.getCustomName().visit(it -> {
                        customName.append(it);
                        return Optional.empty();
                    });
                }
                String bannerName = customName.toString();
                String baseColor = banner.getColorForState().name();
                baseColor = baseColor.substring(0, 1).toUpperCase()+baseColor.substring(1);

                List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns =  banner.getPatterns();

                String txtFormat = textFormatSetting.get().label;
                StringBuilder patternsList = new StringBuilder();

                String cc = flairColor.get().label;
                if (cc.equals(TextColor.Random.label)) {
                    cc = StardustUtil.rCC();
                }
                if (!bannerName.trim().isEmpty()) {
                    patternsList.append("§8<").append(cc).append("§o✨§r§8> ")
                        .append(cc).append(txtFormat).append(bannerName).append("\n§r");
                } else {
                    patternsList.append("§8<").append(cc).append("§o✨§r§8> ")
                        .append(cc).append(txtFormat).append(baseColor).append(" Banner\n§r");
                }

                for (Pair<RegistryEntry<BannerPattern>, DyeColor> patternEntry : patterns) {
                    String patternColor = patternEntry.getSecond().name().charAt(0)
                        +patternEntry.getSecond().name().substring(1).toLowerCase();

                    if (patternColor.contains("_")) {
                        int i = patternColor.indexOf("_");
                        patternColor = patternColor.substring(0, i)+" "+patternColor.substring(i+1, i+2).toUpperCase()
                            +patternColor.substring(i+2);
                    }

                    patternsList.append(cc).append("   ◦ ").append("§7")
                        .append(txtFormat).append(patternColor).append(" ")
                        .append(patternNameFromID(patternEntry.getFirst().value().getId())).append("\n");
                }

                String bannerData = patternsList.toString().trim();
                mc.player.sendMessage(Text.of(bannerData));

                if (copyToClipboard.get()) {
                    NbtCompound metadata = banner.toInitialChunkDataNbt();
                    mc.keyboard.setClipboard(metadata.toString());
                    ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7"
                                + txtFormat + "Copied NBT data to clipboard§7."
                        ),
                        "Clipboard update".hashCode()
                    );
                }

                this.lastEventPos = pos;
            } else if (blockEntity instanceof SignBlockEntity sign) {
                NbtCompound metadata = sign.toInitialChunkDataNbt();
                if (copyToClipboard.get()) {
                    mc.keyboard.setClipboard(metadata.toString());
                    ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7§o"
                                + "Copied NBT data to clipboard."
                        ),
                        "Clipboard update".hashCode()
                    );
                } else {
                    mc.player.sendMessage(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7"+metadata
                        )
                    );
                }

                this.lastEventPos = pos;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.totalTicksEnabled >= 65535) this.totalTicksEnabled = 0;

        // InteractBlockEvents fire twice sometimes, so we must cull the extra ones manually.
        this.totalTicksEnabled++;
        if (this.totalTicksEnabled % 20 == 0) {
            this.lastEventPos = this.lastEventPos.add(2000000, 2000000, 2000000);
        }
    }
}
