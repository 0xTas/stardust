package dev.stardust.commands;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.Text;
import dev.stardust.Stardust;
import dev.stardust.util.StardustUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;
import dev.stardust.util.commands.ApiHandler;
import dev.stardust.util.MessageFormatter;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.commands.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

/**
 * @author uxmlen
 * created on 5/3/2025
 */
public class QueueStatus2b2t extends Command {
    private static final String API_ENDPOINT = "/queue";
    private static final String ETA_ENDPOINT = "/queue/eta-equation";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // default ETA constants as fallback
    private static final QueueEtaEquation DEFAULT_REGULAR = new QueueEtaEquation(25.0, 1.15);
    private static final QueueEtaEquation DEFAULT_PRIORITY = new QueueEtaEquation(12.0, 1.05);

    // cache for ETA equations
    private static QueueEtaEquation regularEta = DEFAULT_REGULAR;
    private static QueueEtaEquation priorityEta = DEFAULT_PRIORITY;
    private static Instant lastEtaUpdate = Instant.EPOCH;

    public QueueStatus2b2t() {
        super("queuestatus2b2t", "Check the current 2b2t queue status with ETA.", "queue", "q2b2t");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            MeteorExecutor.execute(() -> {
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                if (player == null) return;

                if (Duration.between(lastEtaUpdate, Instant.now()).toMinutes() >= 15) {
                    updateEtaEquations();
                }

                String requestString = ApiHandler.API_2B2T_URL + API_ENDPOINT;
                String response = new ApiHandler().fetchResponse(requestString);
                if (response == null) return;

                try {
                    displayQueueStatus(player, response);
                } catch (Exception err) {
                    Stardust.LOG.error("[QueueStatus2b2t] Failed to process queue data: " + err.getMessage());
                    player.sendMessage(Text.of(MessageFormatter.formatErrorMessage("Failed to get queue status")));
                }
            });
            return SINGLE_SUCCESS;
        });
    }

    private void displayQueueStatus(ClientPlayerEntity player, String response) throws Exception {
        JsonObject queueJson = JsonParser.parseString(response).getAsJsonObject();
        String timeStr = queueJson.get("time").getAsString();
        int prioQueue = queueJson.get("prio").getAsInt();
        int regularQueue = queueJson.get("regular").getAsInt();

        OffsetDateTime time = OffsetDateTime.parse(timeStr);
        String formattedTime = time.format(TIME_FORMATTER);

        String cc = StardustUtil.rCC();
        String priorityEta = getQueueEta(prioQueue, true);
        String regularEta = getQueueEta(regularQueue, false);

        String message = "§8<" + cc + "§o✨" + "§r§8> §7Queue Status (as of " +
            MessageFormatter.formatValue(formattedTime) + "§7):\n" +
            "    §7Priority: " + MessageFormatter.formatValue(prioQueue) +
            " §7(ETA: " + MessageFormatter.formatValue(priorityEta) + "§7)\n" +
            "    §7Regular: " + MessageFormatter.formatValue(regularQueue) +
            " §7(ETA: " + MessageFormatter.formatValue(regularEta) + "§7)";

        player.sendMessage(Text.of(message));
    }

    private void updateEtaEquations() {
        ApiHandler apiHandler = new ApiHandler();
        String response = apiHandler.fetchResponse(ApiHandler.API_2B2T_URL + ETA_ENDPOINT);

        if (response == null || response.equals("204 Undocumented")) {
            // use default values if API fails
            lastEtaUpdate = Instant.now();
            return;
        }

        try {
            JsonObject equation = JsonParser.parseString(response).getAsJsonObject();

            // Extract values with fallbacks to defaults
            double regularFactor = getDoubleOrDefault(equation, "regularFactor", DEFAULT_REGULAR.factor());
            double regularPow = getDoubleOrDefault(equation, "regularPow", DEFAULT_REGULAR.pow());
            double priorityFactor = getDoubleOrDefault(equation, "priorityFactor", DEFAULT_PRIORITY.factor());
            double priorityPow = getDoubleOrDefault(equation, "priorityPow", DEFAULT_PRIORITY.pow());

            regularEta = new QueueEtaEquation(regularFactor, regularPow);
            priorityEta = new QueueEtaEquation(priorityFactor, priorityPow);

            lastEtaUpdate = Instant.now();
        } catch (Exception e) {
            Stardust.LOG.error("[QueueStatus2b2t] Failed to parse ETA equations: " + e.getMessage());
            // use default values if parsing fails
            regularEta = DEFAULT_REGULAR;
            priorityEta = DEFAULT_PRIORITY;
            lastEtaUpdate = Instant.now();
        }
    }

    private double getDoubleOrDefault(JsonObject json, String key, double defaultValue) {
        return json.has(key) ? json.get(key).getAsDouble() : defaultValue;
    }

    private long getQueueWait(final int queuePos, boolean isPriority) {
        QueueEtaEquation equation = isPriority ? priorityEta : regularEta;
        return (long) (equation.factor() * (Math.pow(queuePos, equation.pow())));
    }

    private String getEtaStringFromSeconds(final long totalSeconds) {
        if (totalSeconds <= 0) return "00:00:00";

        final int hour = (int) (totalSeconds / 3600);
        final int minutes = (int) ((totalSeconds / 60) % 60);
        final int seconds = (int) (totalSeconds % 60);

        return String.format("%02d:%02d:%02d", hour, minutes, seconds);
    }

    private String getQueueEta(final int queuePos, boolean isPriority) {
        if (queuePos <= 0) return "Now";
        return getEtaStringFromSeconds(getQueueWait(queuePos, isPriority));
    }

    private record QueueEtaEquation(double factor, double pow) {
    }
}
