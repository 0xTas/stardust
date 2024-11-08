package dev.stardust.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Invoker("sendImmediately")
    void invokeSendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush);
}
