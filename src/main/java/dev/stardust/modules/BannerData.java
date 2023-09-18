package dev.stardust.modules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import dev.stardust.Stardust;
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

    public BannerData() {
        super(
            Stardust.CATEGORY, "BannerData", "View fancy-formatted NBT data for banners."
        );
    }

    @Override
    public void onDeactivate() {
        this.totalTicksEnabled = 0;
    }

    @EventHandler
    private void onRightClickBlock(InteractBlockEvent event) {
        if (mc == null || mc.world == null || mc.player == null) return;

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
                if (Objects.equals(cc, TextColor.Random.label)) {
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
                        .append(StardustUtil.patternNameFromID(patternEntry.getFirst().value().getId())).append("\n");
                }

                String bannerData = patternsList.toString().trim();
                mc.player.sendMessage(Text.of(bannerData));

                if (copyToClipboard.get()) {
                    NbtCompound metadata = banner.toInitialChunkDataNbt();
                    mc.keyboard.setClipboard(metadata.toString());
                    mc.player.sendMessage(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7"
                                 + txtFormat + "Copied NBT data to clipboard§7."
                        )
                    );
                }

                this.lastEventPos = pos;
            } else if (blockEntity instanceof SignBlockEntity sign) {
                NbtCompound metadata = sign.toInitialChunkDataNbt();
                if (copyToClipboard.get()) {
                    mc.keyboard.setClipboard(metadata.toString());
                    mc.player.sendMessage(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7§o"
                                + "Copied NBT data to clipboard."
                        )
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
        if (!this.isActive()) return;
        if (this.totalTicksEnabled >= 65535) this.totalTicksEnabled = 0;

        // InteractBlockEvents fire twice sometimes, so we must cull the extra ones manually.
        this.totalTicksEnabled++;
        if (this.totalTicksEnabled % 20 == 0) {
            this.lastEventPos = this.lastEventPos.add(2000000, 2000000, 2000000);
        }
    }
}
