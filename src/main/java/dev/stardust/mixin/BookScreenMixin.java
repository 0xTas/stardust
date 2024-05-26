package dev.stardust.mixin;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.text.Text;
import dev.stardust.modules.AntiToS;
import dev.stardust.util.StardustUtil;
import dev.stardust.modules.BookTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [@0xTas] <root@0xTas.dev>
 **/
@Mixin(BookScreen.class)
public abstract class BookScreenMixin extends Screen {
    @Shadow private int pageIndex;
    @Shadow
    @Mutable private int cachedPageIndex;
    @Shadow
    private BookScreen.Contents contents;

    // See BookTools.java && AntiToS.java
    protected BookScreenMixin(Text title) { super(title); }


    @Unique
    private boolean deobfuscated = false;
    @Unique
    private ButtonWidget deobfuscateButton;
    @Unique
    private List<String> obfuscatedPages = new ArrayList<>();

    @Unique
    private void deobfuscateBook(ButtonWidget btn) {
        if (this.deobfuscated) {
            reobfuscateBook(btn);
            return;
        }

        if (contents instanceof BookScreen.WrittenBookContents) {
            List<String> pages = ((WrittenBookContentsAccessor) contents).getPages();
            List<String> deobfuscatedPages = new java.util.ArrayList<>(List.of());
            for (String page : pages) {
                deobfuscatedPages.add(page.replace("§k", ""));
            }

            ((WrittenBookContentsAccessor) contents).setPages(deobfuscatedPages);

            btn.setAlpha(0.5f);
            btn.setTooltip(Tooltip.of(Text.of("§8Restore this tome's secrets..")));
            this.cachedPageIndex = -1;
            btn.setMessage(
                Text.of("§0<"+StardustUtil.rCC()+"§o✨§r§0> "+StardustUtil.rCC()+"§o§kReobfuscate "+"§0<"
                    +StardustUtil.rCC()+"§o✨§r§0> ")
            );
            this.deobfuscated = true;
        }
    }

    @Unique
    private void reobfuscateBook(ButtonWidget btn) {
        if (contents instanceof BookScreen.WrittenBookContents) {
            btn.setAlpha(1f);
            btn.setTooltip(Tooltip.of(Text.of("§8Reveal this tome's secrets..")));
            btn.setMessage(Text.of("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "));

            ((WrittenBookContentsAccessor) contents).setPages(this.obfuscatedPages);
            if (!this.obfuscatedPages.get(this.cachedPageIndex).contains("§k")) {
                btn.visible = false;
            }

            this.cachedPageIndex = -1;
            this.deobfuscated = false;
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void mixinInit(CallbackInfo ci) {
        if (!(this.contents instanceof BookScreen.WrittenBookContents)) return;

        Modules modules = Modules.get();
        if (modules == null) return;

        List<String> pages = ((WrittenBookContentsAccessor) this.contents).getPages();
        AntiToS antiToS = modules.get(AntiToS.class);
        BookTools bookTools = modules.get(BookTools.class);
        if (antiToS.isActive()) {
            List<String> filtered = new ArrayList<>();
            for (String page : pages) {
                if (antiToS.containsBlacklistedText(page)) {
                    filtered.add(antiToS.censorText(page));
                } else filtered.add(page);
            }
            ((WrittenBookContentsAccessor) this.contents).setPages(filtered);
            this.cachedPageIndex = -1;
        } else if (bookTools.skipDeobfuscation()) return;

        this.deobfuscateButton = this.addDrawableChild(
            ButtonWidget.builder(
                    Text.of("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "),
                    this::deobfuscateBook)
                .dimensions(this.width / 2 - 59, 217, 120, 20)
                .tooltip(Tooltip.of(Text.of("§8Reveal this tome's secrets..")))
                .build());

        this.deobfuscateButton.visible = pages.get(this.pageIndex).contains("§k");
        if (pages.stream().anyMatch(page -> page.contains("§k"))) {
            this.obfuscatedPages = ((WrittenBookContentsAccessor) this.contents).getPages();
        }
    }

    @Inject(method = "updatePageButtons", at = @At("TAIL"))
    private void mixinUpdatePageButtons(CallbackInfo ci) {
        if (this.deobfuscated) return;
        if (!(this.contents instanceof BookScreen.WrittenBookContents)) return;

        BookTools bookTools = Modules.get().get(BookTools.class);
        if (bookTools.skipDeobfuscation()) return;

        List<String> pages = ((WrittenBookContentsAccessor) contents).getPages();
        this.deobfuscateButton.visible = pages.get(this.pageIndex).contains("§k");
    }
}
