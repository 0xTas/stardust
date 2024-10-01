package dev.stardust.mixin;

import dev.stardust.Stardust;
import net.minecraft.text.Text;
import javax.annotation.Nullable;
import dev.stardust.util.StardustUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import meteordevelopment.orbit.EventHandler;
import org.spongepowered.asm.mixin.injection.At;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.MeteorClient;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *
 *     Adds "Illegal Disconnect" functionality to Meteor's built-in AutoLog module.
 **/
@Mixin(value = AutoLog.class, remap = false)
public abstract class AutoLogMixin extends Module {
    public AutoLogMixin(Category category, String name, String description) {
        super(category, name, description);
        MeteorClient.EVENT_BUS.subscribe(this); // TODO: sus
    }

    @Shadow
    @Final
    private SettingGroup sgGeneral;
    @Shadow
    @Final
    private Setting<Boolean> toggleOff;

    @Unique
    private boolean didLog = false;
    @Unique
    @Nullable
    private Text disconnectReason = null;
    @Unique
    @Nullable
    private Setting<Boolean> forceKick = null;

    @Override
    public void onDeactivate() {
        if (toggleOff.get() && didLog) {
            MeteorClient.EVENT_BUS.subscribe(this);
        }
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/misc/AutoLog;entities:Lmeteordevelopment/meteorclient/settings/Setting;"))
    private void addIllegalDisconnectSetting(CallbackInfo ci) {
        forceKick = sgGeneral.add(
            new BoolSetting.Builder()
                .name("illegal-disconnect")
                .description("Tip: Change the illegal disconnect method in your Meteor config settings (Stardust category.)")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onTick",at = @At("HEAD"), cancellable = true)
    private void preventNullPointerExceptions(CallbackInfo ci) {
        if (!Utils.canUpdate()) ci.cancel();
    }

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void maybeForceKick(String reason, CallbackInfo ci) {
        if (forceKick != null && forceKick.get()) {
            ci.cancel();
            didLog = true;
            if (toggleOff.get()) toggle();
            disconnectReason = Text.literal("§8[§aAutoLog§8] §f" + reason);
            StardustUtil.illegalDisconnect(true, Stardust.illegalDisconnectMethodSetting.get());
        }
    }

    @Unique
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (disconnectReason == null || !(event.packet instanceof DisconnectS2CPacket packet))  return;
        if (didLog) {
            ((DisconnectS2CPacketAccessor)(Object) packet).setReason(disconnectReason);
            MeteorClient.EVENT_BUS.unsubscribe(this);
            disconnectReason = null;
            didLog = false;
        }
    }
}
