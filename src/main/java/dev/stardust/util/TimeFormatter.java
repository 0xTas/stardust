package dev.stardust.util;

import java.time.ZoneId;
import java.util.Locale;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for formatting time-related data.
 *
 * @author uxmlen
 * created on 5/3/2025
 */
public class TimeFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM dd yyyy, HH:mm", Locale.US);

    private static final DateTimeFormatter SHORT_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

    /**
     * Format an ISO-8601 timestamp string into a human-readable datetime.
     */
    public static String formatTimestamp(String isoTimestamp, String colorCode) {
        Instant instant = Instant.parse(isoTimestamp);
        ZonedDateTime zonedTime = instant.atZone(ZoneId.systemDefault());
        String formatted = zonedTime.format(DATE_TIME_FORMATTER);
        return String.join(" §r§7at " + colorCode + "§o", formatted.split(", "));
    }

    /**
     * Format an ISO-8601 timestamp string into a short date.
     */
    public static String formatShortDate(String isoTimestamp) {
        Instant instant = Instant.parse(isoTimestamp);
        ZonedDateTime zonedTime = instant.atZone(ZoneId.systemDefault());
        return zonedTime.format(SHORT_DATE_FORMATTER);
    }

    /**
     * Format seconds into a human-readable playtime format with color codes.
     */
    public static String formatPlaytime(long playtimeSeconds, String colorCode) {
        if (playtimeSeconds <= 0) {
            return "none";
        }

        long days = TimeUnit.SECONDS.toDays(playtimeSeconds);
        playtimeSeconds -= TimeUnit.DAYS.toSeconds(days);

        long hours = TimeUnit.SECONDS.toHours(playtimeSeconds);
        playtimeSeconds -= TimeUnit.HOURS.toSeconds(hours);

        long minutes = TimeUnit.SECONDS.toMinutes(playtimeSeconds);
        playtimeSeconds -= TimeUnit.MINUTES.toSeconds(minutes);

        long seconds = playtimeSeconds;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" §7Days, ").append(colorCode);
        if (hours > 0) sb.append(hours).append(" §7Hours, ").append(colorCode);
        if (minutes > 0) sb.append(minutes).append(" §7Minutes, ").append(colorCode);
        if (seconds > 0) sb.append(seconds).append(" §7Seconds");

        String result = sb.toString();
        if (result.endsWith(", " + colorCode)) {
            result = result.substring(0, result.length() - (", " + colorCode).length());
        }

        return result;
    }

    /**
     * Format playtime in a simplified way (shows the largest unit).
     */
    public static String formatSimplifiedPlaytime(long playtimeSeconds) {
        if (playtimeSeconds <= 0) {
            return "none";
        }

        long days = TimeUnit.SECONDS.toDays(playtimeSeconds);
        long hours = TimeUnit.SECONDS.toHours(playtimeSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(playtimeSeconds);

        if (days >= 60) {
            return days / 30 + " months";
        } else if (days >= 30) {
            return "1 month";
        } else if (days >= 14) {
            return days / 7 + " weeks";
        } else if (days >= 7) {
            return "1 week";
        } else if (days >= 2) {
            return days + " days";
        } else if (days > 0) {
            return "1 day";
        } else if (hours >= 2) {
            return hours + " hours";
        } else if (hours > 0) {
            return "1 hour";
        } else if (minutes >= 2) {
            return minutes + " minutes";
        } else if (minutes > 0) {
            return "1 minute";
        } else if (playtimeSeconds > 1) {
            return playtimeSeconds + " seconds";
        } else {
            return "1 second";
        }
    }
}
