package com.ishland.c2me.rewrites.chunksystem.common.threadstate;

import com.ishland.c2me.base.common.threadstate.RunningWork;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

public record ChunkTaskWork(ServerWorld world, ChunkPos chunkPos, NewChunkStatus status, boolean isUpgrade) implements RunningWork {

    public ChunkTaskWork(ChunkLoadingContext context, NewChunkStatus status, boolean isUpgrade) {
        this(
                ((IThreadedAnvilChunkStorage) context.tacs()).getWorld(),
                context.holder().getKey(),
                status,
                isUpgrade
        );
    }

    @Override
    public String toString() {
        if (isUpgrade) {
            return String.format(
                    "Upgrading chunk %s to %s in world %s",
                    chunkPos,
                    status,
                    world.getRegistryKey().getValue()
            );
        } else {
            return String.format(
                    "Downgrading chunk %s from %s in world %s",
                    chunkPos,
                    status,
                    world.getRegistryKey().getValue()
            );
        }
    }
}
