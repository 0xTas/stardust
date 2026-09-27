package dev.stardust.mixin;

import net.minecraft.util.Util;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: EntryListWidgetMixin.java && NeedleAngleStateMixin.java && stardust.accesswidener
 **/
@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class ServerEntryMixin extends MultiplayerServerListWidget.Entry {
    @Shadow
    @Final
    private ServerInfo server;

    @Shadow
    protected abstract boolean canConnect();

    @Shadow
    @Final
    private MultiplayerScreen screen;

    @Shadow
    protected abstract void swapEntries(int i, int j);

    @Shadow
    private long time;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    MultiplayerServerListWidget field_19117;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"), cancellable = true)
    private void render2b2tClock(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        String name = this.server.name;
        String address = this.server.address;
        if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
            ci.cancel();

            // Prevent the reorder buttons from highlighting when hovering over the extended (clock) part of the widget by checking that o > 0
            if (this.client.options.getTouchscreen().getValue() || hovered) {
                context.fill(x, y, x + 32, y + 32, -1601138544);

                int o = mouseX - x;
                int p = mouseY - y;
                if (this.canConnect()) {
                    if (o < 32 && o > 16) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, MultiplayerServerListWidget.JOIN_HIGHLIGHTED_TEXTURE, x, y, 32, 32);
                    } else {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, MultiplayerServerListWidget.JOIN_TEXTURE, x, y, 32, 32);
                    }
                }

                if (index > 0) {
                    if (o < 16 && o > 0 && p < 16) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, MultiplayerServerListWidget.MOVE_UP_HIGHLIGHTED_TEXTURE, x, y, 32, 32);
                    } else {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, MultiplayerServerListWidget.MOVE_UP_TEXTURE, x, y, 32, 32);
                    }
                }

                if (index < this.screen.getServerList().size() - 1) {
                    if (o < 16 && o > 0 && p > 16) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, MultiplayerServerListWidget.MOVE_DOWN_HIGHLIGHTED_TEXTURE, x, y, 32, 32);
                    } else {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, MultiplayerServerListWidget.MOVE_DOWN_TEXTURE, x, y, 32, 32);
                    }
                }
            }

            // See NeedleAngleStateMixin.java for custom clock time source logic
            RenderUtils.drawItem(
                context, Items.CLOCK.getDefaultStack(),
                x - 34, y, 2.0f, false
            );
        }
    }

    // Prevent the reorder buttons from activating when clicking on the extended (clock) part of the widget by checking that d > 0.0
    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void hijackMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        String name = this.server.name;
        String address = this.server.address;
        if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
            cir.cancel();
            double d = mouseX - (double) this.field_19117.getRowLeft();
            double e = mouseY - (double) this.field_19117.getRowTop(this.field_19117.children().indexOf(this));

            if (d <= 32.0) {
                if (d < 32.0 && d > 16.0 && this.canConnect()) {
                    this.screen.select(this);
                    this.screen.connect();
                    cir.setReturnValue(true);
                }

                int i = this.screen.serverListWidget.children().indexOf(this);
                if (d < 16.0 && d > 0.0 && e < 16.0 && i > 0) {
                    this.swapEntries(i, i - 1);
                    cir.setReturnValue(true);
                }

                if (d < 16.0 && d > 0.0 && e > 16.0 && i < this.screen.getServerList().size() - 1) {
                    this.swapEntries(i, i + 1);
                    cir.setReturnValue(true);
                }
            }

            this.screen.select(this);
            if (Util.getMeasuringTimeMs() - this.time < 250L) {
                this.screen.connect();
            }

            this.time = Util.getMeasuringTimeMs();
            cir.setReturnValue(true);
        }
    }
}
