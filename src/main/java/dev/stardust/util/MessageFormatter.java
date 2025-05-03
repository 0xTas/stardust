package dev.stardust.util;

import dev.stardust.util.StardustUtil;

/**
 * Utility class for formatting messages consistently
 *
 * @author uxmlen
 * created on 5/3/2025
 */
public class MessageFormatter {

    /**
     * Format a player-related message with the standard prefix
     *
     * @param playerName The name of the player
     * @param message The message to display about the player
     * @return Formatted message string
     */
    public static String formatPlayerMessage(String playerName, String message) {
        String colorCode = StardustUtil.rCC();
        return "§8<" + colorCode + "§o✨" + "§r§8> " + colorCode + "§o" +
               playerName + "§r§7 " + message;
    }

    /**
     * Format an error message with the standard prefix
     *
     * @param message The error message
     * @return Formatted error message string
     */
    public static String formatErrorMessage(String message) {
        return "§8<" + StardustUtil.rCC() + "§o✨" + "§r§8> §4" + message + "§7.";
    }

    /**
     * Format a simple notification with the standard prefix
     *
     * @param message The notification message
     * @return Formatted notification string
     */
    public static String formatNotification(String message) {
        return "§8<" + StardustUtil.rCC() + "§o✨" + "§r§8> §7" + message;
    }

    /**
     * Format a colored value to be inserted into messages
     *
     * @param value The value to format
     * @return Formatted value string
     */
    public static String formatValue(Object value) {
        return StardustUtil.rCC() + "§o" + value + "§7";
    }
}
