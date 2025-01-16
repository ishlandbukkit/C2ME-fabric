package com.ishland.c2me.base.mixin.access;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.server.world.ChunkLevelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkLevelManager.NearbyChunkTicketUpdater.class)
public interface IChunkLevelManagerNearbyChunkTicketUpdater {

    @Accessor
    Long2IntMap getDistances();

}
