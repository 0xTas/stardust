package dev.stardust.modules;

import java.util.Arrays;
import java.util.Optional;
import dev.stardust.Stardust;
import net.minecraft.screen.*;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.component.type.NbtComponent;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.component.DataComponentTypes;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class LoreLocator extends Module {
    public LoreLocator() { super(Stardust.CATEGORY, "LoreLocator", "Slot highlighter for rare, unique, and anomalous items."); }

    private final SettingGroup sgRares = settings.createGroup("Rares Settings");
    private final SettingGroup sgUniques = settings.createGroup("Uniques Settings");

    private final Setting<Boolean> illegalEnchants = sgRares.add(
        new BoolSetting.Builder()
            .name("illegal-enchants")
            .description("Highlight items with illegal enchantments like Mending/Infinity, or stacked Protection.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlySilkyShears = sgRares.add(
        new BoolSetting.Builder()
            .name("exclusive-silky-shears")
            .description("Highlight silk touch shears only if they have no other enchants.")
            .defaultValue(false)
            .visible(illegalEnchants::get)
            .build()
    );

    private final Setting<Boolean> onlyInfinityMending = sgRares.add(
        new BoolSetting.Builder()
            .name("exclusive-mending/Infinity")
            .description("Highlight bows & books that have ONLY mending & infinity applied.")
            .defaultValue(false)
            .visible(illegalEnchants::get)
            .build()
    );

    private final Setting<Boolean> negativeDurability = sgRares.add(
        new BoolSetting.Builder()
            .name("negative-durability")
            .description("Highlight items with negative true durability (all negative durability items show in-game as 0 durability items in 1.21.)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> petrifiedSlabs = sgRares.add(
        new BoolSetting.Builder()
            .name("alpha-slabs")
            .description("Highlight alpha slabs (now petrified oak slabs.)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> lagRockets = sgRares.add(
        new BoolSetting.Builder()
            .name("lag-rockets")
            .description("Highlight lag rockets.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> illegalFish = sgRares.add(
        new BoolSetting.Builder()
            .name("illegal-fish")
            .description("Highlight illegal tropical fish with black as one of their colors. These are no longer obtainable as of 1.21.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> renamedItems = sgUniques.add(
        new BoolSetting.Builder()
            .name("renamed-items")
            .description("Highlight renamed items in GUIs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renamedShulks = sgUniques.add(
        new BoolSetting.Builder()
            .name("renamed-shulkers")
            .description("Highlight renamed shulker boxes, even if they contain no renamed items.")
            .defaultValue(false)
            .visible(renamedItems::get)
            .build()
    );

    private final Setting<Boolean> writtenBooks = sgUniques.add(
        new BoolSetting.Builder()
            .name("written-books")
            .description("Highlight written books.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> metadataSearch = settings.getDefaultGroup().add(
        new StringSetting.Builder()
            .name("metadata-search")
            .description("Fuzzy search for item NBT data. Notable usage examples: specific book authors, item names, or enchants.")
            .defaultValue("")
            .build()
    );

    private final Setting<Boolean> splitQueries = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("split-queries")
            .description("Split search queries into multiple items separated by commas. Disable to treat commas literally in the search instead.")
            .defaultValue(true)
            .build()
    );

    public final Setting<SettingColor> color = settings.getDefaultGroup().add(
        new ColorSetting.Builder()
            .name("highlight-color")
            .defaultValue(new SettingColor(138, 71, 221, 69))
            .build()
    );

    private final Setting<Boolean> ownInventory = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("inventory-highlight")
            .description("Highlight items meeting the above criteria on the player inventory screen.")
            .defaultValue(false)
            .build()
    );

    private int enchantmentsCount(ItemStack stack) {
        int count = 0;
        if (!stack.isEmpty()) {
            count = stack.getItem() == Items.ENCHANTED_BOOK
                ? stack.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantments().size()
                : stack.getEnchantments().getSize();
        }
        return count;
    }

    private boolean shouldIgnoreCurrentScreenHandler(ClientPlayerEntity player) {
        if (mc.currentScreen == null) return true;
        if (player.currentScreenHandler == null) return true;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler instanceof PlayerScreenHandler) return !ownInventory.get();
        return !(handler instanceof AbstractFurnaceScreenHandler || handler instanceof GenericContainerScreenHandler
            || handler instanceof Generic3x3ContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler
            || handler instanceof HopperScreenHandler || handler instanceof HorseScreenHandler);
    }

    // See DrawContextMixin.java
    public boolean shouldHighlightSlot(ItemStack stack) {
        if (mc.player == null) return false;
        if (stack.isEmpty() || shouldIgnoreCurrentScreenHandler(mc.player)) return false;

        if (Utils.hasItems(stack)) {
            ItemStack[] stacks = new ItemStack[27];
            Utils.getItemsInContainerItem(stack, stacks);
            for (ItemStack s : stacks) {
                if (shouldHighlightSlot(s)) return true;
            }
        }

        if (!metadataSearch.get().trim().isEmpty()) {
            ComponentMap metadata = stack.getComponents();
            String query = metadataSearch.get().toLowerCase();

            if (splitQueries.get() && query.contains(",")) {
                String[] queries = query.split(",");

                if (metadata != null && Arrays.stream(queries).anyMatch(q -> metadata.toString().toLowerCase().contains(q.trim())
                    || metadata.toString().toLowerCase().contains(q.trim().replace(" ", "_")))) {
                    return true;
                } else if (Arrays.stream(queries).anyMatch(q -> stack.getName().getString().toLowerCase().contains(q.trim())
                    || stack.getItem().getDefaultStack().getName().getString().toLowerCase().contains(q.trim()))) {
                    return true;
                }
            } else {
                if (metadata != null) {
                    if (metadata.toString().toLowerCase().contains(query.trim())) return true;
                    else if (metadata.toString().toLowerCase().contains(query.trim().replace(" ", "_"))) return true;
                }

                if (stack.getName().getString().toLowerCase().contains(query.trim())) return true;
                else if (stack.getItem().getDefaultStack().getName().getString().toLowerCase().contains(query.trim())) return true;
            }
        }

        if (!renamedShulks.get() && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            if (stack.getItem() == Items.SHULKER_BOX || AutoDyeShulkers.isColoredShulker(stack.getItem())) return false;
        }

        if (lagRockets.get() && stack.contains(DataComponentTypes.FIREWORKS)) {
            FireworksComponent firework = stack.get(DataComponentTypes.FIREWORKS);
            if (firework.explosions().size() == 7) return true;
        }

        if (illegalFish.get() && stack.isOf(Items.TROPICAL_FISH_BUCKET)) {
            NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.BUCKET_ENTITY_DATA, NbtComponent.DEFAULT);
            if (!nbtComponent.isEmpty()) {
                Optional<TropicalFishEntity.Variant> optional = nbtComponent.get(TropicalFishEntity.Variant.CODEC.fieldOf("BucketVariantTag")).result();
                if (optional.isPresent()) {
                    TropicalFishEntity.Variant variant = optional.get();
                    String string = "color.minecraft." + variant.baseColor();
                    String string2 = "color.minecraft." + variant.patternColor();
                    int i = TropicalFishEntity.COMMON_VARIANTS.indexOf(variant);
                    if (i == -1) {
                        if (string.contains("black") || string2.contains("black")) {
                            return true;
                        }
                    }
                }
            }
        }

        if (writtenBooks.get() && stack.getItem() == Items.WRITTEN_BOOK) return true;
        if (petrifiedSlabs.get() && stack.getItem() == Items.PETRIFIED_OAK_SLAB) return true;
        if (renamedItems.get() && stack.contains(DataComponentTypes.CUSTOM_NAME)) return true;

        if (negativeDurability.get() && stack.isDamageable()) {
            if (stack.getOrDefault(DataComponentTypes.DAMAGE, stack.getDamage()) >= stack.getMaxDamage()) return true;
        }

        if (illegalEnchants.get() && (stack.getItem() == Items.ENCHANTED_BOOK || stack.hasEnchantments())) {
            int enchantmentsCount = enchantmentsCount(stack);
            if (stack.getItem() == Items.SHEARS && Utils.hasEnchantment(stack, Enchantments.SILK_TOUCH)) {
                return enchantmentsCount == 1 || !onlySilkyShears.get();
            }

            if (stack.getItem() == Items.ENCHANTED_BOOK) {
                if (enchantmentsCount == 0 || enchantmentsCount > 7) return true;
            }

            boolean hasProtection = Utils.hasEnchantment(stack, Enchantments.PROTECTION);
            if (Utils.hasEnchantment(stack, Enchantments.FIRE_PROTECTION)) {
                if (!hasProtection) hasProtection = true;
                else return true;
            }
            if (Utils.hasEnchantment(stack, Enchantments.BLAST_PROTECTION)) {
                if (!hasProtection) hasProtection = true;
                else return true;
            }
            if (Utils.hasEnchantment(stack, Enchantments.PROJECTILE_PROTECTION)) {
                if (hasProtection) return true;
            }

            if (Utils.hasEnchantment(stack, Enchantments.INFINITY) && Utils.hasEnchantment(stack, Enchantments.MENDING)) {
                return enchantmentsCount == 2 || !onlyInfinityMending.get();
            }
        }

        return false;
    }
}
