package dev.stardust.modules;

import java.util.*;
import dev.stardust.Stardust;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import javax.annotation.Nullable;
import net.minecraft.fluid.Fluids;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import java.util.function.Predicate;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.LivingEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.ai.TargetPredicate;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.entity.passive.AxolotlEntity;
import meteordevelopment.meteorclient.utils.Utils;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.entity.passive.TropicalFishEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class AxolotlTools extends Module {
    public AxolotlTools() {
        super(Stardust.CATEGORY, "AxolotlTools", "Extrasensory perception for axolotl variants, auto-collector & auto-breeder.");
    }

    public enum InteractionMode { Full, Trigger }
    public enum EspMode { Sides, Lines, Both, None }
    public enum AxolotlMode { None, Breed, Catch, Release }
    public enum VariantBehavior { @SuppressWarnings("unused") Esp, Interact, Both, None }

    private final SettingGroup sgEsp = settings.createGroup("Esp Settings");
    private final SettingGroup sgAuto = settings.createGroup("AutoCatch/Breed Settings");
    private final SettingGroup sgVariantChoices = settings.createGroup("Variant Settings");

    private final Setting<Boolean> espVariants = sgEsp.add(
        new BoolSetting.Builder()
            .name("Esp")
            .description("Extrasensory perception for axolotl variants.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> espTracers = sgEsp.add(
        new BoolSetting.Builder()
            .name("Tracers")
            .defaultValue(true)
            .visible(espVariants::get)
            .build()
    );

    private final Setting<EspMode> espMode = sgEsp.add(
        new EnumSetting.Builder<EspMode>()
            .name("Wireframe Mode")
            .defaultValue(EspMode.Both)
            .visible(espVariants::get)
            .build()
    );

    private final Setting<Integer> sidesAlpha = sgEsp.add(
        new IntSetting.Builder()
            .name("Sides Alpha")
            .range(0, 255)
            .sliderRange(0 ,255)
            .defaultValue(69)
            .visible(espVariants::get)
            .build()
    );

    private final Setting<Integer> linesAlpha = sgEsp.add(
        new IntSetting.Builder()
            .name("Lines Alpha")
            .range(0, 255)
            .sliderRange(0, 255)
            .defaultValue(137)
            .visible(espVariants::get)
            .build()
    );

    private final Setting<AxolotlMode> axolotlMode = sgAuto.add(
        new EnumSetting.Builder<AxolotlMode>()
            .name("Axolotl Mode")
            .description("Axolotl interaction mode. Catch them, breed them, or do nothing (which allows catching fish instead.)")
            .defaultValue(AxolotlMode.None)
            .onChanged(it -> {
                if (it != AxolotlMode.None) this.disableFishModes();
            })
            .build()
    );

    private final Setting<Boolean> catchBabies = sgAuto.add(
        new BoolSetting.Builder()
            .name("Catch Baby Axolotls")
            .description("Automatically catch nearby baby axolotls in water buckets (works in catch mode.)")
            .defaultValue(false)
            .visible(() -> axolotlMode.get().equals(AxolotlMode.Catch))
            .build()
    );

    private final Setting<Boolean> onlyCatchBabies = sgAuto.add(
        new BoolSetting.Builder()
            .name("Only Catch Babies")
            .defaultValue(false)
            .visible(() -> catchBabies.get() && catchBabies.isVisible())
            .build()
    );

    private final Setting<Boolean> feedBabies = sgAuto.add(
        new BoolSetting.Builder()
            .name("Feed Baby Axolotls")
            .description("Feed baby axolotls buckets of tropical fish to make them grow up faster (works in breed mode.)")
            .defaultValue(false)
            .visible(() -> axolotlMode.get().equals(AxolotlMode.Breed))
            .build()
    );

    private final Setting<Boolean> onlyFeedBabies = sgAuto.add(
        new BoolSetting.Builder()
            .name("Only Feed Babies")
            .description("Feed baby axolotls buckets of tropical fish to make them grow up faster (works in breed mode.)")
            .defaultValue(false)
            .visible(() -> feedBabies.get() && feedBabies.isVisible())
            .build()
    );

    private final Setting<InteractionMode> interactionMode = sgAuto.add(
        new EnumSetting.Builder<InteractionMode>()
            .name("Interaction Mode")
            .description("Full uses spoofed rotations while Trigger handles the interaction when you look at a valid entity.")
            .defaultValue(InteractionMode.Full)
            .build()
    );

    private final Setting<Boolean> fillBuckets = sgAuto.add(
        new BoolSetting.Builder()
            .name("Fill Buckets")
            .description("Automatically fill empty buckets before catching axolotls or tropical fish.")
            .defaultValue(false)
            .visible(() -> interactionMode.get().equals(InteractionMode.Full))
            .build()
    );

    private final Setting<Boolean> emptyBuckets = sgAuto.add(
        new BoolSetting.Builder()
            .name("Empty Buckets")
            .description("Automatically empty buckets of water after feeding axolotls the fish they contained.")
            .defaultValue(false)
            .visible(() -> interactionMode.get().equals(InteractionMode.Full))
            .build()
    );

    private final Setting<Boolean> catchFish = sgAuto.add(
        new BoolSetting.Builder()
            .name("Catch Tropical Fish")
            .description("Automatically catch nearby tropical fish in water buckets.")
            .defaultValue(true)
            .visible(() -> axolotlMode.get().equals(AxolotlMode.None))
            .build()
    );

    private final Setting<Boolean> fishFarm = sgAuto.add(
        new BoolSetting.Builder()
            .name("Farm Tropical Fish")
            .description("Automatically afk-farm buckets of tropical fish (requires a farming setup.)")
            .defaultValue(false)
            .visible(() -> catchFish.isVisible() && catchFish.get())
            .onChanged(it -> {
                if (it) interactionMode.set(InteractionMode.Trigger);
                else interactionMode.set(InteractionMode.Full);
            })
            .build()
    );

    private final Setting<Integer> tickRate = sgAuto.add(
        new IntSetting.Builder()
            .name("Tick Rate")
            .description("Lower values have a higher chance of rejecting & desyncing your interactions. 10+ recommended for Full mode.")
            .range(2, 10000)
            .sliderRange(5, 100)
            .defaultValue(10)
            .build()
    );

    private final Setting<VariantBehavior> interactPink = sgVariantChoices.add(
        new EnumSetting.Builder<VariantBehavior>()
            .name("Pink Variant")
            .description("Esp and/or automatically interact with nearby pink axolotl variants.")
            .defaultValue(VariantBehavior.Both)
            .onChanged(it -> {
                if (it == VariantBehavior.Both || it == VariantBehavior.Interact) {
                    this.interactVariants.add(AxolotlEntity.Variant.LUCY.toString());
                } else this.interactVariants.remove(AxolotlEntity.Variant.LUCY.toString());
            })
            .build()
    );

    private final Setting<VariantBehavior> interactWild = sgVariantChoices.add(
        new EnumSetting.Builder<VariantBehavior>()
            .name("Brown Variant")
            .description("Esp and/or automatically interact with nearby brown axolotl variants.")
            .defaultValue(VariantBehavior.Both)
            .onChanged(it -> {
                if (it == VariantBehavior.Both || it == VariantBehavior.Interact) {
                    this.interactVariants.add(AxolotlEntity.Variant.WILD.toString());
                } else this.interactVariants.remove(AxolotlEntity.Variant.WILD.toString());
            })
            .build()
    );

    private final Setting<VariantBehavior> interactGold = sgVariantChoices.add(
        new EnumSetting.Builder<VariantBehavior>()
            .name("Gold Variant")
            .description("Esp and/or automatically interact with nearby gold axolotl variants.")
            .defaultValue(VariantBehavior.Both)
            .onChanged(it -> {
                if (it == VariantBehavior.Both || it == VariantBehavior.Interact) {
                    this.interactVariants.add(AxolotlEntity.Variant.GOLD.toString());
                } else this.interactVariants.remove(AxolotlEntity.Variant.GOLD.toString());
            })
            .build()
    );

    private final Setting<VariantBehavior> interactCyan = sgVariantChoices.add(
        new EnumSetting.Builder<VariantBehavior>()
            .name("Cyan Variant")
            .description("Esp and/or automatically interact with nearby cyan axolotl variants.")
            .defaultValue(VariantBehavior.Both)
            .onChanged(it -> {
                if (it == VariantBehavior.Both || it == VariantBehavior.Interact) {
                    this.interactVariants.add(AxolotlEntity.Variant.CYAN.toString());
                } else this.interactVariants.remove(AxolotlEntity.Variant.CYAN.toString());
            })
            .build()
    );

    private final Setting<VariantBehavior> interactBlue = sgVariantChoices.add(
        new EnumSetting.Builder<VariantBehavior>()
            .name("Blue Variant")
            .description("Esp and/or automatically interact with nearby blue axolotl variants.")
            .defaultValue(VariantBehavior.Both)
            .onChanged(it -> {
                if (it == VariantBehavior.Both || it == VariantBehavior.Interact) {
                    this.interactVariants.add(AxolotlEntity.Variant.BLUE.toString());
                } else this.interactVariants.remove(AxolotlEntity.Variant.BLUE.toString());
            })
            .build()
    );

    private int timer = 0;
    private int rotPriority = 69420;
    private final Set<String> interactVariants = new HashSet<>();

    private void disableFishModes() {
        fishFarm.set(false);
        catchFish.set(false);
    }

    private boolean hasEmptySlots() {
        if (mc.player == null) return false;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            if (mc.player.getInventory().getStack(n).isEmpty()) return true;
        }
        return false;
    }

    private boolean hasNoValidBucket(Item bucketType) {
        if (mc.player == null) return true;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            if (mc.player.getInventory().getStack(n).getItem() == bucketType) return false;
        }
        return true;
    }

    private boolean trySwapValidBucket(Item bucketType) {
        if (mc.player == null) return false;
        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if (stack.getItem() == bucketType) {
                if (n < 9) InvUtils.swap(n, false);
                else InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos getNearbyWaterSource(boolean toEmpty) {
        if (mc.world == null || mc.player == null) return null;
        for (BlockPos pos : BlockPos.iterateOutwards(mc.player.getBlockPos(), 4, toEmpty ? 1 : 4, 4)) {
            if (mc.world.getFluidState(pos).getFluid() == Fluids.WATER) return pos;
        }
        return null;
    }

    private <T extends LivingEntity> boolean tryInteractMobFull(T entity, Item bucketType) {
        if (mc.interactionManager == null) return true;
        if (mc.player == null || mc.world == null) return true;

        for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
            ItemStack stack = mc.player.getInventory().getStack(n);
            if (!(stack.getItem() == bucketType)) continue;

            if (n != mc.player.getInventory().selectedSlot) {
                if (n < 9) InvUtils.swap(n, false);
                else InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);
            }
            AtomicReference<ActionResult> result = new AtomicReference<>();
            Rotations.rotate(
                Rotations.getYaw(entity),
                Rotations.getPitch(entity, Target.Body), rotPriority,
                () -> result.set(mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND))
            );
            ++rotPriority;
            return result.get() == ActionResult.SUCCESS || result.get() == ActionResult.CONSUME;
        }

        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
            Text.of("§8<§c§o✨§r§8> §4§oNo valid bucket types found in inventory§8§o."),
            "noBucketsFound".hashCode()
        );

        return false;
    }

    private <T extends LivingEntity> boolean tryInteractMobTrigger(T entity, Item bucketType) {
        if (mc.interactionManager == null) return true;
        if (mc.player == null || mc.world == null) return true;

        ItemStack currentStack = mc.player.getMainHandStack();
        if (currentStack.getItem() != bucketType) {
            boolean foundBucket = false;
            for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                ItemStack stack = mc.player.getInventory().getStack(n);
                if (stack.getItem() == bucketType) {
                    foundBucket = true;
                    if (n < 9) InvUtils.swap(n, false);
                    else InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);
                    break;
                }
            }
            if (!foundBucket) {
                if (!fishFarm.get()) ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                    Text.of("§8<§c§o✨§r§8> §4§oNo valid bucket types found in inventory§8§o."),
                    "noBucketsFound".hashCode()
                );
                return false;
            }
        }
        ActionResult result = mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);

        return result == ActionResult.SUCCESS || result == ActionResult.CONSUME;
    }

    @Override
    public void onActivate() {
        switch (interactPink.get()) {
            case Both, Interact -> interactVariants.add(AxolotlEntity.Variant.LUCY.toString());
        }
        switch (interactWild.get()) {
            case Both, Interact -> interactVariants.add(AxolotlEntity.Variant.WILD.toString());
        }
        switch (interactGold.get()) {
            case Both, Interact -> interactVariants.add(AxolotlEntity.Variant.GOLD.toString());
        }
        switch (interactCyan.get()) {
            case Both, Interact -> interactVariants.add(AxolotlEntity.Variant.CYAN.toString());
        }
        switch (interactBlue.get()) {
            case Both, Interact -> interactVariants.add(AxolotlEntity.Variant.BLUE.toString());
        }
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        rotPriority = 69420;
        interactVariants.clear();
    }

    @EventHandler private void onTick(TickEvent.Pre event) {
        if (mc.interactionManager == null) return;
        if (mc.player == null || mc.world == null) return;
        if (axolotlMode.get() == AxolotlMode.None && !catchFish.get()) return;

        ItemStack current = mc.player.getInventory().getMainHandStack();
        if ((current.isFood() || Utils.isThrowable(current.getItem())) && mc.player.getItemUseTime() > 0) {
            ++timer;
            return;
        }

        if (timer >= tickRate.get()) {
            timer = 0;
            List<AxolotlEntity> axolotls = new ArrayList<>();
            List<TropicalFishEntity> tropicalFishes = new ArrayList<>();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof AxolotlEntity axolotl) axolotls.add(axolotl);
                else if (entity instanceof TropicalFishEntity fishy) tropicalFishes.add(fishy);
            }

            if (axolotlMode.get() != AxolotlMode.None && !axolotls.isEmpty()) {
                switch (interactionMode.get()) {
                    case Full -> {
                        if (axolotlMode.get() == AxolotlMode.Release) {
                            BlockPos source = getNearbyWaterSource(true);
                            if (current.getItem() != Items.AXOLOTL_BUCKET) {
                                if (trySwapValidBucket(Items.AXOLOTL_BUCKET)) {
                                    timer = tickRate.get()-1;
                                    return;
                                }
                            } else if (source != null) {
                                Rotations.rotate(
                                    Rotations.getYaw(source),
                                    Rotations.getPitch(source), 69420,
                                    () -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND)
                                );
                                ++timer;
                                return;
                            }
                        }

                        axolotls = axolotls
                            .stream()
                            .filter(ax -> ax.getBlockPos().isWithinDistance(mc.player.getBlockPos(), 3))
                            .toList();

                        TargetPredicate predicate = TargetPredicate.createNonAttackable();
                        Predicate<LivingEntity> targetTest = ax -> interactVariants.contains(((AxolotlEntity) ax).getVariant().toString());

                        targetTest = targetTest
                            .and(ax -> (axolotlMode.get() == AxolotlMode.Catch
                                && (catchBabies.get() ? !onlyCatchBabies.get() || ax.isBaby() : !ax.isBaby())));

                        targetTest = targetTest
                            .or(ax -> (axolotlMode.get() == AxolotlMode.Breed
                                && (feedBabies.get() ? !onlyFeedBabies.get() || ax.isBaby() : !ax.isBaby())));

                        predicate.setPredicate(targetTest);
                        AxolotlEntity target = mc.world.getClosestEntity(
                            axolotls, predicate, mc.player,
                            mc.player.getX(), mc.player.getY(), mc.player.getZ()
                        );

                        if (target != null) {
                            if (axolotlMode.get() == AxolotlMode.Catch) {
                                if (fillBuckets.get() && hasNoValidBucket(Items.WATER_BUCKET) && current.getItem() != Items.BUCKET) {
                                    if (trySwapValidBucket(Items.BUCKET)) {
                                        timer = tickRate.get()-1;
                                        return;
                                    }
                                } else if (fillBuckets.get() && hasNoValidBucket(Items.WATER_BUCKET) && current.getItem() == Items.BUCKET) {
                                    BlockPos source = getNearbyWaterSource(false);
                                    if (source != null && (hasEmptySlots() || current.getCount() == 1)) {
                                        Rotations.rotate(
                                            Rotations.getYaw(source),
                                            Rotations.getPitch(source), 69420,
                                            () -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND)
                                        );
                                        ++timer;
                                        return;
                                    } else {
                                        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                                            Text.of("§8<§6§o✨§r§8> §4§oFull inventory prevents auto-filling buckets§8§o!"),
                                            "fullInventoryWarning".hashCode()
                                        );
                                    }
                                }
                                if (tryInteractMobFull(target, Items.WATER_BUCKET)) return;
                            } else if (axolotlMode.get() == AxolotlMode.Breed) {
                                if (tryInteractMobFull(target, Items.TROPICAL_FISH_BUCKET)) return;
                                else if (emptyBuckets.get() && hasNoValidBucket(Items.TROPICAL_FISH_BUCKET)) {
                                    if (current.getItem() != Items.WATER_BUCKET) {
                                        if (trySwapValidBucket(Items.WATER_BUCKET)) {
                                            timer = tickRate.get()-1;
                                            return;
                                        }
                                    } else {
                                        BlockPos source = getNearbyWaterSource(true);
                                        if (source != null) {
                                            Rotations.rotate(
                                                Rotations.getYaw(source),
                                                Rotations.getPitch(source), 69420,
                                                () -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND)
                                            );
                                            ++timer;
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case Trigger -> {
                        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
                            if (hit.getEntity() instanceof AxolotlEntity axolotl) {
                                if (axolotlMode.get() == AxolotlMode.Catch) {
                                    if (!catchBabies.get() && axolotl.isBaby()) return;
                                    else if (catchBabies.get() && onlyCatchBabies.get() && !axolotl.isBaby()) return;

                                    if (!interactVariants.contains(axolotl.getVariant().toString())) return;
                                    else if (tryInteractMobTrigger(axolotl, Items.WATER_BUCKET)) return;
                                } else if (axolotlMode.get() == AxolotlMode.Breed) {
                                    if (!feedBabies.get() && axolotl.isBaby()) return;
                                    if (feedBabies.get() && onlyFeedBabies.get() && !axolotl.isBaby()) return;
                                    if (!interactVariants.contains(axolotl.getVariant().toString())) return;
                                    if (tryInteractMobTrigger(axolotl, Items.TROPICAL_FISH_BUCKET)) return;
                                    else {
                                        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                                            Text.of("§8<§5§o✨§r§8> §4§oThat axolotl isn't ready to eat yet§8§o!"),
                                            "breedingCooldownUpdate".hashCode()
                                        );
                                    }
                                }
                            }
                        } else if (axolotlMode.get() == AxolotlMode.Release) {
                            Entity camera = mc.cameraEntity;

                            if (camera == null) return;
                            HitResult result = camera.raycast(3, mc.getTickDelta(), true);
                            if (result.getType() == HitResult.Type.BLOCK) {
                                BlockHitResult hit = (BlockHitResult) result;
                                if (mc.world.getFluidState(hit.getBlockPos()).getFluid() == Fluids.WATER) {
                                    if (current.getItem() != Items.AXOLOTL_BUCKET) {
                                        if (trySwapValidBucket(Items.AXOLOTL_BUCKET)) {
                                            timer = tickRate.get() - 1;
                                            return;
                                        }
                                    } else {
                                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (catchFish.get() && !tropicalFishes.isEmpty()) {
                switch (interactionMode.get()) {
                    case Full -> {
                        tropicalFishes = tropicalFishes
                            .stream()
                            .filter(fishy -> fishy.getBlockPos().isWithinDistance(mc.player.getBlockPos(), 3))
                            .toList();

                        TropicalFishEntity target = mc.world.getClosestEntity(
                            tropicalFishes, TargetPredicate.createNonAttackable(), mc.player,
                            mc.player.getX(), mc.player.getY(), mc.player.getZ()
                        );

                        if (target != null) {
                            if (tryInteractMobFull(target, Items.WATER_BUCKET)) return;
                        }
                    }
                    case Trigger -> {
                        // Original farm video: https://www.youtube.com/watch?v=pdDIrU4CdnU
                        // Using this you can run a modified version of Rays Works' afk tropical fish farm on 2b2t.
                        // Just skip the storage pool and build hoppers under your tracks and closest walls instead.
                        // Then fill your inventory with stacks of empty buckets (but leave 1 hotbar slot empty.)
                        // You can then run the farm without holding down RMB and the fish buckets will end up
                        // in your hoppers, which you can hook up to a storage system below the minecart rails.
                        // Make sure to align your crosshair so that you can't interact with anything but water & fish.
                        // This code will keep the cycle going until your inventory runs out of empty buckets.
                        if (fishFarm.get()) {
                            if (current.getItem() != Items.BUCKET && hasNoValidBucket(Items.WATER_BUCKET)) {
                                trySwapValidBucket(Items.BUCKET);
                            }

                            if (current.getItem() == Items.BUCKET && hasNoValidBucket(Items.WATER_BUCKET)) {
                                if (hasEmptySlots() || current.getCount() == 1) {
                                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                                } else {
                                    ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(
                                        Text.of("§8<§6§o✨§r§8> §4§oFull inventory prevents auto-filling buckets§8§o!"),
                                        "fullInventoryWarning".hashCode()
                                    );
                                }
                                ++timer;
                                return;
                            }

                            if (!hasNoValidBucket(Items.TROPICAL_FISH_BUCKET)) {
                                for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                                    if (mc.player.getInventory().getStack(n).getItem() == Items.TROPICAL_FISH_BUCKET) {
                                        InvUtils.drop().slot(n);
                                        ++timer;
                                        return;
                                    }
                                }
                            }
                        }

                        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
                            if (hit.getEntity() instanceof TropicalFishEntity fishy) {
                                if (tryInteractMobTrigger(fishy, Items.WATER_BUCKET)) return;
                            }
                        }
                    }
                }
            }
        }
        ++timer;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!espVariants.get()) return;
        if (mc.player == null || mc.world == null) return;

        List<AxolotlEntity> axolotls = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AxolotlEntity axolotl) axolotls.add(axolotl);
        }

        axolotls = axolotls
            .stream()
            .filter(ax -> ax.getBlockPos()
                .isWithinDistance(
                    mc.player.getBlockPos(),
                    mc.options.getViewDistance().getValue() * 16
                )
            ).toList();

        for (AxolotlEntity axolotl : axolotls) {
            SettingColor lineColor;
            SettingColor sideColor;
            switch (axolotl.getVariant()) {
                case LUCY -> {
                    switch (interactPink.get()) {
                        case None, Interact -> { continue; }
                    }
                    sideColor = new SettingColor(224, 173, 203, sidesAlpha.get());
                    lineColor = new SettingColor(147, 81, 110, linesAlpha.get());
                }
                case WILD -> {
                    switch (interactWild.get()) {
                        case None, Interact -> { continue; }
                    }
                    sideColor = new SettingColor(136, 100, 69, sidesAlpha.get());
                    lineColor = new SettingColor(75, 57, 40, linesAlpha.get());
                }
                case GOLD -> {
                    switch (interactGold.get()) {
                        case None, Interact -> { continue; }
                    }
                    sideColor = new SettingColor(241, 198, 26, sidesAlpha.get());
                    lineColor = new SettingColor(197, 150, 19, linesAlpha.get());
                }
                case CYAN -> {
                    switch (interactCyan.get()) {
                        case None, Interact -> { continue; }
                    }
                    sideColor = new SettingColor(198, 209, 224, sidesAlpha.get());
                    lineColor = new SettingColor(77, 138, 174, linesAlpha.get());
                }
                case BLUE -> {
                    switch (interactBlue.get()) {
                        case None, Interact -> { continue; }
                    }
                    sideColor = new SettingColor(52, 40, 121, sidesAlpha.get());
                    lineColor = new SettingColor(188, 114, 34, linesAlpha.get());
                }
                default -> {
                    sideColor = new SettingColor(109, 181, 148, sidesAlpha.get());
                    lineColor = new SettingColor(77, 124, 103, linesAlpha.get());
                }
            }

            switch (espMode.get()) {
                case Lines -> WireframeEntityRenderer.render(event, axolotl, 1, sideColor, lineColor, ShapeMode.Lines);
                case Sides -> WireframeEntityRenderer.render(event, axolotl, 1, sideColor, lineColor, ShapeMode.Sides);
                case Both -> WireframeEntityRenderer.render(event, axolotl, 1, sideColor, lineColor, ShapeMode.Both);
                case None -> {} // do nothing
            }
            if (espTracers.get()) {
                event.renderer.line(
                    RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                    axolotl.getBoundingBox().getCenter().x,
                    axolotl.getBoundingBox().getCenter().y,
                    axolotl.getBoundingBox().getCenter().z,
                    lineColor
                );
            }
        }
    }
}
