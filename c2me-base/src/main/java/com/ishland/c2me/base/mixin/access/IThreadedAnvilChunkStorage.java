package com.ishland.c2me.base.mixin.access;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerationContext;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerChunkLoadingManager.class)
public interface IThreadedAnvilChunkStorage {

    @Accessor
    ServerWorld getWorld();

    @Invoker
    boolean invokeUpdateHolderMap();

    @Invoker
    boolean invokeSave(Chunk chunk);

    @Accessor
    ThreadExecutor<Runnable> getMainThreadExecutor();

    @Accessor
    ServerLightingProvider getLightingProvider();

    @Accessor
    ChunkGenerationContext getGenerationContext();

    @Invoker
    void invokeSendToPlayers(ChunkHolder chunkHolder, WorldChunk chunk);

    @Accessor
    WorldGenerationProgressListener getWorldGenerationProgressListener();

    @Accessor
    AtomicInteger getTotalChunksLoadedCount();

    @Invoker
    ChunkHolder invokeGetChunkHolder(long pos);

    @Invoker
    void invokeOnChunkStatusChange(ChunkPos chunkPos, ChunkLevelType levelType);

    @Accessor
    Long2LongMap getChunkToNextSaveTimeMs();

    @Accessor
    Long2ObjectLinkedOpenHashMap<ChunkHolder> getCurrentChunkHolders();

    @Accessor
    void setChunkHolderListDirty(boolean value);

    @Invoker
    CompletableFuture<Optional<NbtCompound>> invokeGetUpdatedChunkNbt(ChunkPos chunkPos);

    @Accessor
    PointOfInterestStorage getPointOfInterestStorage();

    @Accessor
    ServerChunkLoadingManager.LevelManager getLevelManager();

}
