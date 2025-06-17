package dev.stardust.mixin.meteor;

import java.util.Arrays;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Adds a `Modified by: Stardust` label to built-ins enhanced by Stardust.
 **/
@Mixin(value = ModuleScreen.class, remap = false)
public abstract class ModuleScreenMixin extends WindowScreen {
    @Shadow
    @Final
    private Module module;

    @Shadow
    private WCheckbox active;

    @Shadow
    private WKeybind keybind;

    @Shadow
    private WContainer settingsContainer;

    public ModuleScreenMixin(GuiTheme theme, WWidget icon, String title) {
        super(theme, icon, title);
    }

    @Unique
    private final String[] mods = {
        "auto-mend", "auto-log", "better-tooltips", "exp-thrower", "freecam", "nametags", "no-render", "notifier"
    };

    @Inject(method = "initWidgets", at = @At("HEAD"), cancellable = true)
    private void maybeHijackInitWidgets(CallbackInfo ci) {
        if (this.module.addon != null && this.module.addon != MeteorClient.ADDON) return;
        if (Arrays.stream(mods).anyMatch(mod -> this.module.name.equalsIgnoreCase(mod))) {
            ci.cancel();

            // Description
            add(theme.label(module.description, Utils.getWindowWidth() / 2.0));

            // Add `Modified by` field for built-ins enhanced by Stardust
            WHorizontalList addon = add(theme.horizontalList()).expandX().widget();
            addon.add(theme.label("Modified by: ").color(theme.textSecondaryColor())).widget();
            addon.add(theme.label("Stardust").color(new Color(147, 233, 190))).widget();

            // Settings
            if (!module.settings.groups.isEmpty()) {
                settingsContainer = add(theme.verticalList()).expandX().widget();
                settingsContainer.add(theme.settings(module.settings)).expandX();
            }

            WWidget widget = module.getWidget(theme);

            if (widget != null) {
                add(theme.horizontalSeparator()).expandX();
                Cell<WWidget> cell = add(widget);
                if (widget instanceof WContainer) cell.expandX();
            }

            // Bind
            WSection section = add(theme.section("Bind", true)).expandX().widget();

            // Keybind
            WHorizontalList bind = section.add(theme.horizontalList()).expandX().widget();

            bind.add(theme.label("Bind: "));
            keybind = bind.add(theme.keybind(module.keybind)).expandX().widget();
            keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);

            WButton reset = bind.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
            reset.action = keybind::resetBind;

            // Toggle on bind release
            WHorizontalList tobr = section.add(theme.horizontalList()).widget();

            tobr.add(theme.label("Toggle on bind release: "));
            WCheckbox tobrC = tobr.add(theme.checkbox(module.toggleOnBindRelease)).widget();
            tobrC.action = () -> module.toggleOnBindRelease = tobrC.checked;

            // Chat feedback
            WHorizontalList cf = section.add(theme.horizontalList()).widget();

            cf.add(theme.label("Chat Feedback: "));
            WCheckbox cfC = cf.add(theme.checkbox(module.chatFeedback)).widget();
            cfC.action = () -> module.chatFeedback = cfC.checked;

            add(theme.horizontalSeparator()).expandX();

            // Bottom
            WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

            // Active
            bottom.add(theme.label("Active: "));
            active = bottom.add(theme.checkbox(module.isActive())).expandCellX().widget();
            active.action = () -> {
                if (module.isActive() != active.checked) module.toggle();
            };

            // Config sharing
            WHorizontalList sharing = bottom.add(theme.horizontalList()).right().widget();
            WButton copy = sharing.add(theme.button(GuiRenderer.COPY)).widget();
            copy.action = () -> {
                if (toClipboard()) {
                    OkPrompt.create()
                        .title("Module copied!")
                        .message("The settings for this module are now in your clipboard.")
                        .message("You can also copy settings using Ctrl+C.")
                        .message("Settings can be imported using Ctrl+V or the paste button.")
                        .id("config-sharing-guide")
                        .show();
                }
            };
            copy.tooltip = "Copy config";

            WButton paste = sharing.add(theme.button(GuiRenderer.PASTE)).widget();
            paste.action = this::fromClipboard;
            paste.tooltip = "Paste config";
        }
    }
}
