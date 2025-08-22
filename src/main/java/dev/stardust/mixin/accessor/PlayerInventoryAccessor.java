package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.entity.player.PlayerInventory;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selectedSlot")
    int getSelectedSlot();

    @Accessor("selectedSlot")
    void setSelectedSlot(int slot);
}
