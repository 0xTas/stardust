package dev.stardust.mixin;

import java.util.Arrays;
import java.util.stream.Collectors;

import dev.stardust.modules.AntiToS;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;

/**
 * Keep AntiToS sanitation on the abstract helper that both sign renderers call.
 */
@Mixin(AbstractSignBlockEntityRenderer.class)
public abstract class AbstractSignBlockEntityRendererMixin {
    // AntiToS: sanitize what's about to be drawn
    @ModifyVariable(method = "renderText", at = @At("HEAD"), argsOnly = true)
    private SignText stardust$modifyRenderedText(SignText signText) {
        Modules modules = Modules.get();
        if (modules == null) return signText;

        AntiToS antiToS = modules.get(AntiToS.class);
        if (antiToS == null || !antiToS.isActive()) return signText;

        String testText = Arrays.stream(signText.getMessages(false))
            .map(Text::getString)
            .collect(Collectors.joining(" "))
            .trim();

        return antiToS.containsBlacklistedText(testText)
            ? antiToS.familyFriendlySignText(signText)
            : signText;
    }
}
