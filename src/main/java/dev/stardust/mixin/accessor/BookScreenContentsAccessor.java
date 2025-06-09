package dev.stardust.mixin.accessor;

import java.util.List;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.ingame.BookScreen;

@Mixin(BookScreen.Contents.class)
public interface BookScreenContentsAccessor {
    @Accessor
    List<Text> getPages();

    @Mutable
    @Accessor("pages")
    void setPages(List<Text> pages);
}
