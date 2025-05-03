package dev.stardust.commands;

import dev.stardust.Stardust;
import net.minecraft.text.Text;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import dev.stardust.util.StardustUtil;
import dev.stardust.util.TimeFormatter;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;
import dev.stardust.util.commands.ApiHandler;
import dev.stardust.util.MessageFormatter;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 * <p>
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class LastSeen2b2t extends Command {
    private final String API_ENDPOINT = "/seen?playerName=";

    public LastSeen2b2t() {
        super("lastseen2b2t", "Check the last-seen status of a 2b2t player.", "ls", "seen");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word()).executes(ctx -> {
            String playerName = ctx.getArgument("player", String.class);
            fetchLastSeen(playerName);
            return SINGLE_SUCCESS;
        }));
    }

    private void fetchLastSeen(String playerName) {
        MeteorExecutor.execute(() -> {
            var player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestString = ApiHandler.API_2B2T_URL + API_ENDPOINT + playerName.trim();
            String response = new ApiHandler().fetchResponse(requestString);

            if (response == null) return;

            if (response.equals("204 Undocumented") || response.contains("\"lastSeen\":null")) {
                player.sendMessage(Text.of(MessageFormatter.formatErrorMessage("That player has not been seen...")));
                return;
            }

            try {
                var seenJson = JsonParser.parseString(response).getAsJsonObject();
                if (seenJson.has("lastSeen")) {
                    String lastSeen = seenJson.get("lastSeen").getAsString();
                    String formattedTimestamp = TimeFormatter.formatTimestamp(lastSeen, StardustUtil.rCC());

                    player.sendMessage(Text.of(MessageFormatter.formatPlayerMessage(
                        playerName, "was last seen on " + formattedTimestamp + "§7."
                    )));
                } else {
                    ApiHandler.sendErrorResponse();
                    Stardust.LOG.warn("[Stardust] received unexpected output from api.2b2t.vc: " + seenJson);
                }
            } catch (Exception e) {
                Stardust.LOG.error("[LastSeen2b2t] Failed to parse response: " + e.getMessage());
                ApiHandler.sendErrorResponse();
            }
        });
    }
}
