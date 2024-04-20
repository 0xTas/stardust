package dev.stardust.mixin;

import java.util.ArrayList;
import net.minecraft.text.Text;
import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.*;
import dev.stardust.util.StardustUtil;
import dev.stardust.modules.BookTools;
import net.minecraft.client.gui.screen.Screen;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.gui.tooltip.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow
    private boolean dirty;
    @Shadow
    private boolean signing;

    // See BookTools.java
    protected BookEditScreenMixin(Text title) { super(title); }

    @Unique
    private boolean rainbowMode = false;
    @Unique
    private boolean didFormatPage = false;
    @Unique
    private String activeFormatting = "";
    @Unique
    private @Nullable StardustUtil.RainbowColor lastCC = null;
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

        if (rainbowMode) {
            activeFormatting = format;
        }else if (this.signing) {
            ((BookEditScreenAccessor) this).getBookTitleSelectionManager().insert(format);
        } else {
            this.didFormatPage = true;
            ((BookEditScreenAccessor) this).getCurrentPageSelectionManager().insert(format);
        }
    }

    @Unique
    private void onClickRainbowButton(ButtonWidget btn) {
        rainbowMode = !rainbowMode;
        if (rainbowMode) {
            btn.setMessage(Text.of(uCC()+"🌈"));
            btn.setTooltip(Tooltip.of(Text.of(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §2On")));
        } else {
            btn.setMessage(Text.of("🌈"));
            btn.setTooltip(Tooltip.of(Text.of(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §4Off")));
        }
    }

    @Unique
    private String uCC() {
        // Return a random color code that follows the pattern of the rainbow.
        if (lastCC == null) {
            lastCC = StardustUtil.RainbowColor.getFirst();
        } else {
            lastCC = StardustUtil.RainbowColor.getNext(lastCC);
        }
        return lastCC.labels[ThreadLocalRandom.current().nextInt(lastCC.labels.length)];
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mixinInit(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        BookTools bookTools = modules.get(BookTools.class);
        if (bookTools.skipFormatting()) return;

        int offset = 0;
        boolean odd = false;
        for (StardustUtil.TextColor color : StardustUtil.TextColor.values()) {
            if (color.label.isEmpty()) continue;

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
            if (format.label.isEmpty()) continue;

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

        if (odd) offset += 12;
        odd = !odd;
        this.buttons.add(
            this.addDrawableChild(
                ButtonWidget.builder(
                    Text.of("🌈"),
                    this::onClickRainbowButton
                )
                .dimensions(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 22, 10)
                .tooltip(Tooltip.of(Text.of(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §4Off")))
                .build()
            )
        );
    }

    @Inject(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SelectionManager;insert(Ljava/lang/String;)V", shift = At.Shift.BEFORE))
    private void mixinCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!rainbowMode || signing) return;
        didFormatPage = true;
        if (activeFormatting.equals("§r")) {
            activeFormatting = "";
            ((BookEditScreenAccessor) this).getCurrentPageSelectionManager().insert("§r" + uCC());
        } else {
            ((BookEditScreenAccessor) this).getCurrentPageSelectionManager().insert(uCC() + activeFormatting);
        }
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
        Modules modules = Modules.get();
        if (modules == null) return;
        BookTools bookTools = modules.get(BookTools.class);
        if (bookTools.skipFormatting()) return;

        for (ButtonWidget btn : this.buttons) {
            btn.visible = !signing || bookTools.shouldFormatTitles();
        }
    }
}
