package dev.stardust.mixin;

import java.util.Arrays;
import net.minecraft.text.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import dev.stardust.util.LogUtil;
import dev.stardust.modules.AntiToS;
import dev.stardust.modules.ChatSigns;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gui.Drawable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.injection.At;
import dev.stardust.mixin.accessor.StyleAccessor;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.AbstractParentElement;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement implements Drawable {

    @Shadow
    @Final
    @Mutable
    protected Text title;

    // See AntiToS.java
    @Inject(method = "render", at = @At("HEAD"))
    private void censorScreenTitles(CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        AntiToS tos = mods.get(AntiToS.class);
        if (!tos.isActive() || !tos.containsBlacklistedText(this.title.getString())) return;
        MutableText txt = Text.literal(tos.censorText(this.title.getString()));
        this.title = txt.setStyle(this.title.getStyle());
    }

    // See ChatSigns.java
    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true)
    private void handleClickESP(@Nullable Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style == null) return;
        ClickEvent event = style.getClickEvent();
        if (event == null || event.getAction() != ClickEvent.Action.RUN_COMMAND) return;

        if (event.getValue().startsWith("clickESP~")) {
            String[] args = event.getValue().split("~");

            String mod;
            BlockPos pos;
            try {
                mod = args[1];
                String posStr = args[2];
                long packedPos = Long.parseLong(posStr);
                pos = BlockPos.fromLong(packedPos);
            } catch (Exception err) {
                LogUtil.error("Invalid custom ClickEvent syntax: "+Arrays.toString(args)+"\n"+err, "ScreenMixin");
                return;
            }
            cir.cancel();
            cir.setReturnValue(true);
            long now = System.currentTimeMillis();

            switch (mod) {
                case "chatSigns" -> {
                    Modules mods = Modules.get();
                    if (mods == null) return;
                    ChatSigns chatSigns = mods.get(ChatSigns.class);
                    if (chatSigns.toggleClickESP(pos, now)) {
                        ((StyleAccessor) style).setHoverEvent(
                            new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("§4§oDisable §7§oESP for this sign.")
                            )
                        );
                    } else {
                        ((StyleAccessor) style).setHoverEvent(
                            new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("§2§oEnable §7§oESP for this sign.")
                            )
                        );
                    }
                }
                case "reserved" -> {}
            }
        }
    }
}
