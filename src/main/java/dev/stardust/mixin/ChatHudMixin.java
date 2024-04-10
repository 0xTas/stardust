package dev.stardust.mixin;

import net.minecraft.text.Text;
import dev.stardust.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ChatHud.class)
public class ChatHudMixin {

    // See AntiToS.java
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text censorChatMessage(Text message) {
        AntiToS antiToS = Modules.get().get(AntiToS.class);
        if (!antiToS.isActive() || !antiToS.chatSetting.get()) return message;
        return (antiToS.containsBlacklistedText(message.getString()) ? Text.of(antiToS.censorText(message.getString())) : message);
    }
}
