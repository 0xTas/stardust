package dev.stardust.mixin.accessor;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public interface EntityTrackerUpdateS2CPacketAccessor {
    @Mutable
    @Accessor("trackedValues")
    void setTrackedValues(List<DataTracker.SerializedEntry<?>> trackedValues);
}
