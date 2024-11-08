package dev.stardust.util;

import java.awt.*;
import java.io.File;
import java.time.Instant;
import dev.stardust.Stardust;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.network.packet.Packet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.network.packet.c2s.play.*;
import io.netty.util.internal.ThreadLocalRandom;
import meteordevelopment.meteorclient.utils.Utils;
import dev.stardust.mixin.ClientConnectionAccessor;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
public class StardustUtil {
    public enum RainbowColor {
        Reds(new String[]{"§c", "§4"}),
        Yellows(new String[]{"§e", "§6"}),
        Greens(new String[]{"§a", "§2"}),
        Cyans(new String[]{"§b", "§3"}),
        Blues(new String[]{"§9", "§1"}),
        Purples(new String[]{"§d", "§5"});

        public final String[] labels;

        RainbowColor(String[] labels) { this.labels = labels; }

        public static RainbowColor getFirst() {
            return RainbowColor.values()[ThreadLocalRandom.current().nextInt(RainbowColor.values().length)];
        }

        public static RainbowColor getNext(RainbowColor previous) {
            return switch (previous) {
                case Reds -> Yellows;
                case Yellows -> Greens;
                case Greens -> Cyans;
                case Cyans -> Blues;
                case Blues -> Purples;
                case Purples -> Reds;
            };
        }
    }

    public enum TextColor {
        Black("§0"), White("§f"), Gray("§8"), Light_Gray("§7"),
        Dark_Green("§2"), Green("§a"), Dark_Aqua("§3"), Aqua("§b"),
        Dark_Blue("§1"), Blue("§9"), Dark_Red("§4"), Red("§c"),
        Dark_Purple("§5"), Purple("§d"), Gold("§6"), Yellow("§e"),
        Random("");

        public final String label;

        TextColor(String label) {
            this.label = label;
        }
    }

    public enum TextFormat {
        Plain(""), Italic("§o"), Bold("§l"),
        Underline("§n"), Strikethrough("§m"),
        Obfuscated("§k");

        public final String label;

        TextFormat(String label) {
            this.label = label;
        }
    }

    /** Random Color-Code */
    public static String rCC() {
        String color = "§7";
        TextColor[] colors = TextColor.values();

        // Omit gray, light_gray, and black from accent colors.
        while (color.equals("§0") || color.equals("§8") || color.equals("§7")) {
            int luckyIndex = ThreadLocalRandom.current().nextInt(colors.length);
            color = colors[luckyIndex].label;
        }

        return color;
    }

    public static ItemStack chooseMenuIcon() {
        int luckyIndex = ThreadLocalRandom.current().nextInt(menuIcons.length);

        return menuIcons[luckyIndex];
    }

