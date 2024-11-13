package com.ishland.c2me.opts.scheduling.mixin.idle_tasks.autosave.enhanced_autosave;

import com.ishland.c2me.opts.scheduling.common.idle_tasks.IThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerChunkLoadingManager.class)
public abstract class MixinThreadedAnvilChunkStorage implements IThreadedAnvilChunkStorage {

    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> currentChunkHolders;

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkHolders;

    @Shadow @Final private ThreadExecutor<Runnable> mainThreadExecutor;

    @Shadow @Final private ServerWorld world;

    @Shadow protected abstract boolean save(ChunkHolder chunkHolder, long currentTime);

    @Shadow @Final private AtomicInteger chunksBeingSavedCount;
    @Shadow @Final private LongSet chunksToSave;

    @Shadow protected abstract @Nullable ChunkHolder getChunkHolder(long pos);

    @Unique
    private static final int c2me$maxSearchPerCall = 256;

    @Unique
    private static final int c2me$maxConcurrentSaving = 256;

    @Unique
    @Override
    public boolean c2me$runOneChunkAutoSave() {
        if (!this.mainThreadExecutor.isOnThread()) {
            throw new ConcurrentModificationException("runOneChunkAutoSave called async");
        }

        if (this.world.isSavingDisabled()) {
            return false;
        }

        LongIterator iterator = this.chunksToSave.iterator();
        long measuringTimeMs = Util.getMeasuringTimeMs();
        int i = 0;
        while (iterator.hasNext() && (i ++) < c2me$maxSearchPerCall && this.chunksBeingSavedCount.get() < c2me$maxConcurrentSaving) {
            final long pos = iterator.nextLong();
            final ChunkHolder chunkHolder = this.currentChunkHolders.get(pos);
            if (chunkHolder == null) continue;
            if (this.save(chunkHolder, measuringTimeMs)) {
                return true;
            }
        }

        return false;
    }
}
