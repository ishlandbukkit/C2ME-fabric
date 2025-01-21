package com.ishland.c2me.notickvd.common;

import com.ishland.c2me.notickvd.common.iterators.ChunkIterator;
import com.ishland.c2me.notickvd.common.iterators.SpiralIterator;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.c2me.rewrites.chunksystem.common.ducks.IChunkSystemAccess;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemTicket;
import com.ishland.flowsched.scheduler.StatusAdvancingScheduler;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongFunction;

public class PlayerNoTickLoader {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ItemTicket.TicketType TICKET_TYPE = new ItemTicket.TicketType("c2me:notickvd");
    public static final ChunkTicketType<ChunkPos> VANILLA_TICKET_TYPE = ChunkTicketType.create("c2me_notickvd", Comparator.comparingLong(ChunkPos::toLong));

    private final ServerChunkLoadingManager tacs;
    private final NoTickSystem noTickSystem;
    private final Long2ReferenceLinkedOpenHashMap<ChunkIterator> iterators = new Long2ReferenceLinkedOpenHashMap<>();
    private final LongSet managedChunks = new LongLinkedOpenHashSet();
    private final LongFunction<ChunkIterator> createFunction = pos -> new SpiralIterator(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos), this.viewDistance);
    private final ReferenceArrayList<CompletableFuture<Void>> chunkLoadFutures = new ReferenceArrayList<>();
    private final AtomicBoolean closing = new AtomicBoolean(false);

    private int viewDistance = 12;
    private boolean dirtyManagedChunks = false;
    private boolean recreateIterators = false;
    private volatile long pendingLoadsCountSnapshot = 0L;

    public PlayerNoTickLoader(ServerChunkLoadingManager tacs, NoTickSystem noTickSystem) {
        this.tacs = tacs;
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
        this.recreateIterators = true;
    }

    public void tick() {
        if (this.closing.get()) {
            clearTickets();
            return;
        }

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

        this.tickFutures();
    }

    private void clearTickets() {
        LongIterator iterator = this.managedChunks.iterator();
        while (iterator.hasNext()) {
            long pos = iterator.nextLong();

            this.removeTicket0(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));

            iterator.remove();
        }
    }

    void tickFutures() {
        this.chunkLoadFutures.removeIf(CompletableFuture::isDone);

        if (this.closing.get()) return;
        while (this.chunkLoadFutures.size() < Config.maxConcurrentChunkLoads && this.addOneTicket());

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

    private boolean addOneTicket() {
        ObjectBidirectionalIterator<Long2ReferenceMap.Entry<ChunkIterator>> iteratorIterator = this.iterators.long2ReferenceEntrySet().fastIterator();
        while (iteratorIterator.hasNext()) {
            Long2ReferenceMap.Entry<ChunkIterator> entry = iteratorIterator.next();
            ChunkIterator iterator = entry.getValue();
            while (iterator.hasNext()) {
                ChunkPos pos = iterator.next();
                if (this.managedChunks.add(pos.toLong())) {
                    this.chunkLoadFutures.add(loadChunk(pos.x, pos.z));
                    this.iterators.getAndMoveToLast(entry.getLongKey());
                    return true;
                }
            }
        }

        return false;
    }

    private CompletableFuture<Void> loadChunk(int x, int z) {
        CompletableFuture<Void> future = this.loadChunk0(x, z);
        future.thenRunAsync(() -> {
            try {
                this.chunkLoadFutures.remove(future);
                this.tickFutures();
            } catch (Throwable t) {
                LOGGER.error("Error while loading chunk [{}, {}]", x, z, t);
            }
        }, this.noTickSystem.executor);
        return future;
    }

    private CompletableFuture<Void> loadChunk0(int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);
        ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder = ((IChunkSystemAccess) this.tacs).c2me$getTheChunkSystem().addTicket(
                pos,
                TICKET_TYPE,
                pos,
                NewChunkStatus.SERVER_ACCESSIBLE_CHUNK_SENDING,
                StatusAdvancingScheduler.NO_OP
        );
        this.noTickSystem.mainBeforeTicketTicks.add(() -> this.tacs.getTicketManager().addTicketWithLevel(VANILLA_TICKET_TYPE, pos, 33, pos));
        return holder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE);
    }

    private void removeTicket0(int x, int z) {
        ChunkPos pos = new ChunkPos(x, z);
        ((IChunkSystemAccess) this.tacs).c2me$getTheChunkSystem().removeTicket(
                pos,
                TICKET_TYPE,
                pos,
                NewChunkStatus.SERVER_ACCESSIBLE_CHUNK_SENDING
        );
        this.noTickSystem.mainBeforeTicketTicks.add(() -> this.tacs.getTicketManager().removeTicketWithLevel(VANILLA_TICKET_TYPE, pos, 33, pos));
    }

    public long getPendingLoadsCount() {
        return this.pendingLoadsCountSnapshot;
    }

    public void close() {
        this.closing.set(true);
    }
}