    private static final ItemStack[] discIcons = {
        Items.MUSIC_DISC_5.getDefaultStack(),
        Items.MUSIC_DISC_11.getDefaultStack(),
        Items.MUSIC_DISC_13.getDefaultStack(),
        Items.MUSIC_DISC_CAT.getDefaultStack(),
        Items.MUSIC_DISC_FAR.getDefaultStack(),
        Items.MUSIC_DISC_MALL.getDefaultStack(),
        Items.MUSIC_DISC_STAL.getDefaultStack(),
        Items.MUSIC_DISC_WARD.getDefaultStack(),
        Items.MUSIC_DISC_WAIT.getDefaultStack(),
        Items.MUSIC_DISC_CHIRP.getDefaultStack(),
        Items.MUSIC_DISC_STRAD.getDefaultStack(),
        Items.MUSIC_DISC_RELIC.getDefaultStack(),
        Items.MUSIC_DISC_BLOCKS.getDefaultStack(),
        Items.MUSIC_DISC_MELLOHI.getDefaultStack(),
        Items.MUSIC_DISC_PIGSTEP.getDefaultStack(),
        Items.MUSIC_DISC_OTHERSIDE.getDefaultStack(),
    };
    private static final ItemStack[] doorIcons = {
        Items.OAK_DOOR.getDefaultStack(),
        Items.BIRCH_DOOR.getDefaultStack(),
        Items.BAMBOO_DOOR.getDefaultStack(),
        Items.CHERRY_DOOR.getDefaultStack(),
        Items.JUNGLE_DOOR.getDefaultStack(),
        Items.ACACIA_DOOR.getDefaultStack(),
        Items.SPRUCE_DOOR.getDefaultStack(),
        Items.WARPED_DOOR.getDefaultStack(),
        Items.CRIMSON_DOOR.getDefaultStack(),
        Items.MANGROVE_DOOR.getDefaultStack(),
        Items.DARK_OAK_DOOR.getDefaultStack(),
    };
    private static final ItemStack[] menuIcons = {
        Items.CAKE.getDefaultStack(),
        Items.BEDROCK.getDefaultStack(),
        Items.GOAT_HORN.getDefaultStack(),
        Items.DRAGON_EGG.getDefaultStack(),
        Items.FILLED_MAP.getDefaultStack(),
        Items.PINK_TULIP.getDefaultStack(),
        Items.TURTLE_EGG.getDefaultStack(),
        Items.NETHER_STAR.getDefaultStack(),
        Items.WITHER_ROSE.getDefaultStack(),
        Items.PINK_PETALS.getDefaultStack(),
        Items.WARPED_SIGN.getDefaultStack(),
        Items.CHERRY_SIGN.getDefaultStack(),
        Items.WRITTEN_BOOK.getDefaultStack(),
        Items.DAMAGED_ANVIL.getDefaultStack(),
        Items.CHERRY_SAPLING.getDefaultStack(),
        Items.JACK_O_LANTERN.getDefaultStack(),
        Items.FIREWORK_ROCKET.getDefaultStack(),
        Items.TOTEM_OF_UNDYING.getDefaultStack(),
        Items.LIME_SHULKER_BOX.getDefaultStack(),
        Items.AMETHYST_CLUSTER.getDefaultStack(),
        Items.FLOWERING_AZALEA.getDefaultStack(),
        Items.PINK_SHULKER_BOX.getDefaultStack(),
        Items.GILDED_BLACKSTONE.getDefaultStack(),
        Items.HEART_POTTERY_SHERD.getDefaultStack(),
        Items.LIGHT_BLUE_SHULKER_BOX.getDefaultStack(),
        Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack(),
        Items.HEARTBREAK_POTTERY_SHERD.getDefaultStack(),
        discIcons[ThreadLocalRandom.current().nextInt(discIcons.length)],
        doorIcons[ThreadLocalRandom.current().nextInt(doorIcons.length)],
        getCustomIcons()[ThreadLocalRandom.current().nextInt(getCustomIcons().length)]
    };

    private static ItemStack[] getCustomIcons() {
        ItemStack enchantedPick = new ItemStack(
            ThreadLocalRandom.current().nextInt(2) == 0 ? Items.DIAMOND_PICKAXE : Items.NETHERITE_PICKAXE);
        enchantedPick.addEnchantment(Enchantments.MENDING, 1);

        ItemStack sword32k = new ItemStack(
            ThreadLocalRandom.current().nextInt(2) == 0 ? Items.DIAMOND_SWORD : Items.WOODEN_SWORD);
        sword32k.addEnchantment(Enchantments.SHARPNESS, 32767);

        ItemStack illegalBow = new ItemStack(Items.BOW);
        illegalBow.addEnchantment(Enchantments.MENDING, 1);
        illegalBow.addEnchantment(Enchantments.INFINITY, 1);

        ItemStack bindingPumpkin = new ItemStack(Items.CARVED_PUMPKIN);
        bindingPumpkin.addEnchantment(Enchantments.BINDING_CURSE, 1);

        ItemStack[] enchantedGlass = new ItemStack[] {
            Items.GLASS.getDefaultStack(),
            Items.RED_STAINED_GLASS.getDefaultStack(),
            Items.CYAN_STAINED_GLASS.getDefaultStack(),
            Items.LIME_STAINED_GLASS.getDefaultStack(),
            Items.PINK_STAINED_GLASS.getDefaultStack(),
            Items.WHITE_STAINED_GLASS.getDefaultStack(),
            Items.BLACK_STAINED_GLASS.getDefaultStack(),
            Items.LIGHT_BLUE_STAINED_GLASS.getDefaultStack(),
        };

        for (ItemStack g : enchantedGlass) {
            g.addEnchantment(Enchantments.MENDING, 1);
        }

        ItemStack cgiElytra = new ItemStack(Items.ELYTRA);
        cgiElytra.addEnchantment(Enchantments.MENDING, 420);

        ItemStack ripTridentFly = new ItemStack(Items.TRIDENT);
        ripTridentFly.addEnchantment(Enchantments.RIPTIDE, 3);

        return new ItemStack[] {
            enchantedPick, sword32k, illegalBow, bindingPumpkin, cgiElytra, ripTridentFly,
            enchantedGlass[ThreadLocalRandom.current().nextInt(enchantedGlass.length)]
        };
    }

