package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.google.common.base.Preconditions;
import com.ishland.c2me.base.common.config.ModStatuses;
import com.ishland.c2me.base.common.threadstate.ThreadInstrumentation;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.base.mixin.access.IWorldChunk;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.c2me.rewrites.chunksystem.common.compat.lithium.LithiumChunkStatusTrackerInvoker;
import com.ishland.c2me.rewrites.chunksystem.common.fapi.LifecycleEventInvoker;
import com.ishland.c2me.rewrites.chunksystem.common.threadstate.ChunkTaskWork;
import com.ishland.flowsched.scheduler.Cancellable;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ServerAccessible extends NewChunkStatus {

    public ServerAccessible(int ordinal) {
        super(ordinal, ChunkStatus.FULL);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
        final Chunk chunk = context.holder().getItem().get().chunk();
        Preconditions.checkState(chunk instanceof ProtoChunk, "Chunk must be a proto chunk");
        ProtoChunk protoChunk = (ProtoChunk) chunk;

        if (Config.suppressGhostMushrooms) {
            ServerWorld serverWorld = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
            ChunkRegion chunkRegion = new ChunkRegion(serverWorld, context.chunks(), ChunkGenerationSteps.GENERATION.get(ChunkStatus.FULL), chunk);

            ChunkPos chunkPos = context.holder().getKey();

            ShortList[] postProcessingLists = protoChunk.getPostProcessingLists();
            for (int i = 0; i < postProcessingLists.length; i++) {
                if (postProcessingLists[i] != null) {
                    for (ShortListIterator iterator = postProcessingLists[i].iterator(); iterator.hasNext(); ) {
                        short short_ = iterator.nextShort();
                        BlockPos blockPos = ProtoChunk.joinBlockPos(short_, protoChunk.sectionIndexToCoord(i), chunkPos);
                        BlockState blockState = protoChunk.getBlockState(blockPos);

                        if (blockState.getBlock() == Blocks.BROWN_MUSHROOM || blockState.getBlock() == Blocks.RED_MUSHROOM) {
                            if (!blockState.canPlaceAt(chunkRegion, blockPos)) {
                                protoChunk.setBlockState(blockPos, Blocks.AIR.getDefaultState(), false); // TODO depends on the fact that the chunk system always locks the current chunk
                            }
                        }
                    }
                }
            }
        }

        return CompletableFuture.runAsync(() -> {
            try (var ignored = ThreadInstrumentation.getCurrent().begin(new ChunkTaskWork(context, this, true))) {
                ServerWorld serverWorld = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
                final WorldChunk worldChunk = toFullChunk(protoChunk, serverWorld);

                worldChunk.setLevelTypeProvider(context.holder().getUserData().get()::getLevelType);
                worldChunk.setUnsavedListener(((IThreadedAnvilChunkStorage) context.tacs()).getGenerationContext().unsavedListener());
                context.holder().getItem().set(new ChunkState(worldChunk, new WrapperProtoChunk(worldChunk, false), ChunkStatus.FULL));
                if (!((IWorldChunk) worldChunk).isLoadedToWorld()) {
                    worldChunk.loadEntities();
                    worldChunk.setLoadedToWorld(true);
                    worldChunk.updateAllBlockEntities();
                    worldChunk.addChunkTickSchedulers(serverWorld);
                    if (ModStatuses.fabric_lifecycle_events_v1) {
                        LifecycleEventInvoker.invokeChunkLoaded(serverWorld, worldChunk, !(protoChunk instanceof WrapperProtoChunk));
                    }
                }

                ((IThreadedAnvilChunkStorage) context.tacs()).getCurrentChunkHolders().put(context.holder().getKey().toLong(), context.holder().getUserData().get());
                ((IThreadedAnvilChunkStorage) context.tacs()).setChunkHolderListDirty(true);
            }
        }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
    }

    private static WorldChunk toFullChunk(ProtoChunk protoChunk, ServerWorld serverWorld) {
        WorldChunk worldChunk;
        if (protoChunk instanceof WrapperProtoChunk) {
            worldChunk = ((WrapperProtoChunk) protoChunk).getWrappedChunk();
        } else {
            worldChunk = new WorldChunk(serverWorld, protoChunk, worldChunkx -> {
                final List<NbtCompound> entities = protoChunk.getEntities();
                if (!entities.isEmpty()) {
                    serverWorld.addEntities(EntityType.streamFromNbt(entities, serverWorld, SpawnReason.LOAD));
                }
            });
        }
        return worldChunk;
    }

    @Override
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context, Cancellable cancellable) {
        ChunkState state = context.holder().getItem().get();
        final Chunk chunk = state.chunk();
        Preconditions.checkState(chunk instanceof WorldChunk, "Chunk must be a full chunk");
        return CompletableFuture.runAsync(() -> {
            ((IThreadedAnvilChunkStorage) context.tacs()).getCurrentChunkHolders().remove(context.holder().getKey().toLong());
            ((IThreadedAnvilChunkStorage) context.tacs()).setChunkHolderListDirty(true);
            final WorldChunk worldChunk = (WorldChunk) chunk;
//            worldChunk.setLoadedToWorld(false);
//            worldChunk.removeChunkTickSchedulers(((IThreadedAnvilChunkStorage) context.tacs()).getWorld());
            worldChunk.setLevelTypeProvider(null);
            worldChunk.setUnsavedListener(pos -> {
            });
            LithiumChunkStatusTrackerInvoker.invokeOnChunkInaccessible(((IThreadedAnvilChunkStorage) context.tacs()).getWorld(), context.holder().getKey());
            context.holder().getItem().set(new ChunkState(state.protoChunk(), state.protoChunk(), ChunkStatus.FULL));
        }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
    }

    @Override
    public String toString() {
        return "minecraft:full, Border";
    }
}
