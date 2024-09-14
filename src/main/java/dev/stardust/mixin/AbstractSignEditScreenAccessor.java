package dev.stardust.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.block.entity.SignText;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;

@Mixin(AbstractSignEditScreen.class)
public interface AbstractSignEditScreenAccessor {
    @Accessor
    String[] getMessages();

    @Mutable
    @Accessor("messages")
    void setMessages(String[] messages);

    @Mutable
    @Accessor("text")
    void setText(SignText text);
}
