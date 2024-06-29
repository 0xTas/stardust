package dev.stardust.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    @Mutable
    @Accessor("pitch")
    void setPitch(float pitch);
}
