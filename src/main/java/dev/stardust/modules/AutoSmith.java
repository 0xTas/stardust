package dev.stardust.modules;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.item.*;
import dev.stardust.Stardust;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.trim.ArmorTrim;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.registry.entry.RegistryEntry;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AutoSmith extends Module {
    public AutoSmith() { super(Stardust.CATEGORY, "AutoSmith", "Automatically upgrade gear or trim armor in smithing tables."); }

    private final SettingGroup modeSettings = settings.createGroup("Smithing Mode");
    private final SettingGroup trimSettings = settings.createGroup("Armor Trims");

    public enum SmithingMode {
        Trim, Upgrade
    }
    public enum ArmorMaterials {
        Iron, Gold, Chain, Turtle, Leather, Diamond, Netherite;
        public boolean materialEquals(RegistryEntry<net.minecraft.item.ArmorMaterial> material) {
            return switch (this) {
                case Iron -> material == net.minecraft.item.ArmorMaterials.IRON;
                case Gold -> material == net.minecraft.item.ArmorMaterials.GOLD;
                case Chain -> material == net.minecraft.item.ArmorMaterials.CHAIN;
                case Turtle -> material == net.minecraft.item.ArmorMaterials.TURTLE;
                case Leather -> material == net.minecraft.item.ArmorMaterials.LEATHER;
                case Diamond -> material == net.minecraft.item.ArmorMaterials.DIAMOND;
                case Netherite -> material == net.minecraft.item.ArmorMaterials.NETHERITE;
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

    private final Setting<SmithingMode> modeSetting = modeSettings.add(
        new EnumSetting.Builder<SmithingMode>()
            .name("Smithing Mode")
            .defaultValue(SmithingMode.Upgrade)
            .build()
    );

    private final Setting<Boolean> overwriteTrims = modeSettings.add(
        new BoolSetting.Builder()
            .name("Overwrite Trims")
            .description("Trim armor pieces which already contain a different trim pattern or material.")
            .defaultValue(false)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> helmetType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("Helmet Armor Type")
            .description("Which type of helmet to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> helmetTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("Helmet Armor Trim")
            .description("Which armor trim to apply onto helmets.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> helmetTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("Helmet Trim Material")
            .description("What material to use for helmet armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> chestplateType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("Chestplate Armor Type")
            .description("Which type of chestplates to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> chestplateTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("Chestplate Armor Trim")
            .description("Which armor trim to apply onto chestplates.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> chestplateTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("Chestplate Trim Material")
            .description("What material to use for chestplate armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> leggingsType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("Leggings Armor Type")
            .description("Which type of leggings to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> leggingsTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("Leggings Armor Trim")
            .description("Which armor trim to apply onto leggings.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> leggingsTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("Leggings Trim Material")
            .description("What material to use for leggings armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );

    private final Setting<ArmorMaterials> bootsType = trimSettings.add(
        new EnumSetting.Builder<ArmorMaterials>()
            .name("Boots Armor Type")
            .description("Which type of boots to apply trims to.")
            .defaultValue(ArmorMaterials.Netherite)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<ArmorTrims> bootsTrim = trimSettings.add(
        new EnumSetting.Builder<ArmorTrims>()
            .name("Boots Armor Trim")
            .description("Which armor trim to apply onto boots.")
            .defaultValue(ArmorTrims.Eye)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );
    private final Setting<TrimMaterial> bootsTrimMaterial = trimSettings.add(
        new EnumSetting.Builder<TrimMaterial>()
            .name("Boots Trim Material")
            .description("What material to use for boots armor trims.")
            .defaultValue(TrimMaterial.Amethyst)
            .visible(() -> modeSetting.get() == SmithingMode.Trim)
            .build()
    );

    // See WorldMixin.java
    public final Setting<Boolean> muteSmithy = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Mute Smithing Table")
            .description("Mute the smithing table sounds.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Close Screen")
            .description("Automatically close the crafting screen when no more gear can be upgraded.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Disable on Done")
            .description("Automatically disable the module when no more gear can be upgraded.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Sound Ping")
            .description("Play a sound cue when no more gear can be trimmed or upgraded.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> pingVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("Ping Volume")
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(1.0)
            .build()
    );

    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("Delay")
            .description("You may need to increase this if your ping is high.")
            .range(2, 100)
            .sliderRange(2, 20)
            .defaultValue(4)
            .build()
    );

    private int timer = 0;
    private boolean notified = false;
    private boolean foundEquip = false;
    private boolean foundIngots = false;
    private boolean foundTemplates = false;
    private boolean resettingTemplates = false;
    private boolean resettingMaterials = false;
    private final List<ArmorItem.Type> exhaustedArmorTypes = new ArrayList<>();

    private boolean isValidEquipmentForUpgrading(ItemStack stack) {
        return (stack.getItem() instanceof ArmorItem armor && armor.getMaterial() == net.minecraft.item.ArmorMaterials.DIAMOND)
            || (stack.getItem() instanceof ToolItem tool && tool.getMaterial() == ToolMaterials.DIAMOND);
    }

    private boolean isValidEquipmentForTrimming(ItemStack stack) { // TODO: add 1.21 trims
        if (stack.getItem() instanceof ArmorItem armor) {
            boolean correctMaterial = false;
            RegistryEntry<ArmorMaterial> armorMaterial = armor.getMaterial();
            if (exhaustedArmorTypes.contains(armor.getType())) return false;
            switch (armor.getType()) {
                case BOOTS -> correctMaterial = bootsType.get().materialEquals(armorMaterial);
                case HELMET -> correctMaterial = helmetType.get().materialEquals(armorMaterial);
                case LEGGINGS -> correctMaterial = leggingsType.get().materialEquals(armorMaterial);
                case CHESTPLATE -> correctMaterial = chestplateType.get().materialEquals(armorMaterial);
            }

            if (!correctMaterial) return false;
            if (stack.contains(DataComponentTypes.TRIM)) {
                if (!overwriteTrims.get()) return false;
                String pattern = stack.get(DataComponentTypes.TRIM).getPattern().getIdAsString();
                String material = stack.get(DataComponentTypes.TRIM).getMaterial().getIdAsString();

                switch (armor.getType()) {
                    case BOOTS -> {
                        if (!bootsTrim.get().label.equals(pattern) || !bootsTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(ArmorItem.Type.BOOTS)) return true;
                        }
                    }
                    case HELMET -> {
                        if (!helmetTrim.get().label.equals(pattern) || !helmetTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(ArmorItem.Type.HELMET)) return true;
                        }
                    }
                    case LEGGINGS -> {
                        if (!leggingsTrim.get().label.equals(pattern) || !leggingsTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(ArmorItem.Type.LEGGINGS)) return true;
                        }
                    }
                    case CHESTPLATE -> {
                        if (!chestplateTrim.get().label.equals(pattern) || !chestplateTrimMaterial.get().label.equals(material)) {
                            if (hasRequiredMaterialsForTrimming(ArmorItem.Type.CHESTPLATE)) return true;
                        }
                    }
                }
            } else return true;
        }
        return false;
    }

    private boolean hasRequiredMaterialsForTrimming(ArmorItem.Type type) {
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

        for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
            ItemStack stack = ss.getSlot(n).getStack();
            if (stack.getItem() == needed) return true;
        }
        return false;
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        notified = false;
        foundEquip = false;
        foundIngots = false;
        foundTemplates = false;
        resettingTemplates = false;
        resettingMaterials = false;
        exhaustedArmorTypes.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.currentScreen == null) onDeactivate();
        if (!(mc.player.currentScreenHandler instanceof SmithingScreenHandler ss)) return;

        ++timer;
        if (timer >= tickRate.get()) {
            timer = 0;
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
            switch (modeSetting.get()) {
                case Trim -> {
                    ItemStack output = ss.getSlot(SmithingScreenHandler.OUTPUT_ID).getStack();
                    if (!output.isEmpty()) {
                        if (output.contains(DataComponentTypes.TRIM)) {
                            ArmorTrim trimData = output.get(DataComponentTypes.TRIM);
                            String pattern = trimData.getPattern().getIdAsString();
                            String material = trimData.getMaterial().getIdAsString();
                            switch (((ArmorItem) output.getItem()).getType()) {
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
                            notified = true;
                            if (disableOnDone.get()) toggle();
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1f);
                            mc.player.sendMessage(
                                Text.of("§8<§a§o✨§8> §2§oNo armor left to trim§8§o.")
                            );
                        }
                    } else if (!foundIngots) {
                        Item neededMaterial = null;
                        ItemStack armorToTrim = ss.getSlot(SmithingScreenHandler.EQUIPMENT_ID).getStack();
                        if (!(armorToTrim.getItem() instanceof ArmorItem)) {
                            foundEquip = false;
                            resettingTemplates = true;
                            resettingMaterials = true;
                            InvUtils.shiftClick().slotId(SmithingScreenHandler.EQUIPMENT_ID);
                            Stardust.LOG.error("[AutoSmith] Item in equipment slot was not armor!");
                            return;
                        }
                        switch (((ArmorItem) armorToTrim.getItem()).getType()) {
                            case BOOTS -> {
                                switch (bootsTrimMaterial.get()) {
                                    case Gold -> neededMaterial = Items.GOLD_INGOT;
                                    case Iron -> neededMaterial = Items.IRON_INGOT;
                                    case Lapis -> neededMaterial = Items.LAPIS_LAZULI;
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

                        if (neededMaterial == null) {
                            Stardust.LOG.error("neededMaterial was somehow null!");
                            return;
                        }
                        for (int n = 4; n < mc.player.getInventory().main.size() + 4; n++) {
                            ItemStack stack = ss.getSlot(n).getStack();
                            if (stack.getItem() == neededMaterial) {
                                foundIngots = true;
                                InvUtils.shiftClick().slotId(n);
                                break;
                            }
                        }
                        if (!foundIngots && !notified) {
                            if (!exhaustedArmorTypes.contains(((ArmorItem) armorToTrim.getItem()).getType())) {
                                exhaustedArmorTypes.add(((ArmorItem) armorToTrim.getItem()).getType());
                                return;
                            }
                            notified = true;
                            if (disableOnDone.get()) toggle();
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1f);
                            mc.player.sendMessage(
                                Text.of("§8<§a§o✨§8> §c§oNo valid trim materials left to use§8§o!")
                            );
                        }
                    } else if (!foundTemplates) {
                        Item neededPattern = null;
                        ItemStack armorToTrim = ss.getSlot(SmithingScreenHandler.EQUIPMENT_ID).getStack();
                        if (!(armorToTrim.getItem() instanceof ArmorItem)) {
                            foundEquip = false;
                            resettingTemplates = true;
                            resettingMaterials = true;
                            InvUtils.shiftClick().slotId(SmithingScreenHandler.EQUIPMENT_ID);
                            Stardust.LOG.error("[AutoSmith] Item in equipment slot was not armor!");
                            return;
                        }
                        switch (((ArmorItem) armorToTrim.getItem()).getType()) {
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
                        if (neededPattern == null) {
                            Stardust.LOG.error("neededPattern was somehow null!");
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
                            if (!exhaustedArmorTypes.contains(((ArmorItem) armorToTrim.getItem()).getType())) {
                                exhaustedArmorTypes.add(((ArmorItem) armorToTrim.getItem()).getType());
                                return;
                            }
                            notified = true;
                            if (disableOnDone.get()) toggle();
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1f);
                            mc.player.sendMessage(
                                Text.of("§8<§a§o✨§8> §c§oNo valid trim templates left to use§8§o!")
                            );
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
                            notified = true;
                            if (disableOnDone.get()) toggle();
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1f);
                            mc.player.sendMessage(
                                Text.of("§8<§a§o✨§8> §2§oNo gear left to upgrade§8§o.")
                            );
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
                            notified = true;
                            if (disableOnDone.get()) toggle();
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1f);
                            mc.player.sendMessage(
                                Text.of("§8<§a§o✨§8> §c§oNo netherite ingots left to use§8§o!")
                            );
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
                            notified = true;
                            if (disableOnDone.get()) toggle();
                            if (closeOnDone.get()) mc.player.closeHandledScreen();
                            if (pingOnDone.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1f);
                            mc.player.sendMessage(
                                Text.of("§8<§a§o✨§8> §c§oNo netherite smithing templates left to use§8§o!")
                            );
                        }
                    } else {
                        timer = tickRate.get() - 1;
                    }
                }
            }
        }
    }
}
