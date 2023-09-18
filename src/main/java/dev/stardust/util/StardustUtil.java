package dev.stardust.util;

import io.netty.util.internal.ThreadLocalRandom;


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
public class StardustUtil {
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
        TextColor[] colors = TextColor.values();
        int luckyIndex = ThreadLocalRandom.current().nextInt(0, colors.length);

        String color = colors[luckyIndex].label;

        // Omit gray, light_gray, and black from accent colors.
        if (color.equals("§0") || color.equals("§8") || color.equals("§7")) color = "§e";

        return color;
    }

    public static String patternNameFromID(String id) {
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
            case "pig" -> "Sount (Piglin)";
            default -> "Oasis Sigil";
        };
    }
}
