package dev.stardust.mixin;

import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import dev.stardust.modules.StashBrander;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ForgingScreen<AnvilScreenHandler> {
    @Shadow
    private TextFieldWidget nameField;

    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory playerInventory, Text title, Identifier texture) {
        super(handler, playerInventory, title, texture);
    }

    /**
     * See StashBrander.java
     * Helps to minimize packet spam by drastically reducing the amount of RenameItemC2SPackets that are sent.
     * */
    @Inject(method = "onSlotUpdate", at = @At("HEAD"), cancellable = true)
    private void maybeCancelNameFieldUpdate(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        StashBrander sb = mods.get(StashBrander.class);

        if (slotId == 0 && sb.isActive()) {
            ci.cancel();
            this.nameField.setEditable(true);
            this.setFocused(this.nameField);
        }
    }
}
