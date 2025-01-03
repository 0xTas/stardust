package dev.stardust.mixin;

import java.util.Optional;
import net.minecraft.text.Text;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtCompound;
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

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    @Unique
    private @Nullable Setting<Boolean> showRevertedIllegals = null;

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

        showRevertedIllegals = sgOther.add(
            new BoolSetting.Builder()
                .name("show-reverted-illegals")
                .description("Show illegal items which have had their count set to 0.")
                .build()
        );
    }

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void appendTrueDurability(ItemStackTooltipEvent event, CallbackInfo ci) {
        if (!event.itemStack.isDamageable()) return;

        int damage = event.itemStack.getDamage();
        int maxDamage = event.itemStack.getMaxDamage();
        NbtCompound metadata = event.itemStack.getNbt();

        if (metadata == null) return;
        Optional<Integer> trueDamage = Optional.empty();
        if (metadata.contains("tag", NbtElement.COMPOUND_TYPE)) {
            NbtCompound tag = metadata.getCompound("tag");

            int index = tag.toString().indexOf("Damage:");
            if (index != -1) {
                try {
                    String subStr = tag.toString().substring(index + 7);
                    trueDamage = Optional.of(Integer.parseInt(subStr.substring(0, subStr.indexOf(',') == -1 ? subStr.indexOf('}') : subStr.indexOf(','))));
                } catch (Exception ignored) {}
            }
        }
        if (trueDamage.isPresent()) {
            int dmg = trueDamage.get();
            if (rawDamageTag != null && rawDamageTag.get()) {
                event.list.add(Text.literal("§7Damage§3: §a§o" + dmg + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
            }
            if (trueDurability != null && trueDurability.get()) {
                int trueDurability = maxDamage - dmg;
                event.list.add(Text.literal("§7Durability§3: §a§o" + trueDurability + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
            }
        } else {
            if (rawDamageTag != null && rawDamageTag.get()) {
                event.list.add(Text.literal("§7Damage§3: §a§o" + damage + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
            }
            if (trueDurability != null && trueDurability.get()) {
                int durability = maxDamage - damage;
                event.list.add(Text.literal("§7Durability§3: §a§o" + durability + " §8[§7Max§3: §a§o" + maxDamage + "§8]"));
            }
        }
    }
}
