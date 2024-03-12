package dev.stardust.modules;

import java.util.Collection;
import dev.stardust.Stardust;
import net.minecraft.util.Hand;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.item.GoatHornItem;
import net.minecraft.sound.SoundEvents;
import meteordevelopment.orbit.EventHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.network.ClientPlayerEntity;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.settings.ProvidedStringSetting;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;


/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class Honker extends Module {
    public Honker() {
        super(Stardust.CATEGORY, "Honker", "Automatically use goat horns when a player enters your render distance.");
    }

    public static String[] horns = {"Admire", "Call", "Dream", "Feel", "Ponder", "Seek", "Sing", "Yearn", "Random"};

    private final Setting<String> desiredCall = settings.getDefaultGroup().add(
        new ProvidedStringSetting.Builder()
            .name("Horn Preference")
            .description("Which horn to prefer using")
            .supplier(() -> horns)
            .defaultValue("Random")
            .build()
    );

    private final Setting<Boolean> ignoreFakes = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Ignore Fakes")
            .description("Ignore fake players created by modules like Blink.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> hornSpam = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Horn Spam")
            .description("Spam the desired horn as soon as it's done cooling down (every 7 seconds.)")
            .defaultValue(false)
            .onChanged(it -> {if (it) honkDesiredHorn();})
            .build()
    );

    private final Setting<Boolean> muteHorns = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Mute Horns")
            .description("Clientside mute for your own goat horns.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> muteAllHorns = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("Mute All Horns")
            .description("Mute everybody's horns, not just your own.")
            .visible(muteHorns::get)
            .defaultValue(false)
            .build()
    );

    private int ticksSinceUsedHorn = 0;
    private boolean needsMuting = false;

    private void honkHorn(int hornSlot, int activeSlot) {
        if (mc.interactionManager == null) return;

        needsMuting = true;
        InvUtils.move().from(hornSlot).to(activeSlot);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        InvUtils.move().from(activeSlot).to(hornSlot);
    }

    private void honkDesiredHorn() {
        if (mc.player == null) return;
        if ("Random".equals(desiredCall.get())) {
            IntArrayList hornSlots = new IntArrayList();
            for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                ItemStack stack = mc.player.getInventory().getStack(n);
                if (stack.getItem() instanceof GoatHornItem) hornSlots.add(n);
            }
            if (hornSlots.isEmpty()) return;
            if (hornSlots.size() == 1) {
                honkHorn(hornSlots.getInt(0), mc.player.getInventory().selectedSlot);
            } else {
                int luckyIndex = (int) (Math.random() * hornSlots.size());
                honkHorn(hornSlots.getInt(luckyIndex), mc.player.getInventory().selectedSlot);
            }
        } else {
            String desiredCallId = desiredCall.get().toLowerCase() + "_goat_horn";

            int hornIndex = -1;
            for (int n = 0; n < mc.player.getInventory().main.size(); n++) {
                ItemStack stack = mc.player.getInventory().getStack(n);
                if (!(stack.getItem() instanceof GoatHornItem)) continue;

                NbtCompound metadata = stack.getNbt();
                if (metadata == null || !metadata.contains("instrument", NbtElement.STRING_TYPE)) continue;

                String id = metadata.getString("instrument");
                if (id == null) continue;

                hornIndex = n;
                if (id.equals(desiredCallId)) break;
            }

            if (hornIndex != -1) {
                honkHorn(hornIndex, mc.player.getInventory().selectedSlot);
            }
        }
    }

    // See GoatHornItemMixin.java
    public boolean shouldMuteHorns() {
        return muteHorns.get() && needsMuting || muteHorns.get() && muteAllHorns.get();
    }

    @Override
    public void onActivate() {
        if (mc.world == null) return;
        if (hornSpam.get()) {
            ticksSinceUsedHorn = 0;
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && !(entity instanceof ClientPlayerEntity)) {
                honkDesiredHorn();
                break;
            }
        }
        needsMuting = false;
        ticksSinceUsedHorn = 0;
    }

    @EventHandler
    private void onEntityAdd(EntityAddedEvent event) {
        if (hornSpam.get() || mc.player == null) return;
        if (!(event.entity instanceof PlayerEntity player)) return;
        if (player instanceof ClientPlayerEntity) return;

        if (ignoreFakes.get()) {
            Collection<PlayerListEntry> players = mc.player.networkHandler.getPlayerList();
            if (players.stream().noneMatch(entry -> entry.getProfile().getId().equals(player.getUuid()))) return;
        }

        if (!hornSpam.get()) {
            honkDesiredHorn();
            needsMuting = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!hornSpam.get() || mc.player == null) return;

        Item activeItem = mc.player.getActiveItem().getItem();
        if (activeItem.isFood() || activeItem == Items.BOW || activeItem == Items.TRIDENT && mc.mouse.wasRightButtonClicked()) return;

        ++ticksSinceUsedHorn;
        if (ticksSinceUsedHorn > 150) {
            honkDesiredHorn();
            needsMuting = false;
            ticksSinceUsedHorn = 0;
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlaySoundFromEntityS2CPacket) || !shouldMuteHorns()) return;

        SoundEvent soundEvent = ((PlaySoundFromEntityS2CPacket) event.packet).getSound().value();

        for (int n = 0; n < SoundEvents.GOAT_HORN_SOUND_COUNT; n++) {
            if (soundEvent == SoundEvents.GOAT_HORN_SOUNDS.get(n).value()) {
                event.cancel();
                break;
            }
        }
    }
}
