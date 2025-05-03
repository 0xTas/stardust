package dev.stardust.commands;

import com.google.gson.Gson;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import dev.stardust.util.StardustUtil;
import dev.stardust.util.TimeFormatter;
import net.minecraft.command.CommandSource;
import dev.stardust.util.commands.ApiHandler;
import dev.stardust.util.MessageFormatter;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 * <p>
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class Stats2b2t extends Command {
    private final String API_ENDPOINT = "/stats/player?playerName=";

    public Stats2b2t() {
        super("stats2b2t", "Fetch stats for a 2b2t player from api.2b2t.vc.", "stats");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word()).executes(ctx -> {
            String playerName = ctx.getArgument("player", String.class);
            fetchPlayerStats(playerName);
            return SINGLE_SUCCESS;
        }));
    }

    private void fetchPlayerStats(String playerName) {
        MeteorExecutor.execute(() -> {
            ClientPlayerEntity player = mc.player;
            if (player == null) return;

            String requestString = ApiHandler.API_2B2T_URL + API_ENDPOINT + playerName.trim();
            String response = new ApiHandler().fetchResponse(requestString);

            if (response == null) return;
            if (response.equals("204 Undocumented")) {
                player.sendMessage(Text.of(MessageFormatter.formatErrorMessage("Player not found...")));
                return;
            }

            try {
                PlayerStats stats = new Gson().fromJson(response, PlayerStats.class);
                String formattedStats = formatPlayerStats(stats, playerName);
                player.sendMessage(Text.of(formattedStats));
            } catch (Exception err) {
                Stardust.LOG.error("[Stats2b2t] Failed to deserialize Json: " + err);
                error("§7Failed to deserialize response from the server§4..!");
            }
        });
    }

    private String formatPlayerStats(PlayerStats stats, String playerName) {
        String cc = StardustUtil.rCC();

        String formattedFirstSeen = TimeFormatter.formatShortDate(stats.firstSeen);
        String formattedLastSeen = TimeFormatter.formatShortDate(stats.lastSeen);
        String formattedPlaytime = TimeFormatter.formatSimplifiedPlaytime(stats.playtimeSeconds);
        String formattedPlaytimeInMonth = TimeFormatter.formatSimplifiedPlaytime(stats.playtimeSecondsMonth);
        String kdRatioString = String.valueOf((float) stats.killCount / (float) stats.deathCount);

        return "§8<" + cc + "§o✨" + "§r§8> §7§oStats for " + cc + "§o" + playerName + "§7§o:\n"
            + "    §7Joins: " + MessageFormatter.formatValue(stats.joinCount)
            + "\n    §7Leaves: " + MessageFormatter.formatValue(stats.leaveCount)
            + "\n    §7K/D Ratio: " + MessageFormatter.formatValue(kdRatioString)
            + "\n    §7Chats: " + MessageFormatter.formatValue(stats.chatsCount)
            + "\n    §7Prio: " + MessageFormatter.formatValue(stats.prio)
            + "\n    §7First Seen: " + MessageFormatter.formatValue(formattedFirstSeen)
            + "\n    §7Last Seen: " + MessageFormatter.formatValue(formattedLastSeen)
            + "\n    §7Playtime: " + MessageFormatter.formatValue(formattedPlaytime)
            + "\n    §7Playtime in last month: " + MessageFormatter.formatValue(formattedPlaytimeInMonth);
    }

    private record PlayerStats(
        int joinCount, int leaveCount, int deathCount, int killCount, String firstSeen,
        String lastSeen, long playtimeSeconds, long playtimeSecondsMonth, int chatsCount, boolean prio) {
    }
}
