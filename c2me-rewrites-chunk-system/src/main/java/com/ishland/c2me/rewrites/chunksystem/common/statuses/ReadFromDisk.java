package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.ishland.c2me.base.common.config.ModStatuses;
import com.ishland.c2me.base.common.util.RxJavaUtils;
import com.ishland.c2me.base.mixin.access.IServerLightingProvider;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.base.mixin.access.IVersionedChunkStorage;
import com.ishland.c2me.base.mixin.access.IWorldChunk;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.c2me.rewrites.chunksystem.common.ducks.IPOIUnloading;
import com.ishland.c2me.rewrites.chunksystem.common.fapi.LifecycleEventInvoker;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ReadFromDisk extends NewChunkStatus {

    private static final Logger LOGGER = LoggerFactory.getLogger("ReadFromDisk");

    public ReadFromDisk(int ordinal) {
        super(ordinal, ChunkStatus.EMPTY);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
        final Single<ProtoChunk> single = invokeSyncRead(context)
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

    protected static @NonNull Single<ProtoChunk> invokeSyncRead(ChunkLoadingContext context) {
        return Single.defer(() -> Single.fromCompletionStage(((IThreadedAnvilChunkStorage) context.tacs()).invokeGetUpdatedChunkNbt(context.holder().getKey())))
                .map(nbt -> nbt.filter(nbt2 -> {
                    boolean bl = nbt2.contains("Status", NbtElement.STRING_TYPE);
                    if (!bl) {
                        LOGGER.error("Chunk file at {} is missing level data, skipping", context.holder().getKey());
                    }

                    return bl;
                }))
                .observeOn(Schedulers.from(((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor()))
                .map(nbt -> {
                    if (nbt.isPresent()) {
                        ProtoChunk chunk = ChunkSerializer.deserialize(
                                ((IThreadedAnvilChunkStorage) context.tacs()).getWorld(),
                                ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage(),
                                ((IVersionedChunkStorage) context.tacs()).invokeGetStorageKey(),
                                context.holder().getKey(),
                                nbt.get()
                        );
                        return chunk;
                    } else {
                        return createEmptyProtoChunk(context);
                    }
                })
                .doOnError(throwable -> LOGGER.warn("Failed to load chunk at {}", context.holder().getKey(), throwable));
    }

    protected static @NotNull ProtoChunk createEmptyProtoChunk(ChunkLoadingContext context) {
        final ServerWorld world = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
        return new ProtoChunk(context.holder().getKey(), UpgradeData.NO_UPGRADE_DATA, world, world.getRegistryManager().get(RegistryKeys.BIOME), null);
    }

    @Override
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context, Cancellable cancellable) {
        return syncWithLightEngine(context).thenRunAsync(() -> {
            if (context.holder().getTargetStatus().ordinal() >= this.ordinal()) { // saving cancelled
                cancellable.cancel();
                throw new CancellationException();
            }

            final ChunkState chunkState = context.holder().getItem().get();
            Chunk chunk = chunkState.chunk();
            if (chunk instanceof WrapperProtoChunk protoChunk) chunk = protoChunk.getWrappedChunk();

            final boolean loadedToWorld;
            if (chunk instanceof WorldChunk worldChunk) {
                loadedToWorld = ((IWorldChunk) worldChunk).isLoadedToWorld();
                worldChunk.setLoadedToWorld(false);
            } else {
                loadedToWorld = false;
            }

            if (loadedToWorld && ModStatuses.fabric_lifecycle_events_v1 && chunk instanceof WorldChunk worldChunk) {
                LifecycleEventInvoker.invokeChunkUnload(((IThreadedAnvilChunkStorage) context.tacs()).getWorld(), worldChunk);
            }

            if ((context.holder().getFlags() & ItemHolder.FLAG_BROKEN) != 0 && chunk instanceof ProtoChunk) { // do not save broken ProtoChunks
                LOGGER.warn("Not saving partially generated broken chunk {}", context.holder().getKey());
            } else if (chunk instanceof WorldChunk && !chunkState.reachedStatus().isAtLeast(ChunkStatus.FULL)) {
                // do not save WorldChunks that doesn't reach full status: Vanilla behavior
                // If saved, block entities will be lost
            } else {
                ((IThreadedAnvilChunkStorage) context.tacs()).invokeSave(chunk);
            }

            if (loadedToWorld && chunk instanceof WorldChunk worldChunk) {
                ((IThreadedAnvilChunkStorage) context.tacs()).getWorld().unloadEntities(worldChunk);
            }

            ((IServerLightingProvider) ((IThreadedAnvilChunkStorage) context.tacs()).getLightingProvider()).invokeUpdateChunkStatus(chunk.getPos());
            ((IThreadedAnvilChunkStorage) context.tacs()).getLightingProvider().tick();
            ((IThreadedAnvilChunkStorage) context.tacs()).getWorldGenerationProgressListener().setChunkStatus(chunk.getPos(), null);
            ((IThreadedAnvilChunkStorage) context.tacs()).getChunkToNextSaveTimeMs().remove(chunk.getPos().toLong());

            final WorldGenerationProgressListener listener = ((IThreadedAnvilChunkStorage) context.tacs()).getWorldGenerationProgressListener();
            if (listener != null) {
                listener.setChunkStatus(context.holder().getKey(), null);
            }

            ((IPOIUnloading) ((IThreadedAnvilChunkStorage) context.tacs()).getPointOfInterestStorage()).c2me$unloadPoi(context.holder().getKey());

            context.holder().getItem().set(new ChunkState(null, null, null));
        }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
    }

    protected CompletionStage<Void> syncWithLightEngine(ChunkLoadingContext context) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        ((IServerLightingProvider) ((IThreadedAnvilChunkStorage) context.tacs()).getLightingProvider()).invokeEnqueue(
                context.holder().getKey().x,
                context.holder().getKey().z,
                () -> 0,
                ServerLightingProvider.Stage.POST_UPDATE,
                () -> future.complete(null)
        );
        return future;
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
