package dev.stardust.modules;

import java.util.Arrays;
import java.util.HashMap;
import dev.stardust.Stardust;
import net.minecraft.screen.*;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.enchantment.EnchantmentHelper;
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
            .name("Illegal Enchants")
            .description("Highlight items with illegal enchantments like Mending/Infinity, or stacked Protection.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlySilkyShears = sgRares.add(
        new BoolSetting.Builder()
            .name("Exclusive Silky Shears")
            .description("Highlight silk touch shears only if they have no other enchants.")
            .defaultValue(false)
            .visible(illegalEnchants::get)
            .build()
    );

    private final Setting<Boolean> onlyInfinityMending = sgRares.add(
        new BoolSetting.Builder()
            .name("Exclusive Mending/Infinity")
            .description("Highlight bows & books that have ONLY mending & infinity applied.")
            .defaultValue(false)
            .visible(illegalEnchants::get)
            .build()
    );

    private final Setting<Boolean> negativeDurability = sgRares.add(
        new BoolSetting.Builder()
            .name("Negative Durability")
            .description("Highlight items with illegal negative durability values.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> petrifiedSlabs = sgRares.add(
        new BoolSetting.Builder()
            .name("Alpha Slabs")
            .description("Highlight alpha slabs (now petrified oak slabs.)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fd0 = sgRares.add(
        new BoolSetting.Builder()
            .name("Flight Duration 0")
            .description("Highlight firework rockets with no flight duration value (idk if these still exist.)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renamedItems = sgUniques.add(
        new BoolSetting.Builder()
            .name("Renamed Items")
            .description("Highlight renamed items in GUIs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renamedShulks = sgUniques.add(
        new BoolSetting.Builder()
            .name("Renamed Shulkers")
            .description("Highlight renamed shulker boxes, even if they contain no renamed items.")
            .defaultValue(false)
            .visible(renamedItems::get)
            .build()
    );

    private final Setting<Boolean> writtenBooks = sgUniques.add(
        new BoolSetting.Builder()
            .name("Written Books")
            .description("Highlight written books.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> metadataSearch = settings.getDefaultGroup().add(
        new StringSetting.Builder()
            .name("Metadata Search")
            .description("Fuzzy search for item NBT data. Notable usage examples: specific book authors, item names, or enchants.")
            .defaultValue("")
            .build()
    );

    private final Setting<Boolean> splitQueries = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Split Queries")
            .description("Split search queries into multiple items separated by commas. Disable to treat commas literally in the search instead.")
            .defaultValue(true)
            .build()
    );

    public final Setting<SettingColor> color = settings.getDefaultGroup().add(
        new ColorSetting.Builder()
            .name("Highlight Color")
            .defaultValue(new SettingColor(138, 71, 221, 69))
            .build()
    );

    private final Setting<Boolean> ownInventory = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Inventory Highlight")
            .description("Highlight items meeting the above criteria on the player inventory screen.")
            .defaultValue(false)
            .build()
    );

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
            NbtCompound nbt = stack.getSubNbt("BlockEntityTag");

            if (nbt != null) {
                DefaultedList<ItemStack> stacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.readNbt(nbt, stacks);

                for (ItemStack s : stacks) {
                    if (shouldHighlightSlot(s)) return true;
                }
            }
        }

        if (!metadataSearch.get().trim().isEmpty()) {
            NbtCompound metadata = stack.getNbt();
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

        if (!renamedShulks.get() && stack.hasCustomName()) {
            if (stack.getItem() == Items.SHULKER_BOX || AutoDyeShulkers.isColoredShulker(stack.getItem())) return false;
        }

        if (renamedItems.get() && stack.hasCustomName()) return true;
        if (writtenBooks.get() && stack.getItem() == Items.WRITTEN_BOOK) return true;
        if (petrifiedSlabs.get() && stack.getItem() == Items.PETRIFIED_OAK_SLAB) return true;

        if (fd0.get() && stack.getItem() == Items.FIREWORK_ROCKET) {
            NbtCompound nbt = stack.getSubNbt(FireworkRocketItem.FIREWORKS_KEY);
            if (nbt == null || !nbt.contains(FireworkRocketItem.FLIGHT_KEY, NbtElement.NUMBER_TYPE)) return true;
            else if (nbt.getByte("Flight") != 1 && nbt.getByte("Flight") != 2 && nbt.getByte("Flight") != 3) return true;
        }

        if (negativeDurability.get() && stack.isDamageable()) {
            if (stack.getDamage() > stack.getMaxDamage() || stack.getDamage() < 0 || stack.getMaxDamage() < 0) return true;
        }

        if (illegalEnchants.get() && (stack.getItem() == Items.ENCHANTED_BOOK || stack.hasEnchantments())) {
            if (stack.getItem() == Items.SHEARS && EnchantmentHelper.hasSilkTouch(stack)) {
                return EnchantmentHelper.get(stack).size() == 1 || !onlySilkyShears.get();
            }

            NbtList nbt;
            if (stack.getItem() == Items.ENCHANTED_BOOK) {
                int enchants = EnchantmentHelper.get(stack).size();
                if (enchants == 0 || enchants > 7) return true;
                nbt = EnchantedBookItem.getEnchantmentNbt(stack);
            } else {
                nbt = stack.getEnchantments();
            }
            if (nbt == null || nbt.isEmpty()) return true;

            boolean hasMending = false;
            boolean hasInfinity = false;
            boolean hasProtection = false;
            HashMap<Enchantment, Integer> dupes = new HashMap<>();
            for (int n = 0; n < nbt.size(); n++) {
                if (hasMending && hasInfinity) break;
                NbtCompound comp = nbt.getCompound(n);
                Enchantment enchant = Registries.ENCHANTMENT.get(EnchantmentHelper.getIdFromNbt(comp));
                if (enchant == null) return true;
                if (enchant == Enchantments.MENDING) hasMending = true;
                if (enchant == Enchantments.INFINITY) hasInfinity = true;
                if (enchant == Enchantments.PROTECTION || enchant == Enchantments.PROJECTILE_PROTECTION
                    || enchant == Enchantments.BLAST_PROTECTION || enchant == Enchantments.FIRE_PROTECTION) {
                    if (!hasProtection) hasProtection = true;
                    else return true;
                }

                if (dupes.containsKey(enchant)) {
                    dupes.put(enchant, dupes.get(enchant) + 1);
                } else {
                    dupes.put(enchant, 1);
                }
            }
            if (dupes.values().stream().anyMatch(n -> n > 1)) return true;
            if (hasMending && hasInfinity) return nbt.size() == 2 || !onlyInfinityMending.get();
        }

        return false;
    }
}
