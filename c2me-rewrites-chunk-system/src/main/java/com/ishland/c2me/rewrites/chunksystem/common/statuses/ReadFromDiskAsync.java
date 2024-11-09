package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.ishland.c2me.base.common.GlobalExecutors;
import com.ishland.c2me.base.common.config.ModStatuses;
import com.ishland.c2me.base.common.registry.SerializerAccess;
import com.ishland.c2me.base.common.scheduler.IVanillaChunkManager;
import com.ishland.c2me.base.common.theinterface.IDirectStorage;
import com.ishland.c2me.base.common.util.RxJavaUtils;
import com.ishland.c2me.base.mixin.access.ISerializingRegionBasedStorage;
import com.ishland.c2me.base.mixin.access.IServerLightingProvider;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.base.mixin.access.IVersionedChunkStorage;
import com.ishland.c2me.base.mixin.access.IWorldChunk;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.AsyncSerializationManager;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.BlendingInfoUtil;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.ChunkIoMainThreadTaskUtils;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.ProtoChunkExtension;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.SerializingRegionBasedStorageExtension;
import com.ishland.c2me.rewrites.chunksystem.common.ducks.IPOIUnloading;
import com.ishland.c2me.rewrites.chunksystem.common.fapi.LifecycleEventInvoker;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkType;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ReadFromDiskAsync extends ReadFromDisk {

    private static final Logger LOGGER = LoggerFactory.getLogger("ReadFromDiskAsync");

    public ReadFromDiskAsync(int ordinal) {
        super(ordinal);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
        final Single<ProtoChunk> single = invokeAsyncLoad(context)
                .retryWhen(RxJavaUtils.retryWithExponentialBackoff(3, 200))
                .cache()
                .onErrorResumeNext(throwable -> {
                    LOGGER.error("Failed to load chunk {} asynchronously, falling back to sync loading", context.holder().getKey(), throwable);
                    return invokeSyncRead(context)
                            .retryWhen(RxJavaUtils.retryWithExponentialBackoff(3, 200, new RuntimeException("Failed to load asynchronously, falling back to sync loading", throwable)));
                });
        return finalizeLoading(context, single);
    }

    protected static Single<ProtoChunk> invokeAsyncLoad(ChunkLoadingContext context) {
        return Single.defer(() -> Single.fromCompletionStage(((IThreadedAnvilChunkStorage) context.tacs()).invokeGetUpdatedChunkNbt(context.holder().getKey())))
                .map(nbt -> nbt.filter(nbt2 -> {
                    boolean bl = nbt2.contains("Status", NbtElement.STRING_TYPE);
                    if (!bl) {
                        LOGGER.error("Chunk file at {} is missing level data, skipping", context.holder().getKey());
                    }

                    return bl;
                }))
                .observeOn(Schedulers.from(((IVanillaChunkManager) context.tacs()).c2me$getSchedulingManager().positionedExecutor(context.holder().getKey().toLong())))
                .map(nbt -> {
                    final ReferenceArrayList<Runnable> mainThreadQueue = new ReferenceArrayList<>();
                    if (nbt.isPresent()) {
                        ChunkIoMainThreadTaskUtils.push(mainThreadQueue);
                        try {
                            return Pair.of(mainThreadQueue, ChunkSerializer.deserialize(
                                    ((IThreadedAnvilChunkStorage) context.tacs()).getWorld(),
                                    ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage(),
                                    ((IVersionedChunkStorage) context.tacs()).invokeGetStorageKey(),
                                    context.holder().getKey(),
                                    nbt.get()
                            ));
                        } finally {
                            ChunkIoMainThreadTaskUtils.pop(mainThreadQueue);
                        }
                    } else {
                        return Pair.of(mainThreadQueue, createEmptyProtoChunk(context));
                    }
                })
                .flatMap(pair -> {
                    final ServerWorld world = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
                    ProtoChunk protoChunk = pair.right();
                    // blending
                    final ChunkPos pos = context.holder().getKey();
                    protoChunk = protoChunk != null ? protoChunk : new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, world, world.getRegistryManager().get(RegistryKeys.BIOME), null);
                    if (protoChunk.getBelowZeroRetrogen() != null || protoChunk.getStatus().getChunkType() == ChunkType.PROTOCHUNK) {
                        ProtoChunk finalProtoChunk = protoChunk;
                        return Single.defer(() -> Single.fromCompletionStage(BlendingInfoUtil.getBlendingInfos(((IVersionedChunkStorage) context.tacs()).getWorker(), pos)))
                                .doOnSuccess(bitSets -> ((ProtoChunkExtension) finalProtoChunk).setBlendingInfo(pos, bitSets))
                                .map(unused -> pair);
                    } else {
                        return Single.just(pair);
                    }
                })
                .zipWith(
                        Single.defer(() -> Single.fromCompletionStage(((ISerializingRegionBasedStorage) ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage()).getStorageAccess().read(context.holder().getKey()))),
                        (pair, poiNbt) -> {
                            pair.left().addFirst(() -> ((SerializingRegionBasedStorageExtension) ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage()).update(context.holder().getKey(), poiNbt.orElse(null)));
                            return pair;
                        }
                )
                .observeOn(Schedulers.from(((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor()))
                .map(pair -> {
                    ChunkIoMainThreadTaskUtils.drainQueue(pair.left());
                    return pair.right();
                });
    }

    @Override
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context, Cancellable cancellable) {
        final AtomicBoolean loadedToWorld = new AtomicBoolean(false);
        return syncWithLightEngine(context).thenApplyAsync(unused -> {
                    if (context.holder().getTargetStatus().ordinal() >= this.ordinal()) { // saving cancelled
                        cancellable.cancel();
                        return CompletableFuture.<Void>failedFuture(new CancellationException());
                    }

                    final ChunkState chunkState = context.holder().getItem().get();
                    Chunk chunk = chunkState.chunk();
                    if (chunk instanceof WrapperProtoChunk protoChunk) chunk = protoChunk.getWrappedChunk();

                    if (chunk instanceof WorldChunk worldChunk) {
                        loadedToWorld.set(((IWorldChunk) worldChunk).isLoadedToWorld());
                        worldChunk.setLoadedToWorld(false);
                    }

                    if (loadedToWorld.get() && ModStatuses.fabric_lifecycle_events_v1 && chunk instanceof WorldChunk worldChunk) {
                        LifecycleEventInvoker.invokeChunkUnload(((IThreadedAnvilChunkStorage) context.tacs()).getWorld(), worldChunk);
                    }

                    if ((context.holder().getFlags() & ItemHolder.FLAG_BROKEN) != 0 && chunk instanceof ProtoChunk) { // do not save broken ProtoChunks
                        LOGGER.warn("Not saving partially generated broken chunk {}", context.holder().getKey());
                        return CompletableFuture.completedStage((Void) null);
                    } else if (chunk instanceof WorldChunk && !chunkState.reachedStatus().isAtLeast(ChunkStatus.FULL)) {
                        // do not save WorldChunks that doesn't reach full status: Vanilla behavior
                        // If saved, block entities will be lost
                        return CompletableFuture.completedStage((Void) null);
                    } else {
                        return asyncSave(context.tacs(), chunk);
                    }
                }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor())
                .thenCompose(Function.identity())
                .thenAcceptAsync(unused -> {
                    Chunk chunk = context.holder().getItem().get().chunk();
                    if (chunk instanceof WrapperProtoChunk protoChunk) chunk = protoChunk.getWrappedChunk();
                    if (loadedToWorld.get() && chunk instanceof WorldChunk worldChunk) {
                        ((IThreadedAnvilChunkStorage) context.tacs()).getWorld().unloadEntities(worldChunk);
                    }

                    ((IServerLightingProvider) ((IThreadedAnvilChunkStorage) context.tacs()).getLightingProvider()).invokeUpdateChunkStatus(chunk.getPos());
                    ((IThreadedAnvilChunkStorage) context.tacs()).getLightingProvider().tick();
                    ((IThreadedAnvilChunkStorage) context.tacs()).getWorldGenerationProgressListener().setChunkStatus(chunk.getPos(), null);
                    ((IThreadedAnvilChunkStorage) context.tacs()).getChunkToNextSaveTimeMs().remove(chunk.getPos().toLong());

                    ((IPOIUnloading) ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage()).c2me$unloadPoi(context.holder().getKey());

                    context.holder().getItem().set(new ChunkState(null, null, null));
                }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
    }

    private CompletionStage<Void> asyncSave(ServerChunkLoadingManager tacs, Chunk chunk) {
        ((IThreadedAnvilChunkStorage) tacs).getPointOfInterestStorage().saveChunk(chunk.getPos());
        if (!chunk.needsSaving()) {
            return CompletableFuture.completedStage(null);
        } else {
            chunk.setNeedsSaving(false);
            ChunkPos chunkPos = chunk.getPos();

            AsyncSerializationManager.Scope scope = new AsyncSerializationManager.Scope(chunk, ((IThreadedAnvilChunkStorage) tacs).getWorld());
            return CompletableFuture.supplyAsync(() -> {
                        scope.open();
                        AsyncSerializationManager.push(scope);
                        try {
                            return SerializerAccess.getSerializer().serialize(((IThreadedAnvilChunkStorage) tacs).getWorld(), chunk);
                        } finally {
                            AsyncSerializationManager.pop(scope);
                        }
                    }, GlobalExecutors.prioritizedScheduler.executor(16) /* boost priority as we are serializing an unloaded chunk */)
                    .thenAccept((either) -> {
                        if (either.left().isPresent()) {
                            tacs.setNbt(chunkPos, either.left().get());
                        } else {
                            ((IDirectStorage) ((IVersionedChunkStorage) tacs).getWorker()).setRawChunkData(chunkPos, either.right().get());
                        }
                    })
                    .exceptionallyCompose(throwable -> {
                        LOGGER.error("Failed to save chunk {},{} asynchronously, falling back to sync saving", chunkPos.x, chunkPos.z, throwable);
                        return CompletableFuture.runAsync(() -> {
                            chunk.setNeedsSaving(true);
                            ((IThreadedAnvilChunkStorage) tacs).invokeSave(chunk);
                        }, ((IThreadedAnvilChunkStorage) tacs).getMainThreadExecutor());
                    });
        }
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
