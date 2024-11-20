package com.ishland.c2me.base.common.scheduler;

import com.ishland.c2me.base.common.GlobalExecutors;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.util.math.ChunkPos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

public class SchedulingManager {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static final int MAX_LEVEL = ChunkLevels.INACCESSIBLE + 1;
    private final ConcurrentMap<Long, FreeableTaskList> pos2Tasks = new ConcurrentHashMap<>();
    private final Long2IntOpenHashMap prioritiesFromLevel = new Long2IntOpenHashMap();
    private final StampedLock prioritiesLock = new StampedLock();
    private final int id = COUNTER.getAndIncrement();
    private volatile ChunkPos currentSyncLoad = null;

    private final Executor executor;

    {
        prioritiesFromLevel.defaultReturnValue(MAX_LEVEL);
    }

    public SchedulingManager(Executor executor) {
        this.executor = executor;
    }

    public void enqueue(AbstractPosAwarePrioritizedTask task) {
        retry:
        while (true) {
            final long pos = task.getPos();
            final FreeableTaskList locks = this.pos2Tasks.computeIfAbsent(pos, unused -> new FreeableTaskList());
            synchronized (locks) {
                if (locks.freed) continue retry;
                locks.add(task);
            }
            task.setPriority(this.getPriority(pos));
            task.addPostExec(() -> {
                final FreeableTaskList tasks = this.pos2Tasks.get(task.getPos());
                if (tasks != null) {
                    synchronized (tasks) {
                        if (tasks.freed) return;
                        tasks.remove(task);
                        if (tasks.isEmpty()) {
                            tasks.freed = true;
                        }
                    }
                    if (tasks.freed) {
                        this.pos2Tasks.remove(task.getPos());
                    }
                }
            });
            GlobalExecutors.prioritizedScheduler.schedule(task);
            return;
        }
    }

    public void enqueue(long pos, Runnable command) {
        this.enqueue(new WrappingTask(pos, command));
    }

    public Executor positionedExecutor(long pos) {
        return command -> this.enqueue(pos, command);
    }

    public void updatePriorityFromLevel(long pos, int level) {
        this.executor.execute(() -> {
            if (this.getPriorityFromMap(pos) == level) return;
            final long stamp = this.prioritiesLock.writeLock();
            try {
                if (level < MAX_LEVEL) {
                    this.prioritiesFromLevel.put(pos, level);
                } else {
                    this.prioritiesFromLevel.remove(pos);
                }
            } finally {
                this.prioritiesLock.unlockWrite(stamp);
            }
            updatePriorityInternal(pos);
        });
    }

    private void updatePriorityInternal(long pos) {
        final int priority = getPriority(pos);
        final FreeableTaskList locks = this.pos2Tasks.get(pos);
        if (locks != null) {
            synchronized (locks) {
                if (locks.freed) return;
                for (AbstractPosAwarePrioritizedTask lock : locks) {
                    lock.setPriority(priority);
                    GlobalExecutors.prioritizedScheduler.notifyPriorityChange(lock);
                }
            }
        }
    }

    private int getPriority(long pos) {
        final int fromLevel = getPriorityFromMap(pos);
        int fromSyncLoad;
        ChunkPos currentSyncLoad1 = currentSyncLoad;
        if (currentSyncLoad1 != null) {
            final int chebyshevDistance = chebyshev(new ChunkPos(pos), currentSyncLoad1);
            if (chebyshevDistance <= 8) {
                fromSyncLoad = chebyshevDistance;
//                System.out.println("dist for chunk [%d,%d] is %d".formatted(currentSyncLoad.x, currentSyncLoad.z, chebyshevDistance));
            } else {
                fromSyncLoad = MAX_LEVEL;
            }
        } else {
            fromSyncLoad = MAX_LEVEL;
        }
        return Math.min(fromLevel, fromSyncLoad);
    }

    private int getPriorityFromMap(long pos) {
        int fromLevel = MAX_LEVEL;
        long stamp = this.prioritiesLock.tryOptimisticRead();
        try {
            fromLevel = this.prioritiesFromLevel.get(pos);
        } catch (Throwable t) {
        }
        if (!this.prioritiesLock.validate(stamp)) {
            stamp = this.prioritiesLock.readLock();
            try {
                fromLevel = this.prioritiesFromLevel.get(pos);
            } finally {
                this.prioritiesLock.unlockRead(stamp);
            }
        }
        return fromLevel;
    }

    public void setCurrentSyncLoad(ChunkPos pos) {
        executor.execute(() -> {
            if (this.currentSyncLoad != null) {
                final ChunkPos lastSyncLoad = this.currentSyncLoad;
                this.currentSyncLoad = null;
                updateSyncLoadInternal(lastSyncLoad);
            }
            if (pos != null) {
                this.currentSyncLoad = pos;
                updateSyncLoadInternal(pos);
            }
        });
    }

    public int getId() {
        return this.id;
    }

    private void updateSyncLoadInternal(ChunkPos pos) {
        long startTime = System.nanoTime();
        for (int xOff = -8; xOff <= 8; xOff++) {
            for (int zOff = -8; zOff <= 8; zOff++) {
                updatePriorityInternal(ChunkPos.toLong(pos.x + xOff, pos.z + zOff));
            }
        }
        long endTime = System.nanoTime();
    }

    private static int chebyshev(ChunkPos a, ChunkPos b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.z - b.z));
    }

    private static int chebyshev(long a, long b) {
        return Math.max(Math.abs(ChunkPos.getPackedX(a) - ChunkPos.getPackedX(b)), Math.abs(ChunkPos.getPackedZ(a) - ChunkPos.getPackedZ(b)));
    }

    private static class FreeableTaskList extends ObjectArraySet<AbstractPosAwarePrioritizedTask> {

        private boolean freed = false;

    }

}
