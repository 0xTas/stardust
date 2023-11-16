package dev.stardust.modules;

import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import net.minecraft.text.*;
import dev.stardust.Stardust;
import javax.annotation.Nullable;
import java.net.HttpURLConnection;
import dev.stardust.util.StardustUtil;
import java.net.MalformedURLException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class UpdateNotifier extends Module {
    public UpdateNotifier() {
        super(Stardust.CATEGORY, "UpdateNotifier", "Notifies you in chat when a new version of Stardust is available.");

        if (!this.isActive()) { // only way of making the module "enabled by default".
            MeteorClient.EVENT_BUS.subscribe(this);
        }
    }

    private final Setting<Boolean> notify = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Enable Notifications")
            .description("Disable this to disable update notifications.")
            .defaultValue(true)
            .build()
    );

    private boolean notified = false;
    private final String releaseUrl = "https://github.com/0xTas/stardust/releases/latest";


    @Nullable
    public String fetchNewestVersionNumber() {
        HttpURLConnection req;
        try {
            req = (HttpURLConnection) new URL(this.releaseUrl).openConnection();
        } catch (MalformedURLException err) {
            Stardust.LOG.error("[Stardust] Failed to open http connection. Why:\n" + err);
            return null;
        } catch (Exception err) {
            Stardust.LOG.error("[Stardust] Update Notifier failed to make a connection - "+err);
            return null;
        }

        req.setInstanceFollowRedirects(false);
        try {
            req.connect();
        }catch (Exception err) {
            Stardust.LOG.error("[Stardust] Update Notifier failed to make a connection - "+err);
            return null;
        }

        Map<String, List<String>> headerFields = req.getHeaderFields();
        if (headerFields.containsKey("Location")) {
            List<String> field = headerFields.get("Location");
            if (field.isEmpty()) {
                Stardust.LOG.warn("[Stardust] Update Notifier failed to parse a response from Github.");
                return null;
            } else {
                return field.get(0).substring(field.get(0).lastIndexOf("v")+1);
            }
        }

        return null;
    }

    public void notifyUpdate(String newVersion) {
        if (this.notified) return;
        this.notified = true;
        MeteorExecutor.execute(() -> {
            try {
                Thread.sleep(7000);
                if (mc.player != null) {
                    Text txt = Text.of(
                        "§8<"+ StardustUtil.rCC()
                            +"§o✨§r§8> §7A new version of "+StardustUtil.rCC()
                            +"§oStardust §r§8(§o§2"+newVersion+"§r§8) §7is available."
                    );

                    Style style = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("§7§oClick to open the Github page.")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, releaseUrl));

                    MutableText notifyMessage = txt.copyContentOnly().setStyle(style);
                    mc.player.sendMessage(notifyMessage);

                }
            } catch (Exception err) {
                Stardust.LOG.warn("[Stardust] Update Notifier MeteorExecutor task was interrupted! Why:\n" + err);
            }
        });
    }


    @EventHandler
    private void onJoinGameEvent(GameJoinedEvent event) {
        if (!this.notify.get() || this.notified) return;

        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("stardust");
        if (mod.isEmpty()) return;

        String version = mod.get().getMetadata().getVersion().getFriendlyString();
        String newVersion = fetchNewestVersionNumber();

        if (newVersion == null) return;
        if (version.trim().equals(newVersion.trim())) return;
        notifyUpdate(newVersion);
    }
}
