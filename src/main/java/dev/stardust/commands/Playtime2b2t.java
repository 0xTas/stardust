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
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.ClientPlayerEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 * <p>
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class Playtime2b2t extends Command {
    private final String API_ENDPOINT = "/playtime?playerName=";

    public Playtime2b2t() {
        super("playtime2b2t", "Check the playtime of a 2b2t player.", "pt");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word()).executes(ctx -> {
            String playerName = ctx.getArgument("player", String.class);
            fetchPlaytime(playerName);
            return SINGLE_SUCCESS;
        }));
    }

    private void fetchPlaytime(String playerName) {
        MeteorExecutor.execute(() -> {
            var player = MinecraftClient.getInstance().player;
            if (player == null) return;

            String requestString = ApiHandler.API_2B2T_URL + API_ENDPOINT + playerName.trim();
            String response = new ApiHandler().fetchResponse(requestString);

            if (response == null) return;

            if (response.equals("204 Undocumented")) {
                player.sendMessage(Text.of(MessageFormatter.formatErrorMessage("Player not found")));
                return;
            }

            try {
                var ptJson = JsonParser.parseString(response).getAsJsonObject();
                if (ptJson.has("playtimeSeconds")) {
                    long playtimeSeconds = ptJson.get("playtimeSeconds").getAsLong();
                    String cc = StardustUtil.rCC();
                    String formattedPlaytime = TimeFormatter.formatPlaytime(playtimeSeconds, cc);

                    player.sendMessage(Text.of(MessageFormatter.formatPlayerMessage(
                        playerName, "has played for " + cc + formattedPlaytime + "§7."
                    )));
                } else {
                    ApiHandler.sendErrorResponse();
                    Stardust.LOG.warn("[Stardust] received unexpected output from api.2b2t.vc: " + ptJson);
                }
            } catch (Exception e) {
                Stardust.LOG.error("[Playtime2b2t] Failed to parse response: " + e.getMessage());
                ApiHandler.sendErrorResponse();
            }
        });
    }
}
