package dev.stardust.mixin.meteor.accessor;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import meteordevelopment.meteorclient.systems.modules.Category;

@Mixin(value = Category.class, remap = false)
public interface CategoryAccessor {
    @Mutable
    @Accessor("icon")
    void setIcon(ItemStack icon);
}
