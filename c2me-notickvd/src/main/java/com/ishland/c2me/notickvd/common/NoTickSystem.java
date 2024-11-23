package com.ishland.c2me.notickvd.common;

import com.ishland.c2me.base.common.GlobalExecutors;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class NoTickSystem {

    private final ChunkTicketManager chunkTicketManager;

    private final PlayerNoTickLoader playerNoTickLoader;
    private final ConcurrentLinkedQueue<Runnable> pendingActionsOnScheduler = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Runnable> mainBeforeTicketTicks = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Runnable> mainAfterTicketTicks = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isTicking = new AtomicBoolean();
    final Executor executor = GlobalExecutors.asyncScheduler;
    private volatile boolean pendingPurge = false;
    private volatile long age = 0;

    public NoTickSystem(ChunkTicketManager chunkTicketManager) {
        this.chunkTicketManager = chunkTicketManager;
        this.playerNoTickLoader = new PlayerNoTickLoader(chunkTicketManager, this);
    }

    public void addPlayerSource(ChunkPos chunkPos) {
        this.pendingActionsOnScheduler.add(() -> this.playerNoTickLoader.addSource(chunkPos));
    }

    public void removePlayerSource(ChunkPos chunkPos) {
        this.pendingActionsOnScheduler.add(() -> this.playerNoTickLoader.removeSource(chunkPos));
    }

    public void setNoTickViewDistance(int viewDistance) {
        this.pendingActionsOnScheduler.add(() -> this.playerNoTickLoader.setViewDistance(viewDistance));
    }

    public void beforeTicketTicks() {
        drainQueue(this.mainBeforeTicketTicks);
    }

    public void afterTicketTicks() {
        drainQueue(this.mainAfterTicketTicks);
    }

    public void tick(ServerChunkLoadingManager tacs) {
        scheduleTick(tacs);
    }

    private void scheduleTick(ServerChunkLoadingManager tacs) {
        if (!this.pendingActionsOnScheduler.isEmpty() && this.isTicking.compareAndSet(false, true)) {
            List<Runnable> tasks = new ArrayList<>(this.pendingActionsOnScheduler.size() + 3);
            {
                Runnable r;
                while ((r = this.pendingActionsOnScheduler.poll()) != null) {
                    tasks.add(r);
                }
            }
            executor.execute(() -> {
                try {
                    for (Runnable task : tasks) {
                        try {
                            task.run();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }

                    this.playerNoTickLoader.tick(tacs);
                    if (!this.pendingActionsOnScheduler.isEmpty() || !tasks.isEmpty()) scheduleTick(tacs); // run more tasks
                } finally {
                    this.isTicking.set(false);
                }
            });
        }
    }

    private void drainQueue(ConcurrentLinkedQueue<Runnable> queue) {
        Runnable runnable;
        while ((runnable = queue.poll()) != null) {
            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void runPurge(long age) {
        this.age = age;
        this.pendingPurge = true;
    }

    public LongSet getNoTickOnlyChunksSnapshot() {
        return null;
    }

    public long getPendingLoadsCount() {
        return this.playerNoTickLoader.getPendingLoadsCount();
    }
}
