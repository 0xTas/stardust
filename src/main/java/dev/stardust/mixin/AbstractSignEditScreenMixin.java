package dev.stardust.mixin;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.modules.SignatureSign;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {

    @Shadow
    public abstract void close();


    protected AbstractSignEditScreenMixin(Text title) { super(title); }


    // See SignatureSign.java
    @Inject(method = "init", at = @At("HEAD"))
    public void stardustMixinInit(CallbackInfo ci) {
        SignatureSign signatureSign = Modules.get().get(SignatureSign.class);
        if(!signatureSign.isActive()) return;

        ArrayList<String> lines = new ArrayList<>(Arrays.asList(((AbstractSignEditScreenAccessor) this).getMessages()));

        if (!String.join(" ", lines).trim().isEmpty()) return;

        SignText signature = signatureSign.getSignature();
        List<String> msgs = Arrays.stream(signature.getMessages(false)).map(Text::getString).toList();
        String[] messages = new String[msgs.size()];
        messages = msgs.toArray(messages);

        ((AbstractSignEditScreenAccessor) this).setText(signature);
        ((AbstractSignEditScreenAccessor) this).setMessages(messages);
        if (signatureSign.needsDisabling()) {
            signatureSign.toggle();
        }

        if (signatureSign.getAutoConfirm()) {
            this.close();
        }
    }
}
