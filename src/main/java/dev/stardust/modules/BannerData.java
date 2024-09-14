package dev.stardust.modules;

import java.util.Optional;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.block.entity.*;
import net.minecraft.nbt.NbtCompound;
import dev.stardust.util.StardustUtil;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import dev.stardust.util.StardustUtil.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import net.minecraft.component.type.BannerPatternsComponent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
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

    private final Setting<Boolean> bannerNameOnly = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Names Only")
            .description("Display banner names on right click only.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> signData = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Sign Data")
            .description("Display NBT data when right clicking signs.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> copyToClipboard = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("Copy to Clipboard")
        .description("Copy NBT data to your clipboard.")
        .defaultValue(false)
        .build()
    );

    private int timer = 0;
    private BlockPos lastEventPos = new BlockPos(0,0,0);

    private String patternNameFromAssetID(String id) {
        StringBuilder sb = new StringBuilder();

        boolean capitalize = true;
        for (char c : id.toCharArray()) {
            if (capitalize) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
                continue;
            }
            if (c == '_') {
                sb.append(' ');
                capitalize = true;
                continue;
            }
            sb.append(c);
        }

        return sb.toString();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
    }

    @EventHandler
    private void onRightClickBlock(InteractBlockEvent event) {
        if (mc.world == null || mc.player == null) return;

        BlockHitResult result = event.result;
        if (isActive() && result.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = result.getBlockPos();
            BlockEntity blockEntity = mc.world.getBlockEntity(pos);

            if (blockEntity == null) return;
            // InteractBlockEvents fire twice sometimes, so we must cull the extra ones manually.
            if (lastEventPos == pos) return;

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
                baseColor = baseColor.charAt(0) +baseColor.substring(1).toLowerCase();

                BannerPatternsComponent patterns = banner.getPatterns();

                String txtFormat = textFormatSetting.get().label;
                StringBuilder patternsList = new StringBuilder();

                String cc = flairColor.get().label;
                if (cc.equals(TextColor.Random.label)) {
                    cc = StardustUtil.rCC();
                }
                if (!bannerName.trim().isEmpty()) {
                    patternsList.append("§8<").append(cc).append("§o✨§r§8> ")
                        .append(cc).append(txtFormat).append(bannerName);
                } else {
                    patternsList.append("§8<").append(cc).append("§o✨§r§8> ")
                        .append(cc).append(txtFormat).append(baseColor).append(" Banner");
                }

                if (!bannerNameOnly.get()) {
                    patternsList.append("\n§r");
                    patternsList.append(cc).append("   ◦ ").append("§7")
                        .append(txtFormat).append(baseColor).append(" ").append("Base").append("\n");

                    for (BannerPatternsComponent.Layer layer : patterns.layers()) {
                        String patternColor = layer.color().name().charAt(0)
                            +layer.color().name().substring(1).toLowerCase();

                        if (patternColor.contains("_")) {
                            int i = patternColor.indexOf("_");
                            patternColor = patternColor.substring(0, i)+" "+patternColor.substring(i+1, i+2).toUpperCase()
                                +patternColor.substring(i+2);
                        }

                        patternsList.append(cc).append("   ◦ ").append("§7")
                            .append(txtFormat).append(patternColor).append(" ")
                            .append(patternNameFromAssetID(layer.pattern().value().assetId().getPath())).append("\n");
                    }
                }

                String bannerData = patternsList.toString().trim();
                mc.player.sendMessage(Text.of(bannerData));

                if (copyToClipboard.get()) {
                     mc.keyboard.setClipboard(patterns.toString());
                    ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7"
                                + txtFormat + "Copied NBT data to clipboard§8."
                        ),
                        "clipboardUpdate".hashCode()
                    );
                }

                lastEventPos = pos;
            } else if (blockEntity instanceof SignBlockEntity sign) {
                if (!signData.get()) return;
                NbtCompound metadata = sign.createNbt(mc.world.getRegistryManager());
                if (copyToClipboard.get()) {
                    mc.keyboard.setClipboard(metadata.toString());
                    ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7§o"
                                + "Copied NBT data to clipboard§8."
                        ),
                        "clipboardUpdate".hashCode()
                    );
                } else {
                    mc.player.sendMessage(
                        Text.of(
                            "§8<"+StardustUtil.rCC()+"§o✨§r§8> §7"+metadata
                        )
                    );
                }

                lastEventPos = pos;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // InteractBlockEvents fire twice sometimes, so we must cull the extra ones manually.
        timer++;
        if (timer >= 20) {
            timer = 0;
            lastEventPos = lastEventPos.add(2000000, 2000000, 2000000);
        }
    }
}
