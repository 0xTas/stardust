package dev.stardust.mixin.accessor;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;

@Mixin(DisconnectS2CPacket.class)
public interface DisconnectS2CPacketAccessor {
    @Mutable
    @Accessor("reason")
    void setReason(Text reason);
}
