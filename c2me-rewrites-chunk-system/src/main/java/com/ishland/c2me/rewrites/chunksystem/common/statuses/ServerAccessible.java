package com.ishland.c2me.rewrites.chunksystem.common.statuses;

import com.google.common.base.Preconditions;
import com.ishland.c2me.base.common.config.ModStatuses;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.base.mixin.access.IWorldChunk;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.c2me.rewrites.chunksystem.common.fapi.LifecycleEventInvoker;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerationStep;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ServerAccessible extends NewChunkStatus {

    private static final KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] deps;

    static {
        final NewChunkStatus depStatus = NewChunkStatus.fromVanillaStatus(ChunkLevels.getStatusForAdditionalLevel(1));
        deps = new KeyStatusPair[]{
                new KeyStatusPair<>(new ChunkPos(-1, -1), depStatus),
                new KeyStatusPair<>(new ChunkPos(-1, 0), depStatus),
                new KeyStatusPair<>(new ChunkPos(-1, 1), depStatus),
                new KeyStatusPair<>(new ChunkPos(0, -1), depStatus),
                new KeyStatusPair<>(new ChunkPos(0, 1), depStatus),
                new KeyStatusPair<>(new ChunkPos(1, -1), depStatus),
                new KeyStatusPair<>(new ChunkPos(1, 0), depStatus),
                new KeyStatusPair<>(new ChunkPos(1, 1), depStatus),
        };
    }

    public ServerAccessible(int ordinal) {
        super(ordinal, ChunkStatus.FULL);
    }

    @Override
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context) {
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
            ServerWorld serverWorld = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
            final WorldChunk worldChunk = toFullChunk(protoChunk, serverWorld);

            worldChunk.setLevelTypeProvider(context.holder().getUserData().get()::getLevelType);
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

            if (needSendChunks()) {
                sendChunkToPlayer(context.tacs(), context.holder());
            }
        }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
    }

    private static boolean needSendChunks() {
        return false;
    }

    private static void sendChunkToPlayer(ServerChunkLoadingManager tacs, ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder) {
        final Chunk chunk = holder.getItem().get().chunk();
        if (chunk instanceof WorldChunk worldChunk) {
            NewChunkHolderVanillaInterface holderVanillaInterface = holder.getUserData().get();
            CompletableFuture<?> completableFuturexx = holderVanillaInterface.getPostProcessingFuture();
            if (completableFuturexx.isDone()) {
                ((IThreadedAnvilChunkStorage) tacs).invokeSendToPlayers(holderVanillaInterface, worldChunk);
            } else {
                completableFuturexx.thenAcceptAsync(v -> ((IThreadedAnvilChunkStorage) tacs).invokeSendToPlayers(holderVanillaInterface, worldChunk), ((IThreadedAnvilChunkStorage) tacs).getMainThreadExecutor());
            }
        }
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
    public CompletionStage<Void> downgradeFromThis(ChunkLoadingContext context) {
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
            context.holder().getItem().set(new ChunkState(state.protoChunk(), state.protoChunk(), ChunkStatus.FULL));
        }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
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
        return "minecraft:full, Border";
    }
}
