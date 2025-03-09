package dev.stardust.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Rarity;
import net.minecraft.nbt.NbtElement;
import dev.stardust.modules.AntiToS;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.text.MutableText;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.jetbrains.annotations.Nullable;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract Rarity getRarity();

    // See AntiToS.java
    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;hasCustomName()Z"))
    private void censorItemTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir, @Local(ordinal = 0)LocalRef<MutableText> name) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(name.get().getString())) {
            name.set(Text.empty().append(antiToS.censorText(name.get().getString()).formatted(this.getRarity().formatting)));
        }
    }

    @Inject(method = "toHoverableText", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;hasCustomName()Z"))
    private void censorHoveredText(CallbackInfoReturnable<Text> cir, @Local(ordinal = 0)LocalRef<MutableText> name) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(name.get().getString())) {
            name.set(Text.empty().append(antiToS.censorText(name.get().getString())));
        }
    }

    // See BetterTooltipsMixin.java
    @Inject(method = "fromNbt", at = @At("HEAD"), cancellable = true)
    private static void showRevertedIllegals(NbtCompound nbt, CallbackInfoReturnable<ItemStack> cir) {
        if (nbt == null) return;
        Modules mods = Modules.get();
        if (mods == null) return;

        BetterTooltips tooltips = mods.get(BetterTooltips.class);
        var showReverted = tooltips.settings.get("show-reverted-illegals");

        Optional<Integer> trueDamage = Optional.empty();
        if (showReverted != null && ((boolean) showReverted.get())) {
            Item item = Registries.ITEM.get(new Identifier(nbt.getString("id")));
            int count = nbt.getByte("Count");

            Text customName = null;
            NbtList enchants = null;
            if (nbt.contains("tag", NbtElement.COMPOUND_TYPE)) {
                NbtCompound tag = nbt.getCompound("tag");
                int index = tag.toString().indexOf("Damage:");
                if (index != -1) {
                    try {
                        String subStr = tag.toString().substring(index + 7);
                        trueDamage = Optional.of(Integer.parseInt(subStr.substring(0, subStr.indexOf(',') == -1 ? subStr.indexOf('}') : subStr.indexOf(','))));
                    } catch (Exception ignored) {}
                }

                if (tag.contains("Enchantments", NbtElement.LIST_TYPE)) {
                    enchants = tag.getList("Enchantments", NbtElement.COMPOUND_TYPE);
                }
                if (tag.contains("display", NbtElement.COMPOUND_TYPE) && tag.getCompound("display").contains("Name", NbtElement.STRING_TYPE)) {
                    customName = Text.Serialization.fromJson(tag.getCompound("display").getString("Name"));
                }
            }

            if (count <= 0 && item != Items.AIR) {
                if (item.isDamageable() && trueDamage.isPresent()) {
                    int dmg = trueDamage.get();
                    nbt.putInt("Damage", dmg);
                }
                if (enchants != null) {
                    nbt.put("Enchantments", enchants);
                }
                if (customName != null) {
                    nbt.put("display", new NbtCompound());
                    NbtCompound displayTag = nbt.getCompound("display");
                    displayTag.putString("Name", Text.Serialization.toJsonString(customName));
                }
                cir.cancel();
                cir.setReturnValue(new ItemStack(item.getRegistryEntry(), 69, Optional.of(nbt)));
            }
        }
    }

    @Inject(method = "setDamage", at = @At("HEAD"), cancellable = true)
    private void enableTrueDamage(int damage, CallbackInfo ci) {
        if (damage >= 0) return;
        Modules mods = Modules.get();
        if (mods == null) return;

        BetterTooltips tooltips = mods.get(BetterTooltips.class);
        var rawDamageTag = tooltips.settings.get("raw-damage-tag");
        var trueDurability = tooltips.settings.get("true-durability");
        if ((rawDamageTag != null && ((boolean) rawDamageTag.get())) || (trueDurability != null && ((boolean) trueDurability.get()))) {
            ci.cancel();
            ((ItemStack)(Object) this).getOrCreateNbt().putInt("Damage", damage);
        }
    }
}
