package dev.stardust.util.commands;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import javax.annotation.Nullable;
import java.net.URISyntaxException;
import dev.stardust.util.StardustUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import dev.stardust.util.gson.OffsetDateTimeAdapter;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class ApiHandler {
    public static final String API_2B2T_URL = "https://api.2b2t.vc";

    // Configure Gson with custom type adapters for Java 8 date/time types
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    // Cache for API responses with expiration
    private static final ConcurrentHashMap<String, CachedApiResponse<?>> responseCache = new ConcurrentHashMap<>();

    public static void sendErrorResponse() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage(
                Text.of(
                    "§8<"+StardustUtil.rCC()
                        +"§o✨"+"§r§8> §4An error occurred§7, §4please try again later or check §7latest.log §4for more info§7.."
                )
            );
        }
    }

    @Nullable
    public String fetchResponse(String requestString) {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest req;
        try {
            req = HttpRequest.newBuilder().uri(new URI(requestString))
                .header("Accept", "*/*")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();
        } catch (URISyntaxException err) {
            sendErrorResponse();
            err.printStackTrace();
            return null;
        }

        if (req == null) {
            sendErrorResponse();
            return null;
        }

        HttpResponse<String> res = null;
        try {
            res = client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).get();
        } catch (Exception err) {
            err.printStackTrace();
        }

        if (res == null) {
            sendErrorResponse();
            return null;
        }

        if (res.statusCode() == 200) {
            return res.body();
        } else if (res.statusCode() == 204) {
            return "204 Undocumented";
        } else {
            sendErrorResponse();
            Stardust.LOG.warn("[Stardust] received unexpected response from api.2b2t.vc: "+res);
        }

        return null;
    }

    /**
     * Get the properly configured Gson instance
     *
     * @return Gson instance with all required type adapters
     */
    public static Gson getGson() {
        return GSON;
    }

    /**
     * Generic GET method to fetch and deserialize API responses
     *
     * @param <T> The type to deserialize the response into
     * @param endpoint The API endpoint to call
     * @param responseType The class to deserialize the response into
     * @return Optional containing the deserialized response, or empty if failed
     */
    public <T> Optional<T> get(String endpoint, Class<T> responseType) {
        return get(endpoint, responseType, Duration.ofMinutes(5));
    }

    /**
     * Generic GET method with cache expiration control
     *
     * @param <T> The type to deserialize the response into
     * @param endpoint The API endpoint to call
     * @param responseType The class to deserialize the response into
     * @param cacheExpiration How long to cache the response
     * @return Optional containing the deserialized response, or empty if failed
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String endpoint, Class<T> responseType, Duration cacheExpiration) {
        // Check cache first
        String fullUrl = endpoint.startsWith("http") ? endpoint : API_2B2T_URL + endpoint;
        CachedApiResponse<?> cachedResponse = responseCache.get(fullUrl);

        if (cachedResponse != null && !cachedResponse.isExpired()) {
            return Optional.of((T) cachedResponse.response);
        }

        // Not in cache or expired, fetch from API
        String responseStr = fetchResponse(fullUrl);
        if (responseStr == null || responseStr.equals("204 Undocumented")) {
            return Optional.empty();
        }

        try {
            T result = GSON.fromJson(responseStr, responseType);

            // Cache the result
            responseCache.put(fullUrl, new CachedApiResponse<>(result, Instant.now().plus(cacheExpiration)));

            return Optional.of(result);
        } catch (Exception e) {
            Stardust.LOG.error("[ApiHandler] Failed to deserialize response: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Class to store cached API responses with expiration time
     */
    private static class CachedApiResponse<T> {
        private final T response;
        private final Instant expiration;

        public CachedApiResponse(T response, Instant expiration) {
            this.response = response;
            this.expiration = expiration;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiration);
        }
    }
}
