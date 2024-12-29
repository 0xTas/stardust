package dev.stardust.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Adds true durability options to BetterTooltips
 **/
@Mixin(value = BetterTooltips.class, remap = false)
public abstract class BetterTooltipsMixin extends Module {
    @Shadow
    @Final
    private SettingGroup sgOther;

    public BetterTooltipsMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Unique
    private @Nullable Setting<Boolean> trueDurability = null;

    @Unique
    private @Nullable Setting<Boolean> rawDamageTag = null;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/BetterTooltips;beehive:Lmeteordevelopment/meteorclient/settings/Setting;", shift = At.Shift.AFTER))
    private void addDurabilitySettings(CallbackInfo ci) {
        trueDurability = sgOther.add(
            new BoolSetting.Builder()
                .name("true-durability")
                .description("Show the true durability of an item.")
                .defaultValue(false)
                .build()
        );

        rawDamageTag = sgOther.add(
            new BoolSetting.Builder()
                .name("raw-damage-tag")
                .description("Show the raw Damage tag of an item.")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void appendTrueDurability(ItemStackTooltipEvent event, CallbackInfo ci) {
        if (!event.itemStack.isDamageable()) return;

        int damage = event.itemStack.getDamage();
        int maxDamage = event.itemStack.getMaxDamage();
        if (rawDamageTag.get()) {
            event.list.add(Text.literal("§7Damage§3: §a§o" + damage + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
        }
        if (trueDurability != null && trueDurability.get()) {
            int durability = maxDamage - damage;
            event.list.add(Text.literal("§7Durability§3: §a§o" + durability + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
        }
    }
}
