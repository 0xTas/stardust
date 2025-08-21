package dev.stardust.mixin.meteor.accessor;

import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IVisible;

@Mixin(value = Setting.class, remap = false)
public interface SettingAccessor {
    @Mutable
    @Accessor("visible")
    void setVisible(IVisible visible);

    @Mutable
    @Accessor("onChanged")
    <T>
    void setOnChanged(Consumer<T> onChanged);

    @Mutable
    @Accessor("description")
    void setDescription(String description);
}
