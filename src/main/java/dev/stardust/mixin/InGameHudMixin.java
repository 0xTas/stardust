package dev.stardust.mixin;

import net.minecraft.text.Text;
import dev.stardust.modules.AntiToS;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.DrawContext;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    private ItemStack currentStack;

    // See AntiToS.java
    @Inject(
        method = "renderHeldItemTooltip",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z")
    )
    private void censorItemTooltip(DrawContext context, CallbackInfo ci, @Local LocalRef<MutableText> itemName) {
        if (this.currentStack.isEmpty()) return;

        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(itemName.get().getString())) {
            itemName.set(Text.empty().append(antiToS.censorText(itemName.get().getString())).formatted(this.currentStack.getRarity().getFormatting()));
        }
    }
}
