package dev.stardust.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(BannerBlockEntityRenderer.class)
public class BannerBlockEntityRendererMixin {

    // See NoRenderMixin.java
    @Inject(method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V", at = @At("HEAD"), cancellable = true)
    private void onRender(BannerBlockEntity bannerBlockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (bannerBlockEntity.getWorld() != null) {
            Modules mods = Modules.get();
            if (mods == null) return;
            NoRender noRender = mods.get(NoRender.class);
            if (!noRender.isActive()) return;

            Text bannerName = bannerBlockEntity.getCustomName();
            var bannerSetting = noRender.settings.get("cody-banners");

            if (bannerSetting == null || bannerName == null) return;
            if ((boolean) bannerSetting.get() && bannerName.getString().contains("codysmile11")) {
                ci.cancel();
            }
        }
    }
}
