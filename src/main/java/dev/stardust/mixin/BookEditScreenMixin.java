package dev.stardust.mixin;

import java.util.Objects;
import java.util.ArrayList;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import dev.stardust.util.StardustUtil;
import dev.stardust.modules.BookTools;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow
    private boolean dirty;
    @Shadow
    private boolean signing;


    protected BookEditScreenMixin(Text title) { super(title); }


    @Unique
    private boolean didFormatPage = false;
    @Unique
    private final ArrayList<ButtonWidget> buttons = new ArrayList<>();

    @Unique
    private void onClickColorButton(ButtonWidget btn) {
        String color = btn.getMessage().getString().substring(0, 2);

        if (this.signing) {
            ((BookEditScreenAccessor) this).getBookTitleSelectionManager().insert(color);
        } else {
            this.didFormatPage = true;
            ((BookEditScreenAccessor) this).getCurrentPageSelectionManager().insert(color);
        }
    }

    @Unique
    private void onClickFormatButton(ButtonWidget btn) {
        String format = btn.getMessage().getString().substring(0, 2);

        if (this.signing) {
            ((BookEditScreenAccessor) this).getBookTitleSelectionManager().insert(format);
        } else {
            this.didFormatPage = true;
            ((BookEditScreenAccessor) this).getCurrentPageSelectionManager().insert(format);
        }
    }

    // See BookTools.java
    @Inject(method = "init", at = @At("TAIL"))
    private void mixinInit(CallbackInfo ci) {
        BookTools bookTools = Modules.get().get(BookTools.class);
        if (bookTools.skipFormatting()) return;

        int offset = 0;
        boolean odd = false;
        for (StardustUtil.TextColor color : StardustUtil.TextColor.values()) {
            if (Objects.equals(color.label, "")) continue;

            this.buttons.add(
                this.addDrawableChild(
                    ButtonWidget.builder(
                            Text.of(color.label+"§l◼"),
                            this::onClickColorButton
                        )
                        .dimensions(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                        .tooltip(Tooltip.of(Text.of("§7"+color.name().replace("_", " "))))
                        .build())
            );

            if (odd) offset += 12;
            odd = !odd;
        }

        for (StardustUtil.TextFormat format : StardustUtil.TextFormat.values()) {
            if (Objects.equals(format.label, "")) continue;

            this.buttons.add(
                this.addDrawableChild(
                    ButtonWidget.builder(
                            Text.of(format.label+"A"),
                            this::onClickFormatButton
                        )
                        .dimensions(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                        .tooltip(Tooltip.of(Text.of("§7"+format.name())))
                        .build())
            );

            if (odd) offset += 12;
            odd = !odd;
        }

        this.buttons.add(
            this.addDrawableChild(
                ButtonWidget.builder(
                        Text.of("§rA"),
                        this::onClickFormatButton
                    )
                    .dimensions(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                    .tooltip(Tooltip.of(Text.of("§7Reset Formatting")))
                    .build()
            )
        );
    }

    @Inject(method = "finalizeBook", at = @At("HEAD"))
    private void mixinFinalizeBook(CallbackInfo ci) {
        if (!this.dirty) return;
        if (this.didFormatPage) {
            ((BookEditScreenAccessor) this).getCurrentPageSelectionManager().insert("§r");
        }
    }

    @Inject(method = "changePage", at = @At("HEAD"))
    private void mixinChangePage(CallbackInfo ci) {
        this.didFormatPage = false;
    }

    @Inject(method = "updateButtons", at = @At("TAIL"))
    private void mixinUpdateButtons(CallbackInfo ci) {
        BookTools bookTools = Modules.get().get(BookTools.class);
        if (bookTools.skipFormatting()) return;

        for (ButtonWidget btn : this.buttons) {
            btn.visible = !signing || bookTools.shouldFormatTitles();
        }
    }
}
