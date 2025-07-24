package dev.stardust.mixin.accessor;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

@Mixin(PlayerListS2CPacket.Entry.class)
public interface PlayerListS2CPacketAccessor {
    @Mutable
    @Accessor("profile")
    void setProfile(GameProfile profile);
}
