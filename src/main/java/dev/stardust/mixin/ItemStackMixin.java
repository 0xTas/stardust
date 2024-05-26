package dev.stardust.mixin;

import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import dev.stardust.modules.AntiToS;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.jetbrains.annotations.Nullable;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
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

    // See AntiToS.java
    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;hasCustomName()Z", shift = At.Shift.BEFORE))
    private void censorItemTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir, @Local(ordinal = 0)LocalRef<MutableText> name) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(name.get().getString())) {
            name.set(Text.empty().append(antiToS.censorText(name.get().getString()).formatted(this.getRarity().formatting)));
        }
    }

    @Inject(method = "toHoverableText", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;hasCustomName()Z", shift = At.Shift.BEFORE))
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
