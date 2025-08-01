package dev.stardust.commands;

import java.util.Locale;
import java.time.ZoneId;
import java.time.Instant;
import com.google.gson.Gson;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import java.time.ZonedDateTime;
import dev.stardust.util.LogUtil;
import java.util.concurrent.TimeUnit;
import dev.stardust.util.StardustUtil;
import java.time.format.DateTimeFormatter;
import net.minecraft.command.CommandSource;
import dev.stardust.util.commands.ApiHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 * credit to <a href="https://github.com/rfresh2">rfresh for the 2b api</a>
 **/
public class Stats2b2t extends Command {
    private final String API_ENDPOINT = "/stats/player?playerName=";
    public Stats2b2t() { super("stats2b2t", "Fetch stats for a 2b2t player from api.2b2t.vc.", "stats"); }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word()).executes(ctx -> {
            MeteorExecutor.execute(() -> {
                ClientPlayerEntity player = mc.player;

                if (player == null) return;
                String playerString = ctx.getArgument("player", String.class);
                String requestString = ApiHandler.API_2B2T_URL + API_ENDPOINT + playerString.trim();
                String response = new ApiHandler().fetchResponse(requestString);

                if (response == null) return;
                if (response.equals("204 Undocumented")) {
                    player.sendMessage(
                        Text.of(
                            "§8<"+ StardustUtil.rCC()+"§o✨"+"§r§8> §4§oPlayer not found§7..."
                        ), false
                    );
                } else {
                    try {
                        Gson gson = new Gson();
                        String cc = StardustUtil.rCC();
                        PlayerStats stats = gson.fromJson(response, PlayerStats.class);

                        Instant firstInstant = Instant.parse(stats.firstSeen);
                        Instant lastInstant = Instant.parse(stats.lastSeen);
                        ZonedDateTime firstZonedTime = firstInstant.atZone(ZoneId.systemDefault());
                        ZonedDateTime lastZonedTime = lastInstant.atZone(ZoneId.systemDefault());
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

                        String formattedFirstSeen = firstZonedTime.format(fmt);
                        String formattedLastSeen = lastZonedTime.format(fmt);

                        String formattedPlaytime;
                        String formattedPlaytimeInMonth;
                        long playtimeSeconds = stats.playtimeSeconds;
                        long playtimeSecondsInMonth = stats.playtimeSecondsMonth;

                        if (playtimeSeconds <= 0) {
                            formattedPlaytime = "none";
                        } else {
                            long days = TimeUnit.SECONDS.toDays(playtimeSeconds);
                            long hours = TimeUnit.SECONDS.toHours(playtimeSeconds);
                            long minutes = TimeUnit.SECONDS.toMinutes(playtimeSeconds);

                            if (days >= 60) {
                                formattedPlaytime = days / 30 + " months";
                            } else if (days >= 30) {
                                formattedPlaytime = "1 month";
                            } else if (days >= 14) {
                                formattedPlaytime = days / 7 + " weeks";
                            } else if (days >= 7) {
                                formattedPlaytime = "1 week";
                            } else if (days >= 2) {
                                formattedPlaytime = days + " days";
                            } else if (days > 0) {
                                formattedPlaytime = "1 day";
                            } else if (hours >= 2) {
                                formattedPlaytime = hours + " hours";
                            } else if (hours > 0) {
                                formattedPlaytime = "1 hour";
                            } else if (minutes >= 2) {
                                formattedPlaytime = minutes + " minutes";
                            } else if (minutes > 0) {
                                formattedPlaytime = "1 minute";
                            } else if (playtimeSeconds > 1) {
                                formattedPlaytime = playtimeSeconds + " seconds";
                            } else {
                                formattedPlaytime = "1 second";
                            }
                        }

                        if (playtimeSecondsInMonth <= 0) {
                            formattedPlaytimeInMonth = "none";
                        } else {
                            long daysInMonth = TimeUnit.SECONDS.toDays(playtimeSecondsInMonth);
                            long hoursInMonth = TimeUnit.SECONDS.toHours(playtimeSecondsInMonth);
                            long minutesInMonth = TimeUnit.SECONDS.toMinutes(playtimeSecondsInMonth);

                            if (daysInMonth >= 28) {
                                formattedPlaytimeInMonth = "1 month";
                            } else if (daysInMonth >= 14) {
                                formattedPlaytimeInMonth = daysInMonth / 7 + " weeks";
                            } else if (daysInMonth >= 7) {
                                formattedPlaytimeInMonth = "1 week";
                            } else if (daysInMonth >= 2) {
                                formattedPlaytimeInMonth = daysInMonth + " days";
                            } else if (daysInMonth > 0) {
                                formattedPlaytimeInMonth = "1 day";
                            } else if (hoursInMonth >= 2) {
                                formattedPlaytimeInMonth = hoursInMonth + " hours";
                            } else if (hoursInMonth > 0) {
                                formattedPlaytimeInMonth = "1 hour";
                            } else if (minutesInMonth >= 2) {
                                formattedPlaytimeInMonth = minutesInMonth + " minutes";
                            } else if (minutesInMonth > 0) {
                                formattedPlaytimeInMonth = "1 minute";
                            } else if (playtimeSecondsInMonth > 1) {
                                formattedPlaytimeInMonth = playtimeSecondsInMonth + " seconds";
                            } else {
                                formattedPlaytimeInMonth = "1 second";
                            }
                        }

                        String kdRatioString = String.valueOf((float) stats.killCount / (float) stats.deathCount);
                        player.sendMessage(
                            Text.of(
                                "§8<" + StardustUtil.rCC() + "§o✨" + "§r§8> §7§oStats for "+cc+"§o" + playerString + "§7§o:\n"
                                + "    §7Joins: "+cc+"§o"+stats.joinCount+"\n    §7Leaves: "+cc+"§o"+stats.leaveCount
                                + "\n    §7K/D Ratio: "+cc+"§o"+kdRatioString
                                + "\n    §7Chats: "+cc+"§o"+stats.chatsCount+"\n    §7Prio: "+cc+"§o"+stats.prio
                                + "\n    §7First Seen: "+cc+"§o"+formattedFirstSeen+"\n    §7Last Seen: "+cc+"§o"+formattedLastSeen
                                + "\n    §7Playtime: "+cc+"§o"+formattedPlaytime+"\n    §7Playtime in last month: "+cc+"§o"+formattedPlaytimeInMonth
                            ), false
                        );
                    } catch (Exception err) {
                        error("§7Failed to deserialize response from the server§4..!");
                        LogUtil.error("Failed to deserialize Json: " + err, this.getName());
                    }
                }
            });
            return SINGLE_SUCCESS;
        }));
    }

    private record PlayerStats(
        int joinCount, int leaveCount, int deathCount, int killCount, String firstSeen,
        String lastSeen, long playtimeSeconds, long playtimeSecondsMonth, int chatsCount, boolean prio) {}
}
