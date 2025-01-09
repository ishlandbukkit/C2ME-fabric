package com.ishland.c2me.rewrites.chunksystem.mixin.async_serialization;

import com.ishland.c2me.rewrites.chunksystem.common.async_chunkio.ProtoChunkExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkRegion.class)
public abstract class MixinChunkRegion implements StructureWorldAccess {

    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;method_66016(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V"))
    private void waitForFutureBeforeNotifyChanges(ServerWorld instance, BlockPos pos, BlockState oldBlock, BlockState newBlock, Operation<Void> operation) {
        final Chunk chunk = this.getChunk(pos);
        if (chunk instanceof ProtoChunk protoChunk) {
            final CompletableFuture<Void> future = ((ProtoChunkExtension) protoChunk).getInitialMainThreadComputeFuture();
            if (future != null && !future.isDone()) {
                future.thenRun(() -> operation.call(instance, pos, oldBlock, newBlock));
                return;
            }
        }
        operation.call(instance, pos, oldBlock, newBlock);
    }

}
