package com.ishland.c2me.base.mixin.access;

import net.minecraft.server.world.ChunkLevelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkLevelManager.DistanceFromNearestPlayerTracker.class)
public interface IChunkLevelManagerDistanceFromNearestPlayerTracker {

    @Accessor
    int getMaxDistance();

}
