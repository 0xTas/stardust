package dev.stardust.modules;

import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import dev.stardust.util.StardustUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import dev.stardust.mixin.meteor.accessor.ChatUtilsAccessor;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class ChatPrefix extends Module {
    public ChatPrefix() { super(Stardust.CATEGORY, "ChatPrefix", "Change Meteor's client-message prefix to whatever you like."); }

    public final Setting<String> delim1 = settings.getDefaultGroup().add(
        new StringSetting.Builder()
            .name("delimiter-1")
            .description("String to use for the first prefix delimiter.")
            .defaultValue("<")
            .onChanged(it -> this.applyPrefix(it, this.prefix.get(), this.delim2.get()))
            .build()
    );

    public final Setting<String> delim2 = settings.getDefaultGroup().add(
        new StringSetting.Builder()
            .name("delimiter-2")
            .description("String to use for the second prefix delimiter. Trailing space is NOT added automatically.")
            .defaultValue("> ")
            .onChanged(it -> this.applyPrefix(this.delim1.get(), this.prefix.get(), it))
            .build()
    );

    public final Setting<String> prefix = settings.getDefaultGroup().add(
        new StringSetting.Builder()
            .name("prefix")
            .description("What string to use for the chat prefix. This will have formatting applied to it.")
            .defaultValue("✨")
            .onChanged(it -> this.applyPrefix(this.delim1.get(), it, this.delim2.get()))
            .build()
    );

    public final Setting<StardustUtil.TextFormat> format = settings.getDefaultGroup().add(
        new EnumSetting.Builder<StardustUtil.TextFormat>()
            .name("format")
            .description("What formatting to apply to the prefix text.")
            .defaultValue(StardustUtil.TextFormat.Plain)
            .onChanged(this::applyPrefix)
            .build()
    );

    public final Setting<SettingColor> color = settings.getDefaultGroup().add(
        new ColorSetting.Builder()
            .name("color")
            .description("What color to use for the prefix text.")
            .defaultValue(new SettingColor(0, 255, 138))
            .onChanged(this::applyPrefix)
            .build()
    );

    @Override
    public void onActivate() {
        applyPrefix();
    }

    @Override
    public void onDeactivate() {
        ChatUtilsAccessor.setPrefix(
            Text.empty()
                .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                .append("[")
                .append(Text.literal("Meteor").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(MeteorClient.ADDON.color.getPacked()))))
                .append("] ")
        );
    }

    private void applyPrefix() {
        if (toFormatting(this.format.get()) == Formatting.RESET) {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(this.delim1.get())
                    .append(Text.literal(this.prefix.get()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color.get().getPacked()))))
                    .append(this.delim2.get())
            );
        } else {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(this.delim1.get())
                    .append(Text.literal(this.prefix.get()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color.get().getPacked())).withFormatting(toFormatting(this.format.get()))))
                    .append(this.delim2.get())
            );
        }
    }

    private void applyPrefix(String delim1, String prefix, String delim2) {
        if (toFormatting(this.format.get()) == Formatting.RESET) {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(delim1)
                    .append(Text.literal(prefix).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color.get().getPacked()))))
                    .append(delim2)
            );
        } else {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(delim1)
                    .append(Text.literal(prefix).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color.get().getPacked())).withFormatting(toFormatting(this.format.get()))))
                    .append(delim2)
            );
        }
    }

    private void applyPrefix(SettingColor color) {
        if (toFormatting(this.format.get()) == Formatting.RESET) {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(this.delim1.get())
                    .append(Text.literal(this.prefix.get()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getPacked()))))
                    .append(this.delim2.get())
            );
        } else {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(this.delim1.get())
                    .append(Text.literal(this.prefix.get()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getPacked())).withFormatting(toFormatting(this.format.get()))))
                    .append(this.delim2.get())
            );
        }
    }

    private void applyPrefix(StardustUtil.TextFormat format) {
        if (toFormatting(format) == Formatting.RESET) {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(this.delim1.get())
                    .append(Text.literal(this.prefix.get()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color.get().getPacked()))))
                    .append(this.delim2.get())
            );
        } else {
            ChatUtilsAccessor.setPrefix(
                Text.empty()
                    .setStyle(Style.EMPTY.withFormatting(Formatting.GRAY))
                    .append(this.delim1.get())
                    .append(Text.literal(this.prefix.get()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color.get().getPacked())).withFormatting(toFormatting(format))))
                    .append(this.delim2.get())
            );
        }
    }

    private Formatting toFormatting(StardustUtil.TextFormat format) {
        return switch (format) {
            case Italic -> Formatting.ITALIC;
            case Bold -> Formatting.BOLD;
            case Underline -> Formatting.UNDERLINE;
            case Strikethrough -> Formatting.STRIKETHROUGH;
            case Obfuscated -> Formatting.OBFUSCATED;
            default -> Formatting.RESET;
        };
    }
}
