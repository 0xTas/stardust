package dev.stardust.config;

import dev.stardust.util.StardustUtil;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;

public class StardustConfig {
    public static Setting<Boolean> greenSplashTextSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> rotateSplashTextSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> directConnectButtonSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> illegalDisconnectButtonSetting = new BoolSetting.Builder().build();
    public static Setting<Boolean> disableMeteorClientTelemetry = new BoolSetting.Builder().build();
    public static Setting<Boolean> antiInventoryPacketKick = new BoolSetting.Builder().build();
    public static Setting<StardustUtil.IllegalDisconnectMethod> illegalDisconnectMethodSetting = new EnumSetting.Builder<StardustUtil.IllegalDisconnectMethod>().defaultValue(StardustUtil.IllegalDisconnectMethod.Slot).build();

    public static void initialize() {
        SettingGroup sgStardust = Config.get().settings.createGroup("Stardust");

        // See SplashTextRendererMixin.java
        greenSplashTextSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("green-splash-text")
                .description(">Makes the title splash texts green.")
                .defaultValue(false)
                .build()
        );
        // See TitleScreenMixin.java
        rotateSplashTextSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("rotate-splash-text")
                .description("Picks a new random splash text every 20 seconds.")
                .defaultValue(false)
                .build()
        );
        directConnectButtonSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("direct-connect-button")
                .description("Adds a button to the main menu that directly connects you to 2b2t.org")
                .defaultValue(false)
                .build()
        );
        // See GameMenuScreenMixin.java
        illegalDisconnectButtonSetting = sgStardust.add(
            new BoolSetting.Builder()
                .name("illegal-disconnect-button")
                .description("Adds a button to the main menu that forces the server to kick you when pressed.")
                .defaultValue(false)
                .build()
        );
        // See StardustUtil.java
        illegalDisconnectMethodSetting = sgStardust.add(
            new EnumSetting.Builder<StardustUtil.IllegalDisconnectMethod>()
                .name("illegal-disconnect-method")
                .description("The method to use to cause the server to kick you.")
                .defaultValue(StardustUtil.IllegalDisconnectMethod.Chat)
                .build()
        );
        // See OnlinePlayersMixin.java
        disableMeteorClientTelemetry = sgStardust.add(
            new BoolSetting.Builder()
                .name("disable-meteor-telemetry")
                .description("Disables sending periodic telemetry pings to meteorclient.com for their online player count api.")
                .defaultValue(false)
                .build()
        );
        // See PacketSpamManager.java
        antiInventoryPacketKick = sgStardust.add(
            new BoolSetting.Builder()
                .name("anti-packet-spam-kick")
                .description("Attempts to prevent you from getting kicked for sending too many invalid inventory packets.")
                .defaultValue(false)
                .build()
        );
    }
}
