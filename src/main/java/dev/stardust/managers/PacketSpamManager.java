package dev.stardust.managers;

import dev.stardust.Stardust;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import meteordevelopment.meteorclient.MeteorClient;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
public class PacketSpamManager {
    public PacketSpamManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    /**
     * 2b2t now kicks you for sending too many invalid inventory packets,
     * as well as for sending too many valid inventory packets too quickly.
     * This manager attempts to prevent you from sending invalid QUICK_MOVE packets,
     * which will get you kicked after just a small handful of them are sent in a short period of time.
     * This prevents most kicks when using InventoryTweaks to shift-hold-click for fast QUICK_MOVE interactions.
     **/
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!Stardust.antiInventoryPacketKick.get()) return;
        if (!(event.packet instanceof ClickSlotC2SPacket packet)) return;
        if (!packet.getActionType().equals(SlotActionType.QUICK_MOVE)) return;

        int origin = packet.getSlot();
        ScreenHandler handler = mc.player.currentScreenHandler;
        ItemStack toMove = handler.getSlot(origin).getStack();

        if (toMove.isEmpty()) {
            return;
        }

        int start;
        int until;
        if (handler instanceof PlayerScreenHandler) {
            if (origin < 9) {
                // from armor/crafting to hotbar/inventory
                start = 9;
                until = 44;
            } else if (origin < 36) {
                // from inventory to hotbar
                start = 36;
                until = 45;
            } else {
                // from hotbar to inventory
                start = 9;
                until = 36;
            }
        } else {
            // double chest
            if (handler.slots.size() > 63) {
                // from player to container
                if (origin >= 54) {
                    start = 0;
                    until = 54;
                } else {
                    // from container to player
                    start = 54;
                    until = handler.slots.size();
                }
            } else {
                // from player to container
                if (origin >= 27) {
                    start = 0;
                    until = 27;
                } else {
                    // from container to player
                    start = 27;
                    until = handler.slots.size();
                }
            }
        }

        boolean foundValidSlot = false;
        for (int n = start; n < until; n++) {
            ItemStack stack = handler.getSlot(n).getStack();
            if (stack.isEmpty() || (ItemStack.areItemsAndComponentsEqual(toMove, stack) && stack.getCount() < stack.getMaxCount())) {
                foundValidSlot = true;
                break;
            }
        }

        if (!foundValidSlot) {
            event.cancel();
        }
    }
}
