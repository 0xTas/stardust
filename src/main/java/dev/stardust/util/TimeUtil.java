package dev.stardust.util;

import java.io.File;
import java.io.Writer;
import java.time.Instant;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import java.text.DecimalFormat;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;
import dev.stardust.util.commands.ApiHandler;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.MeteorClient;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Allows tracking and syncing with 2b2t's current world time via api.2b2t.vc/time
 **/
public class TimeUtil {
    public record TimeData(String lastUpdated, long worldTime) {}

    private @Nullable TimeData time = null;

    private boolean exhausted;
    private int totalAPIFetchAttempts;
    private long lastAPIFetchAttempt = -1;
    private static final Gson GSON = new Gson();
    private final double refreshInterval = ThreadLocalRandom.current().nextDouble(2.0, 8.0);

    public @Nullable TimeData getTime() {
        if (time != null) {
            // Periodically update time data to avoid drift if the client stays running for long periods of time
            Instant lastUpdated = Instant.parse(time.lastUpdated());
            if (System.currentTimeMillis() - lastUpdated.toEpochMilli() >= (3.6e6 * refreshInterval)) {
                lastAPIFetchAttempt = -1;
                TimeData newData = fetchServerTime();
                if (newData != null) {
                    time = newData;
                }
            }

            return time;
        }

        return getTimeData();
    }

    private @Nullable TimeData getTimeData() {
        long now = System.currentTimeMillis();
        if (lastAPIFetchAttempt != -1 && now - lastAPIFetchAttempt > (15000L * totalAPIFetchAttempts)) {
            lastAPIFetchAttempt = -1;
        }
        if (StardustUtil.checkOrCreateFile(mc, "meteor-client/worldTime-2b2t.json")) {
            try {
                File timeFile = MeteorClient.FOLDER.toPath().resolve("worldTime-2b2t.json").toFile();

                FileReader reader = new FileReader(timeFile);
                TimeData data = GSON.fromJson(reader, TimeData.class);

                reader.close();
                if (data != null) {
                    Instant updatedAt = Instant.parse(data.lastUpdated());

                    long lastUpdated = updatedAt.toEpochMilli();
                    if (now - lastUpdated >= (3.6e6 * ThreadLocalRandom.current().nextDouble(1.01337, 4.42069))) {
                        if (lastAPIFetchAttempt == -1) {
                            LogUtil.warn("Logged world time is outdated, attempting to refresh it from the api..!");
                        } else {
                            LogUtil.warn(
                                "Got potentially-outdated 2b2t world time from file: lastUpdate: "
                                    + lastUpdated + " | worldTime: " + data.worldTime()
                            );
                            time = data;
                            return data;
                        }
                    } else {
                        DecimalFormat df = new DecimalFormat("#.00");
                        LogUtil.info(
                            "Got 2b2t world time from file: lastUpdate: "
                                + data.lastUpdated() + " | worldTime: "+ data.worldTime()
                                + " | refresh: " + df.format(refreshInterval)
                        );
                        time = data;
                        return data;
                    }
                }
            } catch (Exception err) {
                LogUtil.warn("Failed loading time data from meteor-client/worldTime-2b2t.json! Why: " + err);
            }
        }

        return fetchServerTime();
    }

    private @Nullable TimeData fetchServerTime() {
        ++totalAPIFetchAttempts;
        lastAPIFetchAttempt = System.currentTimeMillis();
        if (totalAPIFetchAttempts >= 25) {
            if (!exhausted) {
                exhausted = true;
                LogUtil.warn("Exhausted all attempts at fetching world time data from api.2b2t.vc..!");
            }
            return null;
        }

        LogUtil.info("Fetching 2b2t world time from api.2b2t.vc...");
        final String WORLD_TIME_ENDPOINT = "/time";
        final String API_URL = ApiHandler.API_2B2T_URL;
        String response = new ApiHandler().fetchResponse(API_URL + WORLD_TIME_ENDPOINT);

        if (response == null || response.trim().isBlank()) return null;

        JsonElement timeJson = JsonParser.parseString(response);

        long worldTime = -69L;
        String lastUpdated = timeJson.getAsJsonObject().get("lastUpdated").getAsString();
        if (timeJson.getAsJsonObject().has("worldTime")) {
            worldTime = timeJson.getAsJsonObject().get("worldTime").getAsLong();
        }

        if (lastUpdated != null && worldTime != -69L) {
            DecimalFormat df = new DecimalFormat("#.00");
            LogUtil.info(
                "Got 2b2t world time from api.2b2t.vc: lastUpdate: "
                    + lastUpdated + " | worldTime: " + worldTime + " | refresh: " + df.format(refreshInterval)
            );
            TimeData data = new TimeData(lastUpdated, worldTime);
            saveTimeData(data);

            return data;
        }

        LogUtil.warn("Failed retrieving 2b2t world time from api.2b2t.vc..!");
        return null;
    }

    private void saveTimeData(TimeData data) {
        time = data;
        LogUtil.info("Saving 2b2t world time data to meteor-client/worldTime-2b2t.json!");
        if (StardustUtil.checkOrCreateFile(mc, "meteor-client/worldTime-2b2t.json")) {
            try {
                File timeFile = MeteorClient.FOLDER.toPath().resolve("worldTime-2b2t.json").toFile();
                Writer writer = new FileWriter(timeFile);
                GSON.toJson(data, writer);
                writer.close();
            } catch (Exception err) {
                LogUtil.error("Failed saving time data to meteor-client/worldTime-2b2t.json! Why: " + err);
            }
        }
    }
}
