package dev.stardust.modules;

import java.util.List;
import java.util.ArrayList;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import dev.stardust.util.MsgUtil;
import net.minecraft.text.HoverEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.StringListSetting;
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

    private final Setting<IgnoreStyle> ignoreStyle = settings.getDefaultGroup().add(
        new EnumSetting.Builder<IgnoreStyle>()
            .name("ignore-advertisers")
            .description("Whether to ignore accounts which trigger the blocked patterns filter.")
            .defaultValue(IgnoreStyle.Ignore)
            .build()
    );
    private final Setting<List<String>> patterns = settings.getDefaultGroup().add(
        new StringListSetting.Builder()
            .name("blocked-patterns")
            .description("Chat messages matching any of these patterns will be blocked, and ignore preferences applied to the culprit.")
            .defaultValue(
                List.of(
                    "thishttp", "discord.com", "discord.gg", "gg/", "com/", "/invite/", "% off",
                    ".store", "cheapest price", "cheapest kit", "cheap price", "cheap kit", "use code", "at checkout", "join now"
                )
            )
            .build()
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.getNetworkHandler() == null) return;
        if (!(event.packet instanceof GameMessageS2CPacket packet)) return;

        if (packet.content() == null) return;
        String content = packet.content().getString();
        for (String pattern : patterns.get()) {
            if (content.toLowerCase().contains(pattern.toLowerCase())) {
                event.cancel(); // fuck yo packets
                if (!ignoreStyle.get().equals(IgnoreStyle.None)) {
                    String name = getNameFromMessage(content);

                    String cmd;
                    if (ignoreStyle.get().equals(IgnoreStyle.Ignore)) {
                        cmd = "ignore";
                    } else {
                        cmd = "ignorehard";
                    }

                    if (name.isBlank()) {
                        cmd = "ignoredeathmsgs";
                        List<String> responsible = new ArrayList<>();
                        extractNamesFromDeathMessage(packet.content(), responsible);
                        for (String culprit : responsible) {
                            if (chatFeedback) {
                                MsgUtil.sendModuleMsg(
                                    "Ignoring death-message advertiser \"§c" + culprit + "§7\"§a..!",
                                    this.name
                                );
                            }
                            ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                                new CommandExecutionC2SPacket(cmd + " " + culprit), null, true
                            );
                        }
                    } else {
                        ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                            new CommandExecutionC2SPacket(cmd + " " + name), null, true
                        );
                    }
                }
                break;
            }
        }
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
