package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ServerAccessibleChunkSending extends NewChunkStatus {

    private static final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] deps;

    static {
        deps = EMPTY_DEPENDENCIES;
    }

    public ServerAccessibleChunkSending(int ordinal) {
        super(ordinal, ChunkStatus.FULL);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context, Cancellable cancellable) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependencies(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        return relativeToAbsoluteDependencies(holder, deps);
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependenciesToRemove(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        return EMPTY_DEPENDENCIES;
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependenciesToAdd(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        return EMPTY_DEPENDENCIES;
    }

    @Override
    public String toString() {
        return "minecraft:full, Border, Chunk Sending";
    }
}