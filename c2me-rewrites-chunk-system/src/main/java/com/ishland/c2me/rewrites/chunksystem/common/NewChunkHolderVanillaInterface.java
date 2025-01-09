package com.ishland.c2me.rewrites.chunksystem.common;

import com.ishland.c2me.base.common.theinterface.IFastChunkHolder;
import com.ishland.c2me.base.common.util.SneakyThrow;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.StatusAdvancingScheduler;
import com.ishland.flowsched.util.Assertions;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkLoadingManager;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.AbstractChunkHolder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerationStep;
import net.minecraft.world.chunk.ChunkLoader;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class NewChunkHolderVanillaInterface extends ChunkHolder implements IFastChunkHolder {

    private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.createOrderedList();

    private final TheChunkSystem chunkSystem;
    private final ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> newHolder;
    private NewChunkStatus deferredStatus = null;
    private NewChunkStatus loadedDeferredStatus = null;

    public NewChunkHolderVanillaInterface(TheChunkSystem chunkSystem, ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> newHolder, HeightLimitView world, LightingProvider lightingProvider, PlayersWatchingChunkProvider playersWatchingChunkProvider) {
        super(newHolder.getKey(), ChunkLevels.INACCESSIBLE, world, lightingProvider, (pos1, levelGetter, targetLevel, levelSetter) -> {}, playersWatchingChunkProvider);
        this.chunkSystem = chunkSystem;
        this.newHolder = newHolder;
    }

    private CompletableFuture<OptionalChunk<Chunk>> wrapOptionalChunkFuture(CompletableFuture<?> future) {
        if (future.isDone()) {
            if (future.isCompletedExceptionally()) {
                Throwable throwable = future.exceptionNow();
                while (throwable instanceof CompletionException) {
                    throwable = throwable.getCause();
                }
                if (throwable == ItemHolder.UNLOADED_EXCEPTION) {
                    return ChunkHolder.UNLOADED_FUTURE;
                } else {
                    return (CompletableFuture<OptionalChunk<Chunk>>) future; // it's fine to cast here
                }
            } else {
                return CompletableFuture.completedFuture(OptionalChunk.of(this.newHolder.getItem().get().chunk()));
            }
        }
        return future.handle((unused, throwable) -> {
            while (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }
            if (throwable == ItemHolder.UNLOADED_EXCEPTION) {
                return ChunkHolder.UNLOADED;
            } else if (throwable != null) {
                SneakyThrow.sneaky(throwable);
                return null;
            } else {
                return OptionalChunk.of(this.newHolder.getItem().get().chunk());
            }
        });
    }

    private CompletableFuture<OptionalChunk<Chunk>> wrapOptionalChunkProtoFuture(CompletableFuture<?> future) {
        if (future.isDone()) {
            if (future.isCompletedExceptionally()) {
                Throwable throwable = future.exceptionNow();
                while (throwable instanceof CompletionException) {
                    throwable = throwable.getCause();
                }
                if (throwable == ItemHolder.UNLOADED_EXCEPTION) {
                    return ChunkHolder.UNLOADED_FUTURE;
                } else {
                    return (CompletableFuture<OptionalChunk<Chunk>>) future; // it's fine to cast here
                }
            } else {
                return CompletableFuture.completedFuture(OptionalChunk.of(this.newHolder.getItem().get().protoChunk()));
            }
        }
        return future.handle((unused, throwable) -> {
            while (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }
            if (throwable == ItemHolder.UNLOADED_EXCEPTION) {
                return ChunkHolder.UNLOADED;
            } else if (throwable != null) {
                SneakyThrow.sneaky(throwable);
                return null;
            } else {
                return OptionalChunk.of(this.newHolder.getItem().get().protoChunk());
            }
        });
    }

    private CompletableFuture<OptionalChunk<WorldChunk>> wrapOptionalWorldChunkFuture(CompletableFuture<?> future) {
        if (future.isDone()) {
            if (future.isCompletedExceptionally()) {
                Throwable throwable = future.exceptionNow();
                while (throwable instanceof CompletionException) {
                    throwable = throwable.getCause();
                }
                if (throwable == ItemHolder.UNLOADED_EXCEPTION) {
                    return ChunkHolder.UNLOADED_WORLD_CHUNK_FUTURE;
                } else {
                    return (CompletableFuture<OptionalChunk<WorldChunk>>) future; // it's fine to cast here
                }
            } else {
                final Chunk chunk = this.newHolder.getItem().get().chunk();
                if (chunk instanceof WorldChunk worldChunk) {
                    return CompletableFuture.completedFuture(OptionalChunk.of(worldChunk));
                } else {
                    return ChunkHolder.UNLOADED_WORLD_CHUNK_FUTURE; // might have unloaded at this point
                }
            }
        }
        return future.handle((unused, throwable) -> {
            while (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }
            if (throwable == ItemHolder.UNLOADED_EXCEPTION) {
                return ChunkHolder.UNLOADED_WORLD_CHUNK;
            } else if (throwable != null) {
                SneakyThrow.sneaky(throwable);
                return null;
            } else {
                final Chunk chunk = this.newHolder.getItem().get().chunk();
                if (chunk instanceof WorldChunk worldChunk) {
                    return OptionalChunk.of(worldChunk);
                } else {
                    return ChunkHolder.UNLOADED_WORLD_CHUNK; // might have unloaded at this point
                }
            }
        });
    }

    /**
     * @apiNote it is the caller's responsibility to ensure the holder is kept loaded
     */
    public void updateDeferredStatus(NewChunkStatus status) {
        synchronized (this) {
            if (this.deferredStatus == status) return;
            if (this.deferredStatus == null) { // && status != null
                Assertions.assertTrue(this.loadedDeferredStatus == null);
                this.deferredStatus = status;
                return;
            }
            if (status == null) { // && this.deferredStatus != null
                if (this.loadedDeferredStatus != null) {
                    ChunkPos pos1 = this.getPos();
                    this.chunkSystem.removeTicket(pos1, TicketTypeExtension.VANILLA_DEFERRED_LOAD, pos1, this.loadedDeferredStatus);
                    this.loadedDeferredStatus = null;
                }
                this.deferredStatus = status;
            }
            // both nonnull and different
            if (this.loadedDeferredStatus != null && this.loadedDeferredStatus.ordinal() > status.ordinal()) {
                ChunkPos pos1 = this.getPos();
                this.chunkSystem.removeTicket(pos1, TicketTypeExtension.VANILLA_DEFERRED_LOAD, pos1, this.loadedDeferredStatus);
                this.loadedDeferredStatus = null;
            }
            this.deferredStatus = status;
        }
    }

    private void triggerDeferredLoad(NewChunkStatus requestedStatus) {
        if (!Config.lowMemoryMode) return;
        synchronized (this) {
            if (this.loadedDeferredStatus != null && this.loadedDeferredStatus.ordinal() >= requestedStatus.ordinal()) {
                return; // nothing to do
            }
            if (this.deferredStatus == null || this.deferredStatus.ordinal() < requestedStatus.ordinal()) {
                return; // not deferred
            }
            // the holder should be valid here
            NewChunkStatus ticketToDiscard = this.loadedDeferredStatus;
            ChunkPos pos1 = this.getPos();
            ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder1 = this.chunkSystem.addTicket(pos1, TicketTypeExtension.VANILLA_DEFERRED_LOAD, pos1, requestedStatus, StatusAdvancingScheduler.NO_OP);
            Assertions.assertTrue(holder1 == this.newHolder);
            if (ticketToDiscard != null) {
                this.chunkSystem.removeTicket(pos1, TicketTypeExtension.VANILLA_DEFERRED_LOAD, pos1, ticketToDiscard);
            }
            this.loadedDeferredStatus = requestedStatus;
        }
    }

    @Override
    public CompletableFuture<OptionalChunk<Chunk>> load(ChunkStatus requestedStatus, ServerChunkLoadingManager chunkLoadingManager) {
        NewChunkStatus status = NewChunkStatus.fromVanillaStatus(requestedStatus);
        triggerDeferredLoad(status);
        final CompletableFuture<Void> futureForStatus = this.newHolder.getFutureForStatus0(status);
        return requestedStatus == ChunkStatus.FULL ? wrapOptionalChunkFuture(futureForStatus) : wrapOptionalChunkProtoFuture(futureForStatus);
    }

    @Override
    public CompletableFuture<OptionalChunk<WorldChunk>> getTickingFuture() {
        synchronized (this.newHolder) {
            return wrapOptionalWorldChunkFuture(this.newHolder.getFutureForStatus0(NewChunkStatus.BLOCK_TICKING));
        }
    }

    @Override
    public CompletableFuture<OptionalChunk<WorldChunk>> getEntityTickingFuture() {
        synchronized (this.newHolder) {
            return wrapOptionalWorldChunkFuture(this.newHolder.getFutureForStatus0(NewChunkStatus.ENTITY_TICKING));
        }
    }

    @Override
    public CompletableFuture<OptionalChunk<WorldChunk>> getAccessibleFuture() {
        synchronized (this.newHolder) {
            return wrapOptionalWorldChunkFuture(this.newHolder.getFutureForStatus0(NewChunkStatus.SERVER_ACCESSIBLE));
        }
    }

    @Nullable
    @Override
    public WorldChunk getWorldChunk() {
        return this.getTickingFuture().getNow(UNLOADED_WORLD_CHUNK).orElse(null); // TODO
    }

    @Override
    public CompletableFuture<?> getPostProcessingFuture() {
        return super.getPostProcessingFuture(); // use vanilla impl
    }

    @Nullable
    @Override
    public WorldChunk getPostProcessedChunk() {
        return super.getPostProcessedChunk(); // use vanilla impl
    }

    @Override
    public CompletableFuture<?> getSavingFuture() {
        return this.newHolder.getOpFuture(); // already safe to use as the implementation creates a new future
    }

    @Override
    public boolean markForBlockUpdate(BlockPos pos) {
        return super.markForBlockUpdate(pos); // use vanilla impl
    }

    @Override
    public boolean markForLightUpdate(LightType lightType, int y) {
        return super.markForLightUpdate(lightType, y); // use vanilla impl
    }

    @Override
    public void flushUpdates(WorldChunk chunk) {
        super.flushUpdates(chunk); // use vanilla impl
    }

    @Override
    public boolean hasPendingUpdates() {
        return super.hasPendingUpdates(); // use vanilla impl
    }

    @Override
    public void combinePostProcessingFuture(CompletableFuture<?> postProcessingFuture) {
        super.combinePostProcessingFuture(postProcessingFuture); // use vanilla impl
    }

    @Override
    public ChunkLevelType getLevelType() {
        return ChunkLevels.getType(this.getLevel());
    }

    @Override
    public ChunkPos getPos() {
        return this.newHolder.getKey();
    }

    @Override
    public int getLevel() {
        return ((NewChunkStatus) this.newHolder.getTargetStatus()).toVanillaLevel();
    }

    @Override
    public int getCompletedLevel() {
        return ((NewChunkStatus) this.newHolder.getStatus()).toVanillaLevel();
    }

    @Override
    public void setLevel(int level) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void updateFutures(ServerChunkLoadingManager chunkStorage, Executor executor) {
        // no-op
    }

    @Override
    public boolean isAccessible() {
        return this.newHolder.getStatus().ordinal() >= NewChunkStatus.SERVER_ACCESSIBLE.ordinal();
    }

    @Override
    public boolean isSavable() {
        return this.getSavingFuture().isDone();
    }

    @Override
    public void updateAccessibleStatus() {
        // no-op
    }

    @Override
    protected void updateStatus(ServerChunkLoadingManager chunkLoadingManager) {
        // no-op
    }

    @Override
    public void replaceWith(WrapperProtoChunk chunk) {
        // no-op
    }

    @Override
    public List<Pair<ChunkStatus, CompletableFuture<OptionalChunk<Chunk>>>> enumerateFutures() {
        List<Pair<ChunkStatus, CompletableFuture<OptionalChunk<Chunk>>>> list = new ArrayList<>();

        for (final ChunkStatus status : CHUNK_STATUSES) {
            final CompletableFuture<Void> futureForStatus = this.newHolder.getFutureForStatus0(NewChunkStatus.fromVanillaStatus(status));
            list.add(Pair.of(status, status == ChunkStatus.FULL ? wrapOptionalChunkFuture(futureForStatus) : wrapOptionalChunkProtoFuture(futureForStatus)));
        }

        return list;
    }

    @Override
    public void incrementRefCount() {
        super.incrementRefCount(); // use vanilla impl
    }

    @Override
    public void decrementRefCount() {
        super.decrementRefCount(); // use vanilla impl
    }

    @Nullable
    @Override
    public Chunk getUncheckedOrNull(ChunkStatus requestedStatus) {
        return this.newHolder.getStatus().ordinal() >= NewChunkStatus.fromVanillaStatus(requestedStatus).ordinal()
                ? this.newHolder.getItem().get().chunk() : null;
    }

    @Nullable
    @Override
    public Chunk getOrNull(ChunkStatus requestedStatus) {
        return this.newHolder.getTargetStatus().ordinal() >= NewChunkStatus.fromVanillaStatus(requestedStatus).ordinal()
                ? this.getUncheckedOrNull(requestedStatus) : null;
    }

    @Nullable
    @Override
    public Chunk getLatest() {
        return this.newHolder.getItem().get().chunk();
    }

    @Nullable
    @Override
    public ChunkStatus getActualStatus() {
        final Chunk chunk = this.getLatest();
        return chunk != null ? chunk.getStatus() : null;
    }

    @Nullable
    @Override
    public ChunkStatus getLatestStatus() {
        return ((NewChunkStatus) this.newHolder.getStatus()).getEffectiveVanillaStatus();
    }

    @Override
    protected void combineSavingFuture(CompletableFuture<?> completableFuture) {
        this.newHolder.submitOp(completableFuture.thenAccept(o -> {}));
    }

    @Override
    protected void setCompletedLevel(int level) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected CompletableFuture<OptionalChunk<Chunk>> generate(ChunkGenerationStep step, ChunkLoadingManager chunkLoadingManager, BoundedRegionArray<AbstractChunkHolder> chunks) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void clearLoader(ChunkLoader loader) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createLoader(ServerChunkLoadingManager chunkLoadingManager, @Nullable ChunkStatus requestedStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected CompletableFuture<OptionalChunk<Chunk>> getOrCreateFuture(ChunkStatus status) {
        NewChunkStatus status1 = NewChunkStatus.fromVanillaStatus(status);
        triggerDeferredLoad(status1);
        final CompletableFuture<Void> futureForStatus = this.newHolder.getFutureForStatus0(status1);
        return status == ChunkStatus.FULL ? this.wrapOptionalChunkFuture(futureForStatus) : this.wrapOptionalChunkProtoFuture(futureForStatus);
    }

    @Override
    protected void unload(@Nullable ChunkStatus from, ChunkStatus to) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void unload(int statusIndex, CompletableFuture<OptionalChunk<Chunk>> previousFuture) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void completeChunkFuture(ChunkStatus status, Chunk chunk) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    protected ChunkStatus getMaxPendingStatus(@Nullable ChunkStatus checkUpperBound) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected boolean progressStatus(ChunkStatus nextStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean cannotBeLoaded(ChunkStatus status) {
        ChunkStatus chunkStatus = ((NewChunkStatus) this.newHolder.getTargetStatus()).getEffectiveVanillaStatus();
        return chunkStatus == null || status.isLaterThan(chunkStatus);
    }

    @Override
    public WorldChunk c2me$immediateWorldChunk() {
        final Chunk chunk = this.newHolder.getItem().get().chunk();
        if (chunk instanceof WorldChunk worldChunk) {
            return worldChunk;
        } else {
            return null;
        }
    }

    public ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> getBackingHolder() {
        return this.newHolder;
    }
}
