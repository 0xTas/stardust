package dev.stardust.mixin.meteor;

import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.LogUtil;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Allows you to preview items from the peek screen by giving you a client-side ghost item when you left-click.
 **/
@Mixin(value = PeekScreen.class, remap = false)
public abstract class PeekScreenMixin extends ShulkerBoxScreen {
    public PeekScreenMixin(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Unique
    private @Nullable BetterTooltips btt = null;

    // See BetterTooltipsMixin.java
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void hijackMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (mc.player == null) return;
        if (btt == null) {
            Modules mods = Modules.get();
            if (mods == null) return;
            btt = mods.get(BetterTooltips.class);

            if (btt == null) return;
        }
        if (!btt.isActive()) return;
        var setting = btt.settings.get("peek-ghost-items");
        if (setting == null) return;
        try {
            if ((boolean) setting.get() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && focusedSlot != null && !focusedSlot.getStack().isEmpty()) {
                FindItemResult empty;
                if (InvUtils.testInMainHand(ItemStack::isEmpty)) {
                    empty = new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());
                } else {
                    empty = InvUtils.find(ItemStack::isEmpty, 0, 8);
                }

                if (empty.found()) {
                    ItemStack stack = focusedSlot.getStack();
                    mc.player.getInventory().setStack(empty.slot(), stack);
                    cir.setReturnValue(true);
                } else {
                    MsgUtil.sendModuleMsg("Peeking at ghost items requires an empty hotbar slot§c..!", "better-tooltips");
                    cir.setReturnValue(false);
                }
            }
        } catch (Exception err) {
            LogUtil.error(err.toString(), "PeekScreenMixin");
        }
    }
}
