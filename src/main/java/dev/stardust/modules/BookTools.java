package dev.stardust.modules;

import dev.stardust.Stardust;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
public class BookTools extends Module {
    public BookTools() { super(Stardust.CATEGORY, "BookTools", "Enhancements for working with books."); }

    private final SettingGroup sgFormat = settings.createGroup("Color & Formatting");
    private final SettingGroup sgDeobfuscate = settings.createGroup("Deobfuscation");

    private final Setting<Boolean> doFormatting = sgFormat.add(
        new BoolSetting.Builder()
            .name("formatting-buttons")
            .description("Adds buttons for coloring & formatting text in writable books.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> doFormatTitles = sgFormat.add(
        new BoolSetting.Builder()
            .name("allow-formatting-titles*")
            .description("*Doesn't work on servers like 2b2t.")
            .visible(doFormatting::get)
            .defaultValue(false)
            .build()
    );

    public final Setting<String> autoTitles = sgFormat.add(
        new StringSetting.Builder()
            .name("auto-title") // See BookEditScreenMixin.java
            .description("Automatically inserts this book title (if not empty) when signing books (for use with unicode chars).")
            .defaultValue("")
            .build()
    );

    private final Setting<Boolean> doDeobfucscation = sgDeobfuscate.add(
        new BoolSetting.Builder()
            .name("deobfuscation-button")
            .description("Adds a button that deobfuscates obfuscated/magic text in written books.")
            .defaultValue(true)
            .build()
    );


    // See BookEditScreenMixin.java
    public boolean skipFormatting() {
        return !doFormatting.get();
    }

    public boolean shouldFormatTitles() {
        return doFormatTitles.get();
    }

    // See BookScreenMixin.java
    public boolean skipDeobfuscation() {
        return !doDeobfucscation.get();
    }
}
