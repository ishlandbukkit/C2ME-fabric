package com.ishland.c2me.rewrites.chunksystem.common;

import com.ishland.c2me.base.common.GlobalExecutors;
import com.ishland.c2me.base.common.scheduler.IVanillaChunkManager;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.flowsched.scheduler.DaemonizedStatusAdvancingScheduler;
import com.ishland.flowsched.scheduler.ExceptionHandlingAction;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import com.ishland.flowsched.scheduler.KeyStatusPair;
import com.ishland.flowsched.util.Assertions;
import io.netty.util.internal.PlatformDependent;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.Util;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.ReportType;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class TheChunkSystem extends DaemonizedStatusAdvancingScheduler<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> {

    private final Logger LOGGER;

    private final Long2IntMap managedTickets = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final SchedulingManager schedulingManager;
    private final Executor backingBackgroundExecutor = GlobalExecutors.prioritizedScheduler.executor(15);
    private Queue<Runnable> backgroundTaskQueue = PlatformDependent.newSpscQueue();
    private final Executor backgroundExecutor = command -> {
        if (Thread.currentThread() != this.thread) {
            command.run();
        } else {
            backgroundTaskQueue.add(command);
        }
    };
    private final Scheduler backgroundScheduler = Schedulers.from(this.backgroundExecutor);
    private final ServerChunkLoadingManager tacs;

    public TheChunkSystem(ThreadFactory threadFactory, ServerChunkLoadingManager tacs) {
        super(threadFactory, TheSpeedyObjectFactory.INSTANCE);
        this.tacs = tacs;
        this.schedulingManager =  ((IVanillaChunkManager) tacs).c2me$getSchedulingManager();
        this.LOGGER = LoggerFactory.getLogger("Chunk System of %s".formatted(((IThreadedAnvilChunkStorage) tacs).getWorld().getRegistryKey().getValue()));
        managedTickets.defaultReturnValue(NewChunkStatus.vanillaLevelToStatus.length - 1);
        this.thread.start();
    }

    @Override
    protected Executor getBackgroundExecutor() {
        return this.backgroundExecutor;
    }

    @Override
    protected Scheduler getSchedulerBackedByBackgroundExecutor() {
        return this.backgroundScheduler;
    }

    @Override
    protected ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> getUnloadedStatus() {
        return NewChunkStatus.NEW;
    }

    @Override
    protected ChunkLoadingContext makeContext(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder, ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> nextStatus, KeyStatusPair<ChunkPos, ChunkState, ChunkLoadingContext>[] dependencies, boolean isUpgrade) {
        Assertions.assertTrue(nextStatus instanceof NewChunkStatus);
        final NewChunkStatus nextStatus1 = (NewChunkStatus) nextStatus;

        int radius;
        if (dependencies.length == 0) {
            radius = 0;
        } else {
            int actualDependencies = dependencies.length + 1;
            radius = (int) ((Math.sqrt(actualDependencies) - 1) / 2);
            Assertions.assertTrue((radius * 2 + 1) * (radius * 2 + 1) == actualDependencies);
        }

        return new ChunkLoadingContext(holder, this.tacs, this.schedulingManager, BoundedRegionArray.create(holder.getKey().x, holder.getKey().z, radius,
                (x, z) -> this.getHolder(new ChunkPos(x, z)).getUserData().get()), dependencies);
    }

    @Override
    protected ExceptionHandlingAction handleTransactionException(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder, ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> nextStatus, boolean isUpgrade, Throwable throwable) {
        if (isUpgrade) {
            LOGGER.error("Error upgrading chunk {} to \"{}\"", holder.getKey(), nextStatus, throwable);
        } else {
            LOGGER.error("Error downgrading chunk {} to \"{}\"", holder.getKey(), nextStatus, throwable);
        }
        if (throwable instanceof CrashException crashException) {
            final MinecraftServer server = ((IThreadedAnvilChunkStorage) this.tacs).getWorld().getServer();
            server.execute(() -> {
                final Path path = server.getRunDirectory().resolve("crash-reports").resolve("crash-" + Util.getFormattedCurrentTime() + "-server.txt");
                crashException.getReport().writeToFile(path, ReportType.MINECRAFT_CHUNK_IO_ERROR_REPORT);
            });
        }
        return ExceptionHandlingAction.MARK_BROKEN;
    }

    @Override
    protected void onItemCreation(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder) {
        super.onItemCreation(holder);
        holder.getUserData().set(new NewChunkHolderVanillaInterface(this, holder, ((IThreadedAnvilChunkStorage) this.tacs).getWorld(), ((IThreadedAnvilChunkStorage) this.tacs).getLightingProvider(), this.tacs));
    }

    @Override
    protected void onItemRemoval(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder) {
        super.onItemRemoval(holder);
    }

    @Override
    protected void onItemUpgrade(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder, ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> statusReached) {
        super.onItemUpgrade(holder, statusReached);
        final NewChunkStatus statusReached1 = (NewChunkStatus) statusReached;
        final NewChunkStatus prevStatus = (NewChunkStatus) statusReached.getPrev();
        final WorldGenerationProgressListener listener = ((IThreadedAnvilChunkStorage) this.tacs).getWorldGenerationProgressListener();
        if (listener != null && prevStatus.getEffectiveVanillaStatus() != statusReached1.getEffectiveVanillaStatus()) {
            listener.setChunkStatus(holder.getKey(), statusReached1.getEffectiveVanillaStatus());
        }
        if (prevStatus.toChunkLevelType() != statusReached1.toChunkLevelType()) {
            ((IThreadedAnvilChunkStorage) this.tacs).getMainThreadExecutor().execute(
                    () -> ((IThreadedAnvilChunkStorage) this.tacs).invokeOnChunkStatusChange(holder.getKey(), statusReached1.toChunkLevelType()));
        }
    }

    @Override
    protected void onItemDowngrade(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder, ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> statusReached) {
        super.onItemDowngrade(holder, statusReached);
        final NewChunkStatus statusReached1 = (NewChunkStatus) statusReached;
        final NewChunkStatus prevStatus = (NewChunkStatus) statusReached.getNext();
        final WorldGenerationProgressListener listener = ((IThreadedAnvilChunkStorage) this.tacs).getWorldGenerationProgressListener();
        if (listener != null && prevStatus.getEffectiveVanillaStatus() != statusReached1.getEffectiveVanillaStatus()) {
            listener.setChunkStatus(holder.getKey(), statusReached1.getEffectiveVanillaStatus());
        }
        if (prevStatus.toChunkLevelType() != statusReached1.toChunkLevelType()) {
            ((IThreadedAnvilChunkStorage) this.tacs).getMainThreadExecutor().execute(
                    () -> ((IThreadedAnvilChunkStorage) this.tacs).invokeOnChunkStatusChange(holder.getKey(), statusReached1.toChunkLevelType()));
        }
    }

    public ChunkHolder vanillaIf$setLevel(long pos, int level) {
        Assertions.assertTrue(!Thread.holdsLock(this.managedTickets));
        synchronized (this.managedTickets) {
            final int oldLevel = this.managedTickets.put(pos, level);
            NewChunkStatus oldStatus = c2me$getDeferredStatusFromVanillaLevel(oldLevel);
            NewChunkStatus newStatus = c2me$getDeferredStatusFromVanillaLevel(level);
            final ChunkPos key = new ChunkPos(pos);
            if (oldStatus != newStatus) {
                NewChunkHolderVanillaInterface vanillaHolder;
                ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder;
                boolean shouldReturnVanillaHolder;
                if (newStatus != this.getUnloadedStatus()) {
                    holder = this.addTicket(key, TicketTypeExtension.VANILLA_LEVEL, key, newStatus, NO_OP);
                    shouldReturnVanillaHolder = true;
                } else {
                    this.managedTickets.remove(pos);
                    holder = this.getHolder(key);
                    shouldReturnVanillaHolder = false;
                }
                Assertions.assertTrue(holder != null, "Holder should be managed by the vanilla interface");
                assert holder != null;
                vanillaHolder = holder.getUserData().get();
                if (Config.lowMemoryMode) {
                    vanillaHolder.updateDeferredStatus(NewChunkStatus.fromVanillaLevel(level));
                }

                if (oldStatus != this.getUnloadedStatus()) {
                    this.removeTicket(key, TicketTypeExtension.VANILLA_LEVEL, key, oldStatus);
                }
                return shouldReturnVanillaHolder ? vanillaHolder : null;
            } else {
                final ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder = this.getHolder(key);
                NewChunkHolderVanillaInterface vanillaHolder;
                if (holder != null) {
                    vanillaHolder = holder.getUserData().get();

                } else {
                    vanillaHolder = null;
                }
                if (Config.lowMemoryMode && vanillaHolder != null) {
                    vanillaHolder.updateDeferredStatus(NewChunkStatus.fromVanillaLevel(level));
                }
                if (newStatus != this.getUnloadedStatus() && vanillaHolder != null) {
                    return vanillaHolder;
                }
                return null;
            }
        }
    }

    private static NewChunkStatus c2me$getDeferredStatusFromVanillaLevel(int level) {
        NewChunkStatus status = NewChunkStatus.fromVanillaLevel(level);
        if (Config.lowMemoryMode) {
            if (status == NewChunkStatus.NEW) {
                return status;
            } else if (status.ordinal() < NewChunkStatus.SERVER_ACCESSIBLE.ordinal()) {
                return NewChunkStatus.DEFERRED;
            } else {
                return status;
            }
        } else {
            return status;
        }
    }

    public int vanillaIf$getManagedLevel(long pos) {
        return this.managedTickets.get(pos);
    }

    @Override
    public boolean tick() {
        boolean tick = super.tick();
        if (!this.backgroundTaskQueue.isEmpty()) {
            Queue<Runnable> queue = this.backgroundTaskQueue;
            this.backgroundTaskQueue = PlatformDependent.newSpscQueue();
            this.backingBackgroundExecutor.execute(() -> {
                Runnable runnable;
                while ((runnable = queue.poll()) != null) {
                    try {
                        runnable.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
        return tick;
    }
}
