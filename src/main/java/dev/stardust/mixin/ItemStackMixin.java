package dev.stardust.mixin;

import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import dev.stardust.modules.AntiToS;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract Rarity getRarity();

    @Shadow
    public abstract Text getName();

    // See AntiToS.java
    @Inject(method = "getFormattedName", at = @At("HEAD"), cancellable = true)
    private void censorItemTooltip(CallbackInfoReturnable<Text> cir) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(this.getName().getString())) {
            cir.setReturnValue(Text.empty().append(antiToS.censorText(this.getName().getString()).formatted(this.getRarity().getFormatting())));
        }
    }

    @Inject(method = "toHoverableText", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z"))
    private void censorHoveredText(CallbackInfoReturnable<Text> cir, @Local(ordinal = 0)LocalRef<MutableText> name) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(name.get().getString())) {
            name.set(Text.empty().append(antiToS.censorText(name.get().getString())));
        }
    }
}
