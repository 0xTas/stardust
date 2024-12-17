package dev.stardust.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
// TODO: Paul wants this to stop the signs from showing up on another client's search somehow...
@Mixin(value = NoRender.class, remap = false)
public abstract class NoRenderMixin extends Module {
    public NoRenderMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Unique
    private final SettingGroup sgCody = settings.createGroup("codysmile11");
    @Unique
    private @Nullable Setting<Boolean> codySigns = null;
    @Unique
    private @Nullable Setting<Boolean> codyPlayer = null;
    @Unique
    private @Nullable Setting<Boolean> codyBanners = null;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/NoRender;noSignText:Lmeteordevelopment/meteorclient/settings/Setting;"))
    private void addNoRenderSettings(CallbackInfo ci) {
        // See EntityRendererMixin.java
        codyPlayer = sgCody.add(
            new BoolSetting.Builder()
                .name("cody")
                .description("Ignore his very existence.")
                .defaultValue(false)
                .build()
        );
        codySigns = sgCody.add(
            new BoolSetting.Builder()
                .name("cody-signs")
                .description("Don't render signs which contain the text \"codysmile11\".")
                .defaultValue(false)
                .build()
        );
        codyBanners = sgCody.add(
            new BoolSetting.Builder()
                .name("cody-banners")
                .description("Don't render banners which contain \"codysmile11\" in the name.")
                .defaultValue(false)
                .build()
        );
    }
}
