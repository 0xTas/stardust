package dev.stardust.mixin;

import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(AnvilScreenHandler.class)
public interface AnvilScreenHandlerAccessor {
    @Accessor
    Property getLevelCost();
}
