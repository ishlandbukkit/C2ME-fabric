package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.ishland.c2me.base.common.util.RxJavaUtils;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.base.mixin.access.IVersionedChunkStorage;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.ChunkIoMainThreadTaskUtils;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ProtoChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public class ReadFromDiskAsync extends ReadFromDisk {

    private static final Logger LOGGER = LoggerFactory.getLogger("ReadFromDiskAsync");

    public ReadFromDiskAsync(int ordinal) {
        super(ordinal);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
        final Single<ProtoChunk> single = invokeAsyncLoad(context)
                .retryWhen(RxJavaUtils.retryWithExponentialBackoff(3, 200))
                .onErrorResumeNext(throwable -> {
                    LOGGER.error("Failed to load chunk {} fully asynchronously, falling back to normal loading", context.holder().getKey(), throwable);
                    return invokeVanillaLoad(context)
                            .retryWhen(RxJavaUtils.retryWithExponentialBackoff(3, 200, new RuntimeException("Failed to load chunk fully asynchronously, falling back to normal loading", throwable)));
                });
        return finalizeLoading(context, single);
    }

    protected static @NonNull Single<ProtoChunk> invokeAsyncLoad(ChunkLoadingContext context) {
        return invokeInitialChunkRead(context)
                .map(chunkSerializer -> {
                    final ReferenceArrayList<Runnable> mainThreadQueue = new ReferenceArrayList<>();
                    if (chunkSerializer.isPresent()) {
                        ChunkIoMainThreadTaskUtils.push(mainThreadQueue);
                        try {
                            return Pair.of(
                                    chunkSerializer.get().convert(
                                            ((IThreadedAnvilChunkStorage) context.tacs()).getWorld(),
                                            ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage(),
                                            ((IVersionedChunkStorage) context.tacs()).invokeGetStorageKey(),
                                            context.holder().getKey()
                                    ),
                                    mainThreadQueue
                            );
                        } finally {
                            ChunkIoMainThreadTaskUtils.pop(mainThreadQueue);
                        }
                    } else {
                        return Pair.of(createEmptyProtoChunk(context), mainThreadQueue);
                    }
                })
                .flatMap(pair -> postChunkLoading(context, pair.first()).toSingleDefault(pair))
                .observeOn(Schedulers.from(((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor()))
                .map(pair -> {
                    ChunkIoMainThreadTaskUtils.drainQueue(pair.second());
                    return pair.first();
                });
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependenciesToRemove(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        return EMPTY_DEPENDENCIES;
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependenciesToAdd(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        return EMPTY_DEPENDENCIES;
    }
}
