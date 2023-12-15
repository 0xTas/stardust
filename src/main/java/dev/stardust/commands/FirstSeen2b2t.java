package dev.stardust.commands;

import java.time.ZoneId;
import java.util.Locale;
import java.time.Instant;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import java.time.ZonedDateTime;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import dev.stardust.util.StardustUtil;
import java.time.format.DateTimeFormatter;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;
import dev.stardust.util.commands.ApiHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class FirstSeen2b2t extends Command {
    private final String API_ENDPOINT = "/seen?playerName=";

    public FirstSeen2b2t() { super("firstseen2b2t", "Check the first-seen status of a 2b2t player.", "fs"); }

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

                    if (response.equals("204 Undocumented") || response.contains("\"firstSeen\":null,")) {
                        if (player == null) return;
                        player.sendMessage(
                            Text.of(
                                "§8<"+StardustUtil.rCC()+"§o✨"+"§r§8> §4§oThat player has not been seen§7..."
                            )
                        );
                    }else {
                        JsonElement seenJson = JsonParser.parseString(response);

                        if (seenJson.getAsJsonObject().has("firstSeen")) {
                            String firstSeen = seenJson.getAsJsonObject().get("firstSeen").getAsString();

                            Instant instant = Instant.parse(firstSeen);
                            ZonedDateTime zonedTime = instant.atZone(ZoneId.systemDefault());
                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM dd yyyy, HH:mm", Locale.US);


                            String cc = StardustUtil.rCC();
                            String formattedTimestamp = String.join(" §r§7at "+cc+"§o", zonedTime.format(fmt).split(", "));
                            if (player != null) {
                                player.sendMessage(
                                    Text.of(
                                        "§8<" + StardustUtil.rCC() + "§o✨" + "§r§8> "+cc+"§o"
                                            + playerString + "§r§7 was first seen on "+cc+"§o" + formattedTimestamp + "§7."
                                    )
                                );
                            }
                        } else {
                            ApiHandler.sendErrorResponse();
                            Stardust.LOG.warn("[Stardust] received unexpected output from api.2b2t.vc: "+seenJson);
                        }
                    }
                });

                return SINGLE_SUCCESS;
            })
        );
    }
}
