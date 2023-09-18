package dev.stardust.commands;

import dev.stardust.Stardust;
import net.minecraft.text.Text;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.util.concurrent.TimeUnit;
import dev.stardust.util.StardustUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;
import dev.stardust.util.commands.ApiHandler;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.ClientPlayerEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class Playtime2b2t extends Command {
    private final String API_ENDPOINT = "/playtime?playerName=";

    public Playtime2b2t() { super("playtime2b2t", "Check the playtime of a 2b2t player.", "pt"); }


    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            argument("player", StringArgumentType.word()).executes(ctx -> {
                MeteorExecutor.execute(() -> {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;

                    String playerString = ctx.getArgument("player", String.class);
                    String requestString = ApiHandler.API_2B2T_URL + API_ENDPOINT + playerString.trim();

                    String response = new ApiHandler().fetchResponse(requestString);
                    if (response == null) return;

                    if (response.equals("204 Undocumented")) {
                        if (player == null) return;
                        player.sendMessage(
                            Text.of(
                                "§8<"+StardustUtil.rCC()+"§o✨"+"§r§8> §4§oPlayer not found§7."
                            )
                        );
                    } else {
                        JsonElement ptJson = JsonParser.parseString(response);

                        if (ptJson.getAsJsonObject().has("playtimeSeconds")) {
                            long playtimeSeconds = ptJson.getAsJsonObject().get("playtimeSeconds").getAsLong();

                            long days  = TimeUnit.SECONDS.toDays(playtimeSeconds);
                            playtimeSeconds -= TimeUnit.DAYS.toSeconds(days);

                            long hours = TimeUnit.SECONDS.toHours(playtimeSeconds);
                            playtimeSeconds -= TimeUnit.HOURS.toSeconds(hours);

                            long minutes = TimeUnit.SECONDS.toMinutes(playtimeSeconds);
                            playtimeSeconds -= TimeUnit.MINUTES.toSeconds(minutes);

                            long seconds = TimeUnit.SECONDS.toSeconds(playtimeSeconds);

                            String cc = StardustUtil.rCC();
                            StringBuilder sb = new StringBuilder()
                                .append("§8<").append(StardustUtil.rCC()).append("§o✨§r§8> ")
                                .append(cc).append("§o").append(playerString).append("§7: ").append(cc);
                            if (days != 0) sb.append(days).append(" §7Days, ").append(cc);
                            if (hours != 0) sb.append(hours).append(" §7Hours, ").append(cc);
                            if (minutes != 0) sb.append(minutes).append(" §7Minutes, ").append(cc);
                            if (seconds != 0) sb.append(seconds).append(" §7Seconds§7.");

                            if (player != null) player.sendMessage(Text.of(sb.toString()));
                        } else {
                            ApiHandler.sendErrorResponse();
                            Stardust.LOG.warn("[Stardust] received unexpected output from api.2b2t.vc: "+ptJson);
                        }
                    }
                });

                return SINGLE_SUCCESS;
            })
        );
    }
}
