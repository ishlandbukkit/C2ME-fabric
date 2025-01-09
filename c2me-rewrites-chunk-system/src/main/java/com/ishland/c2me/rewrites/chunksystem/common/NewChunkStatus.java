package com.ishland.c2me.rewrites.chunksystem.common;

import com.ishland.c2me.rewrites.chunksystem.common.statuses.Deferred;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ReadFromDisk;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ReadFromDiskAsync;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessible;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessibleChunkSending;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerBlockTicking;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerEntityTicking;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.VanillaWorldGenerationDelegate;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

/**
 * Represents the status of a chunk in the chunk system.
 *
 * @implNote Subclasses should be immutable and should not have any non-final fields.
 */
public abstract class NewChunkStatus implements ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> {

    public static final NewChunkStatus[] ALL_STATUSES;

    public static final NewChunkStatus NEW;
    public static final NewChunkStatus DEFERRED;
    public static final NewChunkStatus DISK;
    private static final NewChunkStatus[] VANILLA_WORLDGEN_PIPELINE;
    public static final NewChunkStatus SERVER_ACCESSIBLE;
    public static final NewChunkStatus SERVER_ACCESSIBLE_CHUNK_SENDING;
    public static final NewChunkStatus BLOCK_TICKING;
    public static final NewChunkStatus ENTITY_TICKING;
    public static final NewChunkStatus[] vanillaLevelToStatus;

    static {
        ArrayList<NewChunkStatus> statuses = new ArrayList<>();
        NEW = new NewChunkStatus(statuses.size(), ChunkStatus.EMPTY) {
            @Override
            public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context, Cancellable cancellable) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int toVanillaLevel() {
                return ChunkLevels.INACCESSIBLE + 1;
            }
        };
        statuses.add(NEW);
        DEFERRED = new Deferred(statuses.size());
        statuses.add(DEFERRED);
        DISK = Config.asyncSerialization ? new ReadFromDiskAsync(statuses.size()) : new ReadFromDisk(statuses.size());
        statuses.add(DISK);
        VANILLA_WORLDGEN_PIPELINE = new NewChunkStatus[ChunkStatus.FULL.getIndex() + 1];
        for (ChunkStatus status : ChunkStatus.createOrderedList()) {
            if (status == ChunkStatus.EMPTY) {
                VANILLA_WORLDGEN_PIPELINE[status.getIndex()] = DISK;
                continue;
            } else if (status == ChunkStatus.FULL) {
                continue;
            }

            final NewChunkStatus newChunkStatus = new VanillaWorldGenerationDelegate(statuses.size(), status);
            statuses.add(newChunkStatus);
            VANILLA_WORLDGEN_PIPELINE[status.getIndex()] = newChunkStatus;
        }
        SERVER_ACCESSIBLE = new ServerAccessible(statuses.size());
        statuses.add(SERVER_ACCESSIBLE);
        VANILLA_WORLDGEN_PIPELINE[ChunkStatus.FULL.getIndex()] = SERVER_ACCESSIBLE;
        SERVER_ACCESSIBLE_CHUNK_SENDING = new ServerAccessibleChunkSending(statuses.size());
        statuses.add(SERVER_ACCESSIBLE_CHUNK_SENDING);
        BLOCK_TICKING = new ServerBlockTicking(statuses.size());
        statuses.add(BLOCK_TICKING);
        ENTITY_TICKING = new ServerEntityTicking(statuses.size());
        statuses.add(ENTITY_TICKING);

        vanillaLevelToStatus = IntStream.range(0, ChunkLevels.INACCESSIBLE + 2)
                .mapToObj(NewChunkStatus::fromVanillaStatus0).toArray(NewChunkStatus[]::new);
        ALL_STATUSES = statuses.toArray(NewChunkStatus[]::new);
    }

    private static NewChunkStatus fromVanillaStatus0(int level) {
        return switch (ChunkLevels.getType(level)) {
            case INACCESSIBLE -> {
                final ChunkStatus vanillaStatus = ChunkLevels.getStatus(level);
                if (vanillaStatus == null || vanillaStatus == ChunkStatus.EMPTY) {
                    if (level > ChunkLevels.INACCESSIBLE) {
                        yield NEW;
                    } else {
                        yield DISK;
                    }
                } else {
                    yield VANILLA_WORLDGEN_PIPELINE[vanillaStatus.getIndex()];
                }
            }
            case FULL -> SERVER_ACCESSIBLE;
            case BLOCK_TICKING -> BLOCK_TICKING;
            case ENTITY_TICKING -> ENTITY_TICKING;
        };
    }

    public static NewChunkStatus fromVanillaLevel(int level) {
        if (vanillaLevelToStatus == null) { // special case for static initialization
            return fromVanillaStatus0(level);
        }
        return vanillaLevelToStatus[MathHelper.clamp(level, 0, vanillaLevelToStatus.length - 1)];
    }

    public static NewChunkStatus fromVanillaStatus(ChunkStatus status) {
        if (status == null) {
            return NEW;
        } else {
            return VANILLA_WORLDGEN_PIPELINE[status.getIndex()];
        }
    }

    private final int ordinal;
    private final ChunkStatus effectiveVanillaStatus;

    protected NewChunkStatus(int ordinal, ChunkStatus effectiveVanillaStatus) {
        this.ordinal = ordinal;
        this.effectiveVanillaStatus = effectiveVanillaStatus;
    }

    @Override
    public ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext>[] getAllStatuses() {
        return ALL_STATUSES;
    }

    @Override
    public int ordinal() {
        return this.ordinal;
    }

    public ChunkStatus getEffectiveVanillaStatus() {
        return this.effectiveVanillaStatus;
    }

    public ChunkLevelType toChunkLevelType() {
        if (this.ordinal() < SERVER_ACCESSIBLE.ordinal()) return ChunkLevelType.INACCESSIBLE;
        if (this.ordinal() <= SERVER_ACCESSIBLE.ordinal()) return ChunkLevelType.FULL;
        if (this.ordinal() <= BLOCK_TICKING.ordinal()) return ChunkLevelType.BLOCK_TICKING;
        if (this.ordinal() <= ENTITY_TICKING.ordinal()) return ChunkLevelType.ENTITY_TICKING;
        throw new IncompatibleClassChangeError();
    }

    public int toVanillaLevel() {
        final ChunkLevelType chunkLevelType = this.toChunkLevelType();
        if (chunkLevelType == ChunkLevelType.INACCESSIBLE) {
            return ChunkLevels.getLevelFromStatus(this.getEffectiveVanillaStatus());
        } else {
            return ChunkLevels.getLevelFromType(chunkLevelType);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] getDependencies(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder) {
        return EMPTY_DEPENDENCIES;
    }

    protected static KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] relativeToAbsoluteDependencies(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, ?> holder, KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] relativeDependencies) {
        if (relativeDependencies.length == 0) return EMPTY_DEPENDENCIES;
        final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] dependencies = new KeyStatusPair[relativeDependencies.length];
        for (int i = 0; i < relativeDependencies.length; i++) {
            final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext> pair = relativeDependencies[i];
            dependencies[i] = new KeyStatusPair<>(new ChunkPos(pair.key().x + holder.getKey().x, pair.key().z + holder.getKey().z), pair.status());
        }
        return dependencies;
    }
}
