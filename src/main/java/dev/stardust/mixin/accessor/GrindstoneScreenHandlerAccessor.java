package dev.stardust.mixin.accessor;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.screen.GrindstoneScreenHandler;

@Mixin(GrindstoneScreenHandler.class)
public interface GrindstoneScreenHandlerAccessor {
    @Invoker("grind")
    ItemStack invokeGrind(ItemStack item);

    @Invoker("transferEnchantments")
    void invokeTransferEnchantments(ItemStack target, ItemStack source);
}
