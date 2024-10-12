package dev.stardust.mixin;

import java.time.Instant;
import java.time.Duration;
import dev.stardust.util.StardustUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.GuiTheme;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Cycles to a new random Stardust category icon every 30 minutes.
 **/
@Mixin(value = ModulesScreen.class, remap = false)
public abstract class ModulesScreenMixin extends TabScreen {
    public ModulesScreenMixin(GuiTheme theme, Tab tab) {
        super(theme, tab);
    }

    @Unique
    @Nullable
    private Instant createdAt = null;

    @Inject(method = "createCategory", at = @At("HEAD"))
    private void cycleCategoryIcons(WContainer c, Category category, CallbackInfoReturnable<WWindow> cir) {
        if (category.name.equals("Stardust")) {
            if (!theme.categoryIcons()) {
                ((CategoryAccessor) category).setIcon(StardustUtil.chooseMenuIcon());
            } else if (createdAt == null) {
                createdAt = Instant.now();
            } else {
                Instant now = Instant.now();
                if (Duration.between(createdAt, now).toSeconds() > 1800) {
                    createdAt = now;
                    ((CategoryAccessor) category).setIcon(StardustUtil.chooseMenuIcon());
                }
            }
        }
    }
}
