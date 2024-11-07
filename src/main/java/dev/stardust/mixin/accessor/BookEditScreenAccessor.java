package dev.stardust.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;

@Mixin(BookEditScreen.class)
public interface BookEditScreenAccessor {
    @Accessor
    SelectionManager getCurrentPageSelectionManager();

    @Accessor
    SelectionManager getBookTitleSelectionManager();
}
