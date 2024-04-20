package dev.stardust.mixin;

import java.util.List;
import java.util.Arrays;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import dev.stardust.modules.SignHistorian;
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
    public abstract void close();
    @Shadow
    @Final
    private SignBlockEntity blockEntity;
    @Shadow
    private @Nullable SelectionManager selectionManager;
    @Shadow
    protected abstract void setCurrentRowMessage(String message);

    protected AbstractSignEditScreenMixin(Text title) { super(title); }


    // See SignatureSign.java && SignHistorian.java
    @Inject(method = "init", at = @At("TAIL"))
    public void stardustMixinInit(CallbackInfo ci) {
        if (this.client == null) return;
        Modules modules = Modules.get();

        if (modules == null) return;
        SignHistorian signHistorian = modules.get(SignHistorian.class);
        SignatureSign signatureSign = modules.get(SignatureSign.class);
        if (!signatureSign.isActive() && !signHistorian.isActive()) return;

        SignText restoration = signHistorian.getRestoration(this.blockEntity);
        if (signHistorian.isActive() && restoration != null) {
            List<String> msgs = Arrays.stream(restoration.getMessages(false)).map(Text::getString).toList();
            String[] messages = new String[msgs.size()];
            messages = msgs.toArray(messages);

            ((AbstractSignEditScreenAccessor) this).setText(restoration);
            ((AbstractSignEditScreenAccessor) this).setMessages(messages);

            this.close();
        } else if (signatureSign.isActive()) {
            SignText signature = signatureSign.getSignature(this.blockEntity);
            List<String> msgs = Arrays.stream(signature.getMessages(false)).map(Text::getString).toList();
            String[] messages = new String[msgs.size()];
            messages = msgs.toArray(messages);

            ((AbstractSignEditScreenAccessor) this).setText(signature);
            ((AbstractSignEditScreenAccessor) this).setMessages(messages);
            if (signatureSign.needsDisabling()) {
                signatureSign.disable();
            }

            if (signatureSign.getAutoConfirm()) {
                this.close();
            }

            if ((signatureSign.isActive() && signatureSign.signFreedom.get())) {
                // bypass client-side length limits for sign text by using a truthy predicate in the SelectionManager
                AbstractSignEditScreenAccessor accessor = ((AbstractSignEditScreenAccessor) this);
                this.selectionManager = new SelectionManager(
                    () -> accessor.getMessages()[this.currentRow], this::setCurrentRowMessage,
                    SelectionManager.makeClipboardGetter(this.client), SelectionManager.makeClipboardSetter(this.client),
                    string -> true
                );
            }
        }
    }
}
