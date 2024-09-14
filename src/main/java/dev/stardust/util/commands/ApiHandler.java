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

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class ApiHandler {
    public static final String API_2B2T_URL = "https://api.2b2t.vc";

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
            Stardust.LOG.error("[ApiHandler] "+err);
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
            Stardust.LOG.error("[ApiHandler] "+err);
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
            Stardust.LOG.warn("[ApiHandler] received unexpected response from api.2b2t.vc: "+res);
        }

        return null;
    }
}
