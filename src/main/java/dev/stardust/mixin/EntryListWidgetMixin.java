package dev.stardust.mixin;

import java.util.List;
import java.util.Objects;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import dev.stardust.config.StardustConfig;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.widget.ContainerWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     See also: ServerEntryMixin.java && NeedleAngleStateMixin.java && stardust.accesswidener
 **/
@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin<E extends EntryListWidget.Entry<E>> extends ContainerWidget {
    public EntryListWidgetMixin(int i, int j, int k, int l, Text text) {
        super(i, j, k, l, text);
    }

    @Shadow
    protected abstract E getEntry(int index);

    @Shadow
    private @Nullable E hoveredEntry;

    @Shadow
    protected abstract boolean isSelectedEntry(int index);

    @Shadow
    public abstract int getRowWidth();

    @Shadow
    protected int headerHeight;

    @Shadow
    @Final
    protected int itemHeight;

    @Shadow
    protected abstract int getEntryCount();

    @Shadow
    @Final
    private List<E> children;

    @Inject(method = "renderEntry", at = @At("HEAD"), cancellable = true)
    private void extendSelectionBoxFor2b2tClock(DrawContext context, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight, CallbackInfo ci) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        E entry = this.getEntry(index);
        if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
            ServerInfo info = ((MultiplayerServerListWidget.ServerEntry) entry).getServer();
            String name = info.name;
            String address = info.address;
            if (!name.toLowerCase().contains("2b2t") && !address.equalsIgnoreCase("2b2t.org") && !address.equalsIgnoreCase("connect.2b2t.org")) return;
            if (this.isSelectedEntry(index)) {
                ci.cancel();
                entry.drawBorder(context, index, y, x, entryWidth, entryHeight , mouseX, mouseY, Objects.equals(this.hoveredEntry, entry), delta);

                int i = this.isFocused() ? -1 : -8355712;
                int l = (((EntryListWidget<?>)(Object) this).getX() + (this.width - (entryWidth + 32)) / 2) - 16;
                int j = (((EntryListWidget<?>)(Object) this).getX() + (this.width + (entryWidth + 32)) / 2) - 16;

                context.fill(l, y - 2, j, y + entryHeight + 2, i);
                context.fill(l + 1, y - 1, j - 1, y + entryHeight + 1, -16777216);

                entry.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, Objects.equals(this.hoveredEntry, entry), delta);
            }
        }
    }

    // Make the extended section of the entry which houses the clock hoverable and clickable
    @Inject(method = "getEntryAtPosition", at = @At("HEAD"), cancellable = true)
    private void extendSelectionSpaceFor2b2tClock(double x, double y, CallbackInfoReturnable<E> cir) {
        if (!StardustConfig.serverListWorldTimeClockSetting.get()) return;

        int i = this.getRowWidth() / 2;
        int j = ((EntryListWidget<?>)(Object) this).getX() + this.width / 2;
        int k = j - i;
        int l = j + i;
        int m = MathHelper.floor(y - (double) ((EntryListWidget<?>)(Object) this).getY()) - this.headerHeight + (int) this.getScrollY() - 4;
        int n = m / this.itemHeight;

        E entry = (x >= (double) k && x <= (double) l && n >= 0 && m >= 0 && n < this.getEntryCount())
            ? this.children.get(n) : null;

        if (entry != null) cir.setReturnValue(entry);
        else {
            x = x + 34;
            entry = (x >= (double) k && x <= (double) l && n >= 0 && m >= 0 && n < this.getEntryCount())
                ? this.children.get(n) : null;

            if (entry instanceof MultiplayerServerListWidget.ServerEntry) {
                ServerInfo info = ((MultiplayerServerListWidget.ServerEntry) entry).getServer();
                String name = info.name;
                String address = info.address;
                if (name.toLowerCase().contains("2b2t") || address.equalsIgnoreCase("2b2t.org") || address.equalsIgnoreCase("connect.2b2t.org")) {
                    cir.setReturnValue(entry);
                } else {
                    cir.setReturnValue(null);
                }
            } else {
                cir.setReturnValue(null);
            }
        }
    }
}
