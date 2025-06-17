package dev.stardust.commands;

import dev.stardust.util.MsgUtil;
import dev.stardust.modules.Loadouts;
import dev.stardust.util.StardustUtil;
import net.minecraft.command.CommandSource;
import meteordevelopment.meteorclient.commands.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.modules.Modules;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Loadout extends Command {
    public Loadout() { super("loadout", "Save and load inventory configurations."); }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("save").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            String loadoutName = ctx.getArgument("name", String.class);
            Modules mods = Modules.get();
            if (mods == null) return SINGLE_SUCCESS;
            Loadouts loadouts = mods.get(Loadouts.class);

            if (loadouts.noLoadout(loadoutName)) {
                MsgUtil.sendModuleMsg("Saving loadout" + StardustUtil.rCC() + "..!", loadouts.name);
            } else {
                MsgUtil.sendModuleMsg("Overwriting loadout" + StardustUtil.rCC() + "..!", loadouts.name);
            }
            loadouts.saveLoadout(loadoutName);

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("load").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            String loadoutName = ctx.getArgument("name", String.class);
            Modules mods = Modules.get();
            if (mods == null) return SINGLE_SUCCESS;
            Loadouts loadouts = mods.get(Loadouts.class);

            if (!loadouts.isActive()) {
                loadouts.toggle();
                loadouts.sendToggledMsg();
            }

            if (loadouts.noLoadout(loadoutName)) {
                MsgUtil.sendModuleMsg("No loadout was found with the name \"§c§o" + loadoutName + "§7\"§c..!", loadouts.name);
            } else {
                loadouts.loadLoadout(loadoutName);
                MsgUtil.sendModuleMsg("Loading loadout \"§a§o" + loadoutName + "§7\"§a..!", loadouts.name);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            String loadoutName = ctx.getArgument("name", String.class);
            Modules mods = Modules.get();
            if (mods == null) return SINGLE_SUCCESS;
            Loadouts loadouts = mods.get(Loadouts.class);

            if (loadouts.noLoadout(loadoutName)) {
                MsgUtil.sendModuleMsg("No loadout \"§5§o" + loadoutName + "§7\" to delete§c..!", loadouts.name);
            } else {
                loadouts.deleteLoadout(loadoutName);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("clear").executes(ctx -> {
            Modules mods = Modules.get();
            if (mods == null) return SINGLE_SUCCESS;
            Loadouts loadouts = mods.get(Loadouts.class);

            loadouts.clearLoadouts();
            MsgUtil.sendModuleMsg("Loadouts cleared.", loadouts.name);

            return SINGLE_SUCCESS;
        }));
    }
}
