package dev.stardust.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.stardust.util.MessageFormatter;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Command that retrieves player UUIDs
 *
 * @author uxmlen
 * created on 5/3/2025
 */
public class GetUUID extends Command {

    public GetUUID() {
        super("getuuid", "retrieves a player's uuid");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            var player = MinecraftClient.getInstance().player;

            player.sendMessage(
                Text.of(MessageFormatter.formatNotification("Your UUID is " +
                    MessageFormatter.formatValue(mc.player.getUuid()))));

            return SINGLE_SUCCESS;
        });

        builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
            PlayerEntity player = PlayerArgumentType.get(context);

            player.sendMessage(Text.of(MessageFormatter.formatPlayerMessage(
                player.getGameProfile().getName(),
                "has UUID " + MessageFormatter.formatValue(player.getUuid())
            )));

            return SINGLE_SUCCESS;
        }));
    }

}
