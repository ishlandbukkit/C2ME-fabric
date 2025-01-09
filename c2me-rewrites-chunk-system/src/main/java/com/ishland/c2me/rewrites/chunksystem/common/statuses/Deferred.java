package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.flowsched.scheduler.Cancellable;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Deferred extends NewChunkStatus {

    public Deferred(int ordinal) {
        super(ordinal, ChunkStatus.EMPTY);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context, Cancellable cancellable) {
        return CompletableFuture.completedFuture(null);
    }
}
