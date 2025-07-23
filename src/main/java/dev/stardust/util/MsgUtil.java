package dev.stardust.util;

import java.util.Map;
import java.util.HashMap;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.systems.modules.Module;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.modules.Modules;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
public class MsgUtil {
    private final static Map<String, String> modulePrefixes = new HashMap<>();

    public static String getPrefix() {
        return Formatting.DARK_GRAY + "<" + StardustUtil.rCC() +
            Formatting.ITALIC + "✨" + Formatting.DARK_GRAY + ">";
    }

    public static String getRawPrefix() {
        return "[Stardust]";
    }

    public static String getRawPrefix(String module) {
        return "[" + module + "]";
    }

    public static void initModulePrefixes() {
        for (Module module : Modules.get().getGroup(Stardust.CATEGORY)) {
            String name = module.name;
            String color = StardustUtil.rCC();
            modulePrefixes.put(name, color);
        }
    }

    public static String getModulePrefix(String module) {
        if (!modulePrefixes.containsKey(module)) {
            return String.valueOf(Formatting.DARK_GRAY) + '[' + StardustUtil.rCC() +
                Formatting.ITALIC + Utils.nameToTitle(module) + Formatting.DARK_GRAY + ']';
        } else {
            return String.valueOf(Formatting.DARK_GRAY) + '[' + modulePrefixes.get(module) +
                Formatting.ITALIC + Utils.nameToTitle(module) + Formatting.DARK_GRAY + ']';
        }
    }

    public static void sendMsg(String msg) {
        if (mc.player == null) return;

        try {
            StringBuilder sb = new StringBuilder();
            mc.player.sendMessage(Text.literal(sb.append(getPrefix()).append(' ').append(Formatting.GRAY).append(msg).toString()), false);
        } catch (Exception ignored) {}
    }

    public static void sendModuleMsg(String msg, String module) {
        if (mc.player == null) return;

        try {
            StringBuilder sb = new StringBuilder();
            mc.player.sendMessage(Text.literal(sb.append(getModulePrefix(module)).append(' ').append(Formatting.GRAY).append(msg).toString()), false);
        } catch (Exception ignored) {}
    }

    public static void sendModuleMsg(String msg, Style style, String module) {
        if (mc.player == null) return;

        try {
            String message = getModulePrefix(module) + ' ' + Formatting.GRAY + msg;
            mc.player.sendMessage(Text.literal(message).setStyle(style), false);
        } catch (Exception ignored) {}
    }

    public static void updateMsg(String msg, int hashcode) {
        if (mc.player == null) return;

        try {
            StringBuilder sb = new StringBuilder();
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                Text.literal(sb.append(getPrefix()).append(' ').append(Formatting.GRAY).append(msg).toString()), hashcode
            );
        } catch (Exception ignored) {}
    }

    public static void updateModuleMsg(String msg, String module, int hashcode) {
        if (mc.player == null) return;

        try {
            StringBuilder sb = new StringBuilder();
            ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                Text.literal(sb.append(getModulePrefix(module)).append(' ').append(Formatting.GRAY).append(msg).toString()), hashcode
            );
        } catch (Exception ignored) {}
    }
}
