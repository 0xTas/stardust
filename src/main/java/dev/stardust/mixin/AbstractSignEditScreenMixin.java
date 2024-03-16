package dev.stardust.mixin;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import dev.stardust.modules.SignatureSign;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.util.SelectionManager;
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
    private int currentRow;
    @Shadow
    @Final
    private String[] messages;
    @Shadow
    public abstract void close();
    @Shadow
    @Final
    private SignBlockEntity blockEntity;
    @Shadow
    private @Nullable SelectionManager selectionManager;
    @Shadow
    protected abstract void setCurrentRowMessage(String message);

    protected AbstractSignEditScreenMixin(Text title) { super(title); }


    // See SignatureSign.java
    @Inject(method = "init", at = @At("HEAD"))
    public void stardustMixinInit(CallbackInfo ci) {
        SignatureSign signatureSign = Modules.get().get(SignatureSign.class);
        if(!signatureSign.isActive()) return;

        ArrayList<String> lines = new ArrayList<>(Arrays.asList(((AbstractSignEditScreenAccessor) this).getMessages()));

        if (!String.join(" ", lines).trim().isEmpty()) return;

        SignText signature = signatureSign.getSignature(this.blockEntity);
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

    @Inject(method = "init", at = @At("TAIL"))
    private void disableWidthChecks(CallbackInfo ci) {
        if (this.client == null) return;
        SignatureSign signatureSign = Modules.get().get(SignatureSign.class);

        if ((signatureSign.isActive() && signatureSign.signFreedom.get())) {
            // bypass client-side length limits for sign text by using a truthy predicate in the SelectionManager
            this.selectionManager = new SelectionManager(
                () -> this.messages[this.currentRow], this::setCurrentRowMessage,
                SelectionManager.makeClipboardGetter(this.client), SelectionManager.makeClipboardSetter(this.client),
                string -> true
            );
        }
    }
}
