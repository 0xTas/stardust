package dev.stardust.mixin;

import net.minecraft.item.ItemStack;
import dev.stardust.modules.LoreLocator;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Shadow
    public abstract void fill(int x1, int y1, int x2, int y2, int color);

    // See LoreLocator.java
    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "HEAD"))
    private void highlightNamedItems(TextRenderer textRenderer, ItemStack stack, int x, int y, @Nullable String countOverride, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        LoreLocator ll = modules.get(LoreLocator.class);
        if (!ll.isActive() || !ll.shouldHighlightSlot(stack)) return;
        this.fill(x, y, x + 16, y + 16, ll.color.get().getPacked());
    }
}
