package com.ishland.c2me.opts.scheduling.mixin.idle_tasks.autosave.enhanced_autosave;

import com.ishland.c2me.opts.scheduling.common.idle_tasks.IThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    @Unique
    private static final int c2me$maxSearchPerCall = 256;

    @Unique
    private static final int c2me$maxConcurrentSaving = 256;

    @Unique
    private ObjectBidirectionalIterator<Long2ObjectMap.Entry<ChunkHolder>> c2me$saveChunksIterator;
    @Unique
    private int c2me$saveChunksIteratorHash = 0;

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        if (this.c2me$saveChunksIterator == null || !this.c2me$saveChunksIterator.hasNext()) {
            c2me$enhancedAutoSaveUpdateIterator();
        }
    }

    @Unique
    @Override
    public boolean c2me$runOneChunkAutoSave() {
        if (!this.mainThreadExecutor.isOnThread()) {
            throw new ConcurrentModificationException("runOneChunkAutoSave called async");
        }

        if (this.world.isSavingDisabled()) {
            return false;
        }

        if (this.c2me$saveChunksIteratorHash != System.identityHashCode(this.chunkHolders) || this.c2me$saveChunksIterator == null) {
            c2me$enhancedAutoSaveUpdateIterator();
        }

        long measuringTimeMs = Util.getMeasuringTimeMs();
        int i = 0;
        final ObjectBidirectionalIterator<Long2ObjectMap.Entry<ChunkHolder>> iterator = this.c2me$saveChunksIterator;
        while (iterator.hasNext() && (i ++) < c2me$maxSearchPerCall && this.chunksBeingSavedCount.get() < c2me$maxConcurrentSaving) {
            final Long2ObjectMap.Entry<ChunkHolder> entry = iterator.next();
            final long pos = entry.getLongKey();
            final ChunkHolder chunkHolder = entry.getValue();
            if (chunkHolder == null) continue;
            if (!this.currentChunkHolders.containsKey(pos)) continue;
            if (this.save(chunkHolder, measuringTimeMs)) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private void c2me$enhancedAutoSaveUpdateIterator() {
        final Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkHolders1 = this.chunkHolders;
        final int identityHashCode = System.identityHashCode(chunkHolders1);
        this.c2me$saveChunksIterator = chunkHolders1.long2ObjectEntrySet().iterator();
        this.c2me$saveChunksIteratorHash = identityHashCode;
    }
}
