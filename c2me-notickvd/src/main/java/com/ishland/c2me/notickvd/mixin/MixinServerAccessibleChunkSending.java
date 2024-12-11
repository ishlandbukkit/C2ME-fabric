package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessibleChunkSending;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
        return CompletableFuture.runAsync(() -> {
            if (Config.suppressGhostMushrooms) {
                final WorldChunk chunk = (WorldChunk) context.holder().getItem().get().chunk();
                ServerWorld world = ((IThreadedAnvilChunkStorage) context.tacs()).getWorld();

                ChunkPos chunkPos = context.holder().getKey();

                ShortList[] postProcessingLists = chunk.getPostProcessingLists();
                for (int i = 0; i < postProcessingLists.length; i++) {
                    if (postProcessingLists[i] != null) {
                        for (ShortListIterator iterator = postProcessingLists[i].iterator(); iterator.hasNext(); ) {
                            short short_ = iterator.nextShort();
                            BlockPos blockPos = ProtoChunk.joinBlockPos(short_, chunk.sectionIndexToCoord(i), chunkPos);
                            BlockState blockState = chunk.getBlockState(blockPos);

                            if (!(blockState.getBlock() instanceof FluidBlock)) {
                                BlockState blockState2 = Block.postProcessState(blockState, world, blockPos);
                                if (blockState2 != blockState) {
                                    world.setBlockState(blockPos, blockState2, Block.NO_REDRAW | Block.FORCE_STATE);
                                }
                            }
                        }
                    }
                }
            }
            sendChunkToPlayer(context.tacs(), context.holder());
        }, ((IThreadedAnvilChunkStorage) context.tacs()).getMainThreadExecutor());
    }

    @Unique
    private static void sendChunkToPlayer(ServerChunkLoadingManager tacs, ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder) {
        final WorldChunk worldChunk = (WorldChunk) holder.getItem().get().chunk();
        NewChunkHolderVanillaInterface holderVanillaInterface = holder.getUserData().get();
        CompletableFuture<?> completableFuturexx = holderVanillaInterface.getPostProcessingFuture();
        if (completableFuturexx.isDone()) {
            ((IThreadedAnvilChunkStorage) tacs).invokeSendToPlayers(holderVanillaInterface, worldChunk);
        } else {
            completableFuturexx.thenAcceptAsync(v -> ((IThreadedAnvilChunkStorage) tacs).invokeSendToPlayers(holderVanillaInterface, worldChunk), ((IThreadedAnvilChunkStorage) tacs).getMainThreadExecutor());
        }
    }

}
