package dev.stardust.mixin.accessor;

import net.minecraft.text.Style;
import net.minecraft.text.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Style.class)
public interface StyleAccessor {
    @Mutable
    @Accessor("hoverEvent")
    void setHoverEvent(HoverEvent event);
}
