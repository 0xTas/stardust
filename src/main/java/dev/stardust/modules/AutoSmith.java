package dev.stardust.modules;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import net.minecraft.item.*;
import dev.stardust.Stardust;
import dev.stardust.util.LogUtil;
import dev.stardust.util.MsgUtil;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import meteordevelopment.orbit.EventHandler;
import java.util.concurrent.ThreadLocalRandom;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.screen.slot.SlotActionType;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.item.equipment.trim.ArmorTrim;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import dev.stardust.mixin.accessor.ClientConnectionAccessor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AutoSmith extends Module {
    public AutoSmith() { super(Stardust.CATEGORY, "AutoSmith", "Automatically upgrade gear or trim armor in smithing tables."); }

    private final SettingGroup trimSettings = settings.createGroup("Armor Trims");
    private final SettingGroup modeSettings = settings.createGroup("Smithing Mode");

    public enum ModuleMode {
        Packet, Interact
    }
    public enum SmithingMode {
        Trim, Upgrade
    }
    public enum ArmorMaterials {
        Iron, Gold, Chain, Turtle, Leather, Diamond, Netherite;
        public boolean materialEquals(ArmorMaterial material) {
            return switch (this) {
                case Iron -> material == net.minecraft.item.equipment.ArmorMaterials.IRON;
                case Gold -> material == net.minecraft.item.equipment.ArmorMaterials.GOLD;
                case Chain -> material == net.minecraft.item.equipment.ArmorMaterials.CHAIN;
                case Turtle -> material == net.minecraft.item.equipment.ArmorMaterials.TURTLE_SCUTE;
                case Leather -> material == net.minecraft.item.equipment.ArmorMaterials.LEATHER;
                case Diamond -> material == net.minecraft.item.equipment.ArmorMaterials.DIAMOND;
                case Netherite -> material == net.minecraft.item.equipment.ArmorMaterials.NETHERITE;
            };
        }
    }
    public enum ArmorTrims {
        Eye("minecraft:eye"),
        Vex("minecraft:vex"),
        Rib("minecraft:rib"),
        Bolt("minecraft:bolt"),
        Wild("minecraft:wild"),
        Dune("minecraft:dune"),
        Host("minecraft:host"),
        Ward("minecraft:ward"),
        Tide("minecraft:tide"),
        Flow("minecraft:flow"),
        Coast("minecraft:coast"),
        Snout("minecraft:snout"),
        Spire("minecraft:spire"),
        Raiser("minecraft:raiser"),
        Shaper("minecraft:shaper"),
        Sentry("minecraft:sentry"),
        Silence("minecraft:silence"),
        Wayfinder("minecraft:wayfinder");

        public final String label;

        ArmorTrims(String label) { this.label = label; }
    }
    public enum TrimMaterial {
        Iron("minecraft:iron"),
        Gold("minecraft:gold"),
        Lapis("minecraft:lapis"),
        Resin("minecraft:resin"),
        Copper("minecraft:copper"),
        Quartz("minecraft:quartz"),
        Emerald("minecraft:emerald"),
        Diamond("minecraft:diamond"),
        Redstone("minecraft:redstone"),
        Amethyst("minecraft:amethyst"),
        Netherite("minecraft:netherite");

        public final String label;

        TrimMaterial(String label) { this.label = label; }
    }

    private final Setting<ModuleMode> moduleMode = modeSettings.add(
        new EnumSetting.Builder<ModuleMode>()
            .name("module-mode")
            .description("Packet is significantly faster, but may get you kicked in some scenarios.")
            .defaultValue(ModuleMode.Packet)
            .build()
    );
    private final Setting<Integer> tickRate = modeSettings.add(
        new IntSetting.Builder()
            .name("tick-delay")
            .description("Increase this if the server is kicking you.")
            .visible(() -> moduleMode.get().equals(ModuleMode.Interact))
            .range(2, 100)
            .sliderRange(2, 20)
            .defaultValue(4)
            .build()
    );
    private final Setting<Integer> packetLimit = modeSettings.add(
        new IntSetting.Builder()
            .name("packet-limit")
            .description("Decrease this if the server is kicking you.")
            .visible(() -> moduleMode.get().equals(ModuleMode.Packet))
            .min(20).sliderMax(100)
            .defaultValue(42)
            .build()
    );
    private final Setting<SmithingMode> operatingMode = modeSettings.add(
        new EnumSetting.Builder<SmithingMode>()
            .name("smithing-mode")
            .defaultValue(SmithingMode.Upgrade)
            .build()
    );
    private final Setting<Boolean> overwriteTrims = modeSettings.add(
        new BoolSetting.Builder()
            .name("overwrite-trims")
            .description("Trim armor pieces which already contain a different trim pattern or material.")
            .defaultValue(false)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> helmetType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("helmet-armor-type")
            .description("Which type of helmet to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> helmetTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("helmet-armor-trim")
            .description("Which armor trim to apply onto helmets.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> helmetTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("helmet-trim-material")
            .description("What material to use for helmet armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> chestplateType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("chestplate-armor-type")
            .description("Which type of chestplates to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> chestplateTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("chestplate-armor-trim")
            .description("Which armor trim to apply onto chestplates.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> chestplateTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("chestplate-trim-material")
            .description("What material to use for chestplate armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> leggingsType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("leggings-armor-type")
            .description("Which type of leggings to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> leggingsTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("leggings-armor-trim")
            .description("Which armor trim to apply onto leggings.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> leggingsTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("leggings-trim-material")
            .description("What material to use for leggings armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> bootsType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("boots-armor-type")
            .description("Which type of boots to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> bootsTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("boots-armor-trim")
            .description("Which armor trim to apply onto boots.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> bootsTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("boots-trim-material")
            .description("What material to use for boots armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> operatingMode.get() == SmithingMode.Trim)
            .build()
    );

    // See WorldMixin.java
    public final Setting<Boolean> muteSmithy = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("mute-smithing-table")
            .description("Mute the smithing table sounds.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("close-screen")
            .description("Automatically close the crafting screen when no more gear can be upgraded.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("disable-on-done")
            .description("Automatically disable the module when no more gear can be upgraded.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("sound-ping")
            .description("Play a sound cue when no more gear can be trimmed or upgraded.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> pingVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("ping-volume")
            .visible(pingOnDone::get)
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(0.5)
            .build()
    );
    private final Setting<Boolean> debug = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("debug")
            .description("Displays debug messages in your chat.")
            .defaultValue(false)
            .visible(() -> false) // toggle by command only
            .build()
    );

    private int timer = 0;
    private boolean notified = false;
    private boolean foundEquip = false;
    private boolean foundIngots = false;
    private boolean foundTemplates = false;
    private boolean resettingTemplates = false;
    private boolean resettingMaterials = false;
    private @Nullable ItemStack trimStack = null;
    private @Nullable ItemStack materialStack = null;
    private @Nullable ItemStack equipmentStack = null;
    private @Nullable EquipmentType currentlyLookingFor = null;
    private final IntArrayList projectedEmpty = new IntArrayList();
    private final IntArrayList processedSlots = new IntArrayList();
    private final List<EquipmentType> exhaustedArmorTypes = new ArrayList<>();

    private ArmorMaterial getArmorMaterial(ItemStack armor) {
        if (!(armor.getItem() instanceof ArmorItem)) return net.minecraft.item.equipment.ArmorMaterials.ARMADILLO_SCUTE;

        switch (getItemSlotId(armor)) {
            case 0 -> {
                if (armor.isOf(Items.LEATHER_BOOTS)) return net.minecraft.item.equipment.ArmorMaterials.LEATHER;
                if (armor.isOf(Items.IRON_BOOTS)) return net.minecraft.item.equipment.ArmorMaterials.IRON;
                if (armor.isOf(Items.CHAINMAIL_BOOTS)) return net.minecraft.item.equipment.ArmorMaterials.CHAIN;
                if (armor.isOf(Items.GOLDEN_BOOTS)) return net.minecraft.item.equipment.ArmorMaterials.GOLD;
                if (armor.isOf(Items.DIAMOND_BOOTS)) return net.minecraft.item.equipment.ArmorMaterials.DIAMOND;
                if (armor.isOf(Items.NETHERITE_BOOTS)) return net.minecraft.item.equipment.ArmorMaterials.NETHERITE;
                else return net.minecraft.item.equipment.ArmorMaterials.ARMADILLO_SCUTE;
            }
            case 1 -> {
                if (armor.isOf(Items.LEATHER_LEGGINGS)) return net.minecraft.item.equipment.ArmorMaterials.LEATHER;
                if (armor.isOf(Items.IRON_LEGGINGS)) return net.minecraft.item.equipment.ArmorMaterials.IRON;
                if (armor.isOf(Items.CHAINMAIL_LEGGINGS)) return net.minecraft.item.equipment.ArmorMaterials.CHAIN;
                if (armor.isOf(Items.GOLDEN_LEGGINGS)) return net.minecraft.item.equipment.ArmorMaterials.GOLD;
                if (armor.isOf(Items.DIAMOND_LEGGINGS)) return net.minecraft.item.equipment.ArmorMaterials.DIAMOND;
                if (armor.isOf(Items.NETHERITE_LEGGINGS)) return net.minecraft.item.equipment.ArmorMaterials.NETHERITE;
                else return net.minecraft.item.equipment.ArmorMaterials.ARMADILLO_SCUTE;
            }
            case 2 -> {
                if (armor.isOf(Items.LEATHER_CHESTPLATE)) return net.minecraft.item.equipment.ArmorMaterials.LEATHER;
                if (armor.isOf(Items.IRON_CHESTPLATE)) return net.minecraft.item.equipment.ArmorMaterials.IRON;
                if (armor.isOf(Items.CHAINMAIL_CHESTPLATE)) return net.minecraft.item.equipment.ArmorMaterials.CHAIN;
                if (armor.isOf(Items.GOLDEN_CHESTPLATE)) return net.minecraft.item.equipment.ArmorMaterials.GOLD;
                if (armor.isOf(Items.DIAMOND_CHESTPLATE)) return net.minecraft.item.equipment.ArmorMaterials.DIAMOND;
                if (armor.isOf(Items.NETHERITE_CHESTPLATE)) return net.minecraft.item.equipment.ArmorMaterials.NETHERITE;
                else return net.minecraft.item.equipment.ArmorMaterials.ARMADILLO_SCUTE;
            }
            case 3 -> {
                if (armor.isOf(Items.LEATHER_HELMET)) return net.minecraft.item.equipment.ArmorMaterials.LEATHER;
                if (armor.isOf(Items.IRON_HELMET)) return net.minecraft.item.equipment.ArmorMaterials.IRON;
                if (armor.isOf(Items.CHAINMAIL_HELMET)) return net.minecraft.item.equipment.ArmorMaterials.CHAIN;
                if (armor.isOf(Items.GOLDEN_HELMET)) return net.minecraft.item.equipment.ArmorMaterials.GOLD;
                if (armor.isOf(Items.DIAMOND_HELMET)) return net.minecraft.item.equipment.ArmorMaterials.DIAMOND;
                if (armor.isOf(Items.NETHERITE_HELMET)) return net.minecraft.item.equipment.ArmorMaterials.NETHERITE;
                if (armor.isOf(Items.TURTLE_HELMET)) return net.minecraft.item.equipment.ArmorMaterials.TURTLE_SCUTE;
                else return net.minecraft.item.equipment.ArmorMaterials.ARMADILLO_SCUTE;
            }
            default -> {
                return net.minecraft.item.equipment.ArmorMaterials.ARMADILLO_SCUTE;
            }
        }
    }

    private EquipmentType getEquipmentType(ArmorItem armor) {
        return switch (getItemSlotId(armor.getDefaultStack())) {
            case 0 -> EquipmentType.BOOTS;
            case 1 -> EquipmentType.LEGGINGS;
            case 2 -> EquipmentType.CHESTPLATE;
            case 3 -> EquipmentType.HELMET;
            default -> EquipmentType.BODY;
        };
    }

    private int getItemSlotId(ItemStack itemStack) {
        return itemStack.get(DataComponentTypes.EQUIPPABLE).slot().getEntitySlotId();
    }

    private boolean isValidEquipmentForUpgrading(ItemStack stack) {
        return stack.isOf(Items.DIAMOND_HOE) || stack.isOf(Items.DIAMOND_PICKAXE) || stack.isOf(Items.DIAMOND_AXE)
            || stack.isOf(Items.DIAMOND_SHOVEL) || stack.isOf(Items.DIAMOND_SWORD)  || stack.isOf(Items.DIAMOND_HELMET)
            || stack.isOf(Items.DIAMOND_CHESTPLATE) || stack.isOf(Items.DIAMOND_LEGGINGS) || stack.isOf(Items.DIAMOND_BOOTS);
    }

    private boolean isValidEquipmentForTrimming(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) {
            boolean correctMaterial = false;
            EquipmentType equipmentType = getEquipmentType(armor);
            ArmorMaterial armorMaterial = getArmorMaterial(stack);
            if (exhaustedArmorTypes.contains(equipmentType)) return false;
            if (currentlyLookingFor != null && !equipmentType.equals(currentlyLookingFor)) return false;

            switch (getItemSlotId(stack)) {
                case 0 -> correctMaterial = bootsType.get().materialEquals(armorMaterial);
                case 3 -> correctMaterial = helmetType.get().materialEquals(armorMaterial);
                case 1 -> correctMaterial = leggingsType.get().materialEquals(armorMaterial);
                case 2 -> correctMaterial = chestplateType.get().materialEquals(armorMaterial);
            }

            if (!correctMaterial) return false;
            if (stack.contains(DataComponentTypes.TRIM)) {
                if (!overwriteTrims.get()) return false;
                String pattern = stack.get(DataComponentTypes.TRIM).pattern().getIdAsString();
                String material = stack.get(DataComponentTypes.TRIM).material().getIdAsString();

                switch (equipmentType) {
                    case BOOTS -> {
                        if (!bootsTrim.get().label.equals(pattern) || !bootsTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(equipmentType)) return true;
                        }
                    }
                    case HELMET -> {
                        if (!helmetTrim.get().label.equals(pattern) || !helmetTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(equipmentType)) return true;
                        }
                    }
                    case LEGGINGS -> {
                        if (!leggingsTrim.get().label.equals(pattern) || !leggingsTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(equipmentType)) return true;
                        }
                    }
                    case CHESTPLATE -> {
                        if (!chestplateTrim.get().label.equals(pattern) || !chestplateTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(equipmentType)) return true;
                        }
                    }
                }
            } else return true;
        }
        return false;
    }

    private boolean hasRequiredMaterialsForTrimming(EquipmentType type) {
        boolean hasTemplate = false;
        boolean hasMaterial = false;
        switch (type) {
            case BOOTS -> {
                switch (bootsTrim.get()) {
                    case Eye -> hasTemplate = hasItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Rib -> hasTemplate = hasItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Vex -> hasTemplate = hasItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Dune -> hasTemplate = hasItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Host -> hasTemplate = hasItem(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Tide -> hasTemplate = hasItem(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Ward -> hasTemplate = hasItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wild -> hasTemplate = hasItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Bolt -> hasMaterial = hasItem(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Flow -> hasMaterial = hasItem(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Coast -> hasTemplate = hasItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Snout -> hasTemplate = hasItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Spire -> hasTemplate = hasItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Raiser -> hasTemplate = hasItem(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Sentry -> hasTemplate = hasItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Shaper -> hasTemplate = hasItem(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Silence -> hasTemplate = hasItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wayfinder -> hasTemplate = hasItem(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
                }
                switch (bootsTrimMaterial.get()) {
                    case Iron -> hasMaterial = hasItem(Items.IRON_INGOT);
                    case Gold -> hasMaterial = hasItem(Items.GOLD_INGOT);
                    case Lapis -> hasMaterial = hasItem(Items.LAPIS_LAZULI);
                    case Quartz -> hasMaterial = hasItem(Items.QUARTZ);
                    case Resin -> hasMaterial = hasItem(Items.RESIN_BRICK);
                    case Copper -> hasMaterial = hasItem(Items.COPPER_INGOT);
                    case Emerald -> hasMaterial = hasItem(Items.EMERALD);
                    case Diamond -> hasMaterial = hasItem(Items.DIAMOND);
                    case Amethyst -> hasMaterial = hasItem(Items.AMETHYST_SHARD);
                    case Redstone -> hasMaterial = hasItem(Items.REDSTONE);
                    case Netherite -> hasMaterial = hasItem(Items.NETHERITE_INGOT);
                }
                if (hasTemplate && hasMaterial) return true;
            }
            case HELMET -> {
                switch (helmetTrim.get()) {
                    case Eye -> hasTemplate = hasItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Rib -> hasTemplate = hasItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Vex -> hasTemplate = hasItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Dune -> hasTemplate = hasItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Host -> hasTemplate = hasItem(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Tide -> hasTemplate = hasItem(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Ward -> hasTemplate = hasItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wild -> hasTemplate = hasItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Bolt -> hasMaterial = hasItem(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Flow -> hasMaterial = hasItem(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Coast -> hasTemplate = hasItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Snout -> hasTemplate = hasItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Spire -> hasTemplate = hasItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Raiser -> hasTemplate = hasItem(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Sentry -> hasTemplate = hasItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Shaper -> hasTemplate = hasItem(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Silence -> hasTemplate = hasItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wayfinder -> hasTemplate = hasItem(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
                }
                switch (helmetTrimMaterial.get()) {
                    case Iron -> hasMaterial = hasItem(Items.IRON_INGOT);
                    case Gold -> hasMaterial = hasItem(Items.GOLD_INGOT);
                    case Lapis -> hasMaterial = hasItem(Items.LAPIS_LAZULI);
                    case Quartz -> hasMaterial = hasItem(Items.QUARTZ);
                    case Resin -> hasMaterial = hasItem(Items.RESIN_BRICK);
                    case Copper -> hasMaterial = hasItem(Items.COPPER_INGOT);
                    case Emerald -> hasMaterial = hasItem(Items.EMERALD);
                    case Diamond -> hasMaterial = hasItem(Items.DIAMOND);
                    case Amethyst -> hasMaterial = hasItem(Items.AMETHYST_SHARD);
                    case Redstone -> hasMaterial = hasItem(Items.REDSTONE);
                    case Netherite -> hasMaterial = hasItem(Items.NETHERITE_INGOT);
                }
                if (hasTemplate && hasMaterial) return true;
            }
            case LEGGINGS -> {
                switch (leggingsTrim.get()) {
                    case Eye -> hasTemplate = hasItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Rib -> hasTemplate = hasItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Vex -> hasTemplate = hasItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Dune -> hasTemplate = hasItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Host -> hasTemplate = hasItem(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Tide -> hasTemplate = hasItem(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Ward -> hasTemplate = hasItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wild -> hasTemplate = hasItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Bolt -> hasMaterial = hasItem(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Flow -> hasMaterial = hasItem(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Coast -> hasTemplate = hasItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Snout -> hasTemplate = hasItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Spire -> hasTemplate = hasItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Raiser -> hasTemplate = hasItem(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Sentry -> hasTemplate = hasItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Shaper -> hasTemplate = hasItem(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Silence -> hasTemplate = hasItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wayfinder -> hasTemplate = hasItem(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
                }
                switch (leggingsTrimMaterial.get()) {
                    case Iron -> hasMaterial = hasItem(Items.IRON_INGOT);
                    case Gold -> hasMaterial = hasItem(Items.GOLD_INGOT);
                    case Lapis -> hasMaterial = hasItem(Items.LAPIS_LAZULI);
                    case Quartz -> hasMaterial = hasItem(Items.QUARTZ);
                    case Resin -> hasMaterial = hasItem(Items.RESIN_BRICK);
                    case Copper -> hasMaterial = hasItem(Items.COPPER_INGOT);
                    case Emerald -> hasMaterial = hasItem(Items.EMERALD);
                    case Diamond -> hasMaterial = hasItem(Items.DIAMOND);
                    case Amethyst -> hasMaterial = hasItem(Items.AMETHYST_SHARD);
                    case Redstone -> hasMaterial = hasItem(Items.REDSTONE);
                    case Netherite -> hasMaterial = hasItem(Items.NETHERITE_INGOT);
                }
                if (hasTemplate && hasMaterial) return true;
            }
            case CHESTPLATE -> {
                switch (chestplateTrim.get()) {
                    case Eye -> hasTemplate = hasItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Rib -> hasTemplate = hasItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Vex -> hasTemplate = hasItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Dune -> hasTemplate = hasItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Host -> hasTemplate = hasItem(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Tide -> hasTemplate = hasItem(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Ward -> hasTemplate = hasItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wild -> hasTemplate = hasItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Bolt -> hasMaterial = hasItem(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Flow -> hasMaterial = hasItem(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Coast -> hasTemplate = hasItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Snout -> hasTemplate = hasItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Spire -> hasTemplate = hasItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Raiser -> hasTemplate = hasItem(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Sentry -> hasTemplate = hasItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Shaper -> hasTemplate = hasItem(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Silence -> hasTemplate = hasItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
                    case Wayfinder -> hasTemplate = hasItem(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
                }
                switch (chestplateTrimMaterial.get()) {
                    case Iron -> hasMaterial = hasItem(Items.IRON_INGOT);
                    case Gold -> hasMaterial = hasItem(Items.GOLD_INGOT);
                    case Lapis -> hasMaterial = hasItem(Items.LAPIS_LAZULI);
                    case Quartz -> hasMaterial = hasItem(Items.QUARTZ);
                    case Resin -> hasMaterial = hasItem(Items.RESIN_BRICK);
                    case Copper -> hasMaterial = hasItem(Items.COPPER_INGOT);
                    case Emerald -> hasMaterial = hasItem(Items.EMERALD);
                    case Diamond -> hasMaterial = hasItem(Items.DIAMOND);
                    case Amethyst -> hasMaterial = hasItem(Items.AMETHYST_SHARD);
                    case Redstone -> hasMaterial = hasItem(Items.REDSTONE);
                    case Netherite -> hasMaterial = hasItem(Items.NETHERITE_INGOT);
                }
                if (hasTemplate && hasMaterial) return true;
            }
        }

        return false;
    }

    private boolean hasItem(Item needed) {
        if (mc.player == null) return false;
        if (!(mc.player.currentScreenHandler instanceof SmithingScreenHandler ss)) return false;

        for (int n = 0; n < mc.player.getInventory().main.size() + 4; n++) {
            ItemStack stack = ss.getSlot(n).getStack();
            if (stack.getItem() == needed) return true;
        }
        return false;
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        trimStack = null;
        notified = false;
        foundEquip = false;
        foundIngots = false;
        materialStack = null;
        equipmentStack = null;
        foundTemplates = false;
        processedSlots.clear();
        projectedEmpty.clear();
        currentlyLookingFor = null;
        resettingTemplates = false;
        resettingMaterials = false;
        exhaustedArmorTypes.clear();
    }

    @EventHandler
    private void onScreenOpened(OpenScreenEvent event) {
        if (event.screen instanceof SmithingScreen) {
            notified = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.getNetworkHandler() == null) return;
        if (mc.currentScreen == null) onDeactivate();
        if (!(mc.player.currentScreenHandler instanceof SmithingScreenHandler ss)) return;

        switch (moduleMode.get()) {
            case Packet -> {
                if (notified) return;
                ArrayDeque<ClickSlotC2SPacket> packetQueue = new ArrayDeque<>();

                boolean exhausted = false;
                while (!exhausted) {
                    ClickSlotC2SPacket packet = generateSmithingPacket(ss);

                    if (packet == null) {
                        exhausted = true;
                    } else if (packetQueue.size() >= packetLimit.get()) {
                        exhausted = true;
                        packetQueue.addLast(packet);
                        MsgUtil.sendModuleMsg("Packet limit was hit§c..! §7You may need to run the module again§c...", this.name);
                    } else packetQueue.addLast(packet);
                }

                if (debug.get()) {
                    MsgUtil.sendModuleMsg(
                        "Sending §e" + packetQueue.size() + " §7packets"
                            + StardustUtil.rCC() + "..!", this.name
                    );
                }
                while (!packetQueue.isEmpty()) {
                    ((ClientConnectionAccessor) mc.getNetworkHandler().getConnection()).invokeSendImmediately(
                        packetQueue.removeFirst(), null, true
                    );
                }

                finished();
            }
            case Interact -> {
                if (timer >= tickRate.get()) {
                    timer = 0;
                } else {
                    ++timer;
                    return;
                }

                if (resettingTemplates) {
                    InvUtils.shiftClick().slotId(SmithingScreenHandler.TEMPLATE_ID);
                    timer = tickRate.get() - 1;
                    resettingTemplates = false;
                    return;
                } else if (resettingMaterials) {
                    InvUtils.shiftClick().slotId(SmithingScreenHandler.MATERIAL_ID);
                    timer = tickRate.get() - 1;
                    resettingMaterials = false;
                    return;
                }
                switch (operatingMode.get()) {
                    case Trim -> {
                        ItemStack output = ss.getSlot(SmithingScreenHandler.OUTPUT_ID).getStack();

                        if (!output.isEmpty()) {
                            if (!(output.getItem() instanceof ArmorItem armor)) return;
                            EquipmentType armorType = getEquipmentType(armor);
                            if (output.contains(DataComponentTypes.TRIM)) {
                                ArmorTrim trimData = output.get(DataComponentTypes.TRIM);
                                String pattern = trimData.pattern().getIdAsString();
                                String material = trimData.material().getIdAsString();
                                switch (armorType) {
                                    case BOOTS -> {
                                        if (!bootsTrim.get().label.equals(pattern)) {
                                            foundTemplates = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.TEMPLATE_ID);
                                        } else if (!bootsTrimMaterial.get().label.equals(material)) {
                                            foundIngots = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.MATERIAL_ID);
                                        } else {
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.OUTPUT_ID);

                                            foundEquip = false;
                                            foundIngots = false;
                                            foundTemplates = false;
                                            if (ss.getSlot(SmithingScreenHandler.TEMPLATE_ID).getStack().getCount() >= 1) resettingTemplates = true;
                                            if (ss.getSlot(SmithingScreenHandler.MATERIAL_ID).getStack().getCount() >= 1) resettingMaterials = true;
                                        }
                                    }
                                    case HELMET -> {
                                        if (!helmetTrim.get().label.equals(pattern)) {
                                            foundTemplates = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.TEMPLATE_ID);
                                        } else if (!helmetTrimMaterial.get().label.equals(material)) {
                                            foundIngots = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.MATERIAL_ID);
                                        } else {
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.OUTPUT_ID);

                                            foundEquip = false;
                                            foundIngots = false;
                                            foundTemplates = false;
                                            if (ss.getSlot(SmithingScreenHandler.TEMPLATE_ID).getStack().getCount() >= 1) resettingTemplates = true;
                                            if (ss.getSlot(SmithingScreenHandler.MATERIAL_ID).getStack().getCount() >= 1) resettingMaterials = true;
                                        }
                                    }
                                    case LEGGINGS -> {
                                        if (!leggingsTrim.get().label.equals(pattern)) {
                                            foundTemplates = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.TEMPLATE_ID);
                                        } else if (!leggingsTrimMaterial.get().label.equals(material)) {
                                            foundIngots = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.MATERIAL_ID);
                                        } else {
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.OUTPUT_ID);

                                            foundEquip = false;
                                            foundIngots = false;
                                            foundTemplates = false;
                                            if (ss.getSlot(SmithingScreenHandler.TEMPLATE_ID).getStack().getCount() >= 1) resettingTemplates = true;
                                            if (ss.getSlot(SmithingScreenHandler.MATERIAL_ID).getStack().getCount() >= 1) resettingMaterials = true;
                                        }
                                    }
                                    case CHESTPLATE -> {
                                        if (!chestplateTrim.get().label.equals(pattern)) {
                                            foundTemplates = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.TEMPLATE_ID);
                                        } else if (!chestplateTrimMaterial.get().label.equals(material)) {
                                            foundIngots = false;
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.MATERIAL_ID);
                                        } else {
                                            InvUtils.shiftClick().slotId(SmithingScreenHandler.OUTPUT_ID);

                                            foundEquip = false;
                                            foundIngots = false;
                                            foundTemplates = false;
                                            if (ss.getSlot(SmithingScreenHandler.TEMPLATE_ID).getStack().getCount() >= 1) resettingTemplates = true;
                                            if (ss.getSlot(SmithingScreenHandler.MATERIAL_ID).getStack().getCount() >= 1) resettingMaterials = true;
                                        }
                                    }
                                }
                            } else {
                                foundEquip = false;
                                InvUtils.shiftClick().slotId(SmithingScreenHandler.EQUIPMENT_ID);

                                foundIngots = false;
                                foundTemplates = false;
                                resettingTemplates = true;
                                resettingMaterials = false;
                            }
                        } else if (!foundEquip) {
                            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                                ItemStack stack = ss.getSlot(n).getStack();
                                if (isValidEquipmentForTrimming(stack)) {
                                    foundEquip = true;
                                    InvUtils.shiftClick().slotId(n);
                                    break;
                                }
                            }
                            if (!foundEquip && !notified) {
                                MsgUtil.sendModuleMsg("§2§oNo armor left to trim§8§o.", this.name);
                                finished();
                            }
                        } else if (!foundIngots) {
                            ItemStack armorToTrim = ss.getSlot(SmithingScreenHandler.EQUIPMENT_ID).getStack();
                            if (!(armorToTrim.getItem() instanceof ArmorItem armor)) {
                                foundEquip = false;
                                resettingTemplates = true;
                                resettingMaterials = true;
                                InvUtils.shiftClick().slotId(SmithingScreenHandler.EQUIPMENT_ID);
                                LogUtil.error("Item in equipment slot was not armor..!", this.name);
                                return;
                            }
                            EquipmentType armorType = getEquipmentType(armor);
                            Item neededMaterial = getNeededMaterialItem(armorToTrim);

                            if (neededMaterial == null) {
                                LogUtil.error("neededMaterial was somehow null!");
                                return;
                            }
                            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                                ItemStack stack = ss.getSlot(n).getStack();
                                if (stack.isOf(neededMaterial)) {
                                    foundIngots = true;
                                    InvUtils.shiftClick().slotId(n);
                                    break;
                                }
                            }
                            if (!foundIngots && !notified) {
                                if (!exhaustedArmorTypes.contains(armorType)) {
                                    exhaustedArmorTypes.add(armorType);
                                    return;
                                }
                                MsgUtil.sendModuleMsg("§c§oNo valid trim materials left to use§8§o..!", this.name);
                                finished();
                            }
                        } else if (!foundTemplates) {
                            ItemStack armorToTrim = ss.getSlot(SmithingScreenHandler.EQUIPMENT_ID).getStack();
                            if (!(armorToTrim.getItem() instanceof ArmorItem armor)) {
                                foundEquip = false;
                                resettingTemplates = true;
                                resettingMaterials = true;
                                InvUtils.shiftClick().slotId(SmithingScreenHandler.EQUIPMENT_ID);
                                LogUtil.error("Item in equipment slot was not armor!", this.name);
                                return;
                            }

                            EquipmentType armorType = getEquipmentType(armor);
                            Item neededPattern = getNeededPatternItem(armorToTrim);
                            if (neededPattern == null) {
                                LogUtil.error("neededPattern was somehow null!", this.name);
                                return;
                            }
                            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                                ItemStack stack = ss.getSlot(n).getStack();
                                if (stack.getItem() == neededPattern) {
                                    foundTemplates = true;
                                    InvUtils.shiftClick().slotId(n);
                                    break;
                                }
                            }
                            if (!foundTemplates && !notified) {
                                if (!exhaustedArmorTypes.contains(armorType)) {
                                    exhaustedArmorTypes.add(armorType);
                                    return;
                                }
                                MsgUtil.sendModuleMsg("No valid trim templates left to use§c§o..!", this.name);
                                finished();
                            }
                        } else {
                            timer = tickRate.get() - 1;
                        }
                    }
                    case Upgrade -> {
                        ItemStack output = ss.getSlot(SmithingScreenHandler.OUTPUT_ID).getStack();
                        if (!output.isEmpty()) {
                            InvUtils.shiftClick().slotId(SmithingScreenHandler.OUTPUT_ID);

                            foundEquip = false;
                            int ingotsRemaining = ss.getSlot(SmithingScreenHandler.MATERIAL_ID).getStack().getCount();
                            int templatesRemaining = ss.getSlot(SmithingScreenHandler.TEMPLATE_ID).getStack().getCount();

                            if (ingotsRemaining == 0) foundIngots = false;
                            if (templatesRemaining == 0) foundTemplates = false;
                        } else if (!foundEquip) {
                            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                                ItemStack stack = ss.getSlot(n).getStack();
                                if (isValidEquipmentForUpgrading(stack)) {
                                    foundEquip = true;
                                    InvUtils.shiftClick().slotId(n);
                                    break;
                                }
                            }
                            if (!foundEquip && !notified) {
                                MsgUtil.sendModuleMsg("No gear left to upgrade§c..!", this.name);
                                finished();
                            }
                        }else if (!foundIngots) {
                            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                                ItemStack stack = ss.getSlot(n).getStack();
                                if (stack.getItem() == Items.NETHERITE_INGOT) {
                                    foundIngots = true;
                                    InvUtils.shiftClick().slotId(n);
                                    break;
                                }
                            }
                            if (!foundIngots && !notified) {
                                MsgUtil.sendModuleMsg("No netherite ingots left to use§c..!", this.name);
                                finished();
                            }
                        } else if (!foundTemplates) {
                            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                                ItemStack stack = ss.getSlot(n).getStack();
                                if (stack.getItem() == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                                    foundTemplates = true;
                                    InvUtils.shiftClick().slotId(n);
                                    break;
                                }
                            }
                            if (!foundTemplates && !notified) {
                                MsgUtil.sendModuleMsg("No netherite smithing templates left to use§c..!", this.name);
                                finished();
                            }
                        } else {
                            timer = tickRate.get() - 1;
                        }
                    }
                }
            }
        }
    }

    private void finished() {
        if (mc.player == null) {
            notified = true;
            return;
        }
        if (!notified) {
            if (chatFeedback) {
                MsgUtil.sendModuleMsg("Finished processing items" + StardustUtil.rCC() + "..!", this.name);
            }
            if (pingOnDone.get()) {
                mc.player.playSound(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    pingVolume.get().floatValue(),
                    ThreadLocalRandom.current().nextFloat(0.69f, 1.337f)
                );
            }
        }
        notified = true;
        if (closeOnDone.get()) mc.player.closeHandledScreen();
        if (disableOnDone.get()) toggle();
    }

    @SuppressWarnings("deprecation")
    private @Nullable ClickSlotC2SPacket generateSmithingPacket(SmithingScreenHandler handler) {
        if (mc.player == null) return null;
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
        if (trimStack != null && materialStack != null && equipmentStack != null) {
            // check if correct and take output

            ItemStack armorToTrim = equipmentStack;
            if (operatingMode.get().equals(SmithingMode.Trim) && !(armorToTrim.getItem() instanceof ArmorItem)) {
                LogUtil.error("Item in equipment slot was not armor§c..!", this.name);
                return null;
            }

            Item neededPattern;
            Item neededMaterial;
            if (operatingMode.get().equals(SmithingMode.Trim)) {
                neededPattern = getNeededPatternItem(armorToTrim);
                neededMaterial = getNeededMaterialItem(armorToTrim);
            } else {
                neededMaterial = Items.NETHERITE_INGOT;
                neededPattern = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
            }

            if (operatingMode.get().equals(SmithingMode.Trim) && !trimStack.isOf(neededPattern)) {
                if (debug.get()) {
                    MsgUtil.sendModuleMsg(
                        "Wrong trim stack for armor of type "
                            + getEquipmentType((ArmorItem) armorToTrim.getItem()).name() + "§e..!", this.name
                    );
                }
                changedSlots.put(SmithingScreenHandler.TEMPLATE_ID, ItemStack.EMPTY);

                int shiftClickTargetSlot = predictEmptySlot(handler);
                if (shiftClickTargetSlot == -1) {
                    MsgUtil.sendModuleMsg("Failed to predict empty target slot§c...!", this.name);
                    return null;
                }

                changedSlots.put(shiftClickTargetSlot, trimStack.copy());
                trimStack = null;

                if (debug.get()) {
                    MsgUtil.sendModuleMsg("Moving incorrect template item back to inventory§e..!", this.name);
                }
                return new ClickSlotC2SPacket(
                    handler.syncId, handler.getRevision(), SmithingScreenHandler.TEMPLATE_ID, 0,
                    SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
                );
            }
            if (operatingMode.get().equals(SmithingMode.Trim) && !materialStack.isOf(neededMaterial)) {
                if (debug.get()) {
                    MsgUtil.sendModuleMsg(
                        "Wrong material stack for armor of type "
                            + getEquipmentType((ArmorItem) armorToTrim.getItem()).name() + "§e..!", this.name
                    );
                }
                changedSlots.put(SmithingScreenHandler.MATERIAL_ID, ItemStack.EMPTY);

                int shiftClickTargetSlot = predictEmptySlot(handler);
                if (shiftClickTargetSlot == -1) {
                    MsgUtil.sendModuleMsg("Failed to predict empty target slot§c....!", this.name);
                    return null;
                }

                changedSlots.put(shiftClickTargetSlot, materialStack.copy());
                materialStack = null;

                if (debug.get()) {
                    MsgUtil.sendModuleMsg("Moving incorrect material item back to inventory§e..!", this.name);
                }
                return new ClickSlotC2SPacket(
                    handler.syncId, handler.getRevision(), SmithingScreenHandler.MATERIAL_ID, 0,
                    SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
                );
            }

            // take output
            int trimCount = trimStack.getCount();
            int materialCount = materialStack.getCount();
            changedSlots.put(SmithingScreenHandler.OUTPUT_ID, ItemStack.EMPTY);
            changedSlots.put(SmithingScreenHandler.EQUIPMENT_ID, ItemStack.EMPTY);

            if (trimCount - 1 > 0) {
                ItemStack newTrimStack = trimStack.copyWithCount(trimCount - 1);
                changedSlots.put(SmithingScreenHandler.TEMPLATE_ID, newTrimStack);
                trimStack = newTrimStack;
            } else {
                changedSlots.put(SmithingScreenHandler.TEMPLATE_ID, ItemStack.EMPTY);
                trimStack = null;
            }

            if (materialCount - 1 > 0) {
                ItemStack newMaterialStack = materialStack.copyWithCount(materialCount - 1);
                changedSlots.put(SmithingScreenHandler.MATERIAL_ID, newMaterialStack);
                materialStack = newMaterialStack;
            } else {
                changedSlots.put(SmithingScreenHandler.MATERIAL_ID, ItemStack.EMPTY);
                materialStack = null;
            }

            int shiftClickTargetSlot = predictEmptySlot(handler);
            if (shiftClickTargetSlot == -1) {
                MsgUtil.sendModuleMsg("Failed to predict empty target slot§c..!", this.name);
                return null;
            }

            if (operatingMode.get().equals(SmithingMode.Trim)) {
                // todo: fabricate trim components for output stack if needed (currently not needed)

                ItemStack output = new ItemStack(
                    armorToTrim.getItem().getRegistryEntry(),
                    armorToTrim.getCount(), armorToTrim.getComponentChanges()
                );

                changedSlots.put(shiftClickTargetSlot, output);
            } else {
                ItemStack output = getUpgradedItem(armorToTrim);
                changedSlots.put(shiftClickTargetSlot, output);
            }

            if (debug.get()) MsgUtil.sendModuleMsg("Generated output packet§a..!", this.name);
            equipmentStack = null;
            return new ClickSlotC2SPacket(
                handler.syncId, handler.getRevision(), 3, 0,
                SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
            );
        } else if (equipmentStack == null) {
            // look for valid equipment stack

            if (currentlyLookingFor == null && operatingMode.get().equals(SmithingMode.Trim)) {
                currentlyLookingFor = computeLookingFor();
                if (debug.get() && currentlyLookingFor != null) {
                    MsgUtil.sendModuleMsg("Currently looking for equipment of type: §e" + currentlyLookingFor.name(), this.name);
                }
            }
            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getStack();
                if ((operatingMode.get().equals(SmithingMode.Trim) && isValidEquipmentForTrimming(stack)) || (operatingMode.get().equals(SmithingMode.Upgrade) && isValidEquipmentForUpgrading(stack))) {
                    equipmentStack = stack;
                    processedSlots.add(n);
                    projectedEmpty.add(n);
                    processedSlots.add(SmithingScreenHandler.EQUIPMENT_ID);

                    changedSlots.put(SmithingScreenHandler.EQUIPMENT_ID, stack);
                    changedSlots.put(n, ItemStack.EMPTY);

                    if (trimStack != null && materialStack != null) {
                        if (operatingMode.get().equals(SmithingMode.Trim)) {
                            ItemStack output = new ItemStack(
                                stack.getItem().getRegistryEntry(),
                                stack.getCount(), stack.getComponentChanges()
                            );

                            // todo: fabricate trim components for output stack if needed (currently not needed)
//                            ComponentChanges.Builder cb = new ComponentChanges.Builder()
//                                .add(DataComponentTypes.TRIM, )

                            changedSlots.put(SmithingScreenHandler.OUTPUT_ID, output);
                        } else {
                            ItemStack output = getUpgradedItem(equipmentStack);
                            changedSlots.put(SmithingScreenHandler.OUTPUT_ID, output);
                        }
                    }

                    if (debug.get() && currentlyLookingFor != null) {
                        MsgUtil.sendModuleMsg(
                            "Found valid armor piece of type: §e"
                                + currentlyLookingFor.getName() + "§a..!", this.name
                        );
                    }
                    return new ClickSlotC2SPacket(
                        handler.syncId, handler.getRevision(), n, 0,
                        SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
                    );
                }
            }

            if (operatingMode.get().equals(SmithingMode.Trim)) {
                if (debug.get() && currentlyLookingFor != null) {
                    MsgUtil.sendModuleMsg(
                        "Exhausted all available armor of type: §e"
                            + currentlyLookingFor.name(), this.name
                    );
                }
                exhaustedArmorTypes.add(currentlyLookingFor);
                currentlyLookingFor = null;
                if (exhaustedArmorTypes.size() < 4) {
                    if (debug.get()) {
                        MsgUtil.sendModuleMsg("Recursing to search for the next type§5..!", this.name);
                    }
                    return generateSmithingPacket(handler);
                } else if (debug.get()) {
                    MsgUtil.sendModuleMsg(
                        "Exhausted all armor of all types, no more armor to trim§c..!", this.name
                    );
                }
            }
        } else if (materialStack == null) {
            ItemStack armorToTrim = equipmentStack;
            if (operatingMode.get().equals(SmithingMode.Trim) && !(armorToTrim.getItem() instanceof ArmorItem)) {
                LogUtil.error("Item in equipment slot was not armor..!", this.name);
                return null;
            }
            Item needed;
            if (operatingMode.get().equals(SmithingMode.Trim)) {
                needed = getNeededMaterialItem(armorToTrim);
            } else {
                needed = Items.NETHERITE_INGOT;
            }
            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getStack();
                if (stack.isOf(needed)) {
                    materialStack = stack;
                    processedSlots.add(n);
                    projectedEmpty.add(n);
                    processedSlots.add(SmithingScreenHandler.MATERIAL_ID);

                    changedSlots.put(SmithingScreenHandler.MATERIAL_ID, stack);
                    changedSlots.put(n, ItemStack.EMPTY);

                    if (trimStack != null) {
                        if (operatingMode.get().equals(SmithingMode.Trim)) {
                            // todo: fabricate trim components for output stack if needed (currently not needed)

                            ItemStack output = new ItemStack(
                                stack.getItem().getRegistryEntry(),
                                stack.getCount(), stack.getComponentChanges()
                            );

                            changedSlots.put(SmithingScreenHandler.OUTPUT_ID, output);
                        } else {
                            ItemStack output = getUpgradedItem(equipmentStack);
                            changedSlots.put(SmithingScreenHandler.OUTPUT_ID, output);
                        }
                    }

                    return new ClickSlotC2SPacket(
                        handler.syncId, handler.getRevision(), n, 0,
                        SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
                    );
                }
            }
        } else {
            ItemStack armorToTrim = equipmentStack;
            if (operatingMode.get().equals(SmithingMode.Trim) && !(armorToTrim.getItem() instanceof ArmorItem)) {
                LogUtil.error("Item in equipment slot was not armor..!", this.name);
                return null;
            }
            Item needed;
            if (operatingMode.get().equals(SmithingMode.Trim)) {
                needed = getNeededPatternItem(armorToTrim);
            } else {
                needed = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
            }
            for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getStack();
                if (stack.isOf(needed)) {
                    trimStack = stack;
                    processedSlots.add(n);
                    projectedEmpty.add(n);
                    processedSlots.add(SmithingScreenHandler.TEMPLATE_ID);
                    changedSlots.put(SmithingScreenHandler.TEMPLATE_ID, stack);
                    changedSlots.put(n, ItemStack.EMPTY);

                    if (operatingMode.get().equals(SmithingMode.Trim)) {
                        // todo: fabricate trim components for output stack if needed (currently not)

                        ItemStack output = new ItemStack(
                            stack.getItem().getRegistryEntry(),
                            stack.getCount(), stack.getComponentChanges()
                        );

                        changedSlots.put(SmithingScreenHandler.OUTPUT_ID, output);
                    } else if (equipmentStack != null) {
                        ItemStack output = getUpgradedItem(equipmentStack);
                        changedSlots.put(SmithingScreenHandler.OUTPUT_ID, output);
                    }

                    return new ClickSlotC2SPacket(
                        handler.syncId, handler.getRevision(), n, 0,
                        SlotActionType.QUICK_MOVE, ItemStack.EMPTY, changedSlots
                    );
                }
            }
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    private ItemStack getUpgradedItem(ItemStack original) {
        if (original.isOf(Items.DIAMOND_HELMET)) {
            return new ItemStack(
                Items.NETHERITE_HELMET.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_CHESTPLATE)) {
            return new ItemStack(
                Items.NETHERITE_CHESTPLATE.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_LEGGINGS)) {
            return new ItemStack(
                Items.NETHERITE_LEGGINGS.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_BOOTS)) {
            return new ItemStack(
                Items.NETHERITE_BOOTS.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_SWORD)) {
            return new ItemStack(
                Items.NETHERITE_SWORD.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_PICKAXE)) {
            return new ItemStack(
                Items.NETHERITE_PICKAXE.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_AXE)) {
            return new ItemStack(
                Items.NETHERITE_AXE.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_SHOVEL)) {
            return new ItemStack(
                Items.NETHERITE_SHOVEL.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else if (original.isOf(Items.DIAMOND_HOE)) {
            return new ItemStack(
                Items.NETHERITE_HOE.getRegistryEntry(),
                original.getCount(), original.getComponentChanges()
            );
        } else {
            return original;
        }
    }

    private EquipmentType computeLookingFor() {
        if (!exhaustedArmorTypes.contains(EquipmentType.HELMET)) return EquipmentType.HELMET;
        else if (!exhaustedArmorTypes.contains(EquipmentType.CHESTPLATE)) return EquipmentType.CHESTPLATE;
        else if (!exhaustedArmorTypes.contains(EquipmentType.LEGGINGS)) return EquipmentType.LEGGINGS;
        else return EquipmentType.BOOTS;
    }

    private int predictEmptySlot(SmithingScreenHandler handler) {
        if (mc.player == null) return -1;
        for (int n = mc.player.getInventory().main.size() + 3; n >= 4; n--) {
            if (processedSlots.contains(n) && !projectedEmpty.contains(n)) continue;
            if (projectedEmpty.contains(n)) {
                projectedEmpty.rem(n);
                return n;
            } else if (handler.getSlot(n).getStack().isEmpty()) {
                processedSlots.add(n);
                return n;
            }
        }
        return -1;
    }

    private @Nullable Item getNeededPatternItem(ItemStack armorToTrim) {
        Item neededPattern = null;
        switch (getEquipmentType((ArmorItem) armorToTrim.getItem())) {
            case BOOTS -> {
                switch (bootsTrim.get()) {
                    case Eye -> neededPattern = Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Rib -> neededPattern = Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Vex -> neededPattern = Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Dune -> neededPattern = Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Host -> neededPattern = Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Tide -> neededPattern = Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Ward -> neededPattern = Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wild -> neededPattern = Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Bolt -> neededPattern = Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Flow -> neededPattern = Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Coast -> neededPattern = Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Snout -> neededPattern = Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Spire -> neededPattern = Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Raiser -> neededPattern = Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Sentry -> neededPattern = Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Shaper -> neededPattern = Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Silence -> neededPattern = Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wayfinder -> neededPattern = Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE;
                }
            }
            case HELMET -> {
                switch (helmetTrim.get()) {
                    case Eye -> neededPattern = Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Rib -> neededPattern = Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Vex -> neededPattern = Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Dune -> neededPattern = Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Host -> neededPattern = Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Tide -> neededPattern = Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Ward -> neededPattern = Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wild -> neededPattern = Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Bolt -> neededPattern = Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Flow -> neededPattern = Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Coast -> neededPattern = Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Snout -> neededPattern = Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Spire -> neededPattern = Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Raiser -> neededPattern = Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Sentry -> neededPattern = Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Shaper -> neededPattern = Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Silence -> neededPattern = Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wayfinder -> neededPattern = Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE;
                }
            }
            case LEGGINGS -> {
                switch (leggingsTrim.get()) {
                    case Eye -> neededPattern = Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Rib -> neededPattern = Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Vex -> neededPattern = Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Dune -> neededPattern = Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Host -> neededPattern = Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Tide -> neededPattern = Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Ward -> neededPattern = Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wild -> neededPattern = Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Bolt -> neededPattern = Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Flow -> neededPattern = Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Coast -> neededPattern = Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Snout -> neededPattern = Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Spire -> neededPattern = Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Raiser -> neededPattern = Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Sentry -> neededPattern = Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Shaper -> neededPattern = Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Silence -> neededPattern = Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wayfinder -> neededPattern = Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE;
                }
            }
            case CHESTPLATE -> {
                switch (chestplateTrim.get()) {
                    case Eye -> neededPattern = Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Rib -> neededPattern = Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Vex -> neededPattern = Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Dune -> neededPattern = Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Host -> neededPattern = Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Tide -> neededPattern = Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Ward -> neededPattern = Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wild -> neededPattern = Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Bolt -> neededPattern = Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Flow -> neededPattern = Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Coast -> neededPattern = Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Snout -> neededPattern = Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Spire -> neededPattern = Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Raiser -> neededPattern = Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Sentry -> neededPattern = Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Shaper -> neededPattern = Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Silence -> neededPattern = Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE;
                    case Wayfinder -> neededPattern = Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE;
                }
            }
        }

        return neededPattern;
    }

    private @Nullable Item getNeededMaterialItem(ItemStack armorToTrim) {
        Item neededMaterial = null;
        switch (getEquipmentType((ArmorItem) armorToTrim.getItem())) {
            case BOOTS -> {
                switch (bootsTrimMaterial.get()) {
                    case Gold -> neededMaterial = Items.GOLD_INGOT;
                    case Iron -> neededMaterial = Items.IRON_INGOT;
                    case Lapis -> neededMaterial = Items.LAPIS_LAZULI;
                    case Resin -> neededMaterial = Items.RESIN_BRICK;
                    case Copper -> neededMaterial = Items.COPPER_INGOT;
                    case Quartz -> neededMaterial = Items.QUARTZ;
                    case Diamond -> neededMaterial = Items.DIAMOND;
                    case Emerald -> neededMaterial = Items.EMERALD;
                    case Amethyst -> neededMaterial = Items.AMETHYST_SHARD;
                    case Redstone -> neededMaterial = Items.REDSTONE;
                    case Netherite -> neededMaterial = Items.NETHERITE_INGOT;
                }
            }
            case HELMET -> {
                switch (helmetTrimMaterial.get()) {
                    case Gold -> neededMaterial = Items.GOLD_INGOT;
                    case Iron -> neededMaterial = Items.IRON_INGOT;
                    case Lapis -> neededMaterial = Items.LAPIS_LAZULI;
                    case Resin -> neededMaterial = Items.RESIN_BRICK;
                    case Copper -> neededMaterial = Items.COPPER_INGOT;
                    case Quartz -> neededMaterial = Items.QUARTZ;
                    case Diamond -> neededMaterial = Items.DIAMOND;
                    case Emerald -> neededMaterial = Items.EMERALD;
                    case Amethyst -> neededMaterial = Items.AMETHYST_SHARD;
                    case Redstone -> neededMaterial = Items.REDSTONE;
                    case Netherite -> neededMaterial = Items.NETHERITE_INGOT;
                }
            }
            case LEGGINGS -> {
                switch (leggingsTrimMaterial.get()) {
                    case Gold -> neededMaterial = Items.GOLD_INGOT;
                    case Iron -> neededMaterial = Items.IRON_INGOT;
                    case Lapis -> neededMaterial = Items.LAPIS_LAZULI;
                    case Resin -> neededMaterial = Items.RESIN_BRICK;
                    case Copper -> neededMaterial = Items.COPPER_INGOT;
                    case Quartz -> neededMaterial = Items.QUARTZ;
                    case Diamond -> neededMaterial = Items.DIAMOND;
                    case Emerald -> neededMaterial = Items.EMERALD;
                    case Amethyst -> neededMaterial = Items.AMETHYST_SHARD;
                    case Redstone -> neededMaterial = Items.REDSTONE;
                    case Netherite -> neededMaterial = Items.NETHERITE_INGOT;
                }
            }
            case CHESTPLATE -> {
                switch (chestplateTrimMaterial.get()) {
                    case Gold -> neededMaterial = Items.GOLD_INGOT;
                    case Iron -> neededMaterial = Items.IRON_INGOT;
                    case Lapis -> neededMaterial = Items.LAPIS_LAZULI;
                    case Resin -> neededMaterial = Items.RESIN_BRICK;
                    case Copper -> neededMaterial = Items.COPPER_INGOT;
                    case Quartz -> neededMaterial = Items.QUARTZ;
                    case Diamond -> neededMaterial = Items.DIAMOND;
                    case Emerald -> neededMaterial = Items.EMERALD;
                    case Amethyst -> neededMaterial = Items.AMETHYST_SHARD;
                    case Redstone -> neededMaterial = Items.REDSTONE;
                    case Netherite -> neededMaterial = Items.NETHERITE_INGOT;
                }
            }
        }

        return neededMaterial;
    }
}
