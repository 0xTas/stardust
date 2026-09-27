package dev.stardust.modules;

import java.util.List;
import java.util.ArrayList;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import dev.stardust.util.MsgUtil;
import net.minecraft.text.HoverEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.settings.*;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AdBlocker extends Module {
    public AdBlocker() { super(Stardust.CATEGORY, "AdBlocker", "Blocks advertisers in chat."); }

    public enum IgnoreStyle {
        None, Ignore, HardIgnore
    }

    private final SettingGroup sgAdBlocker = settings.createGroup("AdBlocker");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    private final Setting<IgnoreStyle> ignoreStyle = sgAdBlocker.add(
        new EnumSetting.Builder<IgnoreStyle>()
            .name("ignore-advertisers")
            .description("Whether to ignore accounts which trigger the blocked patterns filter.")
            .defaultValue(IgnoreStyle.Ignore)
            .build()
    );
    private final Setting<List<String>> patterns = sgAdBlocker.add(
        new StringListSetting.Builder()
            .name("blocked-patterns")
            .description("Chat messages matching any of these patterns will be blocked, and ignore preferences applied to the culprit.")
            .defaultValue(
                List.of(
                    "thishttp", "discord.com", "discord.gg", "gg/", "com/", "/invite/", "% off",
                    ".store", "cheapest price", "cheapest kit", "cheap price", "cheap kit", "use code", "at checkout",
                    "join now", "rusherhack.org", "nox2b", ".shop", "/./gg"
                )
            )
            .build()
    );

    private final Setting<Boolean> shouldWhitelist = sgWhitelist.add(
        new BoolSetting.Builder()
            .name("whitelist-enabled")
            .description("Whether to whitelist certain players to exempt them from the AdBlocker.")
            .defaultValue(false)
            .build()
    );
    private final Setting<List<String>> whitelist = sgWhitelist.add(
        new StringListSetting.Builder()
            .name("player-whitelist")
            .description("Player names in this list will not have their messages blocked by the filter.")
            .defaultValue(this.getFriendsList())
            .visible(shouldWhitelist::get)
            .build()
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.getNetworkHandler() == null) return;
        if (!(event.packet instanceof GameMessageS2CPacket packet)) return;

        if (packet.content() == null) return;
        String content = packet.content().getString();
        String senderName = getNameFromMessage(content);
        if (shouldWhitelist.get() && whitelist.get().contains(senderName)) return;

        boolean cancel = false;
        for (String pattern : patterns.get()) {
            if (pattern.isBlank()) continue;
            if (content.toLowerCase().contains(pattern)) {
                if (!ignoreStyle.get().equals(IgnoreStyle.None)) {
                    String cmd;
                    if (ignoreStyle.get().equals(IgnoreStyle.Ignore)) {
                        cmd = "ignore";
                    } else {
                        cmd = "ignorehard";
                    }

                    if (senderName.isBlank()) {
                        cmd = "ignoredeathmsgs";
                        List<String> responsible = new ArrayList<>();
                        extractNamesFromDeathMessage(packet.content(), responsible);

                        for (String culprit : responsible) {
                            if (shouldWhitelist.get() && whitelist.get().contains(culprit)) return;

                            if (chatFeedback) {
                                MsgUtil.sendModuleMsg(
                                    "Ignoring death-message advertiser \"§c" + culprit + "§7\"§a..!",
                                    this.name
                                );
                            }

                            cancel = true;
                            ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                                new CommandExecutionC2SPacket(cmd + " " + culprit), null, true
                            );
                        }
                    } else {
                        cancel = true;
                        ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                            new CommandExecutionC2SPacket(cmd + " " + senderName), null, true
                        );
                    }
                }

                if (cancel || !shouldWhitelist.get() || !whitelist.get().contains(senderName))
                    event.cancel();

                break;
            }
        }
    }

    private List<String> getFriendsList() {
        List<String> friends = new ArrayList<>();
        Friends.get().forEach(friend -> friends.add(friend.getName()));

        return friends;
    }

    private String getNameFromMessage(String message) {
        String name = "";
        String[] parts = message.split(" ");
        if (parts.length >= 3 && parts[1].equals("whispers:")) name = parts[0];
        else if (parts[0].startsWith("<") && parts[0].endsWith(">")) name = parts[0].substring(1, parts[0].length() - 1);

        return name;
    }

    private void extractNamesFromDeathMessage(Text msg, List<String> names) {
        if (msg.getStyle().getHoverEvent() != null) {
            HoverEvent event = msg.getStyle().getHoverEvent();
            if (event.getAction().equals(HoverEvent.Action.SHOW_TEXT)) {
                Text value = (Text) event.getValue(event.getAction());
                if (value != null && value.getString().startsWith("Message ")) {
                    names.add(value.getString().substring(8).trim());
                }
            }
        }

        for (Text sibling : msg.getSiblings()) {
            extractNamesFromDeathMessage(sibling, names);
        }
    }
}
