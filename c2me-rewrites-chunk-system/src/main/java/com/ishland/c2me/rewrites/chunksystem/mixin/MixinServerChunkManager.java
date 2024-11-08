package com.ishland.c2me.rewrites.chunksystem.mixin;

import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WrapperProtoChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager {

    @Shadow @Final private Thread serverThread;

    @Shadow @Final public ServerChunkLoadingManager chunkLoadingManager;

    @Unique
    private long c2me$lastHolderUpdate = System.nanoTime();

    @Shadow
    protected abstract @Nullable ChunkHolder getChunkHolder(long pos);

    @Shadow @Final private long[] chunkPosCache;

    @Shadow @Final private ChunkStatus[] chunkStatusCache;

    @Shadow @Final private Chunk[] chunkCache;

    @Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true)
    private void shortcutGetChunk(int x, int z, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<Chunk> cir) {
        if (Thread.currentThread() != this.serverThread) {
            Chunk chunk = this.c2me$fastpathExistingChunks(x, z, leastStatus);
            if (chunk != null) {
                cir.setReturnValue(chunk);
                return;
            } else if (!create) {
                cir.setReturnValue(null);
                return;
            }
        } else { // on server thread
            if (!create) { // no create required
                Chunk chunk = this.c2me$getFromCahe(x, z, leastStatus, false);
                if (chunk != null) {
                    cir.setReturnValue(chunk);
                } else {
                    cir.setReturnValue(this.c2me$fastpathExistingChunks(x, z, leastStatus));
                }
                return;
            }
        }
    }

    @Unique
    private Chunk c2me$getFromCahe(int x, int z, ChunkStatus leastStatus, boolean create) {
        long l = ChunkPos.toLong(x, z);

        if (this.chunkPosCache == null || this.chunkStatusCache == null || this.chunkCache == null) {
            return null; // no cache
        }

        for (int i = 0; i < 4; i++) {
            if (l == this.chunkPosCache[i] && leastStatus == this.chunkStatusCache[i]) {
                Chunk chunk = this.chunkCache[i];
                if (chunk != null || !create) {
                    return chunk;
                }
            }
        }

        return null;
    }

    @Unique
    private Chunk c2me$fastpathExistingChunks(int x, int z, ChunkStatus leastStatus) {
        final ChunkHolder holder = this.getChunkHolder(ChunkPos.toLong(x, z));
        if (holder != null) {
            final CompletableFuture<OptionalChunk<Chunk>> future = holder.load(leastStatus, this.chunkLoadingManager); // thread-safe in new system
            return future.getNow(ChunkHolder.UNLOADED).orElse(null);
        }
        return null;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;updateChunks()Z", shift = At.Shift.AFTER))
    private void updateHolderMapAfterTick(CallbackInfo ci) {
        ((IThreadedAnvilChunkStorage) this.chunkLoadingManager).invokeUpdateHolderMap();
    }

    @WrapOperation(method = "updateChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;updateHolderMap()Z"))
    private boolean disableUpdateHolderMapOnTask(ServerChunkLoadingManager instance, Operation<Boolean> original) { // holder map only used for compatibility layer
        if (System.nanoTime() - c2me$lastHolderUpdate > 50_000_000L) { // 50ms
            c2me$lastHolderUpdate = System.nanoTime();
            return original.call(instance);
        }
        return false;
    }

}
