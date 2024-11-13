package com.ishland.c2me.base.common.threadstate;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

public record SyncLoadWork(ServerWorld world, ChunkPos chunkPos, ChunkStatus targetStatus, boolean create) implements RunningWork {

    @Override
    public String toString() {
        return String.format("Sync load chunk %s to status %s in world %s (create=%s)", chunkPos, targetStatus, world.getRegistryKey().getValue(), create);
    }

}
