package dev.stardust.mixin;

import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.item.ItemConvertible;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemStack.class)
public interface ItemStackAccessor {
    @Invoker("<init>")
    static ItemStack invokeInit(ItemConvertible item, int count, Optional<NbtCompound> nbt) {
        throw new AssertionError();
    }
}
