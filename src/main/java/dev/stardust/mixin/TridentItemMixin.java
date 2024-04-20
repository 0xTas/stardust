package dev.stardust.mixin;

import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import dev.stardust.modules.RocketMan;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.entity.LivingEntity;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(TridentItem.class)
public abstract class TridentItemMixin extends Item {
    public TridentItemMixin(Settings settings) {
        super(settings);
    }

    // See RocketMan.java
    @Inject(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/TridentItem;getMaxUseTime(Lnet/minecraft/item/ItemStack;)I", shift = At.Shift.AFTER))
    private void bypassTridentChargeTime(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci, @Local(ordinal = 0)LocalIntRef i) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rm = modules.get(RocketMan.class);
        if (!rm.isActive() || !rm.tridentBoost.get()) return;
        i.set(69420);
    }
}