    public static boolean checkOrCreateFile(MinecraftClient mc, String fileName) {
        File file =FabricLoader.getInstance().getGameDir().resolve(fileName).toFile();

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Created "+file.getName()+" in meteor-client folder.")
                        );
                        Text msg = Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Click §2§lhere §r§7to open the file.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));

                        MutableText txt = msg.copyContentOnly().setStyle(style);
                        mc.player.sendMessage(txt);
                    }
                    return true;
                }
            }catch (Exception err) {
                Stardust.LOG.error("[Stardust] Error creating"+file.getAbsolutePath()+"! - Why:\n"+err);
            }
        } else return true;

        return false;
    }

    public static void openFile(MinecraftClient mc, String fileName) {
        File file = FabricLoader.getInstance().getGameDir().resolve(fileName).toFile();

        if (Desktop.isDesktopSupported()) {
            EventQueue.invokeLater(() -> {
                try {
                    Desktop.getDesktop().open(file);
                }catch (Exception err) {
                    Stardust.LOG.error("[Stardust] Failed to open "+ file.getAbsolutePath() +"! - Why:\n"+err);
                }
            });
        } else {
            try {
                Runtime runtime = Runtime.getRuntime();
                if (System.getenv("OS") == null) return;
                if (System.getenv("OS").contains("Windows")) {
                    runtime.exec("rundll32 url.dll, FileProtocolHandler " + file.getAbsolutePath());
                }else {
                    runtime.exec("xdg-open " + file.getAbsolutePath());
                }
            } catch (Exception err) {
                Stardust.LOG.error("[Stardust] Failed to open "+ file.getAbsolutePath() +"! - Why:\n"+err);
                if (mc.player != null) mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"✨§8> §4§oFailed to open "+file.getName()+"§7."));
            }
        }
    }

    public enum IllegalDisconnectMethod {
        Slot, Chat, Interact, Movement, SequenceBreak
    }

    public static void illegalDisconnect(boolean disableAutoReconnect, IllegalDisconnectMethod illegalDisconnectMethod) {
        if (!Utils.canUpdate()) return;
        if (disableAutoReconnect) disableAutoReconnect();

        Packet<?> illegalPacket = null;
        switch (illegalDisconnectMethod) {
            case Slot -> illegalPacket = new UpdateSelectedSlotC2SPacket(-69);
            case Chat -> illegalPacket = new ChatMessageC2SPacket(
                "§",
                Instant.now(),
                NetworkEncryptionUtils.SecureRandomUtil.nextLong(),
                null,
                ((ClientPlayNetworkHandlerAccessor) mc.getNetworkHandler()).getLastSeenMessagesCollector().collect().update()
            );
            case Interact -> illegalPacket = PlayerInteractEntityC2SPacket.interact(mc.player, false, Hand.MAIN_HAND);
            case Movement -> illegalPacket = new PlayerMoveC2SPacket.PositionAndOnGround(Double.NaN, 69, Double.NaN, false);
            case SequenceBreak -> illegalPacket = new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, -420);
        }
        if (illegalPacket != null) ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
            illegalPacket, null, true
        );
    }

    public static void disableAutoReconnect() {
        Modules mods = Modules.get();
        if (mods == null) return;
        AutoReconnect atrc = mods.get(AutoReconnect.class);
        if (atrc.isActive()) atrc.toggle();
    }
}
