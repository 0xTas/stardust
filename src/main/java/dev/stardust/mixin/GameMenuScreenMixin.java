package dev.stardust.mixin;

import dev.stardust.Stardust;
import net.minecraft.text.Text;
import dev.stardust.util.StardustUtil;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.gui.widget.GridWidget;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.GameMenuScreen;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/GridWidget;refreshPositions()V"))
    private void addIllegalDisconnectButton(CallbackInfo ci, @Local GridWidget.Adder adder) {
        if (Stardust.illegalDisconnectButtonSetting.get() && !mc.isInSingleplayer()) {
            adder.add(ButtonWidget.builder(Text.literal("§cIllegal Disconnect"), button -> {
                button.active = false;
                StardustUtil.illegalDisconnect(false, Stardust.illegalDisconnectMethodSetting.get());
            }).width(204).build(), 2);
        }
    }
}
