package dev.stardust.modules;

import java.util.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import net.minecraft.item.*;
import dev.stardust.Stardust;
import net.minecraft.block.*;
import net.minecraft.text.Text;
import java.util.stream.Stream;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.nbt.NbtOps;
import javax.annotation.Nullable;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.util.DyeColor;
import java.util.stream.Collectors;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NbtCompound;
import dev.stardust.util.StardustUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import java.nio.file.StandardOpenOption;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.math.Direction;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryKey;
import com.mojang.serialization.DataResult;
import net.minecraft.block.entity.SignText;
import net.minecraft.util.shape.VoxelShape;
import net.fabricmc.loader.api.FabricLoader;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.client.network.ServerInfo;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class SignHistorian extends Module {
    public SignHistorian() {
        super(Stardust.CATEGORY, "SignHistorian", "Records & restores broken or modified signs.");
    }

    private final String BLACKLIST_FILE = "meteor-client/sign-historian/content-blacklist.txt";

    private final SettingGroup sgESP = settings.createGroup("ESP Settings");
    private final SettingGroup sgSigns = settings.createGroup("Signs Settings");
    private final SettingGroup sgBlacklist = settings.createGroup("Content Blacklist");
    private final SettingGroup sgPrevention = settings.createGroup("Grief Prevention");

    private final Setting<Integer> espRange = sgESP.add(
        new IntSetting.Builder()
            .name("ESP Range")
            .description("Range in blocks to render broken or modified signs.")
            .range(16, 512)
            .sliderRange(16, 256)
            .defaultValue(128)
            .build()
    );

    private final Setting<ESPBlockData> destroyedSettings = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("Destroyed/Missing Signs ESP")
            .description("Tip: left-click on the block a ghost sign is rendered on to view its original text content in your chat.")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(255, 42, 0, 255),
                    new SettingColor(255, 42, 0, 44),
                    true,
                    new SettingColor(255, 42, 0, 137)
                )
            )
            .build()
    );

    private final Setting<ESPBlockData> modifiedSettings = sgESP.add(
        new GenericSetting.Builder<ESPBlockData>()
            .name("Modified Signs ESP")
            .description("Tip: right-click on a modified sign to view its original text content in your chat.")
            .defaultValue(
                new ESPBlockData(
                    ShapeMode.Both,
                    new SettingColor(237, 255, 42, 255),
                    new SettingColor(237, 255, 42, 44),
                    true,
                    new SettingColor(237, 255, 42, 137)
                )
            )
            .build()
    );

    private final Setting<Boolean> strictSetting = sgSigns.add(
        new BoolSetting.Builder()
            .name("Strict Mode")
            .description("Only consider signs to be restored if they have the same dye color and glow ink values as the original.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> persistenceSetting = sgSigns.add(
        new BoolSetting.Builder()
            .name("Persistence")
            .description("Save sign data to a file in order to persist SignHistorian's powers across play-sessions.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    if (this.serverSigns.isEmpty()) {
                        initOrLoadFromSignFile();
                    } else {
                        for (Pair<SignBlockEntity, BlockState> entry : this.serverSigns.values()) {
                            this.saveSignToFile(entry.getLeft(), entry.getRight());
                        }
                        initOrLoadFromSignFile();
                    }
                }
            })
            .build()
    );

    private final Setting<Boolean> ignoreBrokenSetting = sgSigns.add(
        new BoolSetting.Builder()
            .name("Ignore Purposefully Broken")
            .description("Ignores signs you break on purpose (but still tracks them in case you change your mind later.)")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> waxRestoration = sgSigns.add(
        new BoolSetting.Builder()
            .name("Wax Restored Signs")
            .description("Automatically waxes signs that SignHistorian has restored.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> contentBlacklist = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("Content Blacklist")
            .description("Ignore signs that contain specific words or phrases (line-separated list in sign-historian/content-blacklist.txt)")
            .defaultValue(false)
            .onChanged(it -> {
                if (it && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) {
                    this.blacklisted.clear();
                    initBlacklistText();
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Please write one blacklisted item for each line of the file."));
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7Spaces and other punctuation will be treated literally."));
                        mc.player.sendMessage(Text.of("§8<"+StardustUtil.rCC()+"§o✨§r§8> §7You must toggle this setting or the module after updating the blacklist's contents."));
                    }
                }
            })
            .build()
    );

    private final Setting<Boolean> openBlacklistFile = sgBlacklist.add(
        new BoolSetting.Builder()
            .name("Open Blacklist File")
            .description("Open the content-blacklist.txt file.")
            .defaultValue(false)
            .onChanged(it -> {
                if (it) {
                    if (StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) StardustUtil.openFile(mc, BLACKLIST_FILE);
                    resetBlacklistFileSetting();
                }
            })
            .build()
    );

    private final Setting<Boolean> griefPrevention = sgPrevention.add(
        new BoolSetting.Builder()
            .name("Creeper Alarm")
            .description("Attempts to warn you when nearby signs are in danger of an approaching creeper.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatNotification = sgPrevention.add(
        new BoolSetting.Builder()
            .name("Chat Notification")
            .description("Warns you in chat when nearby signs are in danger of an approaching creeper.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> alarmVolume = sgPrevention.add(
        new DoubleSetting.Builder()
            .name("Volume")
            .sliderMax(0)
            .sliderMax(200)
            .defaultValue(0)
            .build()
    );

    private final Setting<Boolean> creeperTracers = sgPrevention.add(
        new BoolSetting.Builder()
            .name("Tracers")
            .description("Display tracers leading to approaching creepers.")
            .defaultValue(true)
            .build()
    );

    private int timer = 0;
    private int dyeSlot = -1;
    private int pingTicks = 0;
    private int gracePeriod = 0;
    private int rotationPriority = 69420;
    private boolean didDisableWaxAura = false;
    private @Nullable BlockPos lastTargetedSign = null;
    private @Nullable RegistryKey<World> currentDim = null;
    private final HashSet<String> blacklisted = new HashSet<>();
    private final Set<BlockPos> signsBrokenByPlayer = new HashSet<>();
    private final Set<SignBlockEntity> modifiedSigns = new HashSet<>();
    private final Set<SignBlockEntity> destroyedSigns = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToWax = new HashSet<>();
    private final HashSet<SignBlockEntity> signsToGlowInk = new HashSet<>();
    private final HashMap<Integer, BlockPos> trackedCreepers = new HashMap<>();
    private final HashSet<CreeperEntity> approachingCreepers = new HashSet<>();
    private final HashMap<SignBlockEntity, DyeColor> signsToColor = new HashMap<>();
    private final Map<BlockPos, Pair<SignBlockEntity, BlockState>> serverSigns = new HashMap<>();

    private void initBlacklistText() {
        File blackListFile = FabricLoader.getInstance().getGameDir().resolve(BLACKLIST_FILE).toFile();

        try(Stream<String> lineStream = Files.lines(blackListFile.toPath())) {
            blacklisted.addAll(lineStream.toList());
        }catch (Exception err) {
            Stardust.LOG.error("[Stardust] Failed to read from "+ blackListFile.getAbsolutePath() +"! - Why:\n"+err);
        }
    }

    private void resetBlacklistFileSetting() { openBlacklistFile.set(false); }

    private void initOrLoadFromSignFile() {
        if (mc.world == null || mc.getNetworkHandler() == null) return;
        Path historianFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client/sign-historian");

        try {
            //noinspection ResultOfMethodCallIgnored
            historianFolder.toFile().mkdirs();
            ServerInfo server = mc.getNetworkHandler().getServerInfo();
            if (server == null) return;

            String address = server.address;
            String dimKey;
            if (currentDim != null) dimKey = currentDim.getValue().toString().replace("minecraft:", "");
            else dimKey = mc.world.getRegistryKey().getValue().toString().replace("minecraft:", "");
            Path signsFile = historianFolder.resolve( dimKey+"."+address+".signs");
            if (signsFile.toFile().exists()) {
                readSignsFromFile(signsFile);
            } else if (signsFile.toFile().createNewFile()) {
                if (mc.player != null) mc.player.sendMessage(
                    Text.of("§8<"+ StardustUtil.rCC()+"✨§8> [§5SignHistorian§8] §7Sign data will be saved to §2§o"+signsFile.getFileName()+" §7in your §7§ometeor-client/sign-historian folder.")
                );
                readSignsFromFile(signsFile);
            }
        } catch (Exception err) {
            Stardust.LOG.error(err.toString());
        }
    }

    private void readSignsFromFile(Path signsFile) {
        try(Stream<String> lineStream = Files.lines(signsFile)) {
            List<String> entries = lineStream.toList();
            for (String sign : entries) {
                try {
                    String[] parts = sign.split(" -\\|- ");
                    if (parts.length != 2) continue;
                    NbtCompound reconstructed = StringNbtReader.parse(parts[0].trim());
                    NbtCompound stateReconstructed = StringNbtReader.parse(parts[1].trim());
                    BlockPos bPos = BlockEntity.posFromNbt(reconstructed);

                    DataResult<BlockState> result = BlockState.CODEC.parse(NbtOps.INSTANCE, stateReconstructed);
                    BlockState state = result.result().orElse(null);

                    if (state == null) continue;
                    BlockEntity be = BlockEntity.createFromNbt(bPos, state, reconstructed);

                    if (be instanceof SignBlockEntity sbeReconstructed) {
                        if (!serverSigns.containsKey(bPos)) {
                            serverSigns.put(bPos, new Pair<>(sbeReconstructed, sbeReconstructed.getCachedState()));
                        }
                    }
                } catch (Exception err) {
                    Stardust.LOG.error("Failed to parse SignBlockEntity Nbt: "+err);
                }
            }
        }catch (Exception e) {
            Stardust.LOG.error(e.toString());
        }
    }

    private void writeSignToFile(NbtCompound metadata, NbtCompound cachedState, Path signsFile) {
        try {
            Files.writeString(signsFile, metadata+" -|- "+cachedState+"\n", StandardOpenOption.APPEND);
        } catch (Exception err) {
            Stardust.LOG.error("[Stardust] "+err);
        }
    }

    private void saveSignToFile(SignBlockEntity sign, BlockState state) {
        if (mc.world == null || mc.getNetworkHandler() == null) return;
        Path historianFolder = FabricLoader.getInstance().getGameDir().resolve("meteor-client/sign-historian");

        try {
            NbtCompound stateNbt = NbtHelper.fromBlockState(state);
            NbtCompound metadata = sign.createNbtWithIdentifyingData();

            //noinspection ResultOfMethodCallIgnored
            historianFolder.toFile().mkdirs();
            ServerInfo server = mc.getNetworkHandler().getServerInfo();
            if (server == null) return;

            String address = server.address.replace(":", "_");
            String dimKey;
            if (currentDim != null) dimKey = currentDim.getValue().toString().replace("minecraft:", "");
            else dimKey = mc.world.getRegistryKey().getValue().toString().replace("minecraft:", "");
            Path signsFile = historianFolder.resolve(dimKey+"."+address+".signs");
            if (signsFile.toFile().exists()) {
                writeSignToFile(metadata, stateNbt, signsFile);
            } else if (signsFile.toFile().createNewFile()) {
                writeSignToFile(metadata, stateNbt, signsFile);
            }
        } catch (Exception err) {
            Stardust.LOG.error("[Stardust] "+err);
        }
    }

    private Vec3d getTracerOffset(BlockPos pos, BlockState state) {
        double offsetX;
        double offsetY;
        double offsetZ;
        try {
            if (state.getBlock() instanceof SignBlock || state.getBlock() instanceof HangingSignBlock) {
                offsetX = pos.getX() + .5;
                offsetY = pos.getY() + .5;
                offsetZ = pos.getZ() + .5;
            } else if (state.getBlock() instanceof WallSignBlock || state.getBlock() instanceof WallHangingSignBlock) {
                Direction facing = state.get(WallSignBlock.FACING);
                switch (facing) {
                    case NORTH -> {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .937;
                    }
                    case EAST -> {
                        offsetX = pos.getX() + .1337;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    }
                    case SOUTH -> {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .1337;
                    }
                    case WEST -> {
                        offsetX = pos.getX() + .937;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    }
                    default -> {
                        offsetX = pos.getX() + .5;
                        offsetY = pos.getY() + .5;
                        offsetZ = pos.getZ() + .5;
                    }
                }
            } else {
                offsetX = pos.getX() + .5;
                offsetY = pos.getY() + .5;
                offsetZ = pos.getZ() + .5;
            }
        } catch (Exception err) {
            offsetX = pos.getX() + .5;
            offsetY = pos.getY() + .5;
            offsetZ = pos.getZ() + .5;
        }

        return new Vec3d(offsetX, offsetY, offsetZ);
    }

    @Nullable
    private BlockPos getTargetedSign() {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return null;
        HitResult trace = player.raycast(7, mc.getTickDelta(), false);
        if (trace != null) {
            BlockPos pos = ((BlockHitResult) trace).getBlockPos();
            if (mc.world.getBlockEntity(pos) instanceof SignBlockEntity) return pos;
        }

        return null;
    }

    // See AbstractSignEditScreenMixin.java
    public @Nullable SignText getRestoration(SignBlockEntity sign) {
        if (!serverSigns.containsKey(sign.getPos())) return null;
        Pair<SignBlockEntity, BlockState> data = serverSigns.get(sign.getPos());

        if (!destroyedSigns.contains(data.getLeft())) return null;
        if (contentBlacklist.get() && containsBlacklistedText(data.getLeft())) return null;
        if (ignoreBrokenSetting.get() && signsBrokenByPlayer.contains(sign.getPos())) return null;

        SignBlockEntity sbe = data.getLeft();
        Text[] restoration = new Text[4];
        for (int n = 0; n < data.getLeft().getFrontText().getMessages(false).length; n++) {
            // Signs placed in 1.8 - 1.12 (the majority of them) are "technically" irreplaceable due to metadata differences.
            // You might say that they're the *new* old signs. Either way you can tell that they've been (re)placed after 1.19.
            // To compensate for this, I'll hide a SignHistorian watermark in the NBT data which should clear up any confusion :]
            if (sbe.createNbt().toString().contains("{\"extra\":[") && n == 3) {
                StringBuilder sb = new StringBuilder();
                int lineLen = mc.textRenderer.getWidth(sbe.getFrontText().getMessage(n, false).getString());
                int spaceLeftHalved = (90 - lineLen) / 2; // center original text

                while (mc.textRenderer.getWidth(sb.toString()) < spaceLeftHalved) sb.append(" ");
                sb.append(sbe.getFrontText().getMessage(n, false).getString());
                while (mc.textRenderer.getWidth(sb.toString()) < 91) sb.append(" ");
                sb.append("**Pre-1.19 sign restored by 0xTas' SignHistorian**");
                restoration[n] = Text.of(sb.toString());
            } else {
                restoration[n] = Text.of(sbe.getFrontText().getMessage(n, false).getString());
            }
        }

        if (sbe.getFrontText().getColor() != DyeColor.BLACK) {
            signsToColor.put(sign, sbe.getFrontText().getColor());
        }
        if (sbe.getFrontText().isGlowing()) {
            signsToGlowInk.add(sign);
        }
        if (waxRestoration.get()) {
            signsToWax.add(sign);
        }

        destroyedSigns.remove(sbe);
        return new SignText(restoration, restoration, DyeColor.BLACK, false);
    }

    private boolean isSameSign(SignBlockEntity sbe1, SignBlockEntity sbe2) {
        SignText front1 = sbe1.getFrontText();
        SignText front2 = sbe2.getFrontText();

        int n = 0;
        for (Text line : front1.getMessages(false)) {
            String compensatedLine = line
                .getString()
                .replace("**Pre-1.19 sign restored by 0xTas' SignHistorian**", "")
                .trim();

            if (!compensatedLine.equals(front2.getMessage(n, false).getString().trim())) return false;
            ++n;
        }

        if (strictSetting.get()) {
            if (sbe1.getFrontText().getColor() != sbe2.getFrontText().getColor()) return false;
            if (sbe1.getFrontText().isGlowing() != sbe2.getFrontText().isGlowing()) return false;
        }

        return ((AbstractSignBlock) sbe1.getCachedState().getBlock()).getWoodType() == ((AbstractSignBlock) sbe2.getCachedState().getBlock()).getWoodType();
    }

    private boolean containsBlacklistedText(SignBlockEntity sbe) {
        String front = Arrays.stream(sbe.getFrontText().getMessages(false))
            .map(Text::getString)
            .collect(Collectors.joining(" "))
            .trim();

        String back = Arrays.stream(sbe.getBackText().getMessages(false))
            .map(Text::getString)
            .collect(Collectors.joining(" "))
            .trim();

        return blacklisted.stream()
            .anyMatch(line -> front.toLowerCase().contains(line.trim().toLowerCase())
                || back.toLowerCase().contains(line.trim().toLowerCase()));
    }

    private boolean hasNearbySigns() {
        if (mc.player == null || mc.world == null) return false;
        for (BlockPos pos : BlockPos.iterateOutwards(mc.player.getBlockPos(), 5, 5, 5)) {
            if (mc.world.getBlockEntity(pos) instanceof SignBlockEntity sbe) {
                if (sbe.getFrontText().hasText(mc.player) || sbe.getBackText().hasText(mc.player)) return true;
            }
        }
        return false;
    }

    private void processSign(@NotNull SignBlockEntity sbe) {
        if (!sbe.getFrontText().hasText(mc.player) && !sbe.getBackText().hasText(mc.player)) return;
        else if (contentBlacklist.get() &&  containsBlacklistedText(sbe)) return;

        BlockPos pos = sbe.getPos();
        if (serverSigns.containsKey(pos)) {
            if (isSameSign(sbe, serverSigns.get(pos).getLeft())) {
                modifiedSigns.remove(serverSigns.get(pos).getLeft());
            } else {
                modifiedSigns.add(serverSigns.get(pos).getLeft());
            }
            destroyedSigns.remove(serverSigns.get(pos).getLeft());
        } else {
            serverSigns.put(pos, new Pair<>(sbe, sbe.getCachedState()));
            if (persistenceSetting.get()) {
                saveSignToFile(sbe, sbe.getCachedState());
            }
        }
    }

    private void interactSign(SignBlockEntity sbe, Item dye) {
        if (mc.player == null || mc.interactionManager == null) return;

        BlockPos pos = sbe.getPos();
        Vec3d hitVec = Vec3d.ofCenter(pos);
        BlockHitResult hit = new BlockHitResult(hitVec, mc.player.getHorizontalFacing().getOpposite(), pos, false);

        ItemStack current = mc.player.getInventory().getMainHandStack();
        if (current.getItem() != dye) {
            for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                ItemStack stack = mc.player.getInventory().getStack(n);
                if (stack.getItem() == dye) {
                    if (current.getItem() instanceof SignItem && current.getCount() > 1) dyeSlot = n;
                    if (n < 9) InvUtils.swap(n, true);
                    else InvUtils.move().from(n).to(mc.player.getInventory().selectedSlot);

                    timer = 3;
                    return;
                }
            }
        } else {
            Rotations.rotate(
                Rotations.getYaw(pos),
                Rotations.getPitch(pos), rotationPriority,
                () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit)
            );
            ++rotationPriority;
        }

        if (dye == Items.GLOW_INK_SAC) {
            signsToGlowInk.remove(sbe);
            if (!signsToWax.contains(sbe) && !signsToColor.containsKey(sbe)) timer = -1;
        } else if (dye == Items.HONEYCOMB){
            signsToWax.remove(sbe);
            if (!signsToColor.containsKey(sbe) && !signsToGlowInk.contains(sbe)) timer = -1;
        } else {
            signsToColor.remove(sbe);
            if (!signsToGlowInk.contains(sbe) && !signsToWax.contains(sbe)) timer = -1;
        }
    }

    @Override
    public void onActivate() {
        if (mc.world == null || mc.player == null) return;
        if (persistenceSetting.get()) initOrLoadFromSignFile();
        if (contentBlacklist.get() && StardustUtil.checkOrCreateFile(mc, BLACKLIST_FILE)) initBlacklistText();
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        pingTicks = 0;
        gracePeriod = 0;
        currentDim = null;
        signsToWax.clear();
        serverSigns.clear();
        signsToColor.clear();
        modifiedSigns.clear();
        destroyedSigns.clear();
        signsToGlowInk.clear();
        trackedCreepers.clear();
        lastTargetedSign = null;
        rotationPriority = 69420;
        didDisableWaxAura = false;
        approachingCreepers.clear();
        signsBrokenByPlayer.clear();
    }

    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        if (mc.player == null) return;
        for (SignBlockEntity sbe : modifiedSigns) {
            if (event.result.getBlockPos().isWithinDistance(sbe.getPos(), 1)) {
                mc.player.sendMessage(Text.of(
                    "§8<"+ StardustUtil.rCC()+"✨§8> §e§lOriginal§7§l: §7§o"+ Arrays.stream(sbe.getFrontText().getMessages(false)).map(Text::getString).collect(Collectors.joining(" "))
                ));
                mc.player.sendMessage(Text.of(
                    "§8<"+ StardustUtil.rCC()+"✨§8> §6§lWoodType§7§l: "+((AbstractSignBlock) sbe.getCachedState().getBlock()).getWoodType().name()
                    +" | §3§lColor§7§l: "+sbe.getText(true).getColor().name()
                    +" | §f§lGlow Ink§7§l: "+sbe.getText(true).isGlowing()
                ));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockAttack(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.packet instanceof PlayerActionC2SPacket packet)) return;
        if (packet.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

        for (SignBlockEntity ghost : destroyedSigns) {
            if (packet.getPos().isWithinDistance(ghost.getPos(), 1.5)) {
                mc.player.sendMessage(Text.of(
                    "§8<"+ StardustUtil.rCC()+"✨§8> §c§lOriginal§7§l: §7§o"+ Arrays.stream(ghost.getFrontText().getMessages(false)).map(Text::getString).collect(Collectors.joining(" "))
                ));
                mc.player.sendMessage(Text.of(
                    "§8<"+ StardustUtil.rCC()+"✨§8> §6§lWoodType§7§l: "+((AbstractSignBlock) ghost.getCachedState().getBlock()).getWoodType().name()
                        +" | §3§lColor§7§l: "+ghost.getText(true).getColor().name()
                        +" | §f§lGlow Ink§7§l: "+ghost.getText(true).isGlowing()
                ));
            }
        }
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (mc.world == null) return;
        if ((event.oldState.getBlock() instanceof SignBlock
            && !(event.newState.getBlock() instanceof SignBlock))
            || (event.oldState.getBlock() instanceof HangingSignBlock
            && !(event.newState.getBlock() instanceof HangingSignBlock))
            || (event.oldState.getBlock() instanceof WallSignBlock
            && !(event.newState.getBlock() instanceof WallSignBlock))
            || (event.oldState.getBlock() instanceof WallHangingSignBlock
            && !(event.newState.getBlock() instanceof WallHangingSignBlock)))
        {
            if (lastTargetedSign == null) return;
            if (lastTargetedSign.getX() == event.pos.getX() && lastTargetedSign.getY() == event.pos.getY() && lastTargetedSign.getZ() == event.pos.getZ()) {
                signsBrokenByPlayer.add(event.pos);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null) return;
        if (currentDim == null) currentDim = mc.world.getRegistryKey();
        else if (currentDim != mc.world.getRegistryKey()) {
            currentDim = mc.world.getRegistryKey();
            serverSigns.clear();
            if (persistenceSetting.get()) initOrLoadFromSignFile();
        }

        BlockPos targeted = getTargetedSign();
        if (lastTargetedSign == null) lastTargetedSign = targeted;
        else if (targeted == null) {
            if (gracePeriod < 2) ++gracePeriod;
            else {
                gracePeriod = 0;
                lastTargetedSign = null;
            }
        } else lastTargetedSign = targeted;
        if (mc.currentScreen instanceof AbstractSignEditScreen) return;

        if (timer == -1 && dyeSlot != -1) {
            if (dyeSlot < 9) InvUtils.swapBack();
            else InvUtils.move().from(mc.player.getInventory().selectedSlot).to(dyeSlot);
            dyeSlot = -1;
            timer = 3;
        }

        WaxAura waxAura = Modules.get().get(WaxAura.class);
        if (!signsToColor.isEmpty() || !signsToGlowInk.isEmpty() || !signsToWax.isEmpty()) {
            if (waxAura.isActive()) {
                waxAura.toggle();
                didDisableWaxAura = true;
            }
        }

        if (timer % 2 == 0) {
            List<BlockPos> inRange = serverSigns.keySet()
                .stream()
                .filter(pos -> pos.isWithinDistance(mc.player.getBlockPos(), espRange.get()))
                .toList();

            for (BlockPos pos : inRange) {
                if (!(mc.world.getBlockEntity(pos) instanceof SignBlockEntity sbe)) {
                    destroyedSigns.add(serverSigns.get(pos).getLeft());
                    modifiedSigns.remove(serverSigns.get(pos).getLeft());
                } else processSign(sbe);
            }

            for (BlockEntity be : Utils.blockEntities()) {
                if (be instanceof SignBlockEntity sbe) processSign(sbe);
            }
        } else if (griefPrevention.get()) {
            approachingCreepers.removeIf(Entity::isRemoved);
            approachingCreepers.removeIf(creeper -> !creeper.getBlockPos().isWithinDistance(mc.player.getBlockPos(), 12));
            approachingCreepers.removeIf(creep -> creep.getBlockPos().isWithinDistance(trackedCreepers.get(creep.getId()), 2));

            if (!approachingCreepers.isEmpty() && hasNearbySigns()) {
                if (pingTicks == 0) {
                    mc.player.playSound(SoundEvents.ENTITY_PHANTOM_HURT, alarmVolume.get().floatValue(), 1f);
                    if (chatNotification.get()) {
                        mc.player.sendMessage(Text.of(
                            "§8<"+ StardustUtil.rCC()+"✨§8> [§5SignHistorian§8] §c§lNEARBY SIGNS IN DANGER OF MOB GRIEFING§7§l."
                        ));
                    }
                }
                ++pingTicks;
                if (pingTicks >= 50) pingTicks = 0;
            }

            for (int id : trackedCreepers.keySet()) {
                if (mc.world.getEntityById(id) instanceof CreeperEntity creeper) {
                    if (creeper.isRemoved()) trackedCreepers.remove(id);
                    else if (hasNearbySigns()) {
                        BlockPos pPos = mc.player.getBlockPos();
                        BlockPos newPos = creeper.getBlockPos();
                        BlockPos lastPos = trackedCreepers.get(id);
                        if (!pPos.isWithinDistance(newPos, 15)) continue;
                        if (newPos.isWithinDistance(lastPos, 2)) {
                            approachingCreepers.remove(creeper);
                            continue;
                        }

                        double oldDelta = pPos.getSquaredDistance(lastPos);
                        double newDelta = pPos.getSquaredDistance(newPos);
                        if (newDelta < oldDelta) approachingCreepers.add(creeper);
                        else approachingCreepers.remove(creeper);
                    }
                }
            }

            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof CreeperEntity creeper)) continue;
                if (!trackedCreepers.containsKey(creeper.getId())) trackedCreepers.put(creeper.getId(), creeper.getBlockPos());
            }
        }

        ++timer;
        if (timer >= 5) {
            timer = 0;

            signsToWax.removeIf(sbe -> !sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 6));
            signsToGlowInk.removeIf(sbe -> !sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 6));
            List<SignBlockEntity> toColor = signsToColor.keySet()
                .stream()
                .filter(sbe -> sbe.getPos().isWithinDistance(mc.player.getBlockPos(), 6))
                .toList();

            if (!toColor.isEmpty()) {
                SignBlockEntity sbe = toColor.get(0);
                interactSign(sbe, DyeItem.byColor(signsToColor.get(sbe)));
                return;
            }

            if (!signsToGlowInk.isEmpty()) {
                List<SignBlockEntity> signs = signsToGlowInk
                    .stream()
                    .toList();

                if (!signs.isEmpty()) {
                    SignBlockEntity sbe = signs.get(0);
                    interactSign(sbe, Items.GLOW_INK_SAC);
                    return;
                }
            }
            if (!signsToWax.isEmpty()) {
                List<SignBlockEntity> signs = signsToWax
                    .stream()
                    .toList();

                if (!signs.isEmpty()) {
                    SignBlockEntity sbe = signs.get(0);
                    interactSign(sbe, Items.HONEYCOMB);
                }
            } else if (didDisableWaxAura && !waxAura.isActive()) {
                waxAura.toggle();
                didDisableWaxAura = false;
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (mc.getNetworkHandler().getPlayerList().size() <= 1) return; // ignore queue

        ESPBlockData mESP = modifiedSettings.get();
        ESPBlockData dESP = destroyedSettings.get();
        for (SignBlockEntity sign : destroyedSigns) {
            if (sign.getCachedState() == null) return;
            if (contentBlacklist.get() && containsBlacklistedText(sign)) continue;
            if (ignoreBrokenSetting.get() && signsBrokenByPlayer.contains(sign.getPos())) continue;
            if (!sign.getPos().isWithinDistance(mc.player.getBlockPos(), espRange.get())) continue;

            VoxelShape shape = sign.getCachedState().getOutlineShape(mc.world, sign.getPos());
            double x1 = sign.getPos().getX() + shape.getMin(Direction.Axis.X);
            double y1 = sign.getPos().getY() + shape.getMin(Direction.Axis.Y);
            double z1 = sign.getPos().getZ() + shape.getMin(Direction.Axis.Z);
            double x2 = sign.getPos().getX() + shape.getMax(Direction.Axis.X);
            double y2 = sign.getPos().getY() + shape.getMax(Direction.Axis.Y);
            double z2 = sign.getPos().getZ() + shape.getMax(Direction.Axis.Z);

            if (dESP.sideColor.a > 0 || dESP.lineColor.a > 0) {
                event.renderer.box(
                    x1, y1, z1, x2, y2, z2,
                    dESP.sideColor, dESP.lineColor,
                    ShapeMode.Both, 0
                );
            }

            if (dESP.tracer && dESP.tracerColor.a > 0) {
                Vec3d offsetVec = getTracerOffset(sign.getPos(), sign.getCachedState());
                event.renderer.line(
                    RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                    offsetVec.x, offsetVec.y, offsetVec.z, dESP.tracerColor
                );
            }
        }
        for (SignBlockEntity sign : modifiedSigns) {
            if (sign.getCachedState() == null) continue;
            if (contentBlacklist.get() && containsBlacklistedText(sign)) continue;
            if (ignoreBrokenSetting.get() && signsBrokenByPlayer.contains(sign.getPos())) continue;
            if (!sign.getPos().isWithinDistance(mc.player.getBlockPos(), espRange.get())) continue;

            VoxelShape shape = sign.getCachedState().getOutlineShape(mc.world, sign.getPos());
            double x1 = sign.getPos().getX() + shape.getMin(Direction.Axis.X);
            double y1 = sign.getPos().getY() + shape.getMin(Direction.Axis.Y);
            double z1 = sign.getPos().getZ() + shape.getMin(Direction.Axis.Z);
            double x2 = sign.getPos().getX() + shape.getMax(Direction.Axis.X);
            double y2 = sign.getPos().getY() + shape.getMax(Direction.Axis.Y);
            double z2 = sign.getPos().getZ() + shape.getMax(Direction.Axis.Z);

            if (mESP.sideColor.a > 0 || mESP.lineColor.a > 0) {
                event.renderer.box(
                    x1, y1, z1, x2, y2, z2,
                    mESP.sideColor, mESP.lineColor,
                    ShapeMode.Both, 0
                );
            }

            if (mESP.tracer && mESP.tracerColor.a > 0) {
                Vec3d offsetVec = getTracerOffset(sign.getPos(), sign.getCachedState());
                event.renderer.line(
                    RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                    offsetVec.x, offsetVec.y, offsetVec.z, mESP.tracerColor
                );
            }
        }

        approachingCreepers.removeIf(Entity::isRemoved);
        approachingCreepers.removeIf(creeper -> !creeper.getBlockPos().isWithinDistance(mc.player.getBlockPos(), 12));
        if (griefPrevention.get() && !approachingCreepers.isEmpty() && hasNearbySigns()) {
            SettingColor dangerColor = new SettingColor(255, 0, 25, 255);
            for (CreeperEntity creeper : approachingCreepers) {
                if (creeperTracers.get()) {
                    WireframeEntityRenderer.render(event, creeper, 1, dangerColor, dangerColor, ShapeMode.Both);

                    event.renderer.line(
                        RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                        creeper.getBoundingBox().getCenter().x, creeper.getBoundingBox().getCenter().y,
                        creeper.getBoundingBox().getCenter().z, dangerColor
                    );
                }
            }
        }
    }
}
