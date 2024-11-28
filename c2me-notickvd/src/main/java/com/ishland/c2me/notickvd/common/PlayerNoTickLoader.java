package com.ishland.c2me.notickvd.common;

import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.notickvd.common.iterators.ChunkIterator;
import com.ishland.c2me.notickvd.common.iterators.SpiralIterator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.function.LongFunction;

public class PlayerNoTickLoader {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ChunkTicketType<Unit> TICKET_TYPE = ChunkTicketType.create("c2me_no_tick_vd", (a, b) -> 0);

    private final ChunkTicketManager ticketManager;
    private final NoTickSystem noTickSystem;
    private final Long2ReferenceLinkedOpenHashMap<ChunkIterator> iterators = new Long2ReferenceLinkedOpenHashMap<>();
    private final LongSet managedChunks = new LongLinkedOpenHashSet();
    private final LongFunction<ChunkIterator> createFunction = pos -> new SpiralIterator(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos), this.viewDistance);
    private final ReferenceArrayList<CompletableFuture<Void>> chunkLoadFutures = new ReferenceArrayList<>();

    private int viewDistance = 12;
    private boolean dirtyManagedChunks = false;
    private boolean recreateIterators = false;
    private volatile long pendingLoadsCountSnapshot = 0L;

    public PlayerNoTickLoader(ChunkTicketManager ticketManager, NoTickSystem noTickSystem) {
        this.ticketManager = ticketManager;
        this.noTickSystem = noTickSystem;
    }

    public void addSource(ChunkPos chunkPos) {
        this.iterators.computeIfAbsent(chunkPos.toLong(), this.createFunction);
    }

    public void removeSource(ChunkPos chunkPos) {
        this.iterators.remove(chunkPos.toLong());
        this.dirtyManagedChunks = true;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        this.dirtyManagedChunks = true;
        this.recreateIterators = true;
    }

    public void tick(ServerChunkLoadingManager tacs) {
        if (this.recreateIterators) {
            this.dirtyManagedChunks = true;
            ObjectBidirectionalIterator<Long2ReferenceMap.Entry<ChunkIterator>> iterator = this.iterators.long2ReferenceEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2ReferenceMap.Entry<ChunkIterator> entry = iterator.next();
                entry.setValue(this.createFunction.apply(entry.getLongKey()));
            }
            this.recreateIterators = false;
        }

        if (this.dirtyManagedChunks) {
            LongIterator chunkIterator = this.managedChunks.iterator();
            ObjectBidirectionalIterator<Long2ReferenceMap.Entry<ChunkIterator>> iteratorIterator = this.iterators.long2ReferenceEntrySet().fastIterator();
            while (chunkIterator.hasNext()) {
                long pos = chunkIterator.nextLong();
                int packedX = ChunkPos.getPackedX(pos);
                int packedZ = ChunkPos.getPackedZ(pos);

                boolean isUsed = false;
                if (iteratorIterator.hasNext()) {
                    while (iteratorIterator.hasNext()) {
                        Long2ReferenceMap.Entry<ChunkIterator> entry = iteratorIterator.next();
                        isUsed |= entry.getValue().isInRange(packedX, packedZ);
                    }
                } else if (iteratorIterator.hasPrevious()) {
                    while (iteratorIterator.hasPrevious()) {
                        Long2ReferenceMap.Entry<ChunkIterator> entry = iteratorIterator.previous();
                        isUsed |= entry.getValue().isInRange(packedX, packedZ);
                    }
                }

                if (!isUsed) {
                    this.removeTicket0(packedX, packedZ);
                    chunkIterator.remove();
                }
            }
            this.dirtyManagedChunks = false;
        }

        this.tickFutures(tacs);

        {
            long pendingLoadsCount = 0L;
            ObjectBidirectionalIterator<Long2ReferenceMap.Entry<ChunkIterator>> iterator = this.iterators.long2ReferenceEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2ReferenceMap.Entry<ChunkIterator> entry = iterator.next();
                pendingLoadsCount += entry.getValue().remaining();
            }
            this.pendingLoadsCountSnapshot = pendingLoadsCount;
        }
    }

    void tickFutures(ServerChunkLoadingManager tacs) {
        this.chunkLoadFutures.removeIf(CompletableFuture::isDone);

        while (this.chunkLoadFutures.size() < Config.maxConcurrentChunkLoads && this.addOneTicket(tacs));
    }

    private boolean addOneTicket(ServerChunkLoadingManager tacs) {
        ObjectBidirectionalIterator<Long2ReferenceMap.Entry<ChunkIterator>> iteratorIterator = this.iterators.long2ReferenceEntrySet().fastIterator();
        while (iteratorIterator.hasNext()) {
            Long2ReferenceMap.Entry<ChunkIterator> entry = iteratorIterator.next();
            ChunkIterator iterator = entry.getValue();
            while (iterator.hasNext()) {
                ChunkPos pos = iterator.next();
                if (this.managedChunks.add(pos.toLong())) {
                    this.chunkLoadFutures.add(loadChunk(tacs, pos.x, pos.z));
                    this.iterators.getAndMoveToLast(entry.getLongKey());
                    return true;
                }
            }
        }

        return false;
    }

    private CompletableFuture<Void> loadChunk(ServerChunkLoadingManager tacs, int x, int z) {
        CompletableFuture<Void> future = this.addTicket0(x, z)
                .thenComposeAsync(unused -> {
                    ChunkHolder holder = ((IThreadedAnvilChunkStorage) tacs).invokeGetChunkHolder(ChunkPos.toLong(x, z));
                    if (holder == null) {
                        LOGGER.warn("No holder created after adding ticket to chunk [{}, {}]", x, z);
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return holder.getAccessibleFuture()
                                .handle((worldChunkOptionalChunk, throwable) -> {
                                    if (throwable != null) {
                                        LOGGER.error("Failed to load chunk [{}, {}]", x, z);
                                    }
                                    return null;
                                });
                    }
                }, this.noTickSystem.mainAfterTicketTicks::add);
        future.thenRunAsync(() -> {
            try {
                this.chunkLoadFutures.remove(future);
                this.tickFutures(tacs);
            } catch (Throwable t) {
                LOGGER.error("Error while loading chunk [{}, {}]", x, z, t);
            }
        }, this.noTickSystem.executor);
        return future;
    }

    private CompletableFuture<Void> addTicket0(int x, int z) {
        return CompletableFuture.runAsync(() -> this.ticketManager.addTicketWithLevel(TICKET_TYPE, new ChunkPos(x, z), 33, Unit.INSTANCE), this.noTickSystem.mainBeforeTicketTicks::add);
    }

    private void removeTicket0(int x, int z) {
        this.noTickSystem.mainBeforeTicketTicks.add(() -> this.ticketManager.removeTicketWithLevel(TICKET_TYPE, new ChunkPos(x, z), 33, Unit.INSTANCE));
    }

    public long getPendingLoadsCount() {
        return this.pendingLoadsCountSnapshot;
    }
}
