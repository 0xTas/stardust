package dev.stardust.mixin;

import net.minecraft.text.Text;
import dev.stardust.modules.AntiToS;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ChatHud.class)
public class ChatHudMixin {

    // See AntiToS.java
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text censorChatMessage(Text message) {
        Modules modules = Modules.get();
        if (modules == null) return message;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return message;
        MutableText mText = Text.literal(antiToS.censorText(message.getString()));
        return (antiToS.containsBlacklistedText(message.getString()) ? mText.setStyle(message.getStyle()) : message);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void maybeCancelAddMessage(Text message, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;
        if (antiToS.chatMode.get() == AntiToS.ChatMode.Remove && antiToS.containsBlacklistedText(message.getString())) ci.cancel();
    }
}
