package dev.stardust.util;

import java.io.File;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;
import dev.stardust.Stardust;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.Packet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.packet.c2s.play.*;
import io.netty.util.internal.ThreadLocalRandom;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket;
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
        Items.MUSIC_DISC_CREATOR.getDefaultStack(),
        Items.MUSIC_DISC_PRECIPICE.getDefaultStack(),
        Items.MUSIC_DISC_OTHERSIDE.getDefaultStack(),
        Items.MUSIC_DISC_CREATOR_MUSIC_BOX.getDefaultStack(),
    };
    private static final ItemStack[] doorIcons = {
        Items.OAK_DOOR.getDefaultStack(),
        Items.IRON_DOOR.getDefaultStack(),
        Items.BIRCH_DOOR.getDefaultStack(),
        Items.BAMBOO_DOOR.getDefaultStack(),
        Items.CHERRY_DOOR.getDefaultStack(),
        Items.JUNGLE_DOOR.getDefaultStack(),
        Items.ACACIA_DOOR.getDefaultStack(),
        Items.SPRUCE_DOOR.getDefaultStack(),
        Items.WARPED_DOOR.getDefaultStack(),
        Items.COPPER_DOOR.getDefaultStack(),
        Items.CRIMSON_DOOR.getDefaultStack(),
        Items.MANGROVE_DOOR.getDefaultStack(),
        Items.DARK_OAK_DOOR.getDefaultStack(),
        Items.EXPOSED_COPPER_DOOR.getDefaultStack(),
        Items.OXIDIZED_COPPER_DOOR.getDefaultStack(),
        Items.WEATHERED_COPPER_DOOR.getDefaultStack()
    };
    private static final ItemStack[] menuIcons = {
        Items.CAKE.getDefaultStack(),
        Items.SPAWNER.getDefaultStack(),
        Items.BEDROCK.getDefaultStack(),
        Items.GOAT_HORN.getDefaultStack(),
        Items.HONEYCOMB.getDefaultStack(),
        Items.LODESTONE.getDefaultStack(),
        Items.DRAGON_EGG.getDefaultStack(),
        Items.FILLED_MAP.getDefaultStack(),
        Items.PINK_TULIP.getDefaultStack(),
        Items.TURTLE_EGG.getDefaultStack(),
        Items.NETHER_STAR.getDefaultStack(),
        Items.WITHER_ROSE.getDefaultStack(),
        Items.PINK_PETALS.getDefaultStack(),
        Items.WARPED_SIGN.getDefaultStack(),
        Items.CHERRY_SIGN.getDefaultStack(),
        Items.WIND_CHARGE.getDefaultStack(),
        Items.WRITTEN_BOOK.getDefaultStack(),
        Items.DAMAGED_ANVIL.getDefaultStack(),
        Items.CHERRY_SAPLING.getDefaultStack(),
        Items.JACK_O_LANTERN.getDefaultStack(),
        Items.KNOWLEDGE_BOOK.getDefaultStack(),
        Items.FIREWORK_ROCKET.getDefaultStack(),
        Items.TOTEM_OF_UNDYING.getDefaultStack(),
        Items.LIME_SHULKER_BOX.getDefaultStack(),
        Items.AMETHYST_CLUSTER.getDefaultStack(),
        Items.FLOWERING_AZALEA.getDefaultStack(),
        Items.PINK_SHULKER_BOX.getDefaultStack(),
        Items.GILDED_BLACKSTONE.getDefaultStack(),
        Items.OMINOUS_TRIAL_KEY.getDefaultStack(),
        Items.HEART_POTTERY_SHERD.getDefaultStack(),
        Items.LIGHT_BLUE_SHULKER_BOX.getDefaultStack(),
        Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack(),
        Items.HEARTBREAK_POTTERY_SHERD.getDefaultStack(),
        Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack(),
        discIcons[ThreadLocalRandom.current().nextInt(discIcons.length)],
        doorIcons[ThreadLocalRandom.current().nextInt(doorIcons.length)],
        getCustomIcons()[ThreadLocalRandom.current().nextInt(getCustomIcons().length)]
    };

    private static ItemStack[] getCustomIcons() {
        // Encoded profile textures taken from illegal player head items on 2b2t.org (except for mine.)
        final String tasHeadTexture = "ewogICJ0aW1lc3RhbXAiIDogMTcyODQwNzM3MDc3MiwKICAicHJvZmlsZUlkIiA6ICJjZTA5ODE3NzBkMjc0NmY1YTM3ODUxODg5NzcxYmEyNyIsCiAgInByb2ZpbGVOYW1lIiA6ICIweFRhcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yZGNlNGNlNWVhOWJjNWI1OTI1MmJlNDk1YTA5ZTQ0ZWFmMzc5NmRmNDY5OTU2MTdmZGQ4ZjFmMTBkNjU0ZjQyIgogICAgfQogIH0KfQ==";
        final String popbobHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTU4NTYsInByb2ZpbGVJZCI6IjBmNzVhODFkNzBlNTQzYzViODkyZjMzYzUyNDI4NGYyIiwicHJvZmlsZU5hbWUiOiJwb3Bib2IiLCJpc1B1YmxpYyI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzEyNTY4ODQ4NWI3MjUxMWFmOWY4NzVjZjQ4NjlmNjYxOTkwNWU2ZjJjNzc3NGIyMjYxNTJjYTY3ODIzODFlNiJ9fX0=";
        final String pyrobyteHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTUxOTUsInByb2ZpbGVJZCI6IjY4YjFiYjExY2ZhMzRlMTZhMDFkYjZkZGRhMGExMDgzIiwicHJvZmlsZU5hbWUiOiJQeXJvYnl0ZSIsImlzUHVibGljIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzhjZTMwODMxYjU1YTI0MTFjMGYzMTI2ZDVhNThlMzE2NDZkNGE4YjZmMzYxZjcyMzc5ZGY0ZTY5OTE0OTkifX19";
        final String iTristanHeadTexture = "ewogICJ0aW1lc3RhbXAiIDogMTcyODUwMDk2NjEwMywKICAicHJvZmlsZUlkIiA6ICI4ZDNmYTEyMmFjNGI0YjM1OGI1MzM5Mjc5NGJkZDU2MSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVTZW5wYWlPZjJiMnQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTcxNTFkNzliMDQzZWY5N2FkMjhhMjc5NDVmODY3OGRmMmE3OGU2NGE1MmQxYzkzMDgwNTdhMjFmMDQyMDNlNCIKICAgIH0KICB9Cn0=";
        final String hausemasterHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTU2NjIsInByb2ZpbGVJZCI6IjhmMmNlNDUzY2VmMjRiM2ViNjg2ZGMyMWI1MTlhMGExIiwicHJvZmlsZU5hbWUiOiJIYXVzZW1hc3RlciIsImlzUHVibGljIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJiY2IyZTE5OTdjN2NiMWJkZjU2MTNkMTMyZWVjNmQ2NzEzM2EyMTYyMWUwZmFlMTU3YTZhZDhmOWIyIn19fQ==";
        final String jackTheRippaHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MTYwOTQxOTUxOTMsInByb2ZpbGVJZCI6IjdmMTk3NjE4MzJjMjQ4NzY4NDFiY2VhMjliZDU4Y2FlIiwicHJvZmlsZU5hbWUiOiJKYWNrdGhlcmlwcGEiLCJpc1B1YmxpYyI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzExYjk0OWE2MWZhNGNjOGZmZjNkM2I0OTY4MmQyZjk2ZjQxMThmOTI4ZDg2MjIyMmVmNjU2ZTMyYTVmMTIifX19";
        final String cytoToxicTCellHeadTexture = "eyJ0aW1lc3RhbXAiOjE0MDY0MTc0NTE1MDgsInByb2ZpbGVJZCI6ImE0YTVlYmM0OWY0ZTQ3OTVhMjUzN2I4YjA1M2ZiMTdmIiwicHJvZmlsZU5hbWUiOiJDeXRvdG94aWNUY2VsbCIsImlzUHVibGljIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTlkMWU2YzRmNjFkZmNmZGE2NDE3MjJmNjU3NzJiMTI3YmI0NDFkMGViMjU4YTM2Y2MxOTEzYmU3NTkyNGIxIn19fQ==";

        // Get textures for the current player's head item
        Optional<Property> currentPlayerProfileProperties = mc.getGameProfile().getProperties().get("textures").stream().findFirst();

        String currentPlayerHeadTexture;
        if (currentPlayerProfileProperties.isPresent()) {
            currentPlayerHeadTexture = currentPlayerProfileProperties.get().value();
        } else {
            currentPlayerHeadTexture = "ewogICJ0aW1lc3RhbXAiIDogMTcyODQ5NzQxNzUwNCwKICAicHJvZmlsZUlkIiA6ICJkMDUwMzNmYzM3N2Q0OGU1ODFiMGJhYTY0NDBmNTIyOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJQYXVsc3RldmUwMDciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTk1YmQzOWQ5M2ZiYjI4NGVhNGEzYmJiMTljNzRlNTUxOGQwODRiNmZiMGQ5YjE1ZWQ2YzU2NzdmMDhkY2FhYyIKICAgIH0KICB9Cn0=";
        }

        String[] playerHeadTextures = {
            currentPlayerHeadTexture, tasHeadTexture, popbobHeadTexture, pyrobyteHeadTexture,
            iTristanHeadTexture, hausemasterHeadTexture, jackTheRippaHeadTexture, cytoToxicTCellHeadTexture
        };

        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
        GameProfile profile = new GameProfile(UUID.randomUUID(), "Stardust");
        ProfileComponent profileComponent = new ProfileComponent(profile);

        // Apply a player head texture to the ItemStack
        profileComponent.properties().put(
            "textures",
            new Property(
                "textures", // Select a random player head texture from the playerHeadTextures array.
                playerHeadTextures[ThreadLocalRandom.current().nextInt(playerHeadTextures.length)],""
            )
        );
        playerHead.set(DataComponentTypes.PROFILE, profileComponent);

        ItemStack enchantedPick = new ItemStack(
            ThreadLocalRandom.current().nextInt(2) == 0 ? Items.DIAMOND_PICKAXE : Items.NETHERITE_PICKAXE);
        enchantedPick.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

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
            g.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        ItemStack cgiElytra = new ItemStack(Items.ELYTRA);
        cgiElytra.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack sword32k = new ItemStack(
            ThreadLocalRandom.current().nextInt(2) == 0 ? Items.DIAMOND_SWORD : Items.WOODEN_SWORD);
        sword32k.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack illegalBow = new ItemStack(Items.BOW);
        illegalBow.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack bindingPumpkin = new ItemStack(Items.CARVED_PUMPKIN);
        bindingPumpkin.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        ItemStack ripTridentFly = new ItemStack(Items.TRIDENT);
        ripTridentFly.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        return new ItemStack[] {
            playerHead,
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
                            Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Created "+file.getName()+" in meteor-client folder."), false
                        );
                        Text msg = Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Click §2§lhere §r§7to open the file.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));

                        MutableText txt = msg.copyContentOnly().setStyle(style);
                        mc.player.sendMessage(txt, false);
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

        try {
            Runtime runtime = Runtime.getRuntime();
            if (System.getenv("OS") == null) return;
            if (System.getenv("OS").contains("Windows")) {
                runtime.exec(new String[]{"rundll32", "url.dll,", "FileProtocolHandler", file.getAbsolutePath()});
            }else {
                runtime.exec(new String[]{"xdg-open", file.getAbsolutePath()});
            }
        } catch (Exception err) {
            Stardust.LOG.error("Failed to open "+ file.getAbsolutePath() +"! - Why:\n"+err);
            if (mc.player != null) mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"✨§8> §4§oFailed to open "+file.getName()+"§7."), false);
        }
    }

    public enum IllegalDisconnectMethod {
        Slot, Chat, Interact, Movement, SequenceBreak, InvalidSettings
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
            case Movement -> illegalPacket = new PlayerMoveC2SPacket.PositionAndOnGround(Double.NaN, 69, Double.NaN, false, false);
            case SequenceBreak -> illegalPacket = new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, -420, 13.37F, 69.69F);
            case InvalidSettings -> illegalPacket = new ClientOptionsC2SPacket(new SyncedClientOptions(
                mc.options.language, -69,
                mc.options.getChatVisibility().getValue(), mc.options.getChatColors().getValue(),
                mc.options.getSyncedOptions().playerModelParts(), mc.options.getMainArm().getValue(),
                mc.options.getSyncedOptions().filtersText(), mc.options.getAllowServerListing().getValue(),
                mc.options.getSyncedOptions().particleStatus()
            ));
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
