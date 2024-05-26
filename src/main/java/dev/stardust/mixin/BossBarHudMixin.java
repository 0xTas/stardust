package dev.stardust.mixin;

import net.minecraft.text.Text;
import dev.stardust.modules.AntiToS;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    // See AntiToS.java
    @Inject(method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V", at = @At("HEAD"))
    private void censorBossBar(DrawContext context, int x, int y, BossBar bossBar, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(bossBar.getName().getString())) {
            bossBar.setName(Text.literal(antiToS.censorText(bossBar.getName().getString()).formatted(bossBar.getName().getStyle())));
        }
    }
}
