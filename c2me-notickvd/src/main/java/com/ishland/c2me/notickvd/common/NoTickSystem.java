package com.ishland.c2me.notickvd.common;

import com.ishland.c2me.base.common.GlobalExecutors;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class NoTickSystem {

    private final PlayerNoTickLoader playerNoTickLoader;
    private final ConcurrentLinkedQueue<Runnable> pendingActionsOnScheduler = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Runnable> mainBeforeTicketTicks = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Runnable> mainAfterTicketTicks = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isTicking = new AtomicBoolean();
    final Executor executor = GlobalExecutors.asyncScheduler;

    public NoTickSystem(ServerChunkLoadingManager tacs) {
        this.playerNoTickLoader = new PlayerNoTickLoader(tacs, this);
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

    public void tick() {
        scheduleTick();
    }

    private void scheduleTick() {
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

                    this.playerNoTickLoader.tick();
                    if (!this.pendingActionsOnScheduler.isEmpty() || !tasks.isEmpty()) scheduleTick(); // run more tasks
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

    public long getPendingLoadsCount() {
        return this.playerNoTickLoader.getPendingLoadsCount();
    }

    public void close() {
        this.playerNoTickLoader.close();
        executor.execute(() -> {
            try {
                this.playerNoTickLoader.tick();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
