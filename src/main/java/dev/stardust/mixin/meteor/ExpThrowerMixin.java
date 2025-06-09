package dev.stardust.mixin.meteor;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.registry.tag.ItemTags;
import java.util.concurrent.ThreadLocalRandom;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.component.DataComponentTypes;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(value = EXPThrower.class, remap = false)
public abstract class ExpThrowerMixin extends Module {
    public ExpThrowerMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Unique
    private @Nullable Setting<Integer> levelCap = null;
    @Unique
    private @Nullable Setting<Boolean> autoToggle = null;
    @Unique
    private @Nullable Setting<Boolean> hotbarSwap = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addLevelCapSetting(CallbackInfo ci) {
        levelCap = this.settings.getDefaultGroup().add(
            new IntSetting.Builder()
                .name("level-cap")
                .description("The level to stop throwing exp bottles at (leave on 0 for unlimited).")
                .min(0).sliderRange(0, 69)
                .defaultValue(0)
                .build()
        );
        autoToggle = this.settings.getDefaultGroup().add(
            new BoolSetting.Builder()
                .name("auto-toggle")
                .description("Automatically disable the module when the level cap is reached.")
                .defaultValue(false)
                .visible(() -> levelCap != null && levelCap.get() > 0)
                .build()
        );
        hotbarSwap = this.settings.getDefaultGroup().add(
            new BoolSetting.Builder()
                .name("hotbar-swap")
                .description("Swap xp from your inventory to your hotbar if none already occupies it.")
                .defaultValue(false)
                .build()
        );
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void stopAtLevelCap(CallbackInfo ci) {
        if (mc.player == null) return;
        if (hotbarSwap != null && hotbarSwap.get()) {
            FindItemResult result = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);

            if (!result.found()) {
                FindItemResult result1 = InvUtils.find(Items.EXPERIENCE_BOTTLE);

                if (result1.found()) {
                    FindItemResult emptySlot = InvUtils.findInHotbar(ItemStack::isEmpty);

                    if (emptySlot.found()) InvUtils.move().from(result1.slot()).to(emptySlot.slot());
                    else {
                        FindItemResult nonCriticalSlot = InvUtils.findInHotbar(stack -> !(stack.getItem() instanceof MiningToolItem) && !(stack.isIn(ItemTags.WEAPON_ENCHANTABLE)) && !(stack.contains(DataComponentTypes.FOOD)));

                        if (nonCriticalSlot.found()) InvUtils.move().from(result1.slot()).to(emptySlot.slot());
                        else {
                            int luckySlot = ThreadLocalRandom.current().nextInt(9);
                            InvUtils.move().from(result1.slot()).to(luckySlot);
                        }
                    }
                }

                ci.cancel();
                return;
            }
        }

        if (levelCap == null || levelCap.get() == 0) return;
        if (mc.player.experienceLevel >= levelCap.get()) {
            ci.cancel();
            if (autoToggle != null && autoToggle.get()) {
                this.toggle();
            }
        }
    }
}
