package com.ishland.c2me.base.mixin.instrumentation;

import com.ishland.c2me.base.common.scheduler.ISyncLoadManager;
import com.ishland.c2me.base.common.scheduler.IVanillaChunkManager;
import com.ishland.c2me.base.common.threadstate.SyncLoadWork;
import com.ishland.c2me.base.common.threadstate.ThreadInstrumentation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
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
    @Shadow @Final private ServerWorld world;
    @Unique
    private volatile ChunkPos currentSyncLoadChunk = null;
    @Unique
    private volatile long syncLoadNanos = 0;

    @Dynamic
    @WrapOperation(method = {
            "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;",
            "getChunkBlocking(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;",
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager$MainThreadExecutor;runTasks(Ljava/util/function/BooleanSupplier;)V"), require = 0)
    private void instrumentAwaitChunk(ServerChunkManager.MainThreadExecutor instance, BooleanSupplier stopCondition, Operation<Void> original, int x, int z, ChunkStatus leastStatus, boolean create) {
        if (Thread.currentThread() != this.serverThread || stopCondition.getAsBoolean()) return;

        this.currentSyncLoadChunk = new ChunkPos(x, z);
        syncLoadNanos = System.nanoTime();
        ((IVanillaChunkManager) this.chunkLoadingManager).c2me$getSchedulingManager().setCurrentSyncLoad(this.currentSyncLoadChunk);
        try (var ignored = ThreadInstrumentation.getCurrent().begin(new SyncLoadWork(this.world, new ChunkPos(x, z), leastStatus, create))) {
            original.call(instance, stopCondition);
        } finally {
            ((IVanillaChunkManager) this.chunkLoadingManager).c2me$getSchedulingManager().setCurrentSyncLoad(null);
            this.currentSyncLoadChunk = null;
        }
    }

    @WrapMethod(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;")
    private Chunk instrumentGetChunk(int x, int z, ChunkStatus leastStatus, boolean create, Operation<Chunk> original) {
        if (Thread.currentThread() != this.serverThread) {
            try (var ignored = ThreadInstrumentation.getCurrent().begin(new SyncLoadWork(this.world, new ChunkPos(x, z), leastStatus, create))) {
                return original.call(x, z, leastStatus, create);
            }
        } else {
            return original.call(x, z, leastStatus, create);
        }
    }

    @Override
    public ChunkPos getCurrentSyncLoad() {
        return this.currentSyncLoadChunk;
    }
}
