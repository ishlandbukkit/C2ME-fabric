package com.ishland.c2me.base.mixin.priority;

import com.ishland.c2me.base.common.scheduler.ISyncLoadManager;
import com.ishland.c2me.base.common.scheduler.IVanillaChunkManager;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager implements ISyncLoadManager {

    @Shadow
    @Final
    private Thread serverThread;

    @Shadow
    protected abstract boolean isMissingForLevel(@Nullable ChunkHolder holder, int maxLevel);

    @Shadow
    @Nullable
    protected abstract ChunkHolder getChunkHolder(long pos);

    @Shadow @Final public ServerChunkLoadingManager chunkLoadingManager;
    @Unique
    private volatile ChunkPos currentSyncLoadChunk = null;
    @Unique
    private volatile long syncLoadNanos = 0;

    @Dynamic
    @Redirect(method = {
            "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;",
            "getChunkBlocking(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;",
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager$MainThreadExecutor;runTasks(Ljava/util/function/BooleanSupplier;)V"), require = 0)
    private void beforeAwaitChunk(ServerChunkManager.MainThreadExecutor instance, BooleanSupplier supplier, int x, int z, ChunkStatus leastStatus, boolean create) {
        if (Thread.currentThread() != this.serverThread || supplier.getAsBoolean()) return;

        this.currentSyncLoadChunk = new ChunkPos(x, z);
        syncLoadNanos = System.nanoTime();
        ((IVanillaChunkManager) this.chunkLoadingManager).c2me$getSchedulingManager().setCurrentSyncLoad(this.currentSyncLoadChunk);
        instance.runTasks(supplier);
        ((IVanillaChunkManager) this.chunkLoadingManager).c2me$getSchedulingManager().setCurrentSyncLoad(null);
        this.currentSyncLoadChunk = null;
    }

    @Override
    public ChunkPos getCurrentSyncLoad() {
        return this.currentSyncLoadChunk;
    }
}
