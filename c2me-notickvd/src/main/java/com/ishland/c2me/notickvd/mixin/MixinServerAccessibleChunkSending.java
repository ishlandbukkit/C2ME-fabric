package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.base.common.threadstate.ThreadInstrumentation;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessibleChunkSending;
import com.ishland.c2me.rewrites.chunksystem.common.threadstate.ChunkTaskWork;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Mixin(ServerAccessibleChunkSending.class)
public class MixinServerAccessibleChunkSending {

    @Mutable
    @Shadow(remap = false)
    @Final
    private static KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] deps;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onCLInit(CallbackInfo ci) {
        NewChunkStatus depStatus = NewChunkStatus.fromVanillaStatus(ChunkStatus.LIGHT);
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

    /**
     * @author ishland
     * @reason do chunk sending
     */
    @Overwrite(remap = false)
    public CompletionStage<Void> upgradeToThis(ChunkLoadingContext context, Cancellable cancellable) {
        ArrayList<BlockPos> blocksToRemove = new ArrayList<>();
        if (Config.suppressGhostMushrooms) {
            ServerWorld serverWorld = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
            ChunkState state = context.holder().getItem().get();
            ChunkRegion chunkRegion = new ChunkRegion(serverWorld, context.chunks(), ChunkGenerationSteps.GENERATION.get(ChunkStatus.FULL), state.protoChunk());
            Chunk chunk = state.chunk();

            ChunkPos chunkPos = context.holder().getKey();

            ShortList[] postProcessingLists = chunk.getPostProcessingLists();
            for (int i = 0; i < postProcessingLists.length; i++) {
                if (postProcessingLists[i] != null) {
                    for (ShortListIterator iterator = postProcessingLists[i].iterator(); iterator.hasNext(); ) {
                        short short_ = iterator.nextShort();
                        BlockPos blockPos = ProtoChunk.joinBlockPos(short_, chunk.sectionIndexToCoord(i), chunkPos);
                        BlockState blockState = chunk.getBlockState(blockPos);

                        if (blockState.getBlock() == Blocks.BROWN_MUSHROOM || blockState.getBlock() == Blocks.RED_MUSHROOM) {
                            if (!blockState.canPlaceAt(chunkRegion, blockPos)) {
                                blocksToRemove.add(blockPos);
                            }
                        }
                    }
                }
            }
        }
        return CompletableFuture.runAsync(() -> {
            try (var ignored = ThreadInstrumentation.getCurrent().begin(new ChunkTaskWork(context, (ServerAccessibleChunkSending) (Object) this, true))) {
                ServerWorld serverWorld = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();
                for (BlockPos blockPos : blocksToRemove) {
                    serverWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState(), Block.NO_REDRAW | Block.FORCE_STATE);
                }
                sendChunkToPlayer(context.tacs(), context.holder());
            }
        }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
    }

    @Unique
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

}
