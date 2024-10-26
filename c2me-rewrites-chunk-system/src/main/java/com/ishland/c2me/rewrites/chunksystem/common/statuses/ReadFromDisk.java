package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.ishland.c2me.base.common.GlobalExecutors;
import com.ishland.c2me.base.common.config.ModStatuses;
import com.ishland.c2me.base.common.registry.SerializerAccess;
import com.ishland.c2me.base.common.theinterface.IDirectStorage;
import com.ishland.c2me.base.common.util.RxJavaUtils;
import com.ishland.c2me.base.mixin.access.IServerLightingProvider;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.base.mixin.access.IVersionedChunkStorage;
import com.ishland.c2me.base.mixin.access.IWorldChunk;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.BlendingInfoUtil;
import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.ProtoChunkExtension;
import com.ishland.c2me.rewrites.chunksystem.common.ducks.IPOIUnloading;
import com.ishland.c2me.rewrites.chunksystem.common.fapi.LifecycleEventInvoker;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkType;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.SerializedChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class ReadFromDisk extends NewChunkStatus {

    private static final Logger LOGGER = LoggerFactory.getLogger("ReadFromDisk");

    public ReadFromDisk(int ordinal) {
        super(ordinal, ChunkStatus.EMPTY);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context) {
        final Single<ProtoChunk> single = invokeVanillaLoad(context)
                .retryWhen(RxJavaUtils.retryWithExponentialBackoff(5, 100));
        return finalizeLoading(context, single);
    }

    protected static @NotNull CompletionStage<Void> finalizeLoading(ChunkLoadingContext context, Single<ProtoChunk> single) {
        return single
                .doOnError(throwable -> ((IThreadedAnvilChunkStorage) context.tacs()).getWorld().getServer().onChunkLoadFailure(throwable, ((IVersionedChunkStorage) context.tacs()).invokeGetStorageKey(), context.holder().getKey()))
                .onErrorResumeNext(throwable -> {
                    if (Config.recoverFromErrors) {
                        return Single.just(createEmptyProtoChunk(context));
                    } else {
                        return Single.error(throwable);
                    }
                })
                .doOnSuccess(chunk -> context.holder().getItem().set(new ChunkState(chunk, chunk, ChunkStatus.EMPTY)))
                .ignoreElement()
                .cache()
                .toCompletionStage(null);
    }

    protected static @NonNull Single<ProtoChunk> invokeVanillaLoad(ChunkLoadingContext context) {
        return invokeInitialChunkRead(context)
                .observeOn(Schedulers.from(((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor()))
                .map(chunkSerializer -> {
                    if (chunkSerializer.isPresent()) {
                        return chunkSerializer.get().convert(
                                ((IThreadedAnvilChunkStorage) context.tacs()).getWorld(),
                                ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage(),
                                ((IVersionedChunkStorage) context.tacs()).invokeGetStorageKey(),
                                context.holder().getKey()
                        );
                    } else {
                        return createEmptyProtoChunk(context);
                    }
                })
                .flatMap(protoChunk -> postChunkLoading(context, protoChunk).toSingleDefault(protoChunk));
    }

    protected static @NotNull Completable postChunkLoading(ChunkLoadingContext context, ProtoChunk protoChunk) {
        final ServerWorld world = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
        // blending
        final ChunkPos pos = context.holder().getKey();
        protoChunk = protoChunk != null ? protoChunk : new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, world, world.getRegistryManager().getOrThrow(RegistryKeys.BIOME), null);
        if (protoChunk.getBelowZeroRetrogen() != null || protoChunk.getStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            ProtoChunk finalProtoChunk = protoChunk;
            return Single.defer(() -> Single.fromCompletionStage(BlendingInfoUtil.getBlendingInfos(((IVersionedChunkStorage) context.tacs()).getWorker(), pos)))
                    .doOnSuccess(bitSets -> ((ProtoChunkExtension) finalProtoChunk).setBlendingInfo(pos, bitSets))
                    .ignoreElement();
        } else {
            return Completable.complete();
        }
    }

    protected static @NotNull Single<Optional<SerializedChunk>> invokeInitialChunkRead(ChunkLoadingContext context) {
        return Single.defer(() -> Single.fromCompletionStage(((IThreadedAnvilChunkStorage) context.tacs()).invokeGetUpdatedChunkNbt(context.holder().getKey())))
                .map(optional -> optional.map(nbtCompound -> {
                    ServerWorld world = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
                    SerializedChunk chunkSerializer = SerializedChunk.fromNbt(world, world.getRegistryManager(), nbtCompound);
                    if (chunkSerializer == null) {
                        LOGGER.error("Chunk file at {} is missing level data, skipping", context.holder().getKey());
                    }

                    return chunkSerializer;
                }))
                .zipWith(
                        Completable.defer(() -> Completable.fromCompletionStage(((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage().load(context.holder().getKey()))).toSingleDefault(ReadFromDisk.class),
                        (chunkSerializer, o) -> chunkSerializer
                );
    }

    protected static @NotNull ProtoChunk createEmptyProtoChunk(ChunkLoadingContext context) {
        final ServerWorld world = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
        return new ProtoChunk(context.holder().getKey(), UpgradeData.NO_UPGRADE_DATA, world, world.getRegistryManager().getOrThrow(RegistryKeys.BIOME), null);
    }

    @Override
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context) {
        final AtomicBoolean loadedToWorld = new AtomicBoolean(false);
        return syncWithLightEngine(context).thenApplyAsync(unused -> {
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
        if (!chunk.tryMarkSaved()) {
            return CompletableFuture.completedStage(null);
        } else {
            ChunkPos chunkPos = chunk.getPos();

            SerializedChunk serializer = SerializedChunk.fromChunk(((IThreadedAnvilChunkStorage) tacs).getWorld(), chunk);
            return CompletableFuture.supplyAsync(() -> {
                        return SerializerAccess.getSerializer().serialize(serializer);
                    }, GlobalExecutors.prioritizedScheduler.executor(16) /* boost priority as we are serializing an unloaded chunk */)
                    .thenCompose((either) -> {
                        if (either.left().isPresent()) {
                            NbtCompound nbtCompound = either.left().get();
                            return tacs.setNbt(chunkPos, () -> nbtCompound);
                        } else {
                            return ((IDirectStorage) ((IVersionedChunkStorage) tacs).getWorker()).setRawChunkData(chunkPos, either.right().get());
                        }
                    });
        }
    }

    protected CompletionStage<?> syncWithLightEngine(ChunkLoadingContext context) {
        return ((IThreadedAnvilChunkStorage) context.tacs()).getLightingProvider().enqueue(
                context.holder().getKey().x,
                context.holder().getKey().z
        );
    }

    @Override
    public String toString() {
        return "minecraft:empty";
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
