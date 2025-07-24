package dev.stardust.mixin;

import net.minecraft.text.Text;
import dev.stardust.util.TextUtil;
import net.minecraft.entity.Entity;
import dev.stardust.modules.AntiToS;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // See AntiToS.java
    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
    private Text censorEntityName(Text name) {
        Modules modules = Modules.get();
        if (modules == null) return name;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return name;

        if (!antiToS.containsBlacklistedText(name.getString())) return name;
        return TextUtil.modifyWithStyle(name.copy(), antiToS::censorText);
    }

    // See NoRenderMixin.java
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PlayerEntity player)) return;

        Modules mods = Modules.get();
        if (mods == null) return;
        NoRender noRender = mods.get(NoRender.class);
        if (!noRender.isActive()) return;

        var codySetting = noRender.settings.get("cody");
        if (codySetting != null && (boolean) codySetting.get() && player.getGameProfile().getName().equals("codysmile11")) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
