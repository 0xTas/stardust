package dev.stardust.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.ingame.BookScreen;


@Mixin(BookScreen.WrittenBookContents.class)
public interface WrittenBookContentsAccessor {
    @Accessor
    List<String> getPages();

    @Mutable
    @Accessor("pages")
    void setPages(List<String> pages);
}
