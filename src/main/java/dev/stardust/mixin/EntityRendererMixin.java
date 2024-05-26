package dev.stardust.mixin;

import net.minecraft.text.Text;
import dev.stardust.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // See AntiToS.java
    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
    private Text censorEntityName(Text name) {
        Modules modules = Modules.get();
        if (modules == null) return name;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return name;

        if (!antiToS.containsBlacklistedText(name.getString())) return name;
        return Text.literal(antiToS.censorText(name.getString())).setStyle(name.getStyle());
    }
}
