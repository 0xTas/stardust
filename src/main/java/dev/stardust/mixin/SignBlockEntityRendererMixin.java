package dev.stardust.mixin;

import java.util.Arrays;
import net.minecraft.text.Text;
import java.util.stream.Collectors;
import dev.stardust.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.block.entity.SignText;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin implements BlockEntityRenderer<SignBlockEntity> {

    // See AntiToS.java
    @ModifyVariable(method = "renderText", at = @At("HEAD"), argsOnly = true)
    private SignText modifyRenderedText(SignText signText) {
        AntiToS antiToS = Modules.get().get(AntiToS.class);
        if (!antiToS.isActive() || !antiToS.signsSetting.get()) return signText;

        String testText = Arrays.stream(signText.getMessages(false))
            .map(Text::getString)
            .collect(Collectors.joining(" "))
            .trim();
        return antiToS.containsBlacklistedText(testText) ? antiToS.familyFriendlySignText(signText) : signText;
    }
}
