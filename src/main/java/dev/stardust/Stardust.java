package dev.stardust;

import org.slf4j.Logger;
import dev.stardust.modules.*;
import dev.stardust.commands.*;
import net.minecraft.item.Items;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Stardust extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Stardust", Items.NETHER_STAR.getDefaultStack());

    @Override
    public void onInitialize() {
        Commands.add(new Playtime2b2t());
        Commands.add(new LastSeen2b2t());
        Commands.add(new FirstSeen2b2t());

        Modules.get().add(new AutoDoors());
        Modules.get().add(new BookTools());
        Modules.get().add(new ChatSigns());
        Modules.get().add(new BannerData());
        Modules.get().add(new SignatureSign());
        Modules.get().add(new UpdateNotifier());
        Modules.get().add(new AutoDrawDistance());

        LOG.info("<✨> Stardust initialized.");
    }


    @Override
    public String getPackage() {
        return "dev.stardust";
    }
    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
    @Override
    public String getWebsite() { return "https://github.com/0xTas/stardust"; }
    @Override
    public GithubRepo getRepo() { return new GithubRepo("0xTas", "Stardust"); }
}
