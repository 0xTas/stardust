package dev.stardust.mixin.meteor;

import net.minecraft.text.Text;
import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.component.DataComponentTypes;
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
 **/
@Mixin(value = BetterTooltips.class, remap = false)
public class BetterTooltipsMixin extends Module {
    @Shadow
    @Final
    private SettingGroup sgOther;

    public BetterTooltipsMixin(Category category, String name, String description, String... aliases) {
        super(category, name, description, aliases);
    }

    @Unique
    private @Nullable Setting<Boolean> rawDamageTag = null;
    @Unique
    private @Nullable Setting<Boolean> trueDurability = null;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/BetterTooltips;beehive:Lmeteordevelopment/meteorclient/settings/Setting;"))
    private void addTrueDurabilitySetting(CallbackInfo ci) {
        trueDurability = sgOther.add(new BoolSetting.Builder()
            .name("true-durability")
            .description("Show the raw damage value of an item.")
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
    private void appendDurabilityTooltip(ItemStackTooltipEvent event, CallbackInfo ci) {
        if (!event.itemStack().isDamageable()) return;

        int maxDamage = event.itemStack().getMaxDamage();
        int damage = event.itemStack().getOrDefault(DataComponentTypes.DAMAGE, event.itemStack().getDamage());

        if (rawDamageTag != null && rawDamageTag.get()) {
            event.appendEnd(Text.literal("§7Damage§3: §a§o" + damage + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
        }
        if (trueDurability != null && trueDurability.get()) {
            int durability = maxDamage - damage;
            event.appendEnd(Text.literal("§7Durability§3: §a§o" + durability + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
        }
    }
}
