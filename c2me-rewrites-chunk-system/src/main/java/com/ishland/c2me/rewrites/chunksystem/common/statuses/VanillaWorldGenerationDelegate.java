package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.ishland.c2me.base.common.scheduler.LockTokenImpl;
import com.ishland.c2me.base.common.scheduler.ScheduledTask;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.flowsched.executor.LockToken;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class VanillaWorldGenerationDelegate extends NewChunkStatus {

    private static final Logger LOGGER = LoggerFactory.getLogger("VanillaWorldGenerationDelegate");

    private static KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependencyFromStep(ChunkGenerationStep step) {
        ArrayList<KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>> deps = new ArrayList<>();
        final GenerationDependencies directDependencies = step.directDependencies();
        for (int x = -directDependencies.getMaxLevel(); x <= directDependencies.getMaxLevel(); x++) {
            for (int z = -directDependencies.getMaxLevel(); z <= directDependencies.getMaxLevel(); z++) {
                if (x == 0 && z == 0) continue;
                final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext> dep =
                        new KeyStatusPair<>(
                                new ChunkPos(x, z), fromVanillaStatus(directDependencies.get(Math.max(Math.abs(x), Math.abs(z))))
                        );
                deps.add(dep);
            }
        }

        return deps.toArray(KeyStatusPair[]::new);
    }

    private static <T> CompletableFuture<T> runTaskWithLock(ChunkPos target, int radius, SchedulingManager schedulingManager, Supplier<CompletableFuture<T>> action) {
        ObjectArrayList<LockToken> lockTargets = new ObjectArrayList<>((2 * radius + 1) * (2 * radius + 1) + 1);
        for (int x = target.x - radius; x <= target.x + radius; x++)
            for (int z = target.z - radius; z <= target.z + radius; z++)
                lockTargets.add(new LockTokenImpl(schedulingManager.getId(), ChunkPos.toLong(x, z), LockTokenImpl.Usage.WORLDGEN));

        final ScheduledTask<T> task = new ScheduledTask<>(
                target.toLong(),
                action,
                lockTargets.toArray(LockToken[]::new));
        schedulingManager.enqueue(task);
        return task.getFuture();
    }

    private final ChunkStatus status;
    private final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] genDeps;
    private final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] loadDeps;
    @Nullable
    private final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] toRemove;
    @Nullable
    private final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] toAdd;

    public VanillaWorldGenerationDelegate(int ordinal, ChunkStatus status) {
        super(ordinal, status);
        this.status = status;
        final ChunkGenerationStep genStep = ChunkGenerationSteps.GENERATION.get(status);
        final ChunkGenerationStep loadStep = ChunkGenerationSteps.LOADING.get(status);
        this.genDeps = getDependencyFromStep(genStep);
        this.loadDeps = getDependencyFromStep(loadStep);

        if (this.genDeps.length != this.loadDeps.length) {
            ObjectOpenHashSet<KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>> toRemove = new ObjectOpenHashSet<>(genDeps);
            toRemove.removeAll(List.of(loadDeps));
            this.toRemove = toRemove.toArray(KeyStatusPair[]::new);

            ObjectOpenHashSet<KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>> toAdd = new ObjectOpenHashSet<>(loadDeps);
            toAdd.removeAll(List.of(genDeps));
            this.toAdd = toAdd.toArray(KeyStatusPair[]::new);
        } else {
            if (Arrays.equals(this.genDeps, this.loadDeps)) {
                this.toRemove = EMPTY_DEPENDENCIES;
                this.toAdd = EMPTY_DEPENDENCIES;
            } else {
                LOGGER.warn("VanillaWorldGenerationDelegate with status {} has the same dependencies length for generation and loading", status);
                this.toRemove = null;
                this.toAdd = null;
            }
        }
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
//        if (context.holder().getKey().equals(new ChunkPos(100, 100)) && this.status == ChunkStatus.FEATURES) {
//            throw new RuntimeException("boom");
//        }
        final ChunkState state = context.holder().getItem().get();
        if (state.reachedStatus().isAtLeast(this.status)) {
            return CompletableFuture.completedStage(null);
        }
        final ChunkGenerationContext chunkGenerationContext = ((IThreadedAnvilChunkStorage) context.tacs()).getGenerationContext();
        Chunk chunk = state.chunk();
        if (chunk.getStatus().isAtLeast(status)) {
            return ChunkGenerationSteps.LOADING.get(status)
                    .run(((IThreadedAnvilChunkStorage) context.tacs()).getGenerationContext(), context.chunks(), chunk)
                    .whenComplete((chunk1, throwable) -> {
                        if (chunk1 != null) {
                            context.holder().getItem().set(new ChunkState(chunk1, (ProtoChunk) chunk1, this.status));
                        }
                    }).thenAccept(__ -> {});
        } else {
            final ChunkGenerationStep step = ChunkGenerationSteps.GENERATION.get(status);

            int radius = Math.max(0, step.blockStateWriteRadius());
            return runTaskWithLock(chunk.getPos(), radius, context.schedulingManager(),
                    () -> step.run(chunkGenerationContext, context.chunks(), chunk)
                            .whenComplete((chunk1, throwable) -> {
                                if (chunk1 != null) {
                                    context.holder().getItem().set(new ChunkState(chunk1, (ProtoChunk) chunk1, this.status));
                                }
                            }).thenAccept(__ -> {})
            );
        }
    }

    @Override
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context, Cancellable cancellable) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependencies(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        final Chunk chunk = holder.getItem().get().chunk();
        if (chunk == null) return genDeps;
        if (chunk.getStatus().isAtLeast(status)) {
            return relativeToAbsoluteDependencies(holder, loadDeps);
        } else {
            return relativeToAbsoluteDependencies(holder, genDeps);
        }
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependenciesToRemove(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        if (this.toRemove == null) return super.getDependenciesToRemove(holder);
        final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] curDep = holder.getDependencies(this);
        if (curDep.length == this.loadDeps.length) return EMPTY_DEPENDENCIES;
        if (curDep.length == this.genDeps.length) {
            final Chunk chunk = holder.getItem().get().chunk();
            if (chunk == null) return EMPTY_DEPENDENCIES;
            if (!chunk.getStatus().isAtLeast(status)) return EMPTY_DEPENDENCIES;
            return relativeToAbsoluteDependencies(holder, toRemove);
        }
        LOGGER.warn("Suspicious dependencies length for VanillaWorldGenerationDelegate with status {} on holder {}", this.status, holder.getKey());
        return super.getDependenciesToRemove(holder);
    }

    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependenciesToAdd(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        if (this.toAdd == null) return super.getDependenciesToAdd(holder);
        final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] curDep = holder.getDependencies(this);
        if (curDep.length == this.loadDeps.length) return EMPTY_DEPENDENCIES;
        if (curDep.length == this.genDeps.length) {
            final Chunk chunk = holder.getItem().get().chunk();
            if (chunk == null) return EMPTY_DEPENDENCIES;
            if (!chunk.getStatus().isAtLeast(status)) return EMPTY_DEPENDENCIES;
            return relativeToAbsoluteDependencies(holder, toAdd);
        }
        LOGGER.warn("Suspicious dependencies length for VanillaWorldGenerationDelegate with status {} on holder {}", this.status, holder.getKey());
        return super.getDependenciesToAdd(holder);
    }

    @Override
    public String toString() {
        return this.status.toString();
    }
}
