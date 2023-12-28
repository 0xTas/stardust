package dev.stardust.mixin;

import javax.annotation.Nullable;
import net.minecraft.client.sound.Source;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.sound.Channel;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(Channel.SourceManager.class)
public interface SourceManagerAccessor {
    @Accessor("source")
    @Nullable
    Source getSource();
}
